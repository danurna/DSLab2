package proxy;

import message.Request;
import message.Response;
import message.request.*;
import message.response.*;
import model.DownloadTicket;
import model.FileserverEntity;
import model.UserEntity;
import server.FileserverRequest;
import util.ChecksumUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.util.HashSet;
import java.util.Set;

/**
 * For each Client our proxy handles, it creates a new ClientProxyBridge
 * to handle requests and keep the connection alive.
 */
public class ClientProxyBridge implements IProxy, Runnable {
    private Socket clientSocket;
    private UserEntity currentUser;
    private MyProxy myProxy;
    private String keysDirectoryPath;
    private PrivateKey privateKey;
    private TCPChannel channel;

    public ClientProxyBridge(Socket clientSocket, MyProxy myProxy) {
        this.clientSocket = clientSocket;
        this.myProxy = myProxy;
        this.currentUser = null;
    }

    @Override
    public LoginResponse login(LoginRequest request) throws IOException {
        UserEntity validUser = myProxy.login(request);
        if (validUser == null) {
            return new LoginResponse(LoginResponse.Type.WRONG_CREDENTIALS);
        }

        if (myProxy.isUserLoggedIn(validUser)) {
            return null;
        }

        //Set currentUser to authenticated user.
        currentUser = validUser;
        currentUser.setOnline(true);
        return new LoginResponse(LoginResponse.Type.SUCCESS);
    }

    @Override
    public Response credits() throws IOException {
        if (currentUser == null) { //User authenticated?
            return null;
        }

        return new CreditsResponse(currentUser.getCredits());
    }

    @Override
    public Response buy(BuyRequest credits) throws IOException {
        if (currentUser == null) { //User authenticated?
            return null;
        }

        currentUser.increaseCredits(credits.getCredits());
        return new BuyResponse(currentUser.getCredits());
    }

    @Override
    public Response list() throws IOException {
        if (currentUser == null) { //User authenticated?
            return null;
        }

        Set<String> files = new HashSet<String>();

        for (FileserverEntity entity : myProxy.getFileserverList()) {
            try {
                ListResponse listResponse = (ListResponse) performFileserverRequest(new ListRequest(), entity);
                if (listResponse != null) //valid response needed.
                    files.addAll(listResponse.getFileNames());
            } catch (IOException e) {
                System.out.println("Connection to one fileserver not successful!");
            }

        }

        return new ListResponse(files);
    }

    @Override
    public Response download(DownloadTicketRequest request) throws IOException {
        if (currentUser == null) { //User authenticated?
            return null;
        }
        String filename = request.getFilename();
        FileserverRequest fileserverRequest = myProxy.getLeastUsedFileserverForFile(filename,myProxy.getReadQuorum() , this);
        
        if (fileserverRequest == null) { //no fs available
            return new MessageResponse("No fileservers available for requested file.");
        }
        
        if (fileserverRequest.getFileserverEntity() == null) { //no fs with file
            return fileserverRequest.getResponse();
        }

        //Use data of fs request.
        InfoResponse infoResponse = (InfoResponse) fileserverRequest.getResponse();
        FileserverEntity fs = fileserverRequest.getFileserverEntity();

        //Enough credits?
        if (infoResponse.getSize() <= currentUser.getCredits()) {
            currentUser.decreaseCredits(infoResponse.getSize());
            fs.increaseUsage(infoResponse.getSize());
            String checksum = ChecksumUtils.generateChecksum(currentUser.getName(), filename, 0, infoResponse.getSize());
            myProxy.registerDownload(filename);
            return new DownloadTicketResponse(new DownloadTicket(currentUser.getName(), request.getFilename(), checksum, fs.getAddress(), fs.getPort()));
        }

        return new MessageResponse("Not enough credits.");
    }

    @Override
    public MessageResponse upload(UploadRequest request) throws IOException {
        if (currentUser == null) { //User authenticated?
            return null;
        }

        boolean didUploadAtLeastOnce = false;
        //Upload file to each fileserver online.

        for (FileserverEntity entity : myProxy.getWriteQuorum()) {
            if (entity.isOnline()) { //FS has to be online.
                try {
                    MessageResponse messageResponse = (MessageResponse) performFileserverRequest(request, entity);
                    System.out.println(messageResponse);
                    didUploadAtLeastOnce = true;
                } catch (SocketException e) { //fileserver socket could be closed.
                    System.out.println("Connection to one fileserver not successful!");
                }
            }
        }

        if (!didUploadAtLeastOnce) {
            return new MessageResponse("Uploading failed because there is no fileserver available. Please try it again later.");
        }

        //Increase credits of user for upload.
        currentUser.increaseCredits(request.getContent().length * 2);
        return new MessageResponse("Uploaded files successful!");
    }

    @Override
    public MessageResponse logout() throws IOException {
        if (currentUser == null) { //User authenticated?
            return null;
        }
        boolean loggedIn = false;

        //Update user
        currentUser.setOnline(loggedIn);
        
        myProxy.getProxyManagementComponent().userLoggedOut(currentUser.getName());

        //Log user out.
        currentUser = null;

        return new MessageResponse(loggedIn ? "Logout failed" : "Successfully logged out.");
    }

    @Override
    public void run() {
        channel = new TCPChannel();
        //Use proxy's private key and give path to keys directory, because
        //depending on client's request we use different public keys.
        channel = new RSAChannelEncryption(channel, privateKey, keysDirectoryPath, false);

        try {
            channel.setStreamsForSocket(clientSocket);

            Object obj;
            while ((obj = channel.readObject()) != null && !Thread.currentThread().isInterrupted()) {
                //System.out.println("PROXY READ AN OBJECT: " + obj);
                Response response = performRequest(obj);

                if (response != null){
                    //System.out.println("PROXY WROTE AN OBJECT: " + response);
                    channel.writeObject(response);
                }

                if( currentUser == null){
                    //If Authentication/Encryption is in use, we need to tell the channel
                    //to reset.
                    if(channel instanceof RSAChannelEncryption){
                        ((RSAChannelEncryption) channel).reset();
                    }
                }

            }

        } catch (EOFException e) {
            //Reached EOF. Nothing unusual.
        } catch (SocketException e) {
            System.out.println("Socket to client closed!");
        } catch (IOException e) {
            //Will occur in case of failed authentication. No problem.
        } catch (ClassNotFoundException e) {
           //Won't occur.
        } finally {
            //Cleanup
            try {
                System.out.println("Closing connection on proxy side!");
                channel.close();
                clientSocket.close();
            } catch (IOException e) {
                //Maybe it is already closed.
            }
        }
    }

    /**
     * Checks instance of obj and performs according action.
     *
     * @param obj
     * @return
     * @throws IOException
     */
    private Response performRequest(Object obj) throws IOException {
        Response response = null;

        if (obj instanceof LoginRequest) {
            response = login((LoginRequest) obj);
            if (response == null) {
                response = new MessageResponse("User already in use.");
            }
        } else if (obj instanceof BuyRequest) {
            response = buy((BuyRequest) obj);
        } else if (obj instanceof CreditsRequest) {
            response = credits();
        } else if (obj instanceof DownloadTicketRequest) {
            response = download((DownloadTicketRequest) obj);
        } else if (obj instanceof ListRequest) {
            response = list();
        } else if (obj instanceof LogoutRequest) {
            response = logout();
        } else if (obj instanceof UploadRequest) {
            response = upload((UploadRequest) obj);
        }

        if (response == null) {
            //Default response if not logged in, is null.
            response = new MessageResponse("Please log in.");
        }

        return response;
    }

    //Handles Socket exception from private method and provides functionality to external classes.
    public Response performFileserverRequestWrapper(Request request, FileserverEntity entity) {
        Response response = null;
        try {
            response = this.performFileserverRequest(request, entity);
        } catch (IOException e) {
            System.out.println("Connection to one fileserver not successful.");
        }

        return response;
    }

    //Establishs connection to fileserver and performs given request.
    private Response performFileserverRequest(Request request, FileserverEntity entity) throws IOException {
        ObjectOutputStream objectOut = null;
        ObjectInputStream objectIn = null;
        Object obj = null;

        try {
            Socket socket = new Socket(entity.getAddress(), entity.getPort());
            objectOut = new ObjectOutputStream(socket.getOutputStream());
            objectIn = new ObjectInputStream(socket.getInputStream());

            objectOut.writeObject(request);
            obj = objectIn.readObject();

            objectOut.close();
            objectIn.close();
            socket.close();
        } catch (EOFException e) {
            //Reached EOF. Nothing unusual.
        } catch (ClassNotFoundException e) {
            //Shouldn't occur.
            e.printStackTrace();
        }

        if (obj != null && obj instanceof Response) {
            return (Response) obj;
        }

        return null;
    }


    public void setPrivateKey(PrivateKey privateKey){
        this.privateKey = privateKey;
    }

    public void setKeysDirectoryPath(String keysDirectoryPath) {
        this.keysDirectoryPath = keysDirectoryPath;
    }
}
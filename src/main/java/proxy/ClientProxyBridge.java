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
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 17.10.13
 * Time: 12:55
 */
public class ClientProxyBridge implements IProxy, Runnable {
    private Socket clientSocket;
    private UserEntity currentUser;
    private MyProxy myProxy;

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
        FileserverRequest fileserverRequest = myProxy.getLeastUsedFileserverForFile(filename, this);

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
        for (FileserverEntity entity : myProxy.getFileserverList()) {
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

        return new MessageResponse(loggedIn ? "Logout failed" : "Successfully logged out.");
    }

    @Override
    public void run() {
        ObjectOutputStream objectOut = null;
        ObjectInputStream objectIn = null;

        try {
            objectOut = new ObjectOutputStream(clientSocket.getOutputStream());
            objectIn = new ObjectInputStream(clientSocket.getInputStream());

            Object obj;
            while ((obj = objectIn.readObject()) != null && !Thread.currentThread().isInterrupted()) {
                Response response = performRequest(obj);

                if (response == null)
                    return;

                objectOut.writeObject(response);
                objectOut.flush();
            }

        } catch (EOFException e) {
            //Reached EOF
        } catch (SocketException e) {
            System.out.println("Socket to client closed!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            //Cleanup
            try {
                objectIn.close();
                objectOut.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
            //Reached EOF
        } catch (ClassNotFoundException e) {
            //Shouldn't occur.
            e.printStackTrace();
        }

        if (obj != null && obj instanceof Response) {
            return (Response) obj;
        }

        return null;
    }


}
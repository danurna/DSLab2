package proxy;

import message.Request;
import message.Response;
import message.request.*;
import message.response.*;
import model.UserEntity;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

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

    public ClientProxyBridge(Socket clientSocket, MyProxy myProxy){
        this.clientSocket = clientSocket;
        this.myProxy = myProxy;
        this.currentUser = null;
    }

    @Override
    public LoginResponse login(LoginRequest request) throws IOException {
        UserEntity validUser = myProxy.login(request);
        if(validUser == null){
            return new LoginResponse(LoginResponse.Type.WRONG_CREDENTIALS);
        }

        //Set currentUser to authenticated user.
        currentUser = validUser;
        currentUser.setOnline(true);
        return new LoginResponse(LoginResponse.Type.SUCCESS);
    }

    @Override
    public Response credits() throws IOException {
        if(currentUser == null){
            return null;
        }

        return new CreditsResponse(currentUser.getCredits());
    }

    @Override
    public Response buy(BuyRequest credits) throws IOException {
        if(currentUser == null){
            return null;
        }

        currentUser.increaseCredits(credits.getCredits());
        return new BuyResponse(currentUser.getCredits());
    }

    @Override
    public Response list() throws IOException {
        if(currentUser == null){
            return null;
        }

        //TODO: implement fileserver first
        return null;
    }

    @Override
    public Response download(DownloadTicketRequest request) throws IOException {
        if(currentUser == null){
            return null;
        }
        //TODO: implement fileserver first
        return null;
    }

    @Override
    public MessageResponse upload(UploadRequest request) throws IOException {
        if(currentUser == null){
            return null;
        }
        //TODO: implement fileserver first
        return null;
    }

    @Override
    public MessageResponse logout() throws IOException {
        if(currentUser == null){
            return null;
        }

        boolean loggedIn = true;

        try{
            clientSocket.close();
            //On successful close, set loggedIn to false.
            loggedIn = false;
        }catch(IOException e){
            e.printStackTrace();
        }

        //Update user
        currentUser.setOnline(loggedIn);

        return new MessageResponse(loggedIn ? "Logout failed" : "Successful logout!" );
    }

    @Override
    public void run() {
        ObjectOutputStream objectOut = null;
        ObjectInputStream objectIn = null;

        try {
            objectOut = new ObjectOutputStream(clientSocket.getOutputStream());
            objectIn = new ObjectInputStream(clientSocket.getInputStream());

            Object obj;
            while( (obj = objectIn.readObject()) != null ){
                System.out.println("Client sent: " + obj);
                Response response = performRequest(obj);

                if(response == null)
                    return;

                objectOut.writeObject(response);
                objectOut.flush();
            }

        } catch (EOFException e){
            System.out.println("Reached EOF");
        } catch (SocketException e){
            System.out.println("Socket closed!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks instance of obj and performs according action.
     * @param obj
     * @return
     * @throws IOException
     */
    private Response performRequest(Object obj) throws IOException {
        Response response = null;

        if(obj instanceof LoginRequest){
            response = login((LoginRequest) obj);
        }else if(obj instanceof BuyRequest){
            response = buy((BuyRequest) obj);
        }else if(obj instanceof CreditsRequest){
            response = credits();
        }else if(obj instanceof DownloadTicketRequest){
            response = download((DownloadTicketRequest) obj);
        }else if(obj instanceof ListRequest){
            response = list();
        }else if(obj instanceof LogoutRequest){
            response = logout();
        }else if(obj instanceof UploadRequest){
            response = upload((UploadRequest) obj);
        }

        if(response == null){
            response = new MessageResponse("Please log in.");
        }

        return response;
    }

}
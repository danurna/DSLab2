package client;

import cli.Command;
import message.Response;
import message.request.*;
import message.response.DownloadTicketResponse;
import message.response.LoginResponse;
import message.response.MessageResponse;
import util.MyUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 09.10.13
 * Time: 18:42
 */
public class MyClientCli implements IClientCli {
    private MyClient client;
    boolean loggedIn;

    public MyClientCli(MyClient client) {
        this.client = client;
        loggedIn = false;
    }

    @Override
    @Command
    public LoginResponse login(String username, String password) throws IOException {
        if (loggedIn) {
            System.out.println("Already logged in.");
            return null;
        }

        Response response = client.sendRequest(new LoginRequest(username, password));

        //Could be message response if user is already logged in with other client
        if (!(response instanceof LoginResponse)) {
            System.out.println(response.toString());
            return null;
        }

        LoginResponse loginResponse = (LoginResponse) response;

        if (loginResponse.getType() == LoginResponse.Type.SUCCESS)
            loggedIn = true;

        return loginResponse;
    }

    @Override
    @Command
    public Response credits() throws IOException {
        return client.sendRequest(new CreditsRequest());
    }

    @Override
    @Command
    public Response buy(long credits) throws IOException {
        return client.sendRequest(new BuyRequest(credits));
    }

    @Override
    @Command
    public Response list() throws IOException {
        return client.sendRequest(new ListRequest());
    }

    @Override
    @Command
    public Response download(String filename) throws IOException {
        Response response = client.sendRequest(new DownloadTicketRequest(filename));
        if (response instanceof DownloadTicketResponse) {
            return client.downloadFile((DownloadTicketResponse) response);
        }

        return response;
    }

    @Override
    @Command
    public MessageResponse upload(String filename) throws IOException {
        File file = client.readFile(filename);
        if (file == null) {
            return new MessageResponse("File does not exist.");
        }

        int version = client.getVersionForFile(filename);
        Response response = client.sendRequest(new UploadRequest(filename, version, MyUtils.convertFileToByteArray(file)));

        if (response instanceof MessageResponse) {
            return (MessageResponse) response;
        }

        return new MessageResponse("Upload failed.");
    }

    @Override
    @Command
    public MessageResponse logout() throws IOException {
        MessageResponse response = (MessageResponse) client.sendRequest(new LogoutRequest());
        if (response != null && response.toString().equals("Successfully logged out."))
            loggedIn = false;

        return response;
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
        logout();
        client.closeConnection();
        System.in.close();
        return new MessageResponse("Bye!");
    }
}

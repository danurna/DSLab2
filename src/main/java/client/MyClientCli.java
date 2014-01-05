package client;

import cli.Command;
import message.Response;
import message.request.*;
import message.response.DownloadTicketResponse;
import message.response.LoginResponse;
import message.response.MessageResponse;
import model.FileserverEntity;
import util.MyUtils;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * Implementation of the Client ClI Interface.
 * Keeps track if logged in or not and also if client is connected to proxy.
 * Usually builds the requests and sends them via our real client (MyClient).
 */
public class MyClientCli implements IClientCli {
    private MyClient client;
    boolean loggedIn;
    
    private Thread shellThread;
    
    private DownloadSubscriptionCallback dlsCallback;

    public MyClientCli(MyClient client, Thread shellThread) {
        this.client = client;
        loggedIn = false;
        this.shellThread = shellThread;
    }

    @Override
    @Command
    public LoginResponse login(String username, String password) throws IOException {
        if (!isConnected()) return null;
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
        if (!isConnected()) return null;
        return client.sendRequest(new CreditsRequest());
    }

    @Override
    @Command
    public Response buy(long credits) throws IOException {
        if (!isConnected()) return null;
        return client.sendRequest(new BuyRequest(credits));
    }

    @Override
    @Command
    public Response list() throws IOException {
        if (!isConnected()) return null;
        return client.sendRequest(new ListRequest());
    }

    @Override
    @Command
    public Response download(String filename) throws IOException {
        if (!isConnected()) return null;
        Response response = client.sendRequest(new DownloadTicketRequest(filename));
        if (response instanceof DownloadTicketResponse) {
            return client.downloadFile((DownloadTicketResponse) response);
        }

        return response;
    }

    @Override
    @Command
    public MessageResponse upload(String filename) throws IOException {
        if (!isConnected()) return null;
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
        if (!isConnected()) return null;
        MessageResponse response = (MessageResponse) client.sendRequest(new LogoutRequest());
        if (response != null && response.toString().equals("Successfully logged out."))
            loggedIn = false;

        return response;
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
    	shellThread.interrupt();
    	client.unexportUnicasts();
    	
        if (client.isConnected()) {
            logout();
            client.closeConnection();
        }

        System.in.close();
        return new MessageResponse("Bye!");
    }
    
    @Command
    public MessageResponse readQuorum() {
    	try {
	    	return new MessageResponse("Read-Quorum is set to "+client.getProxyRMI().getReadQuorumSize()+".");
		} catch (RemoteException e) {
			e.printStackTrace();
			return new MessageResponse("A connection error occured. Please try again later.");
		}
    	
    }
    
    @Command
    public MessageResponse writeQuorum() {
    	try {
	    	return new MessageResponse("Write-Quorum is set to "+client.getProxyRMI().getWriteQuorumSize()+".");
		} catch (RemoteException e) {
			e.printStackTrace();
			return new MessageResponse("A connection error occured. Please try again later.");
		}
    	
    }
    
    @Command
    public MessageResponse topThreeDownloads() {
    	try {
    		String out = "Top Three Downloads:";
    		String[] files = client.getProxyRMI().getTop3DownloadedFiles();
    		for (int i=0;i<3;i++) {
    			out+=files[i];
    		}
	    	return new MessageResponse(out);
		} catch (RemoteException e) {
			e.printStackTrace();
			return new MessageResponse("A connection error occured. Please try again later.");
		}
    }
    
    @Command
    public MessageResponse subscribeToFile(String fileName, int downloadLimit) {
    	try {
    		if (dlsCallback==null) {
    			dlsCallback = new DownloadSubscriptionCallback(client);
    		}
    		client.getProxyRMI().subscribeToFile(fileName, downloadLimit, dlsCallback);;
	    	return new MessageResponse("Subscription sucessfull.");
		} catch (RemoteException e) {
			e.printStackTrace();
			return new MessageResponse("A connection error occured. Please try again later.");
		}
    }

    //Private helper that tries to connect, if not already connected.
    private boolean isConnected() {
        if (client.isConnected()) {
            return true;
        } else {
            loggedIn = false;
            if (client.connectToProxy()) {
                return true;
            } else {
                System.out.println("Proxy not reachable. Please try it again later.");
                return false;
            }
        }
    }
}

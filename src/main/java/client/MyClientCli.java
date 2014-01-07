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
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.PublicKey;

/**
 * Implementation of the Client ClI Interface.
 * Keeps track if logged in or not and also if client is connected to proxy.
 * Usually builds the requests and sends them via our real client (MyClient).
 */
public class MyClientCli implements IClientCli {
    private MyClient client;
    boolean loggedIn;
    
    String userName="";
    
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

        if(response == null){
            System.out.println("Login request failed.");
            return null;
        }

        //Could be message response if user is already logged in with other client
        if (!(response instanceof LoginResponse)) {
            System.out.println(response.toString());
            return null;
        }

        LoginResponse loginResponse = (LoginResponse) response;

        if (loginResponse.getType() == LoginResponse.Type.SUCCESS)
            loggedIn = true;
        
        this.userName = username;
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

        client.userLoggedOut();
        userName = "";

        return response;
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
    	shellThread.interrupt();

        if(dlsCallback == null)
    	    UnicastRemoteObject.unexportObject(dlsCallback, true);
    	
        if (client.isConnected()) {
        	try {
        		logout();
        	} catch (java.net.SocketException e) {}
            client.closeConnection();
        }

        System.in.close();
        return new MessageResponse("Bye!");
    }
    
    @Command
    @Override
    public MessageResponse readQuorum() {
    	try {
	    	return new MessageResponse("Read-Quorum is set to "+client.getProxyRMI().getReadQuorumSize()+".");
		} catch (RemoteException e) {
			e.printStackTrace();
			return new MessageResponse("A connection error occured. Please try again later.");
		}
    	
    }
    
    @Command
    @Override
    public MessageResponse writeQuorum() {
    	try {
	    	return new MessageResponse("Write-Quorum is set to "+client.getProxyRMI().getWriteQuorumSize()+".");
		} catch (RemoteException e) {
			e.printStackTrace();
			return new MessageResponse("A connection error occured. Please try again later.");
		}
    	
    }
    
    @Command
    @Override
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
    @Override
    public MessageResponse subscribe(String fileName, int downloadLimit) {
    	try {
    		if (dlsCallback==null) {
    			dlsCallback = new DownloadSubscriptionCallback(client);
    			UnicastRemoteObject.exportObject(dlsCallback, 0);
    		}
    		String out = client.getProxyRMI().subscribeToFile(fileName, downloadLimit, (loggedIn ? userName : ""), dlsCallback);;
	    	return new MessageResponse(out);
		} catch (RemoteException e) {
			e.printStackTrace();
			return new MessageResponse("A connection error occured. Please try again later.");
		}
    }
    
    @Command
    @Override
    public MessageResponse getProxyPublicKey() {
    	try {
    		PublicKey key = client.getProxyRMI().getProxyPublicKey();
    		if (key==null) {
    			return new MessageResponse("Getting key failed. Please try again later.");
    		}
    		client.setProxyPublicKey(key);
	    	return new MessageResponse("Successfully received public key of Proxy.");
		} catch (RemoteException e) {
			e.printStackTrace();
			return new MessageResponse("A connection error occured. Please try again later.");
		}
    }
    
    @Command
    @Override
    public MessageResponse setUserPublicKey(String userName) {
    	try {
    		PublicKey key = client.getClientPublicKey(userName);
    		if (key==null) {
    			return new MessageResponse("Public key for user "+userName+" not found!");
    		}
    		boolean success = client.getProxyRMI().setClientPublicKey(userName, key);
    		if (!success) {
    			return new MessageResponse("Sending key failed. Please try again later.");
    		}
	    	return new MessageResponse("Successfully received public key of Proxy.");
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

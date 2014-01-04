package client;

import cli.Shell;
import message.Request;
import message.Response;
import message.request.DownloadFileRequest;
import message.response.DownloadFileResponse;
import message.response.DownloadTicketResponse;
import util.ComponentFactory;
import util.Config;
import util.MyUtils;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Set;

import proxy.IProxyRMI;

/**
 * My client class maintains connection to proxy
 * but also downloads and uploads data from fileserver.
 */
public class MyClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String proxyAddress;
    private int tcpPort;
    private String clDir;
    private HashMap<String, Integer> versionMap;
    private boolean connected;
    
	private String rmiBindingName;
	private int proxyRMIPort;
	private String proxyRMIAddress;
	private String keysDir;
	
	private Registry remoteRegistry;
	private IProxyRMI proxyRMI;
	private DownloadSubscriptionCallback dlsCallback;

    public MyClient(Config config, Config mcConfig) {
        if (!this.readConfigFile(config)) {
            //If reading config fails, we fail too.
            return;
        }
        try {
			if (!this.readMCConfigFile(mcConfig)) {
			    //If reading config fails, we fail too.
			    return;
			}
		} catch (RemoteException e) {
			return;
		} catch (NotBoundException e) {
			return;
		}
        
        connected = false;
        versionMap = new HashMap<String, Integer>();
        this.initVersionsMap();
    }

    public static void main(String[] args) {
        try {
            Shell shell = new Shell("client", System.out, System.in);
            new ComponentFactory().startClient(new Config("client"), shell);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Registry getRemoteRegistry() {
    	return remoteRegistry;
    }
    public IProxyRMI getProxyRMI() {
    	return proxyRMI;
    }
    
    //Setup connection to proxy and return whether it was successful or not.
    //Called on first request.
    public boolean connectToProxy() {
        try {
            createSockets();
            connected = true;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reads config values.
     *
     * @return true, if values are valid and existing. False, on resource not found or parse exception.
     */
    private boolean readConfigFile(Config config) {
        try {
            tcpPort = config.getInt("proxy.tcp.port");
            proxyAddress = config.getString("proxy.host");
            clDir = config.getString("download.dir");
        } catch (Exception e) {
            System.err.println("Something went wrong on reading Client properties.\n" +
                    "Please provide information like this:\nKey=YourRealValue \nproxy.tcp.port=12345\n" +
                    "proxy.host=localhost\ndownload.dir=files/client");
            return false;
        }

        File dir = new File(clDir);
        if (dir == null || !dir.isDirectory()) {
            System.err.println("Directory path given in properties file has to contain a directory!");
            return false;
        }

        return true;
    }
    
    /**
     * Reads config values.
     *
     * @return true, if values are valid and existing. False, on resource not found or parse exception.
     * @throws RemoteException 
     * @throws NotBoundException 
     */
    private boolean readMCConfigFile(Config config) throws RemoteException, NotBoundException {
        try {
            rmiBindingName = config.getString("binding.name");
            proxyRMIAddress = config.getString("proxy.host");
            proxyRMIPort = config.getInt("proxy.rmi.port");
            keysDir = config.getString("keys.dir");
        } catch (Exception e) {
        	System.err.println("Something went wrong on reading mc properties.\n" +
                    "Please provide information like this:\nKey=YourRealValue \nbinding.name=myBindingName\n" +
                    "proxy.host=localhost\nproxy.rmi.port=12345\nkeys.dir=keys");
            return false;
        }

        File dir = new File(keysDir);
        if (dir == null || !dir.isDirectory()) {
            System.err.println("Directory path given in properties file has to contain a directory!");
            return false;
        }
        
        remoteRegistry = LocateRegistry.getRegistry(proxyRMIAddress,proxyRMIPort);
        Remote tmp = remoteRegistry.lookup(rmiBindingName);
        if (tmp instanceof IProxyRMI) {
        	proxyRMI = (IProxyRMI) tmp;
        	dlsCallback = new DownloadSubscriptionCallback(this);
        	UnicastRemoteObject.exportObject(dlsCallback, 0);
        } else {
        	return false;
        }

        return true;
    }

    /**
     * Initialize versions map with Version Zero for each file in directory.
     */
    private void initVersionsMap() {
        Set<String> filenames = MyUtils.getFileNamesInDirectory(clDir);
        for (String filename : filenames) {
            versionMap.put(filename, 0);
        }
    }

    //Lookup versionmap for file with given filename.
    public Integer getVersionForFile(String filename) {
        return versionMap.get(filename);
    }

    //Establish connection to proxy with object in- and outputstream.
    private void createSockets() throws IOException {
        socket = new Socket(proxyAddress, tcpPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Send request and receive response. Blocking.
     *
     * @param request Request to send.
     * @return Returned response. Null, if not instance of Response.
     */
    public Response sendRequest(Request request) {
        //If out socket not available, we can not send a request.
        if (out == null) {
            return null;
        }

        try {
            out.writeObject(request);
            out.flush();

            Object object = in.readObject();

            if (object instanceof Response) {
                return (Response) object;
            }

        } catch (IOException e) {
            this.closeConnection();
            System.out.println("Error sending request. Connections closed.");
        } catch (ClassNotFoundException e) {
            //Shouldn't occur.
        }

        return null;
    }

    //Close connection to proxy.
    public void closeConnection() {
        connected = false;

        //If socket wasn't even opened, we don't need to close it.
        if (out == null) {
            return;
        }

        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            //Exceptions can occur here after losing connection
            //nothing to worry about.
        }
    }

    //Tries to read a file with given filename and returns it, if successful.
    public File readFile(String filename) throws IOException {
        File file = new File(clDir + "/" + filename);
        if (file.exists()) {
            return file;
        } else {
            System.out.println("File does not exist.");
            return null;
        }

    }

    public DownloadFileResponse downloadFile(DownloadTicketResponse response) {
        InetAddress serverAddress = response.getTicket().getAddress();
        int serverPort = response.getTicket().getPort();

        Request downloadFileRequest = new DownloadFileRequest(response.getTicket());

        try {
            Socket socket = new Socket(serverAddress, serverPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(downloadFileRequest);
            out.flush();

            Object object = in.readObject();

            //If right response, save file.
            if (object instanceof DownloadFileResponse) {
                DownloadFileResponse downloadFileResponse = (DownloadFileResponse) object;
                //For the meantime we use version 0 everywhere.
                versionMap.put(response.getTicket().getFilename(), 0);
                MyUtils.saveByteArrayAsFile(downloadFileResponse.getContent(), clDir + "/" + response.getTicket().getFilename());
                return downloadFileResponse;
            }

        } catch (IOException e) {
            System.out.println("Error on downloading file. Maybe the fileserver went offline in meantime.");
        } catch (ClassNotFoundException e) {
            //Shouldn't occur.
        }

        return null;
    }

    //Getter for private variable connected.
    //Indicates whether a connection to proxy is established or not.
    public boolean isConnected() {
        return connected;
    }
    
    protected void unexportUnicasts() {
    	try {
			UnicastRemoteObject.unexportObject(dlsCallback, true);
		} catch (NoSuchObjectException e) {}
    }
}

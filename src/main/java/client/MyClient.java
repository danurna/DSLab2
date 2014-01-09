package client;

import cli.Shell;
import message.Request;
import message.Response;
import message.request.DownloadFileRequest;
import message.request.LoginRequest;
import message.response.DownloadFileResponse;
import message.response.DownloadTicketResponse;
import proxy.IProxyRMI;
import proxy.RSAChannelEncryption;
import proxy.TCPChannel;
import util.ComponentFactory;
import util.Config;
import util.MyUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Set;

/**
 * My client class maintains connection to proxy
 * but also downloads and uploads data from fileserver.
 */
public class MyClient {
    private Socket socket;
    private TCPChannel originalTcpChannel;
    private TCPChannel tcpChannel;
    private String proxyAddress;
    private int tcpPort;
    private String clDir;
    private HashMap<String, Integer> versionMap;
    private boolean connected;
    
	private String rmiBindingName;
	private int proxyRMIPort;
	private String proxyRMIAddress;
	private String keysDir;
    private String proxyPubKeyPath;
    private PublicKey proxyPubKey;
    private PrivateKey clientPrivateKey;

	private Registry remoteRegistry;
	private IProxyRMI proxyRMI;
	
	private Shell shell;

    public MyClient(Config config, Config mcConfig, Shell shell) {
        if (!this.readConfigFile(config)) {
            //If reading config fails, we fail too.
            return;
        }

        if (!this.readMCConfigFile(mcConfig)) {
            //If reading config fails, we fail too.
            return;
        }
        
        this.shell = shell;
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
            proxyPubKeyPath = config.getString("proxy.key");
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

        try {
            proxyPubKey =  MyUtils.getPublicKeyForPath(proxyPubKeyPath);
        } catch (IOException e) {
            System.err.println("Could not read proxy's public key. Authentication won't work without it.");
            proxyPubKey = null;
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
    private boolean readMCConfigFile(Config config) {
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
        try{
            remoteRegistry = LocateRegistry.getRegistry(proxyRMIAddress,proxyRMIPort);
            Remote tmp = remoteRegistry.lookup(rmiBindingName);
            if (tmp instanceof IProxyRMI) {
                proxyRMI = (IProxyRMI) tmp;
            } else {
                return false;
            }
        } catch(Exception e){
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
        tcpChannel = new TCPChannel();
        tcpChannel.setStreamsForSocket(socket);
        originalTcpChannel = tcpChannel;
    }

    /**
     * Send request and receive response. Blocking.
     *
     * @param request Request to send.
     * @return Returned response. Null, if not instance of Response.
     */
    public Response sendRequest(Request request) {
        //If out socket not available, we can not send a request.
        if (tcpChannel.getOut() == null) {
            return null;
        }

        //If login request, we need to authenticate first.
        if(request instanceof LoginRequest){
            //If there is no public key set, we can not proceed.
            if( proxyPubKey == null ){
                return null;
            }
            clientPrivateKey = readPrivateKey(keysDir+"/"+((LoginRequest) request).getUsername()+".pem");
            if(clientPrivateKey == null){
                return null;
            }
            tcpChannel = new RSAChannelEncryption(originalTcpChannel, clientPrivateKey, proxyPubKey);
        }

        try {
            tcpChannel.writeObject(request);
            Object object = tcpChannel.readObject();

            if (object instanceof Response) {
                return (Response) object;
            }

        } catch(EOFException e){

        } catch (IOException e){
            this.closeConnection();
        } catch (ClassNotFoundException e) {
            //Shouldn't occur.
        }

        return null;
    }

    //Close connection to proxy.
    public void closeConnection() {
        connected = false;

        //If socket wasn't even open, we don't need to close it.
        if (tcpChannel.getOut() == null) {
            return;
        }

        try {
            tcpChannel.close();
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

    //Reset channel because of log out.
    public void userLoggedOut(){
        tcpChannel = originalTcpChannel;
    }

    private PrivateKey readPrivateKey(String path){
        PrivateKey ret = null;
        try {
            ret = MyUtils.getPrivateKeyForPath(path);
        } catch (IOException e) {
            //Wrong usage or file does not exist.
            System.err.println("No private key for user found.");
            return null;
        }
        return ret;
    }
    
    public Shell getShell() {
    	return shell;
    }
    
    public void setProxyPublicKey(PublicKey publicKey) {
    	this.proxyPubKey = publicKey;
    	try {
			MyUtils.writePublicKeyToPath(this.proxyPubKeyPath, publicKey);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public PublicKey getClientPublicKey(String userName) {
    	try {
			return MyUtils.getPublicKeyForPath(keysDir+System.getProperty("file.seperator")+userName+".pub.pem");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
        }
    }
}

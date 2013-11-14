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
import java.util.HashMap;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 17.10.13
 * Time: 16:02
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

    public MyClient(Config config) {
        if (!this.readConfigFile(config)) {
            //If reading config fails, we fail too.
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

    //Setup connection to proxy and return whether it was successful or not.
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
     * @return true, if values are convertFileToByteArray successfully. False, on resource not found or parse exception.
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
            e.printStackTrace();
        }

        return null;
    }

    public void closeConnection() {
        connected = false;

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
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    //Getter for private variable connected.
    //Indicates whether a connection to proxy is established or not.
    public boolean isConnected() {
        return connected;
    }
}

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
import java.util.Timer;
import java.util.TimerTask;

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

    public MyClient(Config config) {
        if (!this.readConfigFile(config)) {
            System.out.println("Client: Error on reading config file.");
            return;
        }

        versionMap = new HashMap<String, Integer>();
        this.initVersionsMap();
        this.connectToProxy();
    }

    private void connectToProxy() {
        final Timer timer = new Timer();

        //Timer for connection establishing
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    createSockets();
                    timer.cancel();
                    System.out.println("Connected to Proxy.");
                } catch (IOException e) {
                    System.out.println("Connecting to proxy failed. Automatically trying in 3 seconds again.");
                }

            }
        }, 0, 3000);
    }

    public static void main(String[] args) {
        try {
            Shell shell = new Shell("client", System.out, System.in);
            new ComponentFactory().startClient(new Config("client"), shell);
            shell.run();
        } catch (Exception e) {
            e.printStackTrace();
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
            //TODO: print usage!
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


    public Integer getVersionForFile(String filename) {
        return versionMap.get(filename);
    }

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
        try {
            out.writeObject(request);
            out.flush();

            Object object = in.readObject();

            if (object instanceof Response) {
                return (Response) object;
            }

        } catch (IOException e) {
            System.err.println("Error on client sendRequest. Closing connection.");
            this.closeConnection();
            this.connectToProxy();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void closeConnection() {
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
                //TODO: Get real version of downloaded file. For the moment, use 0. DownloadFileResponse doesn't
                //      provide enough information.
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

}

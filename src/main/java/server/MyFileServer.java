package server;

import message.Response;
import message.request.UploadRequest;
import message.response.InfoResponse;
import message.response.MessageResponse;
import util.Config;
import util.MyUtils;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Our fileserver with thread for sending isAlive packages to proxy.
 * Accepts connections and puts them into a new thread. (ProxyServerBridge). Is also used by clients.
 * <p/>
 * This class also keeps track of some opened sockets and closes them on cleanup.
 */
public class MyFileServer {
    private Config config;
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutorService;
    private Collection<Object> activeSockets;
    private int fsAlive;
    private int tcpPort;
    private int proxyUdpPort;
    private String proxyAdress;
    private String fsDir;
    private String secretKeyPath;
    private Key secretKey;
    //Version hashMap
    private HashMap<String, Integer> versionMap;

    public MyFileServer(Config config) {
        this.config = config;
        if (!this.readConfigFile()) {
            return;
        }

        versionMap = new HashMap<String, Integer>();
        executor = Executors.newCachedThreadPool();
        activeSockets = new ArrayList<Object>();
        this.createSockets();
        this.createSendAliveThread();
        this.initVersionsMap();
        try {
            secretKey = MyUtils.readSecretKeybyPath(secretKeyPath);
        } catch (IOException e) {
            //lalala
        }
    }

    /**
     * Reads config values.
     *
     * @return true, if values are convertFileToByteArray successfully. False, on resource not found or parse exception.
     */
    private boolean readConfigFile() {
        try {
            tcpPort = config.getInt("tcp.port");
            proxyUdpPort = config.getInt("proxy.udp.port");
            fsAlive = config.getInt("fileserver.alive");
            proxyAdress = config.getString("proxy.host");
            fsDir = config.getString("fileserver.dir");
            secretKeyPath = config.getString("hmac.key");
        } catch (Exception e) {
            System.err.println("Something went wrong on reading Fileserver properties.\n" +
                    "Please provide information like this:\nKey=YourRealValue \ntcp.port=12345\n" +
                    "proxy.udp.port=12345\nfileserver.alive=1000\nproxy.host=localhost\nfileserver.dir=files/server");
            return false;
        }

        File dir = new File(fsDir);
        if (dir == null || !dir.isDirectory()) {
            System.err.println("Directory path given in properties file has to contain a directory!");
            return false;
        }

        return true;
    }

    private void createSockets() {

        Runnable serverCommunicationListen = new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                try {
                    serverSocket = new ServerSocket(tcpPort);
                    activeSockets.add(serverSocket);
                } catch (IOException e) {
                    System.err.println("Could not listen on tcp port: " + tcpPort);
                    return;
                }

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        activeSockets.add(clientSocket);
                        handleConnection(clientSocket);
                    } catch (IOException e) {
                        //serverSocket accept. Socket has been closed.
                    }
                }
            }
        };

        executor.execute(serverCommunicationListen);
    }

    private void handleConnection(Socket socket) {
        ProxyServerBridge ProxyServerBridge = new ProxyServerBridge(socket, this);
        ProxyServerBridge.setHmacKey(secretKeyPath);
        executor.execute(ProxyServerBridge);
    }

    public void createSendAliveThread() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    DatagramSocket toSocket;

                    @Override
                    public void run() {

                        InetAddress address = null;
                        try {
                            address = InetAddress.getByName(proxyAdress);
                            String s = "!isAlive " + tcpPort;
                            byte[] buf = s.getBytes();
                            buf = s.getBytes();
                            if (toSocket == null)
                                toSocket = new DatagramSocket();
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, proxyUdpPort);
                            toSocket.send(packet);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                },
                0 /* Start delay */,
                fsAlive /* Period */,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Initialize versions map with Version Zero for each file in directory.
     */
    private void initVersionsMap() {
        Set<String> filenames = MyUtils.getFileNamesInDirectory(fsDir);
        for (String filename : filenames) {
            versionMap.put(filename, 0);
        }
    }

    /**
     * Tries to read file inside directory and to create an Inforesponse.
     *
     * @param filename - Filename of requestes fileinfo.
     * @return InfoResponse of file, if exists. Messageresponse, otherwise.
     */
    public Response getFileInfo(String filename) {
        File file = new File(fsDir + "/" + filename);
        if (file.exists() && file.isFile()) {
            return new InfoResponse(filename, file.length());
        }

        return new MessageResponse("File does not exist.");
    }

    /**
     * Tries to read file from fs's directory.
     *
     * @param filename - Filename of file to read from fs's directory.
     * @return File, if exists. Null, otherwise.
     * @throws IOException
     */
    public File readFile(String filename) throws IOException {
        File file = new File(fsDir + "/" + filename);
        if (file.exists() && file.isFile()) {
            return file;
        }

        return null;
    }

    /**
     * Tries to save file from request to fs's directory.
     *
     * @param request - Request with content to save.
     * @return True, if file was saved successfully. False, if error occurred while saving.
     */
    public boolean saveFileFromRequest(UploadRequest request) {
        String path = fsDir + "/" + request.getFilename();
        //Save file
        try {
            MyUtils.saveByteArrayAsFile(request.getContent(), path);
        } catch (IOException e) {
            return false;
        }
        //Update versions info
        versionMap.put(request.getFilename(), request.getVersion());
        return true;
    }

    /**
     * Close threads, sockets and System.in
     *
     * @throws IOException
     */
    public void closeConnections() throws IOException {
        executor.shutdownNow();
        scheduledExecutorService.shutdownNow();
        System.in.close();
        for (Object o : activeSockets) {
            if (o instanceof Socket) {
                Socket s = (Socket) o;
                s.close();
            } else if (o instanceof DatagramSocket) {
                DatagramSocket s = (DatagramSocket) o;
                s.close();
            } else if (o instanceof ServerSocket) {
                ServerSocket s = (ServerSocket) o;
                s.close();
            }
        }
    }

    public Set<String> getFileNames() {
        return MyUtils.getFileNamesInDirectory(fsDir);
    }
}

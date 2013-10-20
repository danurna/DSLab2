package server;

import cli.Shell;
import message.request.UploadRequest;
import message.response.InfoResponse;
import sun.text.normalizer.VersionInfo;
import util.ComponentFactory;
import util.Config;
import util.MyUtils;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 19.10.13
 * Time: 13:30
 */
public class MyFileServer {
    private Config config;
    private ExecutorService executor;
    private int fsAlive;
    private int tcpPort;
    private int proxyUdpPort;
    private String proxyAdress;
    private String fsDir;
    //Version hashMap
    private HashMap<String, VersionInfo> versionMap;

    public static void main(String[] args) {
        try {
            Shell shell = new Shell("fs1", System.out, System.in);
            new ComponentFactory().startFileServer(new Config("fs1"), shell);
            shell.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MyFileServer(Config config) {
        this.config = config;
        if (!this.readConfigFile()) {
            System.out.println("Fileserver: Error on reading config file.");
            return;
        }

        versionMap = new HashMap<String, VersionInfo>();
        executor = Executors.newCachedThreadPool();
        this.createSockets();
        this.createSendAliveThread();
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
        } catch (Exception e) {
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
                } catch (IOException e) {
                    System.err.println("Could not listen on tcp port: " + tcpPort);
                    return;
                }

                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleConnection(clientSocket);
                    } catch (IOException e) {
                        System.err.println("Error on serverSocket accept.");
                        e.printStackTrace();
                    }
                }
            }
        };

        executor.execute(serverCommunicationListen);
    }

    private void handleConnection(Socket socket) {
        System.out.println("handle client for socket " + socket);

        ProxyServerBridge ProxyServerBridge = new ProxyServerBridge(socket, this);
        executor.execute(ProxyServerBridge);
    }

    public void createSendAliveThread() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {

                        InetAddress address = null;
                        try {
                            address = InetAddress.getByName(proxyAdress);
                            String s = "!isAlive " + tcpPort;
                            byte[] buf = s.getBytes();
                            DatagramSocket toSocket = new DatagramSocket();
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

    public Set<String> getFileNames() {
        File file = new File(fsDir);
        Set<String> files = new HashSet<String>();
        files.addAll(Arrays.asList(file.list()));
        return files;
    }

    public File readFile(String filename) throws IOException {
        File file = new File(fsDir + "/" + filename);
        if (file.exists()) {
            return file;
        } else {
            System.out.println("File does not exist.");
            return null;
        }

    }

    public InfoResponse getFileInfo(String filename) {
        File file = new File(fsDir + "/" + filename);

        return new InfoResponse(filename, file.length());
    }

    public boolean saveFileFromRequest(UploadRequest request) throws IOException {
        String path = fsDir + "/" + request.getFilename();
        File file = new File(path);
        if (file.exists()) {
            //TODO: handle file exists.
        } else {
            MyUtils.saveByteArrayAsFile(request.getContent(), path);
            return true;
        }

        return false;
    }
}

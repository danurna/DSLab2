package server;

import cli.Shell;
import message.Response;
import message.request.DownloadFileRequest;
import message.request.InfoRequest;
import message.request.UploadRequest;
import message.request.VersionRequest;
import message.response.MessageResponse;
import util.ComponentFactory;
import util.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
public class MyFileServer implements IFileServer {
    private Config config;
    private ExecutorService executor;
    private int fsAlive;
    private int tcpPort;
    private int proxyUdpPort;
    private String proxyAdress;
    private String fsDir;

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

        executor = Executors.newCachedThreadPool();
        this.createSockets();
        this.createSendAliveThread();
    }

    /**
     * Reads config values.
     *
     * @return true, if values are read successfully. False, on resource not found or parse exception.
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

    }


    @Override
    public Response list() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response download(DownloadFileRequest request) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response info(InfoRequest request) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response version(VersionRequest request) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MessageResponse upload(UploadRequest request) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
}

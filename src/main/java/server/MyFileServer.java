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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private int udpPort;
    private String fsDir;

    public static void main(String[] args) {
        try {
            new ComponentFactory().startFileServer(new Config("fs1"), new Shell("fs1", System.out, System.in));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MyFileServer(Config config) {
        this.config = config;
        if(!this.readConfigFile()){
            System.out.println("Fileserver: Error on reading config file.");
            return;
        }

        executor = Executors.newCachedThreadPool();
        this.createSockets();
    }

    /**
     * Reads config values.
     * @return true, if values are read successfully. False, on resource not found or parse exception.
     */
    private boolean readConfigFile(){
        try{
            tcpPort = config.getInt("tcp.port");
            udpPort = config.getInt("proxy.udp.port");
            fsAlive = config.getInt("fileserver.alive");
            fsDir = config.getString("fileserver.dir");
        }catch(Exception e){
            return false;
        }

        return true;
    }

    private void createSockets(){

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
}

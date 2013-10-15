package proxy;

import cli.Shell;
import message.Response;
import message.request.BuyRequest;
import message.request.DownloadTicketRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.response.LoginResponse;
import message.response.MessageResponse;
import model.FileserverEntity;
import model.UserEntity;
import util.Config;
import util.MyUtils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 12.10.13
 * Time: 15:32
 * To change this template use File | Settings | File Templates.
 */
public class MyProxy implements IProxy{
    private ExecutorService executor;
    private Config config;
    private Shell shell;
    private HashMap<String, UserEntity> userMap;
    private HashMap<String, FileserverEntity> fileserverMap;

    public static void main(String[] args){
        new MyProxy(new Config("proxy")).run();
    }

    //Start proxy with system cli for testing
    private void run() {
        this.readUserProperties();
        this.createSockets();
        this.createFileserverGC();
        shell = new Shell("proxyTest", System.out, System.in);
        shell.register(new MyProxyCli(this));
        shell.run();
    }

    public MyProxy(Config config){
        this.config = config;
        executor = Executors.newCachedThreadPool();
        userMap = new HashMap<String, UserEntity>();
        fileserverMap = new LinkedHashMap<String, FileserverEntity>();
    }

    @Override
    public LoginResponse login(LoginRequest request) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response credits() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response buy(BuyRequest credits) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response list() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response download(DownloadTicketRequest request) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MessageResponse upload(UploadRequest request) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MessageResponse logout() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     *
     * @return Array of UserEntities of logged in users.
     */
    public Collection<UserEntity> getUserList(){
        return userMap.values();
    }

    public Collection<FileserverEntity> getFileserverList(){
        return fileserverMap.values();
    }

    /**
     *
     * Example data:
     * alice.credits = 200
     * bill.credits = 200
     *
     * alice.password = 12345
     * bill.password = 23456
     */
    private void readUserProperties(){
        Properties prop = new Properties();

        try {
            //load a properties file
            prop.load(getClass().getClassLoader().getResourceAsStream("user.properties"));

            for(String string: prop.stringPropertyNames()){
                String[] splitStrings = string.split("\\.");
                if(splitStrings.length == 2){
                    String name = splitStrings[0];

                    if(!userMap.containsKey(name)){
                        //Create new user info for new user.
                        String password = prop.getProperty(name+".password");
                        long credits = Long.parseLong( prop.getProperty(name+".credits") );

                        UserEntity userEntity = new UserEntity(name, password, credits, false);
                        System.out.println(userEntity.getUserInfo());
                        userMap.put(name, userEntity);
                    }

                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private void createSockets(){

        Runnable serverCommunicationListen = new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                try{
                    serverSocket = new ServerSocket(config.getInt("tcp.port"));
                }catch(IOException e){
                    System.err.println("Could not listen on tcp port: " + config.getInt("tcp.port"));
                    return;
                }

                while(true){
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        System.err.println("Error on serverSocket accept.");
                        e.printStackTrace();
                    }
                }
            }
        };

        Runnable serverKeepAliveListen = new Runnable() {
            @Override
            public void run() {
                DatagramSocket datagramSocket = null;
                try{
                    datagramSocket = new DatagramSocket(config.getInt("udp.port"));
                }catch(IOException e){
                    System.err.println("Could not listen on udp port: " + config.getInt("udp.port"));
                    return;
                }

                while(true) {
                    try {
                        byte[] buf = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        datagramSocket.receive(packet);
                        handleReceivedPacket(packet);
                    } catch (IOException e) {
                        System.err.println("Error on packet receive.");
                        e.printStackTrace();
                    }
                }
            }
        };

        executor.execute( serverCommunicationListen );
        executor.execute( serverKeepAliveListen );
    }

    public void createFileserverGC(){
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override public void run() {
                        //Check for each fileserver, if timeout counter is greater than the
                        //value defined in config class. If true, set fileserver entry to offline.
                        for(FileserverEntity entity: fileserverMap.values()){
                            long difference = MyUtils.compareTwoTimeStamps(MyUtils.getCurrentTimestamp(), entity.getLastAliveTime());
                            long timeout = Long.parseLong(config.getString("fileserver.timeout"));
                            if(difference > timeout){
                                entity.setOnline(false);
                            }
                        }
                    }
                },
                0 /* Start delay */,
                config.getInt("fileserver.checkPeriod") /* Period */,
                TimeUnit.MILLISECONDS );
    }

    public void handleClient(final Socket clientSocket){
        System.out.println("handle client for socket " + clientSocket);

        Runnable clientCommunication = new Runnable() {
            @Override
            public void run() {
                BufferedReader in = null;
                PrintWriter out = null;
                ObjectOutputStream objectOut = null;
                ObjectInputStream objectIn = null;

                try {
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );

                    objectIn = new ObjectInputStream(clientSocket.getInputStream());

                    Object obj;
                    while( (obj = objectIn.readObject()) != null ){
                        System.out.println("Client sent: " + obj);

                        objectOut = new ObjectOutputStream(clientSocket.getOutputStream());
                        objectOut.writeObject(new LoginResponse(LoginResponse.Type.SUCCESS));
                        objectOut.flush();
                    }

                } catch (EOFException e){
                    System.out.println("Reached EOF");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            }
        };

        executor.execute( clientCommunication );
    }

    /**
     * Handle the received UDP packet.
     * Synchronized because we are adding users to the fileserver Map.
     * @param packet Received UDP packet to handle.
     */
    public synchronized void handleReceivedPacket(DatagramPacket packet){
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Packet received " + received);

        if(!fileserverMap.containsKey(received)){
            fileserverMap.put(received, new FileserverEntity(packet.getAddress(), Integer.parseInt(received), 0, true));
        }else{
            fileserverMap.get(received).updateLastAliveTime();
            fileserverMap.get(received).setOnline(true);
        }
    }
}

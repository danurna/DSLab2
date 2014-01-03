package proxy;

import cli.Shell;
import message.Response;
import message.request.InfoRequest;
import message.request.LoginRequest;
import message.response.InfoResponse;
import message.response.MessageResponse;
import model.FileserverEntity;
import model.UserEntity;
import server.FileserverRequest;
import util.ComponentFactory;
import util.Config;
import util.MyUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * My proxy class listens on tcp and udp port for connection.
 * If connection arrives at tcp port, put it into a new thread.
 * Garbage Collection of fileserver state is implemented here.
 * <p/>
 * This class also keeps track of some opened sockets and closes them on cleanup.
 */
public class MyProxy {
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutorService;
    private Config config;
    private ConcurrentHashMap<String, UserEntity> userMap;
    private ConcurrentHashMap<String, FileserverEntity> fileserverMap;
    private Collection<Object> activeSockets;
    //Config values
    private int tcpPort;
    private int udpPort;
    private int fsTimeout;
    private int fsPeriod;
    
	private ProxyManagementComponent pmc;

    public static void main(String[] args) {
        try {
            Shell shell = new Shell("proxy", System.out, System.in);
            new ComponentFactory().startProxy(new Config("proxy"), shell);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public MyProxy(Config config) {
        this.config = config;
        if (!this.readConfigFile()) {
            return;
        }

        userMap = new ConcurrentHashMap<String, UserEntity>();
        if (!this.readUserProperties()) {
            return;
        }

        executor = Executors.newCachedThreadPool();
        activeSockets = new ArrayList<Object>();
        fileserverMap = new ConcurrentHashMap<String, FileserverEntity>();
        this.createSockets();
        this.createFileserverGC();
    }
    
    public void setProxyManagementComponent(ProxyManagementComponent pmc) {
    	this.pmc = pmc;
    }
    
    /**
     * Reads config values.
     *
     * @return true, if values are convertFileToByteArray successfully. False, on resource not found or parse exception.
     */
    private boolean readConfigFile() {
        try {
            tcpPort = config.getInt("tcp.port");
            udpPort = config.getInt("udp.port");
            fsPeriod = config.getInt("fileserver.checkPeriod");
            fsTimeout = config.getInt("fileserver.timeout");
        } catch (Exception e) {
            System.err.println("Something went wrong on reading Proxy properties.\n" +
                    "Please provide information like this:\nKey=YourRealValue \ntcp.port=12345\n" +
                    "udp.port=12345\nfileserver.checkPeriod=2000\nfileserver.timout=4000");
            return false;
        }

        return true;
    }

    /**
     * @return Collection of UserEntities of logged in users.
     */
    public Collection<UserEntity> getUserList() {
        return userMap.values();
    }

    /**
     * @return Collection of active fileservers.
     */
    public Collection<FileserverEntity> getFileserverList() {
        return fileserverMap.values();
    }

    /**
     * Checks for user and password of given login request
     *
     * @param request login request
     * @return UserEntitiy of authenticated user. Null, if wrong credentials.
     */
    public UserEntity login(LoginRequest request) {
        UserEntity user = userMap.get(request.getUsername());
        //Exists?
        if (user == null) {
            return null;
        }
        //Right password?
        if (!user.getPassword().equals(request.getPassword())) {
            return null;
        }

        return user;
    }

    /**
     * Reads user data from users.properties and stores them in
     * our ConcurrentHashMap userMap.
     * <p/>
     * Example data:
     * alice.credits = 200
     * bill.credits = 200
     * <p/>
     * alice.password = 12345
     * bill.password = 23456
     */
    private boolean readUserProperties() {
        Properties prop = new Properties();

        try {
            //load a properties file
            prop.load(getClass().getClassLoader().getResourceAsStream("user.properties"));

            for (String string : prop.stringPropertyNames()) {
                String[] splitStrings = string.split("\\."); //Regex for a dot.

                if (splitStrings.length != 2) {
                    throw new IllegalArgumentException();
                }

                String name = splitStrings[0];

                if (!userMap.containsKey(name)) {
                    //Create new user info for new user.
                    String password = prop.getProperty(name + ".password");
                    String creditsString = prop.getProperty(name + ".credits");
                    long credits = Long.parseLong(creditsString);

                    if (name == null || password == null || creditsString == null) {
                        throw new IllegalArgumentException();
                    }

                    UserEntity userEntity = new UserEntity(name, password, credits, false);
                    userMap.put(name, userEntity);
                }
            }

            return true;
        } catch (Exception e) {
            //Wrong usage or file does not exist.
            System.err.println("Something went wrong on reading user properties.\n" +
                    "Please provide information like this for each user:\nKey=YourRealValue \nusername.password=12345\n" +
                    "username.credits=12345");
            return false;
        }

    }

    //Create Sockets for udp keep alive and tcp connections.
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
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        //Server Socket accept. Thrown if socket is closed.
                    } catch (RejectedExecutionException e) {
                        //From handle client
                    }
                }

            }
        };

        Runnable serverKeepAliveListen = new Runnable() {
            @Override
            public void run() {
                DatagramSocket datagramSocket = null;
                try {
                    datagramSocket = new DatagramSocket(udpPort);
                    activeSockets.add(datagramSocket);
                } catch (IOException e) {
                    System.err.println("Could not listen on udp port: " + udpPort);
                    return;
                }

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        byte[] buf = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        datagramSocket.receive(packet);
                        handleReceivedPacket(packet);
                    } catch (IOException e) {
                        //Exception on receive. Thrown if socket is closed.
                    }
                }

            }
        };

        executor.execute(serverCommunicationListen);
        executor.execute(serverKeepAliveListen);
    }

    //Starts scheduled thread execution for GC:
    public void createFileserverGC() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1); //We know there will be only one garbage collector.

        scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        //Check for each fileserver, if timeout counter is greater than the
                        //value defined in config class. If true, set fileserver entry to offline.
                        for (FileserverEntity entity : fileserverMap.values()) {
                            long difference = MyUtils.compareTwoTimeStamps(MyUtils.getCurrentTimestamp(), entity.getLastAliveTime());
                            if (difference > fsTimeout) {
                                entity.setOnline(false);
                            }
                        }
                    }
                },
                0 /* Start delay */,
                fsPeriod /* Period */,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Create new Session for one accepted client socket.
     *
     * @param clientSocket Socket for connection to put into own thread.
     */
    public void handleClient(final Socket clientSocket) {
        ClientProxyBridge clientProxyBridge = new ClientProxyBridge(clientSocket, this);
        executor.execute(clientProxyBridge);
    }

    /**
     * Handle the received UDP packet.
     * Synchronized because we are adding users to the fileserver Map.
     *
     * @param packet Received UDP packet to handle.
     */
    public synchronized void handleReceivedPacket(DatagramPacket packet) {
        String received = new String(packet.getData(), 0, packet.getLength());
        String splitString[] = received.split("\\ ");

        if (splitString.length != 2) {
            return;
        }

        if (splitString[0].equals("!isAlive")) {
            String key = splitString[1];
            if (!fileserverMap.containsKey(key)) {
                fileserverMap.put(key, new FileserverEntity(packet.getAddress(), Integer.parseInt(key), 0, true));
            } else {
                fileserverMap.get(key).updateLastAliveTime();
                fileserverMap.get(key).setOnline(true);
            }
        }

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

    /**
     * Returns the least used and online fileserver of given list.
     *
     * @param list
     * @return least used and online fileserver of given list.
     */
    private FileserverEntity getLeastUsedFileserverFromList(Collection<FileserverEntity> list) {
        FileserverEntity entity = null;

        for (FileserverEntity entity1 : list) {
            if (entity == null) {
                if (entity1.isOnline())
                    entity = entity1;
            } else if (entity1.getUsage() < entity.getUsage() && entity1.isOnline()) {
                entity = entity1;
            }
        }

        return entity;
    }

    /**
     * Searches for least used, online Fileserver having the file for given filename.
     *
     *
     * @param filename - Filename of file, we are searching a fileserver for.
     * @param nr
     *@param bridge   - Bridge to be used to send fileserver request.  @return FileserverRequest with Response and FileserverEntity, if server found. Null, if no fs at all. FileserverRequest w/o FileserverEntity, if file not available.
     */
    public FileserverRequest getLeastUsedFileserverForFile(String filename, Collection<FileserverEntity> nr, ClientProxyBridge bridge) {
        Collection<FileserverEntity> list = nr;

        FileserverEntity fs = getLeastUsedFileserverFromList(list);

        if (fs == null) { //no fs available
            return null;
        }

        Response response = null;

        //Call every fs until we found the file (or no fs has it)
        while (list.size() > 0 && !((response = bridge.performFileserverRequestWrapper(new InfoRequest(filename), fs)) instanceof InfoResponse)) {
            list.remove(fs); //Remove fs without requested file.
            fs = getLeastUsedFileserverFromList(list);
        }

        //Can be null here again, if a fileserver went offline in meantime.
        if (fs == null) { //no fs available
            return null;
        }

        //Last response not info response?
        if (response != null && !(response instanceof InfoResponse)) {
            return new FileserverRequest((MessageResponse) response, null);
        }

        InfoResponse infoResponse = (InfoResponse) response;
        return new FileserverRequest(infoResponse, fs);
    }
    private Collection<FileserverEntity> getQuorum(boolean nR){
        int quorum = 0;
        if (nR){
            quorum = ((int) Math.floor(fileserverMap.size() / 2.0))+1;
        }else{
            quorum = ((int) Math.ceil(fileserverMap.size() / 2.0))+1;
        }
        int counter = 0;
        Collection<FileserverEntity> list = fileserverMap.values();
        Collection<FileserverEntity> quorumlist = new ArrayList<FileserverEntity>();
        for (FileserverEntity entity1 : list) {
            if (entity1.isOnline()){
                if (counter<quorum){
                    quorumlist.add(entity1);
                    counter++;
                }else{
                    for (FileserverEntity entity : quorumlist){
                        if (entity.getUsage()>entity1.getUsage()){
                            quorumlist.remove(entity);
                            quorumlist.add(entity1);
                        }
                    }
                }
            }
        }
        return quorumlist;
    }
    public Collection<FileserverEntity> getNR(){
        return getQuorum(true);
    }

    public Collection<FileserverEntity> getNW(){
        return getQuorum(false);
    }

    /**
     * Checks if username is already in use and logged in.
     *
     * @param entity - Entitiy of user to check.
     * @return true, if user is already logged in at proxy. False, otherwise.
     */
    public boolean isUserLoggedIn(UserEntity entity) {
        //User exists?
        if (userMap.contains(entity)) {
            //User already online?
            if (userMap.get(entity.getName()).isOnline()) {
                return true;
            }
        }

        return false;
    }

}

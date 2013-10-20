package proxy;

import cli.Shell;
import message.request.LoginRequest;
import model.FileserverEntity;
import model.UserEntity;
import util.ComponentFactory;
import util.Config;
import util.MyUtils;

import java.io.Closeable;
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
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 12.10.13
 * Time: 15:32
 */
public class MyProxy {
    private ExecutorService executor;
    private ScheduledExecutorService scheduledExecutorService;
    private Config config;
    private ConcurrentHashMap<String, UserEntity> userMap;
    private ConcurrentHashMap<String, FileserverEntity> fileserverMap;
    private Collection<Closeable> activeSockets;
    //Config values
    private int tcpPort;
    private int udpPort;
    private int fsTimeout;
    private int fsPeriod;

    public static void main(String[] args) {
        try {
            Shell shell = new Shell("proxy", System.out, System.in);
            new ComponentFactory().startProxy(new Config("proxy"), shell);
            shell.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public MyProxy(Config config) {
        this.config = config;
        if (!this.readConfigFile()) {
            System.out.println("Proxy: Error on reading config file.");
            return;
        }

        executor = Executors.newCachedThreadPool();
        activeSockets = new ArrayList<Closeable>();
        userMap = new ConcurrentHashMap<String, UserEntity>();
        fileserverMap = new ConcurrentHashMap<String, FileserverEntity>();
        this.readUserProperties();
        this.createSockets();
        this.createFileserverGC();
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
    private void readUserProperties() {
        Properties prop = new Properties();

        try {
            //load a properties file
            prop.load(getClass().getClassLoader().getResourceAsStream("user.properties"));

            for (String string : prop.stringPropertyNames()) {
                String[] splitStrings = string.split("\\.");

                if (splitStrings.length != 2) {
                    return;
                }

                String name = splitStrings[0];

                if (!userMap.containsKey(name)) {
                    //Create new user info for new user.
                    String password = prop.getProperty(name + ".password");
                    long credits = Long.parseLong(prop.getProperty(name + ".credits"));

                    UserEntity userEntity = new UserEntity(name, password, credits, false);
                    userMap.put(name, userEntity);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

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
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        System.err.println("Error on serverSocket accept.");
                    } catch (RejectedExecutionException e) { //From handle client
                        System.err.println("Rejected Execution");
                    }
                }

/*                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }*/
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
                        System.err.println("Error on packet receive.");
                    }
                }

            }
        };

        executor.execute(serverCommunicationListen);
        executor.execute(serverKeepAliveListen);
    }

    public void createFileserverGC() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);

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

    public void handleClient(final Socket clientSocket) {
        System.out.println("handle client for socket " + clientSocket);

        ClientProxyBridge clientProxyBridge = new ClientProxyBridge(clientSocket, this);
        executor.execute(clientProxyBridge);
    }

    /**
     * Handle the received UDP packet.
     * Synchronized because we are adding users to the fileserver Map.
     *
     * @param packet Received UDP packet to handle.
     */
    public void handleReceivedPacket(DatagramPacket packet) {
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

    public void closeConnections() {
        //TODO: Close all connections
        executor.shutdownNow();
        scheduledExecutorService.shutdownNow();
        for (Closeable c : activeSockets) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public FileserverEntity getLeastUsedFileserver() {
        FileserverEntity entity = null;

        for (FileserverEntity entity1 : fileserverMap.values()) {
            if (entity == null) {
                if (entity1.isOnline())
                    entity = entity1;
            } else if (entity1.getUsage() < entity.getUsage() && entity1.isOnline()) {
                entity = entity1;
            }
        }

        return entity;
    }
}

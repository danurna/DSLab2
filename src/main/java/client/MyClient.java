package client;

import cli.Shell;
import message.Request;
import message.Response;
import util.Config;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 17.10.13
 * Time: 16:02
 */
public class MyClient {
    private Config config;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String proxyAdress;
    private int tcpPort;

    public MyClient(Config config){
        this.config = config;
        if(!this.readConfigFile()){
            System.out.println("Client: Error on reading config file.");
            return;
        }

        this.createSockets();
    }

    public static void main(String[] args){
        new MyClient(new Config("client")).run();
    }

    //Start proxy with system cli for testing
    private void run() {
        Shell shell = new Shell("clientTest", System.out, System.in);
        shell.register(new MyClientCli(this));
        shell.run();
    }

    /**
     * Reads config values.
     * @return true, if values are read successfully. False, on resource not found or parse exception.
     */
    private boolean readConfigFile(){
        try{
            tcpPort = config.getInt("proxy.tcp.port");
            proxyAdress = config.getString("proxy.host");
        }catch(Exception e){
            return false;
        }

        return true;
    }


    private void createSockets() {
        try{
            socket = new Socket(proxyAdress, tcpPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Send request and receive response. Blocking.
     * @param request Request to send.
     * @return Returned response. Null, if not instance of Response.
     */
    public Response sendRequest(Request request){
        try {
            out.writeObject(request);
            out.flush();

            Object object = in.readObject();

            if(object instanceof Response){
                return (Response)object;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }

        return null;
    }


    public void closeConnection(){
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

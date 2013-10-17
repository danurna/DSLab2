package client;

import cli.Shell;
import message.Request;
import message.Response;
import message.request.LoginRequest;
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

    public MyClient(Config config){
        this.config = config;
        this.createSockets();
    }

    public static void main(String[] args){
        new MyClient(new Config("client.properties")).run();
    }

    //Start proxy with system cli for testing
    private void run() {
        Shell shell = new Shell("clientTest", System.out, System.in);
        shell.register(new MyClientCli(this));
        shell.run();
    }

    private void createSockets() {
        try{
            socket = new Socket(config.getString("proxy.host"), config.getInt("proxy.tcp.port"));
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

}

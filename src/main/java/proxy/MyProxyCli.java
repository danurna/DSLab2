package proxy;

import cli.Command;
import message.Response;
import message.response.MessageResponse;
import model.FileserverEntity;
import model.UserEntity;
import model.UserInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 12.10.13
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
public class MyProxyCli implements IProxyCli{
    MyProxy realProxy;


    public MyProxyCli(MyProxy realProxy){
        this.realProxy = realProxy;
    }


    @Override
    @Command
    public Response fileservers() throws IOException {
        for(FileserverEntity fileserver: realProxy.getFileserverList()){
            System.out.println(fileserver);
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Command
    public Response users() throws IOException {
        for(UserEntity user: realProxy.getUserList()){
            System.out.println(user.getUserInfo());
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
        System.out.println("exit()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Command
    public void send() throws IOException {
        System.out.println("send()");
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try{
            socket = new Socket("localhost", 12500);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader( socket.getInputStream() ) );
        }catch(Exception e){
            e.printStackTrace();
        }

        out.println("Hello!");
        System.out.println("echo: " + in.readLine());

        out.close();
        in.close();
        socket.close();
    }

    @Command
    public void sendalive() throws IOException {
        InetAddress address = InetAddress.getByName("localhost");
        int port = 12501;
        String s = "13000";
        byte[] buf = s.getBytes();
        DatagramSocket toSocket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        toSocket.send(packet);
    }
}

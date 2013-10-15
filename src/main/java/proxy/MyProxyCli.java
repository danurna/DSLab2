package proxy;

import cli.Command;
import message.Response;
import message.request.BuyRequest;
import message.request.LoginRequest;
import message.response.LoginResponse;
import message.response.MessageResponse;
import model.FileserverEntity;
import model.UserEntity;
import model.UserInfo;

import java.io.*;
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
        ObjectOutputStream objectOut = null;
        ObjectInputStream objectIn = null;

        try{
            socket = new Socket("localhost", 12500);

            objectOut = new ObjectOutputStream(socket.getOutputStream());
            objectOut.writeObject(new LoginRequest("anna", "12345"));
            objectOut.flush();

            objectIn = new ObjectInputStream(socket.getInputStream());
            Object obj;

            while( (obj = objectIn.readObject()) != null ){
                System.out.println("echo: " + obj);

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                objectOut.writeObject(new LoginRequest("bobo", "12345"));
                objectOut.flush();
            }

        }catch(Exception e){
            e.printStackTrace();
            return;
        }

        objectIn.close();
        objectOut.close();
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

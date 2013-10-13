package proxy;

import cli.Command;
import message.Response;
import message.response.MessageResponse;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 12.10.13
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
public class MyProxyCli implements  IProxyCli{
    MyProxy realProxy;


    public MyProxyCli(MyProxy realProxy){
        this.realProxy = realProxy;
    }


    @Override
    @Command
    public Response fileservers() throws IOException {
        System.out.println("fileservers()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Command
    public Response users() throws IOException {
        System.out.println("users()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
        System.out.println("exit()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

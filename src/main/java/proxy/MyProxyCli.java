package proxy;

import cli.Command;
import message.Response;
import message.response.MessageResponse;
import model.FileserverEntity;
import model.UserEntity;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 12.10.13
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
public class MyProxyCli implements IProxyCli {
    MyProxy realProxy;


    public MyProxyCli(MyProxy realProxy) {
        this.realProxy = realProxy;
    }

    @Override
    @Command
    public Response fileservers() throws IOException {
        for (FileserverEntity fileserver : realProxy.getFileserverList()) {
            System.out.println(fileserver);
        }
        //TODO: use real response type (FileServerInfoResponse)
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Command
    public Response users() throws IOException {
        for (UserEntity user : realProxy.getUserList()) {
            System.out.println(user.getUserInfo());
        }
        //TODO: use real response type (UserInfoResponse)
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
        System.out.println("exit()");
        realProxy.closeConnections();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}

package proxy;

import cli.Command;
import message.Response;
import message.response.FileServerInfoResponse;
import message.response.MessageResponse;
import message.response.UserInfoResponse;
import model.FileServerInfo;
import model.FileserverEntity;
import model.UserEntity;
import model.UserInfo;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of Proxy CLI Inteface.
 * As proxy has only got a few commands, this is straightforward.
 */
public class MyProxyCli implements IProxyCli {
    MyProxy realProxy;
    
    private Thread shellThread;
    
    public MyProxyCli(MyProxy realProxy, Thread shellThread) {
        this.realProxy = realProxy;
        this.shellThread = shellThread;
    }

    @Override
    @Command
    public Response fileservers() throws IOException {
        List<FileServerInfo> list = new LinkedList<FileServerInfo>();
        for (FileserverEntity fileserver : realProxy.getFileserverList()) {
            list.add(fileserver.getFileServerInfo());
        }

        return new FileServerInfoResponse(list);
    }

    @Override
    @Command
    public Response users() throws IOException {
        List<UserInfo> list = new LinkedList<UserInfo>();
        for (UserEntity user : realProxy.getUserList()) {
            list.add(user.getUserInfo());
        }

        return new UserInfoResponse(list);
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
    	shellThread.interrupt();
        realProxy.closeConnections();
        realProxy.getProxyManagementComponent().unexportUnicasts();
        return null;
    }

}

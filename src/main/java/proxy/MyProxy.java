package proxy;

import cli.Shell;
import message.Response;
import message.request.BuyRequest;
import message.request.DownloadTicketRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.response.LoginResponse;
import message.response.MessageResponse;
import util.Config;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 12.10.13
 * Time: 15:32
 * To change this template use File | Settings | File Templates.
 */
public class MyProxy implements IProxy{
    private Config config;
    private Shell shell;

    public static void main(String[] args){
        new MyProxy(new Config("proxy")).run();
    }

    private void run() {
        shell = new Shell("proxy", System.out, System.in);

        shell.register(new MyProxyCli(this));

        shell.run();
    }


    public MyProxy(Config config){
        this.config = config;
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
}

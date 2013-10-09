import message.Response;
import message.response.LoginResponse;
import message.response.MessageResponse;

import java.io.IOException;
import client.IClientCli;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 09.10.13
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
public class MyClient implements IClientCli{

    public static void main(String[] args){

    }

    @Override
    public LoginResponse login(String username, String password) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response credits() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response buy(long credits) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response list() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Response download(String filename) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MessageResponse upload(String filename) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MessageResponse logout() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MessageResponse exit() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

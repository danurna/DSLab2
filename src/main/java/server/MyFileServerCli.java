package server;

import message.response.MessageResponse;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 19.10.13
 * Time: 13:31
 */
public class MyFileServerCli implements IFileServerCli {
    private MyFileServer server;

    public MyFileServerCli(MyFileServer server) {
        this.server = server;
    }

    @Override
    public MessageResponse exit() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

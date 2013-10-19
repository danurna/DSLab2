package server;

import cli.Command;
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
    @Command
    public MessageResponse exit() throws IOException {
        return null;
    }

    @Command
    public MessageResponse readFile(String name) throws IOException {
        server.readFile(name);
        return null;
    }
}

package server;

import cli.Command;
import message.response.MessageResponse;

import java.io.IOException;

/**
 * Fileserver CLI Interface implementation.
 * As only exit ist possible, no need to tell a story here.
 */
public class MyFileServerCli implements IFileServerCli {
    private MyFileServer server;

    public MyFileServerCli(MyFileServer server) {
        this.server = server;
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
        server.closeConnections();
        return new MessageResponse("Bye!");
    }

}

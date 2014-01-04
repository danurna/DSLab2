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
    
    private Thread shellThread;
    
    public MyFileServerCli(MyFileServer server, Thread shellThread) {
        this.server = server;
        this.shellThread = shellThread;
    }

    @Override
    @Command
    public MessageResponse exit() throws IOException {
    	shellThread.interrupt();
        server.closeConnections();
        return new MessageResponse("Bye!");
    }

}

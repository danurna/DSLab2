package proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * TCPChannel Decorator.
 */
public class ChannelDecorator extends TCPChannel{
    private TCPChannel parentChannel;

    public ChannelDecorator(TCPChannel channel){
        this.parentChannel = channel;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return parentChannel.readObject();
    }

    public void writeObject(Object object) throws IOException {
        parentChannel.writeObject(object);
    }

    public void setStreamsForSocket(Socket socket) throws IOException {
        parentChannel.setStreamsForSocket(socket);
    }

    public void close() throws IOException {
        parentChannel.close();
    }

    public void setOut(ObjectOutputStream out){
        parentChannel.setOut(out);
    }

    public ObjectOutputStream getOut(){
        return parentChannel.getOut();
    }

    public void setIn(ObjectInputStream in){
        parentChannel.setIn(in);
    }

    public ObjectInputStream getIn(){
        return parentChannel.getIn();
    }

}
package proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Holds an
 */
public class TCPChannel {
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    public TCPChannel(){

    }

    public void setStreamsForSocket(Socket socket) throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    public void writeObject(Object object) throws IOException {
        out.writeObject(object);
        out.flush();
    }

    public void close() throws IOException {
        in.close();
        out.close();
    }

    public void setOut(ObjectOutputStream out){
        this.out = out;
    }

    public ObjectOutputStream getOut(){
        return out;
    }

    public void setIn(ObjectInputStream in){
        this.in = in;
    }

    public ObjectInputStream getIn(){
        return in;
    }

}

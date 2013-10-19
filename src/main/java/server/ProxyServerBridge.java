package server;

import message.Response;
import message.request.*;
import message.response.ListResponse;
import message.response.MessageResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 19.10.13
 * Time: 22:23
 * To change this template use File | Settings | File Templates.
 */
public class ProxyServerBridge implements IFileServer, Runnable {
    private final Socket socket;
    private final MyFileServer server;

    public ProxyServerBridge(Socket socket, MyFileServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        ObjectOutputStream objectOut = null;
        ObjectInputStream objectIn = null;

        try {
            objectOut = new ObjectOutputStream(socket.getOutputStream());
            objectIn = new ObjectInputStream(socket.getInputStream());

            Object obj;
            while ((obj = objectIn.readObject()) != null) {
                System.out.println("Proxy sent: " + obj);
                Response response = performRequest(obj);

                if (response == null)
                    return;

                objectOut.writeObject(response);
                objectOut.flush();
            }

        } catch (EOFException e) {
            System.out.println("Reached EOF");
        } catch (SocketException e) {
            System.out.println("Socket closed!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response list() throws IOException {
        return new ListResponse(server.getFileNames());
    }

    @Override
    public Response download(DownloadFileRequest request) throws IOException {
        return null;
    }

    @Override
    public Response info(InfoRequest request) throws IOException {
        return null;
    }

    @Override
    public Response version(VersionRequest request) throws IOException {
        return null;
    }

    @Override
    public MessageResponse upload(UploadRequest request) throws IOException {
        return null;
    }

    /**
     * Checks instance of obj and performs according action.
     *
     * @param obj
     * @return
     * @throws IOException
     */
    private Response performRequest(Object obj) throws IOException {
        Response response = null;

        if (obj instanceof ListRequest) {
            response = list();
        } else if (obj instanceof DownloadFileRequest) {
            response = download((DownloadFileRequest) obj);
        } else if (obj instanceof InfoRequest) {
            response = info((InfoRequest) obj);
        } else if (obj instanceof VersionRequest) {
            response = version((VersionRequest) obj);
        } else if (obj instanceof UploadRequest) {
            response = upload((UploadRequest) obj);
        }

        if (response == null) {
            response = new MessageResponse("No valid request sent.");
        }

        return response;
    }


}

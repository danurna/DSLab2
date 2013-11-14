package server;

import message.Response;
import message.request.*;
import message.response.DownloadFileResponse;
import message.response.ListResponse;
import message.response.MessageResponse;
import model.DownloadTicket;
import util.MyUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * For each connection the fileserver receives, it creates a new ProxyServerBridge
 * to handle requests.
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
            while ((obj = objectIn.readObject()) != null) { //EOF Exception
                Response response = performRequest(obj);

                if (response == null)
                    response = new MessageResponse("");

                objectOut.writeObject(response);
                objectOut.flush();
                break;
            }

        } catch (EOFException e) {
            //Reached EOF. Nothing unusual
        } catch (SocketException e) {
            //Socket is already closed.
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //Cleanup
            try {
                objectIn.close();
                objectOut.close();
            } catch (IOException e) {
                //Socket could be already closed.
            }
        }
    }

    @Override
    public Response list() throws IOException {
        return new ListResponse(server.getFileNames());
    }

    @Override
    public Response download(DownloadFileRequest request) throws IOException {
        if (validateTicket(request.getTicket())) {
            File file = server.readFile(request.getTicket().getFilename());
            DownloadFileResponse response = new DownloadFileResponse(request.getTicket(), MyUtils.convertFileToByteArray(file));
            return response;
        } else {
            return new MessageResponse("Validation for download ticket failed.");
        }
    }

    @Override
    public Response info(InfoRequest request) throws IOException {
        if (server.getFileNames().contains(request.getFilename())) {
            return server.getFileInfo(request.getFilename());
        } else {
            return new MessageResponse("File not found.");
        }
    }

    @Override
    public Response version(VersionRequest request) throws IOException {
        return null;
    }

    @Override
    public MessageResponse upload(UploadRequest request) throws IOException {
        boolean uploaded = server.saveFileFromRequest(request);
        return new MessageResponse(uploaded ? "Upload successful." : "Upload failed");
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
        } else if (obj instanceof MessageResponse) {
            response = (MessageResponse) obj;
        }

        if (response == null) {
            response = new MessageResponse("No valid request sent.");
        }

        return response;
    }


    /**
     * Validates ticket with given ticket information and file version number zero.
     *
     * @param ticket - Ticket to validate.
     * @return True, if validation was successful. False, if validation failed.
     */
    private boolean validateTicket(DownloadTicket ticket) {
        try {
            return util.ChecksumUtils.verifyChecksum(ticket.getUsername(), server.readFile(ticket.getFilename()), 0, ticket.getChecksum());
        } catch (IOException e) {
            return false;
        }
    }

}

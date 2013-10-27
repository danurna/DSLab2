package server;

import message.Response;
import model.FileserverEntity;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 27.10.13
 * Time: 16:49
 * To change this template use File | Settings | File Templates.
 */
public class FileserverRequest {
    private Response response;
    private FileserverEntity entity;

    public FileserverRequest(Response response, FileserverEntity entity) {
        this.response = response;
        this.entity = entity;
    }

    public Response getResponse() {
        return response;
    }

    public FileserverEntity getFileserverEntity() {
        return entity;
    }

}

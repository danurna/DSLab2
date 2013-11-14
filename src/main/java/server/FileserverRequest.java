package server;

import message.Response;
import model.FileserverEntity;

/**
 * Capsules a response and the entity linked to that response.
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

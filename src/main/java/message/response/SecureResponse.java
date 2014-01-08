package message.response;

import message.Response;

/**
 * Created with IntelliJ IDEA.
 * User: Felix
 * Date: 07.01.14
 * Time: 21:56
 * To change this template use File | Settings | File Templates.
 */
public class SecureResponse implements Response {
    private Response response;
    private byte[] hash;

    public SecureResponse(byte[] hash, Response response){
        this.hash = hash;
        this.response = response;
    }

    public Response getResponse(){
        return response;
    }

    public byte[] getHash(){
        return hash;
    }

    @Override
    public String toString() {
        return new String(hash) + " " + response.toString();
    }
}

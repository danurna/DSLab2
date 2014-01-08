package message.request;

import message.Request;


/**
 * Created with IntelliJ IDEA.
 * User: Felix
 * Date: 07.01.14
 * Time: 18:30
 * To change this template use File | Settings | File Templates.
 */
public class SecureRequest implements Request{
    private Request request;
    private byte[] hash;

    public SecureRequest(byte[] hash, Request request){
        this.hash = hash;
        this.request = request;
    }

    public Request getRequest(){
        return request;
    }

    public byte[] getHash(){
        return hash;
    }

    @Override
    public String toString() {
        return new String(hash) + " " + request.toString();
    }
}

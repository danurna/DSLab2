package proxy;

import message.request.ClientChallengeRequest;
import message.request.LoginRequest;
import message.response.MessageResponse;
import util.MyUtils;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Adds RSA Authentication and AES encryption to TCPChannel connection.
 */
public class RSAChannelEncryption extends TCPChannel{
    private TCPChannel parentChannel;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String sessionKey;

    public RSAChannelEncryption(TCPChannel channel, PrivateKey privateKey, PublicKey publicKey){
        this.parentChannel = channel;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        Object obj = parentChannel.readObject();

        if( obj instanceof SealedObject ){
            System.out.println("SEALED OBJECT HERE!");

            if(sessionKey == null){
                obj = decryptRSA((SealedObject) obj);
            }else{
                obj = decryptAES((SealedObject) obj);
            }

            if(handleUnsealedObject(obj)){
                return null;
            }

        }

        return obj;
    }

    public void writeObject(Object object) throws IOException {

        if(sessionKey == null){
            startAuthenticationForObject(object);
        }else{
            parentChannel.writeObject(object);
        }

    }

    private Object decryptRSA(SealedObject object){
        Cipher dec  = null;
        try {
            dec = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
            dec.init(Cipher.DECRYPT_MODE, privateKey);
            return object.getObject(dec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object decryptAES(SealedObject object){
        //TODO: IMPLEMENT WITH AES
        return null;
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

    private boolean handleUnsealedObject(Object object){

        if( object instanceof ClientChallengeRequest ){
            ClientChallengeRequest clientChallengeRequest = (ClientChallengeRequest) object;
            System.out.println("WE'VE RECEIVED A ClientChallengeRequest!");
            sessionKey = "test";
            try {
                parentChannel.writeObject(new MessageResponse("Challenge accepted!"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    private void startAuthenticationForObject(Object object){

        if( publicKey == null){
            System.err.println("Can't start authentication without public key.");
            return;
        }

        if( object instanceof LoginRequest ){
            LoginRequest request = (LoginRequest) object;
            byte[] encodedRandomNumber = MyUtils.base64encodeBytes(MyUtils.generateSecureRandomNumber(32));
            ClientChallengeRequest clientChallengeRequest = new ClientChallengeRequest(request.getUsername(), encodedRandomNumber);
            Cipher cipher = null;
            try {
                cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                SealedObject sealedObject = new SealedObject(clientChallengeRequest, cipher);
                parentChannel.writeObject(sealedObject);
            } catch (Exception e){
                e.printStackTrace();
            }

        }else{
            System.err.println("Can't start authentication without login information.");
            return;
        }
    }
}

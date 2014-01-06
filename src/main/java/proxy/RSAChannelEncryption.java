package proxy;

import message.request.ClientChallengeRequest;
import message.request.LoginRequest;
import message.response.ClientChallengeResponse;
import message.response.MessageResponse;
import message.response.ProxyChallengeResponse;
import util.MyUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * Adds RSA Authentication and AES encryption to TCPChannel connection.
 */
public class RSAChannelEncryption extends ChannelDecorator{
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private SecretKey sessionKey;
    private IvParameterSpec ivParameter;
    private String keysDirectoryPath;
    private Cipher encryptRSA;
    private Cipher decryptRSA;
    private Cipher encryptAES;
    private Cipher decryptAES;
    private Object lastObjectSent;
    private boolean shouldStartAuthentication;

    public RSAChannelEncryption(TCPChannel channel, PrivateKey privateKey, PublicKey publicKey){
        super(channel);
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.keysDirectoryPath = "";
        this.shouldStartAuthentication = true;
    }

    public RSAChannelEncryption(TCPChannel channel, PrivateKey privateKey, String keysDirectoryPath, boolean shouldStartAuthentication){
        this(channel, privateKey, (PublicKey)null);
        this.keysDirectoryPath = keysDirectoryPath;
        this.shouldStartAuthentication = shouldStartAuthentication;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        Object obj = super.readObject();

        if( obj instanceof byte[] ){
            //DECODE BASE64
            byte[] byteObj = (byte[])obj;
            byteObj = MyUtils.base64decodeBytes(byteObj);

            //DECIPHER, RSA OR AES
            if(sessionKey == null){
                byteObj = decryptRSA(byteObj);
            }else{
                byteObj = decryptAES(byteObj);
            }

            //REBUILD OBJECT
            obj = MyUtils.deserialize(byteObj);

            if(handleUnsealedObject(obj)){
                return this.readObject();
            }
        }

        return obj;
    }

    public void writeObject(Object object) throws IOException {

        if(sessionKey == null){

            //If no authentication should be started on this channel-end, this boolean-flag is true.
            //e.g. proxy
            if(!shouldStartAuthentication){
                super.writeObject(object);
                return;
            }

            //No secure connection established. Start authentication.
            if(startAuthenticationForObject(object)){
                //If authentication is done, we call this method again as session key should exist.
                this.writeObject(object);
            }
        }else{
            //Secure connection established. Use it.
            if( object instanceof Serializable ){
                byte[] ser = MyUtils.serialize((Serializable) object);
                ser = encryptAES(ser);
                ser = MyUtils.base64encodeBytes(ser);
                super.writeObject(ser);
            }else{
                super.writeObject(object);
            }
        }

    }

    /**
     * Encrypts given object with public key set.
     * @param object object to encrypt.
     * @return encrypted object. Null, if error occured.
     */
    private byte[] encryptRSA(byte[] object){
        try {
            if( encryptRSA == null){
                encryptRSA =  Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
                encryptRSA.init(Cipher.ENCRYPT_MODE, publicKey);
            }

            return encryptRSA.doFinal(object);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Decrypts given object with private key set.
     * @param object object to decrypt.
     * @return decrypted object. Null, if error occured.
     */
    private byte[] decryptRSA(byte[] object){

        try {
            if( decryptRSA == null){
                decryptRSA =  Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
                decryptRSA.init(Cipher.DECRYPT_MODE, privateKey);
            }

            return decryptRSA.doFinal(object);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Encrypts given object with session key and ivParameter set.
     * @param object object to encrypt.
     * @return encrypted object. Null, if error occured.
     */
    private byte[] encryptAES(byte[] object){
        try {
            if( encryptAES == null){
                encryptAES =  Cipher.getInstance("AES/CTR/NoPadding");
                encryptAES.init(Cipher.ENCRYPT_MODE, sessionKey, ivParameter);
            }

            return encryptAES.doFinal(object);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Decrypts given object with session key and ivParameter set.
     * @param object object to decrypt.
     * @return decrypted object. Null, if error occured.
     */
    private byte[] decryptAES(byte[] object){
        try {
            if( decryptAES == null){
                decryptAES =  Cipher.getInstance("AES/CTR/NoPadding");
                decryptAES.init(Cipher.DECRYPT_MODE, sessionKey, ivParameter);
            }

            return decryptAES.doFinal(object);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //Returns true if necessary to read/wait for an answer.
    //For example sending ClientChallengeResponse waits for an ProxyChallengeResponse.
    //But receiving ProxyChallengeResponse, doesn't need to wait.
    //If not it should go up to parent channel.
    private boolean handleUnsealedObject(Object object) throws IOException {

        //Handle incoming 1st message. PROXY SIDE.
        if( object instanceof ClientChallengeRequest ){
            ClientChallengeRequest clientChallengeRequest = (ClientChallengeRequest) object;

            //Set public key according to client challenge
            try {
                publicKey = MyUtils.getPublicKeyForPath(keysDirectoryPath+"/"+ clientChallengeRequest.getUsername() + ".pub.pem");
            } catch (IOException e) {
                String msg = "Could not read public key for user " + clientChallengeRequest.getUsername();
                System.err.println(msg);
                //Send error msg to client.
                super.writeObject(new MessageResponse(msg));
            }

            try {
                ClientChallengeResponse response = buildResponseForRequest(clientChallengeRequest);
                byte[] ser = MyUtils.serialize(response);
                byte[] encodedCipherMsg = MyUtils.base64encodeBytes(encryptRSA(ser));
                super.writeObject(encodedCipherMsg);
                lastObjectSent = response;

                //Wait for expected ProxyChallengeResponse.
                this.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            return true;

        //Handle incoming 2nd message. CLIENT SIDE.
        }else if( object instanceof ClientChallengeResponse){
            ClientChallengeResponse clientChallengeResponse = (ClientChallengeResponse) object;

            //Check if answer response contains given challenge
            if( lastObjectSent instanceof ClientChallengeRequest ){
                ClientChallengeRequest precedingRequest = (ClientChallengeRequest) lastObjectSent;
                if( !Arrays.equals( precedingRequest.getChallenge(), clientChallengeResponse.getClientChallenge() ) ){
                    System.err.println("The proxy's challenge response was wrong. Authentication failed.");
                    throw new IOException("Authentication failed.");
                }
            }

            byte[] byteSecretKey = MyUtils.base64decodeBytes( clientChallengeResponse.getSecretKey() );
            sessionKey = new SecretKeySpec(byteSecretKey, 0, byteSecretKey.length, "AES");
            ivParameter = new IvParameterSpec( MyUtils.base64decodeBytes( clientChallengeResponse.getIvParameter() ));

            //Build response and AES encrypt it with the session key.
            try {
                ProxyChallengeResponse proxyChallengeResponse = new ProxyChallengeResponse(clientChallengeResponse.getProxyChallenge());
                byte[] ser = MyUtils.serialize(proxyChallengeResponse);
                byte[] encodedCipherMsg = MyUtils.base64encodeBytes(encryptAES(ser));
                super.writeObject(encodedCipherMsg);
                lastObjectSent = proxyChallengeResponse;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;

        //Handle incoming 3d message. PROXY SIDE.
        }else if( object instanceof ProxyChallengeResponse ) {
            ProxyChallengeResponse proxyChallengeResponse = (ProxyChallengeResponse) object;

            //Check if answer response contains given challenge
            if( lastObjectSent instanceof ClientChallengeResponse ){
                ClientChallengeResponse precedingResponse = (ClientChallengeResponse) lastObjectSent;
                if( !Arrays.equals( precedingResponse.getProxyChallenge(), proxyChallengeResponse.getProxyChallenge() ) ){
                    System.err.println("The client's challenge response was wrong. Authentication failed.");
                    throw new IOException("Authentication failed.");
                }
            }

            return false;
        }

        return false;
    }

    //Returns if authentication start was successful or not.
    private boolean startAuthenticationForObject(Object object) throws IOException {
        if( publicKey == null){
            System.err.println("Can't start authentication without public key.");
            return false;
        }

        //We need a login request here to build the first request.
        if( object instanceof LoginRequest ){
            LoginRequest request = (LoginRequest) object;
            byte[] encodedRandomNumber = MyUtils.base64encodeBytes(MyUtils.generateSecureRandomNumber(32));
            //Build clientChallengeRequest
            ClientChallengeRequest clientChallengeRequest = new ClientChallengeRequest(request.getUsername(), encodedRandomNumber);

            try {
                //Serialize object
                byte[] ser = MyUtils.serialize(clientChallengeRequest);
                //encrypt and encode base64
                byte[] encodedCipherMsg = MyUtils.base64encodeBytes(encryptRSA(ser));
                super.writeObject(encodedCipherMsg);
                lastObjectSent = clientChallengeRequest;

                //Wait for expected ClientChallengeResponse.
                this.readObject();
            } catch (ClassNotFoundException e){
                //Won't occur
            }

        }else{
            System.err.println("Can't start authentication without login information.");
            return false;
        }

        return true;
    }

    /**
     * Generate parameters for 2nd message. Puts them into challengeResponse object.
     * @param request
     * @return
     */
    private ClientChallengeResponse buildResponseForRequest(ClientChallengeRequest request){
        byte[] cc = request.getChallenge(); //Already base64 encoded from client.
        byte[] pc = MyUtils.base64encodeBytes(MyUtils.generateSecureRandomNumber(32));
        sessionKey = MyUtils.generateSecretAESKey(256);
        byte[] sk = MyUtils.base64encodeBytes(sessionKey.getEncoded());
        ivParameter = new IvParameterSpec(MyUtils.generateSecureRandomNumber(16));
        byte[] iv = MyUtils.base64encodeBytes(ivParameter.getIV());

        ClientChallengeResponse response = new ClientChallengeResponse(cc, pc, sk, iv);
        return response;
    }

    //Resets variables to be ready for new connection
    public void reset(){
        sessionKey = null;
        encryptAES = null;
        decryptAES = null;
        encryptRSA = null;
        decryptRSA = null;
        ivParameter = null;
    }
}
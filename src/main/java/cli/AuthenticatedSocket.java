package cli;

import message.request.ClientChallengeRequest;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.PrivateKey;

public class AuthenticatedSocket extends Socket {
    private Socket decoratedSocket;
    private String sessionKey;
    private PrivateKey privateKey;

    public AuthenticatedSocket(Socket socket, PrivateKey privateKey){
        decoratedSocket = socket;
        this.privateKey = privateKey;
    }

    private void waitForAuthenticationRequest(){
        ObjectOutputStream objectOut = null;
        ObjectInputStream objectIn = null;

        try {
            objectOut = new ObjectOutputStream(this.getOutputStream());
            objectIn = new ObjectInputStream(this.getInputStream());

            Object obj;
            while ((obj = objectIn.readObject()) != null) {
                Object unsealedObject = null;
                if( obj instanceof SealedObject ){
                    //unsealedObject = decryptSealedObject((SealedObject) obj);
                }

                if( unsealedObject instanceof ClientChallengeRequest ){
                    ClientChallengeRequest ccr = (ClientChallengeRequest) unsealedObject;
                }

                if( unsealedObject == null){
                    return;
                }

                return;
            }

        } catch (EOFException e) {
            //Reached EOF. Nothing unusual.
        } catch (SocketException e) {
            System.out.println("Socket to client closed!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            //Cleanup
            try {
                objectIn.close();
                objectOut.close();
                //this.close();
            } catch (IOException e) {
                //Maybe it is already closed.
            }
        }
    }



    private Object getResponseForClientChallenge(ClientChallengeRequest clientChallengeRequest){
        return null;
    }

}

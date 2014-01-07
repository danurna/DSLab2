package message.response;

import message.Response;

/**
 * 2nd Message: The second message is sent by the Proxy and
 * is encrypted using RSA initialized with the user's public key.
 * Its syntax is: !ok <client-challenge> <proxy-challenge> <secret-key> <iv-parameter>.
 */
public class ClientChallengeResponse implements Response{
    private static final long serialVersionUID = 2775359361487082490L;

    private byte[] clientChallenge;
    private byte[] proxyChallenge;
    private byte[] secretKey;
    private byte[] ivParameter;

    public ClientChallengeResponse(byte[] clientChallenge, byte[] proxyChallenge, byte[] secretKey, byte[] ivParameter){
        this.clientChallenge = clientChallenge;
        this.proxyChallenge = proxyChallenge;
        this.secretKey = secretKey;
        this.ivParameter = ivParameter;
    }

    public byte[] getClientChallenge() {
        return clientChallenge;
    }

    public byte[] getProxyChallenge() {
        return proxyChallenge;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public byte[] getIvParameter() {
        return ivParameter;
    }

    @Override
    public String toString() {
        return String.format("!ok %s %s %s %s",
                new String(getClientChallenge()),
                new String(getProxyChallenge()),
                new String(getSecretKey()),
                new String(getIvParameter())
                );
    }
}

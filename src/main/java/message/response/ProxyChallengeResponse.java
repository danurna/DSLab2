package message.response;

import message.Response;

/**
 *
 */
public class ProxyChallengeResponse implements Response{
    private static final long serialVersionUID = 2005359361487082490L;

    private byte[] proxyChallenge;

    public ProxyChallengeResponse(byte[] proxyChallenge){
        this.proxyChallenge = proxyChallenge;
    }

    public byte[] getProxyChallenge() {
        return proxyChallenge;
    }

    @Override
    public String toString() {
        return String.format("%s",
                new String(getProxyChallenge())
        );
    }
}

package message.request;

import message.Request;

import java.io.UnsupportedEncodingException;

public class ClientChallengeRequest implements Request{
    private static final long serialVersionUID = 1283822183213122949L;

    private final String username;
    private final byte[] challenge;

    public ClientChallengeRequest(String username, byte[] challenge) {
        this.username = username;
        this.challenge = challenge;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getChallenge() {
        return challenge;
    }

    @Override
    public String toString() {
        return String.format("!login %s %s", getUsername(),  new String(getChallenge()));
    }
}

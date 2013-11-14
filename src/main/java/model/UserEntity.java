package model;

/**
 * Extended User model because UserInfo doesn't cover the demands.
 */
public class UserEntity {
    private String name;
    private long credits;
    private volatile boolean online;
    private String password;

    public UserEntity(String name, String password, long credits, boolean online) {
        this.name = name;
        this.credits = credits;
        this.online = online;
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getCredits() {
        return credits;
    }

    public synchronized void increaseCredits(long increase) {
        this.credits += increase;
    }

    public synchronized void decreaseCredits(long decrease) {
        this.credits -= decrease;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserInfo getUserInfo() {
        return new UserInfo(name, credits, online);
    }

}

package model;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 13.10.13
 * Time: 17:14
 * To change this template use File | Settings | File Templates.
 */
public class UserEntity{
    private String name;
    private long credits;
    private volatile boolean online;
    private String password;

    public UserEntity(String name, String password, long credits, boolean online){
        this.name = name;
        this.credits = credits;
        this.online = online;
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword(){
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

    public void setCredits(long credits) {
        this.credits = credits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserInfo getUserInfo(){
        return new UserInfo(name, credits, online);
    }

}

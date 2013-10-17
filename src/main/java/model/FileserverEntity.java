package model;

import util.MyUtils;

import java.net.InetAddress;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 14.10.13
 * Time: 17:23
 * To change this template use File | Settings | File Templates.
 */
public class FileserverEntity {
    private InetAddress address;
    private int port;
    private long usage;
    //Volatile because variable has no connection to any other variable nor do
    //we need the previous value.
    private volatile boolean online;
    private volatile Timestamp lastAliveTime;

    public FileserverEntity(InetAddress address, int port, long usage, boolean online) {
        this.address = address;
        this.port = port;
        this.usage = usage;
        this.online = online;
        this.updateLastAliveTime();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public long getUsage() {
        return usage;
    }

    public void setUsage(long usage) {
        this.usage = usage;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Timestamp getLastAliveTime() {
        return lastAliveTime;
    }

    public void updateLastAliveTime(){
        this.lastAliveTime = MyUtils.getCurrentTimestamp();
    }

    public FileServerInfo getFileServerInfo(){
        return new FileServerInfo(address, port, usage, online);
    }

    @Override
    public String toString() {
        return String.format("%1$-15s %2$-5d %3$-7s %4$13d %5$-15s",
                getAddress().getHostAddress(), getPort(),
                isOnline() ? "online" : "offline", getUsage(), getLastAliveTime());
    }

}

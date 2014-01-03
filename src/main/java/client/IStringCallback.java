package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IStringCallback extends Remote {
	void callback(String message) throws RemoteException;
}

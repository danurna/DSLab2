package proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import client.IStringCallback;
import model.FileserverEntity;

public interface IProxyRMI extends Remote {
	int getReadQuorumSize() throws RemoteException;
	int getWriteQuorumSize() throws RemoteException;
	String[] getTop3DownloadedFiles() throws RemoteException;
	void subscribeToFile(String fileName, IStringCallback callback) throws RemoteException;
	byte[] getProxyPublicKey() throws RemoteException;
	void setClientPublicKey(String user, byte[] key) throws RemoteException;
}

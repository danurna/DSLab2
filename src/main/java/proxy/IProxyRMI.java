package proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;
import java.util.Collection;

import client.IStringCallback;
import model.FileserverEntity;

public interface IProxyRMI extends Remote {
	int getReadQuorumSize() throws RemoteException;
	int getWriteQuorumSize() throws RemoteException;
	String[] getTop3DownloadedFiles() throws RemoteException;
	String subscribeToFile(String fileName, int downloadLimit, String username, IStringCallback callback) throws RemoteException;
	PublicKey getProxyPublicKey() throws RemoteException;
	boolean setClientPublicKey(String user, PublicKey publicKey) throws RemoteException;
}

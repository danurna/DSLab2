package proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import client.IStringCallback;
import model.FileserverEntity;

public interface IProxyRMI extends Remote {
	Collection<FileserverEntity> getReadQuorum() throws RemoteException;
	Collection<FileserverEntity> getWriteQuorum() throws RemoteException;
	Collection<FileEntity> getTop3DownloadedFiles() throws RemoteException;
	void subscribeToFile(String fileName, IStringCallback callback) throws RemoteException;
	byte[] getProxyPublicKey() throws RemoteException;
	byte[] setClientPublicKey() throws RemoteException;
}

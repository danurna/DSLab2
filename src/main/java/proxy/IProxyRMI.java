package proxy;

import java.rmi.Remote;
import java.util.Collection;

import model.FileserverEntity;

public interface IProxyRMI extends Remote {
	Collection<FileserverEntity> getReadQuorum();
	Collection<FileserverEntity> getWriteQuorum();
	Collection<FileEntity> getTop3DownloadedFiles();
	void subscribeToFile(String fileName, IStringCallback callback);
	byte[] getProxyPublicKey();
	byte[] setClientPublicKey();
}

package proxy;

import java.rmi.RemoteException;
import java.util.Collection;

import client.IStringCallback;
import model.FileserverEntity;
import model.UserEntity;

public class ProxyRMI implements IProxyRMI {
	
	ProxyManagementComponent pmc;
	
	public ProxyRMI(ProxyManagementComponent pmc) throws RemoteException {
		this.pmc = pmc;
	}
	
	@Override
	public int getReadQuorumSize() throws RemoteException {
		return pmc.getProxy().getNR();
	}

	@Override
	public int getWriteQuorumSize() throws RemoteException {
		return pmc.getProxy().getNW();
	}

	@Override
	public String[] getTop3DownloadedFiles() throws RemoteException {
		return pmc.getProxy().getTop3DownloadedFiles();
	}

	@Override
	public String subscribeToFile(String fileName, int downloadLimit, String userName, IStringCallback callback) throws RemoteException {
		UserEntity user = pmc.getProxy().getUser(userName);
		if (user == null || !user.isOnline()) {
			return "Please login first.";
		}
		int count = pmc.getProxy().getFileDownloadCount(fileName);
		pmc.registerDownloadCallbackEntity(new DownloadCallbackEntity(fileName, userName, count+downloadLimit, callback));
		return "Successfully subscribed for file: "+fileName;
	}

	@Override
	public byte[] getProxyPublicKey() throws RemoteException {
		return pmc.getProxy().getPublicKey();
	}

	@Override
	public void setClientPublicKey(String user, byte[] key) throws RemoteException {
		pmc.writeClientPublicKey(user, key);
	}

}

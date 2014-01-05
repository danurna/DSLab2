package proxy;

import java.rmi.RemoteException;
import java.util.Collection;

import client.IStringCallback;
import model.FileserverEntity;

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
	public void subscribeToFile(String fileName, int downloadLimit, IStringCallback callback) throws RemoteException {
		pmc.registerDownloadCallbackEntity(new DownloadCallbackEntity(fileName, downloadLimit, callback));
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

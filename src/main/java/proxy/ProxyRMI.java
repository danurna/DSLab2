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
	public Collection<FileserverEntity> getReadQuorum() throws RemoteException {
		return pmc.getProxy().getNR();
	}

	@Override
	public Collection<FileserverEntity> getWriteQuorum() throws RemoteException {
		return pmc.getProxy().getNW();
	}

	@Override
	public Collection<FileEntity> getTop3DownloadedFiles() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void subscribeToFile(String fileName, IStringCallback callback) throws RemoteException {
		
	}

	@Override
	public byte[] getProxyPublicKey() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] setClientPublicKey() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}

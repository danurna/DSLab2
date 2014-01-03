package proxy;

import java.util.Collection;

import client.IStringCallback;
import model.FileserverEntity;

public class ProxyRMI implements IProxyRMI {
	
	ProxyManagementComponent pmc;
	
	public ProxyRMI(ProxyManagementComponent pmc) {
		this.pmc = pmc;
	}
	
	@Override
	public Collection<FileserverEntity> getReadQuorum() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<FileserverEntity> getWriteQuorum() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<FileEntity> getTop3DownloadedFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void subscribeToFile(String fileName, IStringCallback callback) {
		
	}

	@Override
	public byte[] getProxyPublicKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] setClientPublicKey() {
		// TODO Auto-generated method stub
		return null;
	}

}

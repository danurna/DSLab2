package client;

import java.rmi.RemoteException;

public class DownloadSubscriptionCallback implements IStringCallback {
	private MyClient client;
	
	public DownloadSubscriptionCallback(MyClient client) throws RemoteException {
		this.client = client;
	}
	@Override
	public void callback(String message) throws RemoteException {
		// TODO Auto-generated method stub

	}
}

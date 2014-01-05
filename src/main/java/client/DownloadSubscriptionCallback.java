package client;

import java.io.IOException;
import java.rmi.RemoteException;

public class DownloadSubscriptionCallback implements IStringCallback {
	private MyClient client;
	
	public DownloadSubscriptionCallback(MyClient client) throws RemoteException {
		this.client = client;
	}
	@Override
	public void callback(String message) throws RemoteException {
		try {
			client.getShell().writeLine(message.toString());
		} catch (IOException e) {}
	}
}

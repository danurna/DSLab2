package client;

public class DownloadSubscriptionCallback implements IStringCallback {
	private MyClient client;
	
	public DownloadSubscriptionCallback(MyClient client) {
		this.client = client;
	}
	@Override
	public void callback(String message) {
		// TODO Auto-generated method stub

	}
}

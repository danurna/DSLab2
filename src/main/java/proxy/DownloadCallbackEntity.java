package proxy;

import client.IStringCallback;

public class DownloadCallbackEntity {
	public final String fileName;
	public final String userName;
	public final int downloadLimit;
	public final IStringCallback callback;
	
	public DownloadCallbackEntity(String fileName, String userName, int downloadLimit, IStringCallback callback) {
		this.fileName = fileName;
		this.userName = userName;
		this.downloadLimit = downloadLimit;
		this.callback = callback;
	}
}

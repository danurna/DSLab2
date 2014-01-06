package proxy;

import client.IStringCallback;

public class DownloadCallbackEntity {
	public final String fileName;
	public final String userName;
	public final int downloadOffset;
	public final int downloadLoop;
	public final IStringCallback callback;
	
	public DownloadCallbackEntity(String fileName, String userName, int downloadOffset, int downloadLoop, IStringCallback callback) {
		this.fileName = fileName;
		this.userName = userName;
		this.downloadOffset = downloadOffset;
		this.downloadLoop = downloadLoop;
		this.callback = callback;
	}
}

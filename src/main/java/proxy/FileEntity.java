package proxy;

public class FileEntity {
	public final String fileName;
	private int downloads;
	
	public FileEntity(String fileName) {
		this(fileName, 0);
	}
	
	public FileEntity(String fileName, int downloads) {
		this.fileName = fileName;
		this.downloads = downloads;
	}
	
	public int getDownloads() {
		return downloads;
	}
	public void addDownload() {
		this.downloads++;
	}
}

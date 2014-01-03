package proxy;

import java.rmi.Remote;

public interface IStringCallback extends Remote {
	void callback(String message);
}

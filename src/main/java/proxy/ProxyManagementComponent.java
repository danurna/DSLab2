package proxy;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import util.Config;

public class ProxyManagementComponent {
	private Config config;
	
	private String bindingName;
	private String proxyHost;
	private int proxyRMIPort;
	private String keys;
	
	private Registry registry;
	
	public ProxyManagementComponent(Config config) {
		this.config = config;
		if (!this.readConfigFile()) {
            return;
        }
		try {
			registry = LocateRegistry.createRegistry(proxyRMIPort);
		} catch (RemoteException e) {
			e.printStackTrace();
			return;
		}
	}
	
	/**
     * Reads config values.
     *
     * @return true, if values are convertFileToByteArray successfully. False, on resource not found or parse exception.
     */
    private boolean readConfigFile() {
        try {
            bindingName = config.getString("binding.name");
            proxyHost = config.getString("proxy.host");
            proxyRMIPort = config.getInt("proxy.rmi.port");
            keys = config.getString("keys.dir");
        } catch (Exception e) {
            System.err.println("Something went wrong on reading Proxy properties.\n" +
                    "Please provide information like this:\nKey=YourRealValue \nbinding.name=myBindingName\n" +
                    "proxy.host=localhost\nproxy.rmi.port=12345\nkeys.dir=keys");
            return false;
        }

        return true;
    }
}

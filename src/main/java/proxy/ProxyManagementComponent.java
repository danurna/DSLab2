package proxy;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import util.Config;

public class ProxyManagementComponent {
	private Config config;
	
	private String bindingName;
	private String proxyHost;
	private int proxyRMIPort;
	private String keysDir;
	
	private Registry registry;
	
	private IProxyRMI proxyRMI;
	
	private MyProxy proxy;
	
	public ProxyManagementComponent(Config config, MyProxy proxy) {
		this.config = config;
		if (!this.readConfigFile()) {
            return;
        }
		try {
			this.proxy = proxy;
			registry = LocateRegistry.createRegistry(proxyRMIPort);
			proxyRMI = new ProxyRMI(this);
			registry.rebind(bindingName, proxyRMI);
			UnicastRemoteObject.exportObject(proxyRMI, 0);
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
            keysDir = config.getString("keys.dir");
        } catch (Exception e) {
            System.err.println("Something went wrong on reading mc properties.\n" +
                    "Please provide information like this:\nKey=YourRealValue \nbinding.name=myBindingName\n" +
                    "proxy.host=localhost\nproxy.rmi.port=12345\nkeys.dir=keys");
            return false;
        }
        
        File dir = new File(keysDir);
        if (dir == null || !dir.isDirectory()) {
            System.err.println("Directory path given in properties file has to contain a directory!");
            return false;
        }
        return true;
    }
    
    protected MyProxy getProxy() {
    	return proxy;
    }
}

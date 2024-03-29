package proxy;

import util.Config;
import util.MyUtils;

import java.io.File;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyManagementComponent {
	private Config config;
	
	private String bindingName;
	private String proxyHost;
	private int proxyRMIPort;
	private String keysDir;
	
	private Registry registry;
	
	private IProxyRMI proxyRMI;
	
	private MyProxy proxy;
	
	private ConcurrentHashMap<String, Collection<DownloadCallbackEntity>> downloadCallbackMap;
	
	public ProxyManagementComponent(Config config, MyProxy proxy) {
		this.config = config;
		if (!this.readConfigFile()) {
            return;
        }
		try {
			this.downloadCallbackMap = new ConcurrentHashMap<String,Collection<DownloadCallbackEntity>>();
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
    
    protected void writeClientPublicKey(String user, PublicKey publicKey) throws IOException {
    	MyUtils.writePublicKeyToPath(keysDir+System.getProperty("file.separator")+user+".pub.pem", publicKey);
    }
    
    protected void unexportUnicasts() {
    	try {
    		registry.unbind(bindingName);
			UnicastRemoteObject.unexportObject(proxyRMI, true);
		} catch (NoSuchObjectException e) {
			
		} catch (AccessException e) {
			
		} catch (RemoteException e) {
			
		} catch (NotBoundException e) {
			
		}
    }
    
    protected void registerDownloadCallbackEntity(DownloadCallbackEntity entity) {
    	Collection<DownloadCallbackEntity> col;
    	synchronized (downloadCallbackMap) {
	    	col = downloadCallbackMap.get(entity.fileName);
    	}
    	if (col==null) {
    		col = new ArrayList<DownloadCallbackEntity>();
    	}
    	col.add(entity);
    	synchronized (downloadCallbackMap) {
    		downloadCallbackMap.put(entity.fileName, col);
    	}
    }
    protected void checkDownloadCallbackEntitys(String fileName,int downloadCount) {
    	synchronized (downloadCallbackMap) {
	    	Collection<DownloadCallbackEntity> col = downloadCallbackMap.get(fileName);
	    	if (col==null) {
	    		return;
	    	} else {

	    		for (DownloadCallbackEntity e : col) {
	    			if ((downloadCount-e.downloadOffset)%e.downloadLoop==0) {
	    				try {
							e.callback.callback("Notification: "+fileName+" got downloaded "+e.downloadLoop+" times!");

						} catch (RemoteException e1) {}
	    			}
	    		}

	    	}
    	}
    }
    
    protected void userLoggedOut(String userName) {
    	synchronized (downloadCallbackMap) {
	    	ArrayList<DownloadCallbackEntity> remove = new ArrayList<DownloadCallbackEntity>();
	    	for (Collection<DownloadCallbackEntity> ce : downloadCallbackMap.values()) {
	    		remove.clear();
	    		for (DownloadCallbackEntity e : ce) {
	    			if (userName.equals(e.userName)) {
	    				remove.add(e);
	    			}
	    		}
	    		ce.removeAll(remove);
	    	}
    	}
    }
}

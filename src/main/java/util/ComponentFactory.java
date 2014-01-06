package util;

import cli.Shell;
import client.IClientCli;
import client.MyClient;
import client.MyClientCli;
import proxy.IProxyCli;
import proxy.IProxyRMI;
import proxy.MyProxy;
import proxy.MyProxyCli;
import proxy.ProxyManagementComponent;
import proxy.ProxyRMI;
import server.IFileServerCli;
import server.MyFileServer;
import server.MyFileServerCli;

/**
 * Provides methods for starting an arbitrary amount of various components.
 */
public class ComponentFactory {

    /**
     * Creates and starts a new client instance using the provided {@link Config} and {@link Shell}.
     *
     * @param config the configuration containing parameters such as connection info
     * @param shell  the {@code Shell} used for processing commands
     * @return the created component after starting it successfully
     * @throws Exception if an exception occurs
     */
    public IClientCli startClient(Config config, Shell shell) throws Exception {
        System.out.println("startClient");
        Thread shellThread = new Thread(shell);
        IClientCli clientCli = new MyClientCli(new MyClient(config, new Config("mc"), shell),shellThread);
        shell.register(clientCli);
        shellThread.start();

        return clientCli;
    }

    /**
     * Creates and starts a new proxy instance using the provided {@link Config} and {@link Shell}.
     *
     * @param config the configuration containing parameters such as connection info
     * @param shell  the {@code Shell} used for processing commands
     * @return the created component after starting it successfully
     * @throws Exception if an exception occurs
     */
    public IProxyCli startProxy(Config config, Shell shell) throws Exception {
        System.out.println("startProxy");
        Thread shellThread = new Thread(shell);
        MyProxy myProxy = new MyProxy(config);
        IProxyCli proxyCli = new MyProxyCli(myProxy,shellThread);
        ProxyManagementComponent pmc = 
        		new ProxyManagementComponent(new Config("mc"), myProxy);
        myProxy.setProxyManagementComponent(pmc);
        
        shell.register(proxyCli);
        shellThread.start();

        return proxyCli;
    }

    /**
     * Creates and starts a new file server instance using the provided {@link Config} and {@link Shell}.
     *
     * @param config the configuration containing parameters such as connection info
     * @param shell  the {@code Shell} used for processing commands
     * @return the created component after starting it successfully
     * @throws Exception if an exception occurs
     */
    public IFileServerCli startFileServer(Config config, Shell shell) throws Exception {
        System.out.println("startFileServer");
        Thread shellThread = new Thread(shell);
        IFileServerCli fileServerCli = new MyFileServerCli(new MyFileServer(config),shellThread);
        shell.register(fileServerCli);
        shellThread.start();

        return fileServerCli;
    }
}

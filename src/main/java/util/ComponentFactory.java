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
        IClientCli clientCli = new MyClientCli(new MyClient(config, new Config("mc")));
        shell.register(clientCli);
        new Thread(shell).start();

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
        MyProxy myProxy = new MyProxy(config);
        IProxyCli proxyCli = new MyProxyCli(myProxy);
        ProxyManagementComponent pmc = 
        		new ProxyManagementComponent(new Config("mc"), myProxy);
        myProxy.setProxyManagementComponent(pmc);
        
        shell.register(proxyCli);
        new Thread(shell).start();

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
        IFileServerCli fileServerCli = new MyFileServerCli(new MyFileServer(config));
        shell.register(fileServerCli);
        new Thread(shell).start();

        return fileServerCli;
    }
}

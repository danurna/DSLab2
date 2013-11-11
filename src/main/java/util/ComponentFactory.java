package util;

import cli.Shell;
import client.IClientCli;
import client.MyClient;
import client.MyClientCli;
import proxy.IProxyCli;
import proxy.MyProxy;
import proxy.MyProxyCli;
import server.IFileServerCli;
import server.MyFileServer;
import server.MyFileServerCli;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides methods for starting an arbitrary amount of various components.
 */
public class ComponentFactory {
    private final ExecutorService executorService = Executors.newCachedThreadPool();

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
        IClientCli clientCli = new MyClientCli(new MyClient(config));
        shell.register(clientCli);
        executorService.execute(shell);
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
        IProxyCli proxyCli = new MyProxyCli(new MyProxy(config));
        shell.register(proxyCli);
        executorService.execute(shell);
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
        executorService.execute(shell);
        return fileServerCli;
    }
}

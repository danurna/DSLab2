package util;

import cli.Shell;
import client.IClientCli;
import client.MyClient;
import client.MyClientCli;
import proxy.IProxyCli;
import proxy.MyProxy;
import proxy.MyProxyCli;
import server.IFileServerCli;

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
		// TODO: create a new client instance (including a Shell) and start it
        System.out.println("startClient");
        IClientCli clientCli = new MyClientCli(new MyClient(config));
        shell.register(clientCli);
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
		// TODO: create a new proxy instance (including a Shell) and start it
        IProxyCli proxyCli = new MyProxyCli(new MyProxy(config));
        shell.register(proxyCli);
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
		// TODO: create a new file server instance (including a Shell) and start it
		return null;
	}
}

package test;

import cli.Shell;
import cli.TestInputStream;
import cli.TestOutputStream;
import client.IClientCli;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import proxy.IProxyCli;
import server.IFileServerCli;
import util.ComponentFactory;
import util.Config;
import util.Util;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class LoginLogoutTest {
    static ComponentFactory componentFactory = new ComponentFactory();
    IProxyCli proxy;
    IFileServerCli server;
    IClientCli client;

    @Before
    public void before() throws Exception {
        proxy = componentFactory.startProxy(new Config("proxy"), new Shell("proxy", new TestOutputStream(System.out), new TestInputStream()));
        Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);

        server = componentFactory.startFileServer(new Config("fs1"), new Shell("fs1", new TestOutputStream(System.out), new TestInputStream()));
        Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);

        client = componentFactory.startClient(new Config("client"), new Shell("client", new TestOutputStream(System.out), new TestInputStream()));
        Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);
    }

    @After
    public void after() throws Exception {
        try {
            proxy.exit();
        } catch (Exception e) {
            // This should not happen. In case it does, output the stack trace for easier trouble shooting.
            e.printStackTrace();
        }
        try {
            server.exit();
        } catch (IOException e) {
            // This should not happen. In case it does, output the stack trace for easier trouble shooting.
            e.printStackTrace();
        }
        try {
            client.exit();
        } catch (IOException e) {
            // This should not happen. In case it does, output the stack trace for easier trouble shooting.
            e.printStackTrace();
        }
    }


    @Test
    public void test() throws Exception {
        String actual = client.credits().toString();
        String expected = "Please log in.";
        assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));

        actual = client.login("alice", "12345").toString();
        expected = "success";
        assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));

        Throwable throwable = null;
        try {
            actual = client.login("alice", "12345").toString();
        } catch (Throwable e) {
            throwable = e;
        }
        assertTrue("Throwable should be of class IOException", throwable instanceof IOException);

        actual = client.logout().toString();
        expected = "Successfully logged out.";
        assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));

        //Login should work again after logout
        actual = client.login("bill", "23456").toString();
        expected = "success";
        assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));

    }


}

package test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import proxy.IProxyCli;
import server.IFileServerCli;
import util.ComponentFactory;
import util.Config;
import cli.Shell;
import cli.TestInputStream;
import cli.TestOutputStream;
import client.IClientCli;

public class RMITest {
	public final int WAIT_FOR_COMPONENT_STARTUP = 3000;
	
	public static void main(String[] args) throws Exception {
		RMITest test = new RMITest();
		test.before();
		test.test();
		test.after();
	}
    static ComponentFactory componentFactory = new ComponentFactory();
    IProxyCli proxy;
    IFileServerCli server;
    IClientCli client;

    public void before() throws Exception {
        proxy = componentFactory.startProxy(new Config("proxy"), new Shell("proxy", new TestOutputStream(System.out), new TestInputStream()));
        Thread.sleep(WAIT_FOR_COMPONENT_STARTUP);

        server = componentFactory.startFileServer(new Config("fs1"), new Shell("fs1", new TestOutputStream(System.out), new TestInputStream()));
        Thread.sleep(WAIT_FOR_COMPONENT_STARTUP);

        client = componentFactory.startClient(new Config("client"), new Shell("client", new TestOutputStream(System.out), new TestInputStream()));
        Thread.sleep(WAIT_FOR_COMPONENT_STARTUP);
    }

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
        System.out.println("Finishing done");
    }

    public void test() throws Exception {
    	System.out.println("TEST STARTING");
    	String actual = client.readQuorum().toString();
    	System.out.println(actual);
    	
    	
        actual = client.login("alice", "12345").toString();
        String expected = "success";
        assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));
        
        actual = client.subscribe("short", 7).toString();
        System.out.println(actual);
        
        actual = client.credits().toString();
        expected = "200";
        assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));
        
        client.buy(100000);
        for (int i=0;i<10;i++) {
	        actual = client.download("short.txt").toString();
	        expected = "!data dslab13";
	        assertTrue(String.format("Response must start with '%s' but was '%s'", expected, actual), actual.startsWith(expected));
        }
        for (int i=0;i<2;i++) {
	        actual = client.download("long.txt").toString();
	        expected = "!data dslab13";
	        //assertTrue(String.format("Response must start with '%s' but was '%s'", expected, actual), actual.startsWith(expected));
        }
        for (int i=0;i<12;i++) {
	        actual = client.download("upload.txt").toString();
	        expected = "!data dslab13";
	        //assertTrue(String.format("Response must start with '%s' but was '%s'", expected, actual), actual.startsWith(expected));
        }
        
        actual = client.credits().toString();
        expected = "193";
        //assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));

        actual = client.upload("upload.txt").toString();
        expected = "Uploaded files";
        //assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));

        actual = client.credits().toString();
        expected = "291";
        //assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));

        actual = client.logout().toString();
        expected = "Successfully logged out.";
        assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));
        
        actual = client.topThreeDownloads().toString();
        System.out.println(actual);
        System.out.println("TEST FINISHED");
    }
}

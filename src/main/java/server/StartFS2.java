package server;

import cli.Shell;
import util.ComponentFactory;
import util.Config;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 11.11.13
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
public class StartFS2 {

    public static void main(String[] args) {
        try {
            String configName = "fs2";
            System.out.println(configName);
            Shell shell = new Shell(configName, System.out, System.in);
            new ComponentFactory().startFileServer(new Config(configName), shell);
            shell.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

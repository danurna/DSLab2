package server;

import cli.Shell;
import util.ComponentFactory;
import util.Config;

/**
 * Starts Fileserver with config fs2.
 */
public class StartFS2 {

    public static void main(String[] args) {
        try {
            String configName = "fs2";
            System.out.println(configName);
            Shell shell = new Shell(configName, System.out, System.in);
            new ComponentFactory().startFileServer(new Config(configName), shell);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

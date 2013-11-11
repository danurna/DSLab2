package util;

import cli.Shell;

/**
 * Created with IntelliJ IDEA.
 * User: danielwiturna
 * Date: 11.11.13
 * Time: 15:52
 * To change this template use File | Settings | File Templates.
 */
public class ShellThreader extends Thread {
    private Shell shell;

    public ShellThreader(Shell shell) {
        super(shell);
        this.shell = shell;
    }

    @Override
    public void run() {
        if (shell != null) {
            try {
                shell.run();
            } catch (java.lang.IllegalArgumentException e) {
                System.out.println("USAGAESEA!");
            }
        }
    }
}

package deepimagej.gui;

public class GuiUtils {

    public static boolean isEDTAlive() {
    	Thread[] threads = new Thread[Thread.activeCount()];
    	Thread.enumerate(threads);

    	for (Thread thread : threads) {
    	    if (thread.getName().startsWith("AWT-EventQueue")) {
    	        if (thread.isAlive()) {
    	            return true;
    	        }
    	    }
    	}
    	return false;
    }
}

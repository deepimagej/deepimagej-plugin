package deepimagej.gui.workers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

import org.apache.commons.compress.archivers.ArchiveException;

import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.model.stardist.StardistAbstract;

public class InstallEnvWorker extends SwingWorker<Void, Void> {

    private final ModelDescriptor descriptor;
    private Consumer<String> consumer;
    private final CountDownLatch latch;
    private final Runnable callback;
    
    private Thread workerThread;

    public InstallEnvWorker(ModelDescriptor descriptor, CountDownLatch latch, Runnable callback) {
        this.descriptor = descriptor;
        this.latch = latch;
        this.callback = callback;
    }
    
    public void setConsumer(Consumer<String> consumer) {
    	this.consumer = consumer;
    }
    
    public ModelDescriptor getDescriptor() {
    	return this.descriptor;
    }

    public CountDownLatch getCountDownLatch() {
    	return this.latch;
    }

    @Override
    protected Void doInBackground() {
    	workerThread = Thread.currentThread();
    	try {
            if (descriptor.getModelFamily().equals(ModelDescriptor.STARDIST)) {
            	StardistAbstract.installRequirements(consumer);
            }
		} catch (IOException | RuntimeException | MambaInstallException | ArchiveException
				| URISyntaxException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		}
        return null;
    }

    @Override
    protected void done() {
    	latch.countDown();
        if (callback != null) {
            callback.run();
        }
    }
    
    public void stopBackground() {
    	if (workerThread != null && workerThread.isAlive())
    		workerThread.interrupt();
    }
}

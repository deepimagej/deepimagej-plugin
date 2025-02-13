package deepimagej.gui.workers;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.compress.archivers.ArchiveException;

import deepimagej.gui.ContentPanel;
import deepimagej.gui.EnvironmentInstaller;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.model.stardist.StardistAbstract;

public class InstallEnvWorker extends SwingWorker<Void, Void> {

    private final EnvironmentInstaller installerPanel;
    private final Runnable callback;
    private final Thread referenceThread;

    public InstallEnvWorker(EnvironmentInstaller installerPanel, Runnable callback) {
        this.installerPanel = installerPanel;
        this.callback = callback;
    	referenceThread = installerPanel.getReferenceThread();
    }

    @Override
    protected Void doInBackground() {
    	try {
            if (installerPanel.getDescriptor().getModelFamily().equals(ModelDescriptor.STARDIST)) {
            	StardistAbstract.installRequirements(installerPanel.getConsumer());
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
    	installerPanel.getCountDownLatch().countDown();
        if (callback != null) {
            callback.run();
        }
    }
}

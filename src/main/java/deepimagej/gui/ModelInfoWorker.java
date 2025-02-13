package deepimagej.gui;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;

public class ModelInfoWorker extends SwingWorker<String, Void> {

    private ModelDescriptor model;
    private TextLoadCallback callback;

    public ModelInfoWorker(ModelDescriptor model, TextLoadCallback callback) {
        this.model = model;
        this.callback = callback;
    }

    @Override
    protected String doInBackground() throws Exception {
        // Perform the time-consuming task of generating the info text
        if (model == null) {
            // Return default text if model is null
            return ContentPanel.INSTALL_INSTRUCTIONS;
        } else {
            // Generate the info from the model
            return model.buildInfo();
        }
    }

    @Override
    protected void done() {
        try {
            String infoText = get();
            SwingUtilities.invokeLater(() -> callback.onTextLoaded(infoText));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> callback.onTextLoadFailed(e));
        }
    }

    @FunctionalInterface
    public interface TextLoadCallback {
        void onTextLoaded(String text);
        
        default void onTextLoadFailed(Exception e) {
            System.err.println("Failed to load text: " + e.getMessage());
        }
    }
}

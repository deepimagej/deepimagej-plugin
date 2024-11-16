package deepimagej.gui;

import javax.swing.SwingWorker;

import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;

public class ModelInfoWorker extends SwingWorker<String, Void> {

    private ModelDescriptor model;
    private ContentPanel contentPanel;

    public ModelInfoWorker(ModelDescriptor model, ContentPanel contentPanel) {
        this.model = model;
        this.contentPanel = contentPanel;
    }

    @Override
    protected String doInBackground() throws Exception {
        // Perform the time-consuming task of generating the info text
        if (model == null) {
            // Return default text if model is null
            return "Detailed model description...";
        } else {
            // Generate the info from the model
            return model.buildInfo();
        }
    }

    @Override
    protected void done() {
        try {
            // Retrieve the result of doInBackground()
            String infoText = get();
            // Update the GUI component on the EDT
            contentPanel.setInfo(infoText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

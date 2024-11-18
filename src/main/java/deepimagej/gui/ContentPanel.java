package deepimagej.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import deepimagej.gui.ImageLoader.ImageLoadCallback;
import deepimagej.gui.ModelInfoWorker.TextLoadCallback;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;

public class ContentPanel extends JPanel {
	
	private JLabel exampleImageLabel;
    private JEditorPane modelInfoArea;
    private JProgressBar progressBar;
	private JLabel progressInfoLabel;
	private JPanel progressLabelPanel;
	private JPanel progressPanel;
	private JScrollPane infoScrollPane;
	private CardLayout progressLabelLayout;
    private final long parentHeight;
    private final long parentWidth;

	private final static double MODEL_VRATIO = 0.4;
	private final static double PROGRESS_VRATIO = 0.2;
	private final static double INFO_VRATIO = 0.7;
	
	private static final long serialVersionUID = -7691139174208436363L;

	protected ContentPanel(int parentWidth, int parentHeight) {
		super(new GridLayout(1, 2));
        this.parentWidth = parentWidth;
        this.parentHeight= parentHeight;
        this.setBorder(new EmptyBorder(5, 5, 5, 15));
        this.setBackground(Color.WHITE);
        this.setPreferredSize(new Dimension(parentWidth, (int) (parentHeight * MODEL_VRATIO)));

        // Example Image Panel
        JPanel exampleImagePanel = new JPanel(new GridBagLayout());
        exampleImagePanel.setBackground(Color.WHITE);

        JLabel exampleTitleLabel = new JLabel("Cover Image", JLabel.CENTER);
        exampleTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (parentHeight * 0.3);
        int logoWidth = parentWidth / 3;
        ImageIcon logoIcon = DefaultIcon.getDefaultIcon(logoWidth, logoHeight);
        exampleImageLabel = new JLabel(logoIcon, JLabel.CENTER);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        exampleImagePanel.add(exampleTitleLabel, gbc);
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 10;
        exampleImagePanel.add(exampleImageLabel, gbc);

        // Model Info Panel
        JPanel modelInfoPanel = new JPanel(new GridBagLayout());
        modelInfoPanel.setBackground(Color.WHITE);

        JLabel infoTitleLabel = new JLabel("Model Information", JLabel.CENTER);
        infoTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        modelInfoArea = new JEditorPane("text/html", "Detailed model description...");
        modelInfoArea.setEditable(false);
        modelInfoArea.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        modelInfoArea.setPreferredSize(new Dimension(0, (int) (INFO_VRATIO * MODEL_VRATIO * this.parentHeight)));
        infoScrollPane = new JScrollPane(modelInfoArea);
        infoScrollPane.setMinimumSize(new Dimension(0, (int) (INFO_VRATIO * MODEL_VRATIO * this.parentHeight)));

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        
        gbc.gridy = 0;
        gbc.weighty = 1;
        modelInfoPanel.add(infoTitleLabel, gbc);
        gbc.gridy = 1;
        gbc.weighty = 10;
        gbc.fill = GridBagConstraints.BOTH;
        modelInfoPanel.add(infoScrollPane, gbc);
        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        createProgressBar();
        modelInfoPanel.add(progressPanel, gbc);

        this.add(exampleImagePanel);
        this.add(modelInfoPanel);
	}
	
	protected void setIcon(Icon icon) {
		this.exampleImageLabel.setIcon(icon);
	}
	
	protected void setInfo(String text) {
		this.modelInfoArea.setText(text);
		modelInfoArea.setCaretPosition(0);
	}
	
	private void createProgressBar() {
        // Create progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension((int) infoScrollPane.getPreferredSize().getWidth(), 
        											(int) (parentHeight * PROGRESS_VRATIO)));
        progressBar.setBackground(Color.LIGHT_GRAY);
        progressBar.setVisible(true);
        progressBar.setForeground(new Color(46, 204, 113)); // Modern green color

        // Create progress label
        progressInfoLabel = new JLabel("Example text");
        progressInfoLabel.setForeground(Color.black);
        progressInfoLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));

        // Panel to hold progress bar and label
        progressPanel = new JPanel(new GridBagLayout());
        progressPanel.setOpaque(false);

        GridBagConstraints progressBarGbc = new GridBagConstraints();
        progressBarGbc.gridx = 0;
        progressBarGbc.weightx = 1;
        progressBarGbc.insets = new Insets(2, 2, 2, 2);
        progressBarGbc.fill = GridBagConstraints.BOTH;
        

        progressBarGbc.gridy = 0;
        progressPanel.add(progressBar, progressBarGbc);
        

        progressBarGbc.gridy = 1;
        progressLabelPanel = new JPanel();
        progressLabelLayout = new CardLayout();
        progressLabelPanel.setLayout(progressLabelLayout);
        progressLabelPanel.add(progressInfoLabel, "visible");
        progressLabelPanel.add(new JPanel(), "invisible");
        progressLabelLayout.show(progressLabelPanel, "invisible");
        progressPanel.add(progressLabelPanel, progressBarGbc);
	}
	
	protected void setDeterminatePorgress(int progress) {
		this.progressBar.setValue(progress);
		progressBar.setStringPainted(true);
		progressBar.setString(progress + "%");
	}
	
	protected void setProgressIndeterminate(boolean indeterminate) {
		this.progressBar.setIndeterminate(indeterminate);
	}
	
	protected void setProgressBarText(String text) {
		this.progressBar.setString(text);
	}
	
	protected int getProgress() {
		return this.progressBar.getValue();
	}
	
	protected String getProgressBarText() {
		return this.progressBar.getString();
	}
	
	protected void setProgressLabelText(String text) {
		if (text == null || text.equals("")) {
			progressLabelLayout.show(progressLabelPanel, "invisible");
			return;
		}
		this.progressInfoLabel.setText(text);
		progressLabelLayout.show(progressLabelPanel, "visible");
	}

	protected void update(ModelDescriptor modelDescriptor, URL path, int logoWidth, int logoHeight) {
    	DefaultIcon.getLoadingIconWithCallback(logoWidth, logoHeight, icon -> {
    		setIcon(icon);
            revalidate();
            repaint();
        });
    	ModelSelectionPanel.ICONS_DISPLAYED.put("main", path);
    	TextLoadCallback callback = new TextLoadCallback() {
    	    @Override
    	    public void onTextLoaded(String infoText) {
    	        if (!ModelSelectionPanel.ICONS_DISPLAYED.get("main").equals(path)) {
    	            return;
    	        }
                setInfo(infoText);
    	        revalidate();
    	        repaint();
    	    }
    	};
        ModelInfoWorker worker = new ModelInfoWorker(modelDescriptor, callback);
        worker.execute();
    	ImageLoader.loadImageIconFromURL(path, logoWidth, logoHeight, new ImageLoadCallback() {
            @Override
            public void onImageLoaded(ImageIcon icon) {
            	if (ModelSelectionPanel.ICONS_DISPLAYED.get("main") != path)
            		return;
            	setIcon(icon);
            	revalidate();
            	repaint();
            }
        });
	}
}

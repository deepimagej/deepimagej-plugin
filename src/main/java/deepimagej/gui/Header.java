package deepimagej.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.Objects;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.engine.installation.EngineInstall;

public class Header extends JPanel {
	
	private final String title;
	private final String subtitle;
	private JLabel logoLabel;
	private JLabel titleLabel;
	private JLabel subtitleLabel;
	private JPanel textPanel;
	private JPanel wrapperPanel;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private final long parentHeight;
    private final long parentWidth;

	private final static double TITLE_VRATIO = 0.1;
	private final static double TITLE_HRATIO = 1;
	private final static double LOGO_VRATIO = 1;
	private final static double LOGO_HRATIO = 1.0 / 7;
    private final static double PROGRESS_BAR_WIDTH_RATIO = 0.15;
	
	private static final long serialVersionUID = -7691139174208436363L;

	protected Header(String title, String subtitle, int parentWidth, int parentHeight) {
        super(new GridBagLayout());
        this.parentWidth = parentWidth;
        this.parentHeight= parentHeight;
		this.title = title;
		this.subtitle = subtitle;
        this.setBackground(Color.GRAY);
        this.setBorder(new LineBorder(Color.BLACK, 2, true));
        this.setPreferredSize(new Dimension(parentWidth, (int) (parentHeight * TITLE_VRATIO)));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (parentHeight * TITLE_VRATIO * LOGO_VRATIO);
        int logoWidth = (int) (parentHeight * TITLE_HRATIO * LOGO_HRATIO);

        // Create logo label with the specified size
        ImageIcon logoIcon = Gui.createScaledIcon(getClass().getClassLoader().getResource(Gui.DIJ_ICON_PATH), logoWidth, logoHeight);
        logoLabel = new JLabel(logoIcon);

        // Title label
        titleLabel = new JLabel(this.title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Subtitle label
        subtitleLabel = new JLabel(this.subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Panel for title and subtitle
        textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        // Create a wrapper panel to hold logo and textPanel inline
        wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setOpaque(false);

        // GridBagConstraints for the logo
        GridBagConstraints logoGbc = new GridBagConstraints();
        logoGbc.gridx = 0;
        logoGbc.gridy = 0;
        logoGbc.anchor = GridBagConstraints.WEST;
        logoGbc.insets = new Insets(0, 0, 0, this.getWidth() / 80);
        wrapperPanel.add(logoLabel, logoGbc);

        // GridBagConstraints for the text panel
        GridBagConstraints textGbc = new GridBagConstraints();
        textGbc.gridx = 1;
        textGbc.gridy = 0;
        textGbc.anchor = GridBagConstraints.WEST;
        wrapperPanel.add(textPanel, textGbc);
        JPanel progressPanel = createProgressBar();

        // Add the wrapperPanel to the titlePanel with custom constraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 3;
        gbc.weighty = 1;
        //gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, (int) (-logoWidth + progressPanel.getPreferredSize().getWidth()), 0, 0);
        this.add(wrapperPanel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 0, 5, 15);
        this.add(progressPanel, gbc);
	}
	
	private JPanel createProgressBar() {
        // Create progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension((int)(parentWidth * PROGRESS_BAR_WIDTH_RATIO), 30));
        progressBar.setBackground(Color.LIGHT_GRAY);
        progressBar.setVisible(true);
        progressBar.setForeground(new Color(46, 204, 113)); // Modern green color

        // Create progress label
        progressLabel = new JLabel("Processing...");
        progressLabel.setForeground(Color.WHITE);
        progressLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Panel to hold progress bar and label
        JPanel progressPanel = new JPanel(new GridBagLayout());
        progressPanel.setPreferredSize(new Dimension((int)(parentWidth * PROGRESS_BAR_WIDTH_RATIO), 60));
        progressPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        progressPanel.add(progressLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        progressPanel.add(progressBar, gbc);
		progressBar.setVisible(false);
		progressLabel.setVisible(false);
        
        return progressPanel;
	}
    
    public void trackEngineInstallation(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
    	if (consumersMap.keySet().size() == 0)
    		return;
    	SwingUtilities.invokeLater(() -> {
            progressBar.setStringPainted(true);
    		progressBar.setString("0%");
    		progressBar.setValue(0);
    		progressLabel.setText("Preparing engines download...");
    		progressBar.setVisible(true);
    		progressLabel.setVisible(true);
    	});
		if (!checkDownloadStarted(consumersMap))
			return;
		SwingUtilities.invokeLater(() -> progressLabel.setText("Installing DL engines..."));
		trackProgress(consumersMap);
    	
    }

    private void trackProgress(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
    	double total = consumersMap.entrySet().stream()
				.map(ee -> ee.getValue().get().get("total" + EngineInstall.NBYTES_SUFFIX))
				.filter(Objects::nonNull).mapToDouble(value -> (Double) value).sum();
    	while (isEDTAlive()) {
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
        	double progress = consumersMap.entrySet().stream()
    				.map(ee -> {
    					Double totalPerc = ee.getValue().get().get("total");
    					totalPerc = totalPerc == null ? 0 : totalPerc;
    					return ee.getValue().get().get("total" + EngineInstall.NBYTES_SUFFIX) * totalPerc;
    				})
    				.filter(Objects::nonNull).mapToDouble(value -> (Double) value).sum();
    		long perc = Math.round(100 * progress / total);
        	SwingUtilities.invokeLater(() -> {
        		progressBar.setString(perc + "%");
        		progressBar.setValue((int) perc);
        	});
        	if (perc == 100) {
            	SwingUtilities.invokeLater(() -> {
            		progressBar.setVisible(false);
            		progressLabel.setVisible(false);
            	});
        		return;
        	}
    	}
    }
    
    
    private static boolean checkDownloadStarted(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
		boolean startedDownload = false;
    	while (!startedDownload && isEDTAlive()) {
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return false;
			}
    		startedDownload = consumersMap.entrySet().stream()
    				.filter(ee -> ee.getValue().get().get("total") != null).findAny().orElse(null) != null;
    	}
    	return startedDownload;
    }
    
    private static boolean isEDTAlive() {
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

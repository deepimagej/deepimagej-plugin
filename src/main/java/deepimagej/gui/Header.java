package deepimagej.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
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
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JPanel progressPanelCard;
    private CardLayout cardLayout;
    private final long parentHeight;
    private final long parentWidth;

	private final static double TITLE_VRATIO = 0.1;
	private final static double TITLE_HRATIO = 1;
	private final static double LOGO_VRATIO = 1;
	private final static double LOGO_HRATIO = 1.0 / 7;
    private final static double PROGRESS_BAR_WIDTH_RATIO = 0.25;
	
	private static final long serialVersionUID = -7691139174208436363L;

	protected Header(String title, String subtitle, int parentWidth, int parentHeight) {
        super(new GridBagLayout());
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
        this.title = title;
        this.subtitle = subtitle;
        this.setBackground(Color.GRAY);
        this.setBorder(new LineBorder(Color.BLACK, 2, true));
        this.setPreferredSize(new Dimension(parentWidth, (int) (parentHeight * TITLE_VRATIO)));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (parentHeight * TITLE_VRATIO * LOGO_VRATIO);
        int logoWidth = (int) (parentHeight * TITLE_HRATIO * LOGO_HRATIO);

        // Create logo label with the specified size
        ImageIcon logoIcon = DefaultIcon.getDefaultIcon(logoWidth, logoHeight);
        JLabel logoLabel = new JLabel(logoIcon);

        // Title label
        JLabel titleLabel = new JLabel(this.title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Subtitle label
        JLabel subtitleLabel = new JLabel(this.subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Panel for title and subtitle
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        // Create a wrapper panel to hold logo and textPanel inline
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
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

        // Create progress panel
        JPanel progressPanel = createProgressBar();
        progressPanelCard = new JPanel();
        cardLayout = new CardLayout();
        progressPanelCard.setLayout(cardLayout);
        progressPanelCard.add(progressPanel, "visible");
        progressPanelCard.add(new JPanel() {{ setOpaque(false); }}, "invisible");
        cardLayout.show(progressPanelCard, "invisible");
        progressPanel.setPreferredSize(new Dimension((int)(this.parentWidth * PROGRESS_BAR_WIDTH_RATIO), (int) this.parentHeight));
        progressPanelCard.setOpaque(false);
        
        // Add components to the main panel with modified constraints
        GridBagConstraints gbc = new GridBagConstraints();
        // Empty space on the left to balance the progress panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = PROGRESS_BAR_WIDTH_RATIO;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JPanel() {{ setOpaque(false); }}, gbc);
        
        // Wrapper panel (logo + text) in center
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        this.add(wrapperPanel, gbc);
        
        // Progress panel on right
        gbc.gridx = 2;
        gbc.weightx = PROGRESS_BAR_WIDTH_RATIO;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(5, 20, 5, 20);
        this.add(progressPanelCard, gbc);
	}
	
	private JPanel createProgressBar() {
        // Create progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setBackground(Color.LIGHT_GRAY);
        progressBar.setForeground(new Color(46, 204, 113)); // Modern green color
        progressBar.setPreferredSize(new Dimension(
                (int)(parentWidth * PROGRESS_BAR_WIDTH_RATIO), 25)); 

        // Create progress label
        progressLabel = new JLabel("Processing...");
        //progressLabel.setForeground(Color.WHITE);
        //progressLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        progressLabel.setBackground(Color.gray);
        progressLabel.setOpaque(true);
        progressLabel.setMinimumSize(new Dimension((int) (100), progressLabel.getPreferredSize().height));
        progressLabel.setPreferredSize(new Dimension((int) (100), progressLabel.getPreferredSize().height));
        progressLabel.setMaximumSize(new Dimension((int) (100), progressLabel.getPreferredSize().height));
        progressLabel.setHorizontalAlignment(JLabel.CENTER);

        // Panel to hold progress bar and label
        JPanel progressPanel = new JPanel(new GridBagLayout());
        progressPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0; // Prevent horizontal expansion
        gbc.weighty = 0;
        progressPanel.add(progressLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Allow horizontal fill
        progressPanel.add(progressBar, gbc);
        
        return progressPanel;
	}
    
    public void trackEngineInstallation(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
    	if (consumersMap.keySet().size() == 0)
    		return;
    	SwingUtilities.invokeLater(() -> {
    		progressBar.setIndeterminate(true);
            progressBar.setStringPainted(true);
    		progressBar.setString("0%");
    		progressLabel.setText("Preparing installation...");
    		cardLayout.show(progressPanelCard, "visible");

    	});
		if (!checkDownloadStarted(consumersMap))
			return;
		SwingUtilities.invokeLater(() -> {
    		progressBar.setIndeterminate(false);
    		progressLabel.setText("Installing DL engines...");
		});
		trackProgress(consumersMap);
    }

    private void trackProgress(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
    	double total = consumersMap.entrySet().stream()
				.map(ee -> ee.getValue().get().get("total" + EngineInstall.NBYTES_SUFFIX))
				.filter(Objects::nonNull).mapToDouble(value -> (Double) value).sum();
    	while (GuiUtils.isEDTAlive()) {
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
        	Entry<String, TwoParameterConsumer<String, Double>> entry = consumersMap.entrySet()
        			.stream().filter(ee -> ee.getValue().get().get("total") != null 
        								&& ee.getValue().get().get("total") != 0 && ee.getValue().get().get("total") != 1)
        			.findFirst().orElse(null);
    		double perc = Math.floor(100 * progress / total);
        	SwingUtilities.invokeLater(() -> {
        		progressBar.setString(perc + "%");
        		progressBar.setValue((int) perc);
        		if (entry != null)
        			this.progressLabel.setText("Installing " + new File(entry.getKey()).getName());
        	});
        	if (perc == 100) {
            	SwingUtilities.invokeLater(() -> cardLayout.show(progressPanelCard, "invisible"));
        		return;
        	}
    	}
    } 
    
    private static boolean checkDownloadStarted(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
		boolean startedDownload = false;
    	while (!startedDownload && GuiUtils.isEDTAlive()) {
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

}

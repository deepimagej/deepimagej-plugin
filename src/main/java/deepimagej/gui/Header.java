package deepimagej.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.LineBorder;

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
    private final static double PROGRESS_BAR_WIDTH_RATIO = 0.25;
	
	private static final long serialVersionUID = -7691139174208436363L;

	protected Header(String title, String subtitle, int parentWidth, int parentHeight) {
        super(new GridBagLayout());
        this.parentWidth = parentWidth;
        this.parentHeight= parentHeight;
		this.title = title;
		this.subtitle = subtitle;
        this.setBackground(Color.GRAY);
        this.setBorder(new LineBorder(Color.BLACK, 5, true));
        this.setSize(new Dimension(parentWidth, (int) (parentHeight * TITLE_VRATIO)));

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
        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.gridx = 0;
        wrapperGbc.gridy = 0;
        wrapperGbc.weightx = 3;
        wrapperGbc.weighty = 1;
        wrapperGbc.anchor = GridBagConstraints.CENTER;
        wrapperGbc.insets = new Insets(0, (int) (-logoWidth + progressPanel.getPreferredSize().getWidth()), 0, 0);
        this.add(wrapperPanel, wrapperGbc);
        
        GridBagConstraints progressGbc = new GridBagConstraints();
        progressGbc.gridx = 1;
        progressGbc.gridy = 0;
        wrapperGbc.weightx = 1;
        wrapperGbc.weighty = 1;
        progressGbc.insets = new Insets(20, 0, 10, 10);
        this.add(progressPanel, progressGbc);
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
        progressPanel.setOpaque(false);

        GridBagConstraints progressLabelGbc = new GridBagConstraints();
        progressLabelGbc.gridx = 0;
        progressLabelGbc.gridy = 0;
        progressLabelGbc.anchor = GridBagConstraints.CENTER;
        progressPanel.add(progressLabel, progressLabelGbc);

        GridBagConstraints progressBarGbc = new GridBagConstraints();
        progressBarGbc.gridx = 0;
        progressBarGbc.gridy = 1;
        progressBarGbc.anchor = GridBagConstraints.CENTER;
        progressPanel.add(progressBar, progressBarGbc);
        
        return progressPanel;
	}

}

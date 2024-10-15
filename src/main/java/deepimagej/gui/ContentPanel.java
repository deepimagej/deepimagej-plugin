package deepimagej.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public class ContentPanel extends JPanel {
	
	private JLabel exampleImageLabel;
    private JTextArea modelInfoArea;
    private JProgressBar progressBar;
	private JLabel progressInfoLabel;
	private JScrollPane infoScrollPane;
    private final long parentHeight;
    private final long parentWidth;

	private final static double MODEL_VRATIO = 0.4;
	private final static double PROGRESS_VRATIO = 0.01;
	private final static double PROGRESS_HRATIO = 0.5;
	
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

        JLabel exampleTitleLabel = new JLabel("Example Image", JLabel.CENTER);
        exampleTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (parentHeight * 0.3);
        int logoWidth = parentWidth / 3;
        ImageIcon logoIcon = ImageLoader.getDefaultIcon(logoWidth, logoHeight);
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

        modelInfoArea = new JTextArea("Detailed model description...");
        modelInfoArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        modelInfoArea.setLineWrap(true);
        modelInfoArea.setWrapStyleWord(true);
        modelInfoArea.setEditable(false);
        infoScrollPane = new JScrollPane(modelInfoArea);
        infoScrollPane.setPreferredSize(new Dimension((int) (this.parentWidth * 0.5), (int) (this.parentHeight * 0.3)));

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        
        gbc.gridy = 0;
        gbc.weighty = 1;
        modelInfoPanel.add(infoTitleLabel, gbc);
        gbc.gridy = 1;
        gbc.weighty = 10;
        modelInfoPanel.add(infoScrollPane, gbc);
        gbc.gridy = 2;
        gbc.weighty = 2;
        modelInfoPanel.add(createProgressBar(), gbc);

        this.add(exampleImagePanel);
        this.add(modelInfoPanel);
	}
	
	protected void setIcon(Icon icon) {
		this.exampleImageLabel.setIcon(icon);
	}
	
	protected void setInfo(String text) {
		this.modelInfoArea.setText(text);
	}
	
	private JPanel createProgressBar() {
        // Create progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension((int) infoScrollPane.getPreferredSize().getWidth(), 
        											(int) (parentHeight * PROGRESS_VRATIO)));
        progressBar.setBackground(Color.LIGHT_GRAY);
        progressBar.setVisible(true);
        progressBar.setForeground(new Color(46, 204, 113)); // Modern green color

        // Create progress label
        progressInfoLabel = new JLabel("");
        progressInfoLabel.setForeground(Color.WHITE);
        progressInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Panel to hold progress bar and label
        JPanel progressPanel = new JPanel(new GridBagLayout());
        progressPanel.setOpaque(false);

        GridBagConstraints progressBarGbc = new GridBagConstraints();
        progressBarGbc.gridx = 0;
        progressBarGbc.weightx = 1;
        progressBarGbc.insets = new Insets(2, 2, 2, 2);
        progressBarGbc.fill = GridBagConstraints.BOTH;
        

        progressBarGbc.gridy = 0;
        progressPanel.add(progressBar, progressBarGbc);
        

        progressBarGbc.gridy = 1;
        progressPanel.add(progressInfoLabel, progressBarGbc);
        
        return progressPanel;
	}
	
	protected void setProgressIndeterminate(boolean indeterminate) {
		this.progressBar.setIndeterminate(indeterminate);
	}
	
	protected void setProgressText(String text) {
		this.progressInfoLabel.setText(text);
	}
}

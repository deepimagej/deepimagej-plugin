package deepimagej.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public class ContentPanel extends JPanel {
	
	private JLabel exampleImageLabel;
    private JTextArea modelInfoArea;
    private final long parentHeight;
    private final long parentWidth;

	private final static double MODEL_VRATIO = 0.4;
	
	private static final long serialVersionUID = -7691139174208436363L;

	protected ContentPanel(int parentWidth, int parentHeight) {
		super(new GridLayout(1, 2, 20, 0));
        this.parentWidth = parentWidth;
        this.parentHeight= parentHeight;
        this.setBorder(new EmptyBorder(20, 20, 20, 20));
        this.setBackground(Color.WHITE);
        this.setPreferredSize(new Dimension(parentWidth, (int) (parentHeight * MODEL_VRATIO)));

        // Example Image Panel
        JPanel exampleImagePanel = new JPanel(new BorderLayout());
        exampleImagePanel.setBackground(Color.WHITE);

        JLabel exampleTitleLabel = new JLabel("Example Image", JLabel.CENTER);
        exampleTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (parentHeight * 0.3);
        int logoWidth = parentWidth / 3;
        ImageIcon logoIcon = Gui.createScaledIcon(getClass().getClassLoader().getResource(Gui.DIJ_ICON_PATH), logoWidth, logoHeight);
        exampleImageLabel = new JLabel(logoIcon, JLabel.CENTER);
        exampleImagePanel.add(exampleTitleLabel, BorderLayout.NORTH);
        exampleImagePanel.add(exampleImageLabel, BorderLayout.CENTER);

        // Model Info Panel
        JPanel modelInfoPanel = new JPanel(new BorderLayout());
        modelInfoPanel.setBackground(Color.WHITE);

        JLabel infoTitleLabel = new JLabel("Model Information", JLabel.CENTER);
        infoTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        modelInfoArea = new JTextArea("Detailed model description...");
        modelInfoArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        modelInfoArea.setLineWrap(true);
        modelInfoArea.setWrapStyleWord(true);
        modelInfoArea.setEditable(false);
        JScrollPane infoScrollPane = new JScrollPane(modelInfoArea);

        modelInfoPanel.add(infoTitleLabel, BorderLayout.NORTH);
        modelInfoPanel.add(infoScrollPane, BorderLayout.CENTER);

        this.add(exampleImagePanel);
        this.add(modelInfoPanel);
	}
	
	protected void setIcon(Icon icon) {
		this.exampleImageLabel.setIcon(icon);
	}
	
	protected void setInfo(String text) {
		this.modelInfoArea.setText(text);
	}
}

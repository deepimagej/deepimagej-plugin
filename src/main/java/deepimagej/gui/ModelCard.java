package deepimagej.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import deepimagej.gui.ImageLoader.ImageLoadCallback;

public class ModelCard extends JPanel {
    

    private static final long serialVersionUID = -5625832740571130175L;

    private JLabel nameLabel;
    private JLabel imageLabel;
    private JLabel nicknameLabel;
    
    private long cardWidth;
    private long cardHeight;
    private final double scale;
    

    private static double CARD_ICON_VRATIO = 0.8;
    private static double CARD_ICON_HRATIO = 0.9;

	private ModelCard(long cardWidth, long cardHeight, double scale) {
    	super(new BorderLayout());
    	this.scale = scale;
    	this.cardWidth = cardWidth;
    	this.cardHeight = cardHeight;
        this.setPreferredSize(new Dimension((int) (cardWidth * scale), (int) (cardHeight * scale)));
        this.setBackground(Color.WHITE);
        this.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        /*URL imagePath = ModelCard.class.getClassLoader().getResource(Gui.DIJ_ICON_PATH);
        ImageIcon logoIcon = Gui.createScaledIcon(imagePath, 
        		(int) (CARD_ICON_HRATIO * cardWidth * scale), (int) (cardHeight * CARD_ICON_VRATIO * scale));
        		*/
        Icon logoIcon = createEmptyIcon((int) (CARD_ICON_HRATIO * cardWidth * scale), (int) (cardHeight * CARD_ICON_VRATIO * scale));
        this.imageLabel = new JLabel(logoIcon, JLabel.CENTER);

        this.nameLabel = new JLabel(Gui.LOADING_STR, JLabel.CENTER);
        this.nameLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (16 * scale)));
        this.nicknameLabel = new JLabel(Gui.LOADING_STR, JLabel.CENTER);
        this.nicknameLabel.setFont(new Font("SansSerif", Font.ITALIC, (int) (14 * scale)));

        this.add(this.nameLabel, BorderLayout.NORTH);
        this.add(this.imageLabel, BorderLayout.CENTER);
        this.add(this.nicknameLabel, BorderLayout.SOUTH);
    }

    protected static ModelCard createModelCard(long cardWidth, long cardHeight, double scale) {
    	ModelCard modelCardPanel = new ModelCard(cardWidth, cardHeight, scale);
        return modelCardPanel;
    }
    
    protected void updateCard(String name, String nickname, URL imagePath) {
    	this.nameLabel.setText(name);
    	this.nicknameLabel.setText(nickname);
    	int iconW = (int) (CARD_ICON_HRATIO * this.cardWidth * scale);
    	int iconH = (int) (this.cardHeight * CARD_ICON_VRATIO * scale);
    	boolean isFile = false;
    	try {
    		isFile = new File(imagePath.toURI()).isFile();
    	} catch (Exception ex) {}
    	
    	if (isFile) {
            ImageIcon logoIcon = Gui.createScaledIcon(imagePath, iconW, iconH);
            imageLabel.setIcon(logoIcon);
            this.revalidate();
            this.repaint();
    	} else {
    		ImageLoader.loadImageIconFromURL(imagePath, iconW, iconH, new ImageLoadCallback() {
                @Override
                public void onImageLoaded(ImageIcon icon) {
                	imageLabel.setIcon(icon);
                    ModelCard.this.revalidate();
                    ModelCard.this.repaint();
                }
            });
    	}
    }
    
    private static ImageIcon createEmptyIcon(int width, int height) {
        // Create a transparent BufferedImage of the specified size
        BufferedImage emptyImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Create an ImageIcon from the empty image
        return new ImageIcon(emptyImage);
    }

    /*
    protected static ModelCard createModelCarwd(String modelName, URL imagePath, String modelNickname, double scale) {
    	ModelCard modelCardPanel = new ModelCard(new BorderLayout());
        int cardHeight = (int) (getHeight() * CARR_VRATIO * 0.9);
        int cardWidth = getWidth() / 3;
        modelCardPanel.setPreferredSize(new Dimension((int) (cardWidth * scale), (int) (cardHeight * scale)));
        modelCardPanel.setBackground(Color.WHITE);
        modelCardPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (getHeight() * CARR_VRATIO / 3);
        int logoWidth = getWidth() / 4;
        ImageIcon logoIcon = createScaledIcon(imagePath, logoWidth, logoHeight);
        JLabel modelImageLabel = new JLabel(logoIcon, JLabel.CENTER);

        JLabel modelNameLabel = new JLabel(modelName, JLabel.CENTER);
        modelNameLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (16 * scale)));
        JLabel modelNicknameLabel = new JLabel(modelNickname, JLabel.CENTER);
        modelNicknameLabel.setFont(new Font("SansSerif", Font.ITALIC, (int) (14 * scale)));

        modelCardPanel.add(modelImageLabel, BorderLayout.CENTER);
        modelCardPanel.add(modelNameLabel, BorderLayout.NORTH);
        modelCardPanel.add(modelNicknameLabel, BorderLayout.SOUTH);

        return modelCardPanel;
    }
    */
}

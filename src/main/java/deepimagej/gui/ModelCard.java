package deepimagej.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import deepimagej.gui.workers.ImageLoaderWorker;
import deepimagej.gui.workers.ImageLoaderWorker.ImageLoadCallback;

public class ModelCard extends JPanel {
    

    private static final long serialVersionUID = -5625832740571130175L;

    private JLabel nameLabel;
    private JLabel imageLabel;
    private JLabel nicknameLabel;
    
    private long cardWidth;
    private long cardHeight;
    private String id;
    private ImageLoaderWorker worker;
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
	
	/**
	 * Set an optional id
	 * @param id
	 * 	the identifier of the card
	 */
	public void setOptionalID(String id) {
		this.id = id;
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
    	
    	DefaultIcon.getLoadingIconWithCallback(iconW, iconH, icon -> {
    		imageLabel.setIcon(icon);
            revalidate();
            repaint();
        });
    	if (worker != null && !worker.isDone())
    		worker.cancelBackground();

    	ImageLoadCallback callback = new ImageLoadCallback() {
            @Override
            public void onImageLoaded(ImageIcon icon) {
            	if (ModelSelectionPanel.ICONS_DISPLAYED.get(id) != imagePath)
            		return;
            	imageLabel.setIcon(icon);
                ModelCard.this.revalidate();
                ModelCard.this.repaint();
            }
        };
    	worker = ImageLoaderWorker.create(imagePath, iconW, iconH, callback);
    	worker.execute();
    }
    
    private static ImageIcon createEmptyIcon(int width, int height) {
        // Create a transparent BufferedImage of the specified size
        BufferedImage emptyImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Create an ImageIcon from the empty image
        return new ImageIcon(emptyImage);
    }
}

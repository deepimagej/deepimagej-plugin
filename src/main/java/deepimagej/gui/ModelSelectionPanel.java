package deepimagej.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;

public class ModelSelectionPanel extends JPanel {

	private int currentIndex = 1;
    private JPanel modelCarouselPanel;
    private ModelCard prevModelPanel;
    private ModelCard selectedModelPanel;
    private ModelCard nextModelPanel;

    private static final double CARR_VRATIO = 0.34;
    private static final double SELECTION_PANE_VRATIO = 0.35;
    private static final double ARROWS_VRATIO = 0.1;
    protected static final double MAIN_CARD_RT = 1;
    protected static final double SECOND_CARD_RT = 0.8;

	protected ModelSelectionPanel(int parentWidth, int parentHeight) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBackground(new Color(236, 240, 241));
        Border lineBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray, 2, true), 
        													"Local");
        Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5); // 10-pixel padding around the content
        this.setBorder(BorderFactory.createCompoundBorder(paddingBorder,lineBorder));
        this.setSize(new Dimension(getWidth(), (int) (getHeight() * SELECTION_PANE_VRATIO)));

        modelCarouselPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        modelCarouselPanel.setBackground(new Color(236, 240, 241));
        modelCarouselPanel.setSize(new Dimension(getWidth(), (int) (this.getHeight() * CARR_VRATIO)));
        
        int cardHeight = (int) (getHeight() * CARR_VRATIO * 0.9);
        int cardWidth = getWidth() / 3;
        prevModelPanel = ModelCard.createModelCard(cardWidth, cardHeight, SECOND_CARD_RT);
        selectedModelPanel = ModelCard.createModelCard(cardWidth, cardHeight, MAIN_CARD_RT);
        nextModelPanel = ModelCard.createModelCard(cardWidth, cardHeight, SECOND_CARD_RT);

        modelCarouselPanel.add(prevModelPanel);
        modelCarouselPanel.add(selectedModelPanel);
        modelCarouselPanel.add(nextModelPanel);

        JButton prevButton = new JButton("◀");
        prevButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        prevButton.addActionListener(e -> updateCarousel(-1));
        prevButton.setPreferredSize(new Dimension(this.getWidth() / 2, (int) (getHeight() * SELECTION_PANE_VRATIO * ARROWS_VRATIO)));

        JButton nextButton = new JButton("▶");
        nextButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        nextButton.addActionListener(e -> updateCarousel(1));
        nextButton.setPreferredSize(new Dimension(this.getWidth() / 2, (int) (getHeight() * SELECTION_PANE_VRATIO * ARROWS_VRATIO)));

        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.X_AXIS));
        navigationPanel.setPreferredSize(new Dimension(this.getWidth(), (int) (getHeight() * SELECTION_PANE_VRATIO * ARROWS_VRATIO)));
        navigationPanel.setBackground(new Color(236, 240, 241));
        navigationPanel.add(prevButton);
        navigationPanel.add(Box.createHorizontalGlue());
        navigationPanel.add(nextButton);

        this.add(modelCarouselPanel);
        this.add(navigationPanel);
	}
    
    private void setCardsData() {
    	this.modelNames = models.stream().map(mm -> mm.getName()).collect(Collectors.toList());
    	this.modelNicknames = models.stream().map(mm -> mm.getNickname()).collect(Collectors.toList());
    	this.modelImagePaths = models.stream().map(mm -> {
    		if (mm.getCovers() == null || mm.getCovers().size() == 0) return this.getClass().getClassLoader().getResource(DIJ_ICON_PATH);
    		File imFile = new File(mm.getCovers().get(0));
    		if (!imFile.exists())
    			imFile = new File(mm.getModelPath() + File.separator + mm.getCovers().get(0));
    		if (!imFile.exists()) 
    			return this.getClass().getClassLoader().getResource(DIJ_ICON_PATH);
    		try {
				return imFile.toURI().toURL();
			} catch (MalformedURLException e) {
				return this.getClass().getClassLoader().getResource(DIJ_ICON_PATH);
			}
    	}).collect(Collectors.toList());
    }
    
    public void updateCards(List<ModelDescriptor> models) {
    	this.models = models;
    	setCardsData();
    	currentIndex = 0;
        redrawModelCards();
    }

    private void updateCarousel(int direction) {
        currentIndex = getWrappedIndex(currentIndex + direction);

        redrawModelCards();
        
        // Update example image and model info
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = createScaledIcon(modelImagePaths.get(currentIndex), logoWidth, logoHeight);
        // TODO contentPanel.setIcon(logoIcon);
        // TODO contentPanel.setInfo("Detailed information for " + modelNames.get(currentIndex));
    }
    
    private void redrawModelCards() {
        prevModelPanel.updateCard(modelNames.get(getWrappedIndex(currentIndex - 1)),
                modelNicknames.get(getWrappedIndex(currentIndex - 1)),
                modelImagePaths.get(getWrappedIndex(currentIndex - 1)));
        selectedModelPanel.updateCard(modelNames.get(currentIndex),
                modelNicknames.get(currentIndex),
                modelImagePaths.get(currentIndex));
        nextModelPanel.updateCard(modelNames.get(getWrappedIndex(currentIndex + 1)),
                modelNicknames.get(getWrappedIndex(currentIndex + 1)),
                modelImagePaths.get(getWrappedIndex(currentIndex + 1)));

        modelCarouselPanel.revalidate();
        modelCarouselPanel.repaint();
    }

    private int getWrappedIndex(int index) {
        int size = modelNames.size();
        return (index % size + size) % size;
    }
}

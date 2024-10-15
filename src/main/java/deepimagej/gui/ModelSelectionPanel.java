package deepimagej.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;

public class ModelSelectionPanel extends JPanel {

	private static final long serialVersionUID = 6264134076603842497L;
	
    private final long parentHeight;
    private final long parentWidth;
    private JPanel modelCarouselPanel;
    private ModelCard prevModelPanel;
    private ModelCard selectedModelPanel;
    private ModelCard nextModelPanel;
    protected JButton nextButton;
    protected JButton prevButton;
    

    private List<String> modelNames;
    private List<String> modelNicknames;
    private List<URL> modelImagePaths;
    private List<ModelDescriptor> models;

    private static final double CARD_VRATIO = 0.8;
    private static final double CARD_HRATIO = 0.33;
    private static final double CARR_VRATIO = 0.95;
    private static final double SELECTION_PANE_VRATIO = 0.35;
    private static final double ARROWS_VRATIO = 0.05;
    protected static final double MAIN_CARD_RT = 1;
    protected static final double SECOND_CARD_RT = 0.8;

    private static final String LOCAL_STR = "Local";
    private static final String BMZ_STR = "Bioimage.io";

	protected ModelSelectionPanel(int parentWidth, int parentHeight) {
        super(new GridBagLayout());
        this.parentWidth = parentWidth;
        this.parentHeight= parentHeight;
        this.setBackground(new Color(236, 240, 241));
        Border lineBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray, 2, true), 
        		LOCAL_STR);
        Border paddingBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        this.setBorder(BorderFactory.createCompoundBorder(paddingBorder,lineBorder));
        this.setPreferredSize(new Dimension(parentWidth, (int) (parentHeight * SELECTION_PANE_VRATIO)));

        modelCarouselPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        modelCarouselPanel.setBackground(new Color(236, 240, 241));
        modelCarouselPanel.setPreferredSize(new Dimension(parentWidth, (int) (parentHeight * SELECTION_PANE_VRATIO * CARR_VRATIO)));
        
        int cardHeight = (int) (this.parentHeight * SELECTION_PANE_VRATIO * CARD_VRATIO);
        int cardWidth = (int) (parentWidth * CARD_HRATIO);
        prevModelPanel = ModelCard.createModelCard(cardWidth, cardHeight, SECOND_CARD_RT);
        selectedModelPanel = ModelCard.createModelCard(cardWidth, cardHeight, MAIN_CARD_RT);
        nextModelPanel = ModelCard.createModelCard(cardWidth, cardHeight, SECOND_CARD_RT);

        modelCarouselPanel.add(prevModelPanel);
        modelCarouselPanel.add(selectedModelPanel);
        modelCarouselPanel.add(nextModelPanel);

        int btnWidth = (int) (this.parentWidth / 2);
        int btnHeight = (int) (this.parentHeight * SELECTION_PANE_VRATIO * ARROWS_VRATIO);
        prevButton = new JButton("◀");
        prevButton.setFont(new Font("SansSerif", Font.BOLD, 10));
        prevButton.setPreferredSize(new Dimension(btnWidth, btnHeight));

        nextButton = new JButton("▶");
        nextButton.setFont(new Font("SansSerif", Font.BOLD, 10));
        nextButton.setPreferredSize(new Dimension(btnWidth, btnHeight));

        JPanel navigationPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcb = new GridBagConstraints();
        gbcb.gridy = 0;
        gbcb.weightx = 1;
        gbcb.weighty = 1;
        gbcb.fill = GridBagConstraints.HORIZONTAL;
        
        navigationPanel.setPreferredSize(new Dimension(parentWidth, btnHeight));
        navigationPanel.setBackground(new Color(236, 240, 241));
        gbcb.gridx = 0;
        navigationPanel.add(prevButton, gbcb);
        gbcb.gridx = 1;
        navigationPanel.add(nextButton, gbcb);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 10;
        
        gbc.gridy = 0;
        gbc.weighty = 20;
        this.add(modelCarouselPanel, gbc);
        gbc.gridy = 1;
        gbc.weighty = 0;
        this.add(navigationPanel, gbc);
	}
    
    private void setCardsData() {
    	this.modelNames = models.stream().map(mm -> mm.getName()).collect(Collectors.toList());
    	this.modelNicknames = models.stream().map(mm -> mm.getNickname()).collect(Collectors.toList());
    	this.modelImagePaths = models.stream().map(mm -> {
    		if (mm.getCovers() == null || mm.getCovers().size() == 0) return this.getClass().getClassLoader().getResource(Gui.DIJ_ICON_PATH);
    		File imFile = new File(mm.getCovers().get(0));
    		if (!imFile.exists())
    			imFile = new File(mm.getModelPath() + File.separator + mm.getCovers().get(0));
    		if (!imFile.exists()) 
    			return this.getClass().getClassLoader().getResource(Gui.DIJ_ICON_PATH);
    		try {
				return imFile.toURI().toURL();
			} catch (MalformedURLException e) {
				return this.getClass().getClassLoader().getResource(Gui.DIJ_ICON_PATH);
			}
    	}).collect(Collectors.toList());
    }
    
    protected void setModels(List<ModelDescriptor> models) {
    	this.models = models;
    	setCardsData();
    	if (SwingUtilities.isEventDispatchThread())
    		redrawModelCards(0);
    	else
    		SwingUtilities.invokeLater(() -> redrawModelCards(0));
    }
    
    protected void redrawModelCards(int currentIndex) {
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
        int size = getModelNames().size();
        return (index % size + size) % size;
    }
    
    public List<String> getModelNames() {
    	return this.modelNames;
    }
    
    public List<String> getModelNicknames() {
    	return this.modelNicknames;
    }
    
    public List<URL> getCoverPaths() {
    	return this.modelImagePaths;
    }
}

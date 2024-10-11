package deepimagej.gui;

import ij.plugin.frame.PlugInFrame;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import deepimagej.Runner;
import deepimagej.tools.ImPlusRaiManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Gui extends PlugInFrame {

    private static final long serialVersionUID = 1081914206026104187L;
	private int currentIndex = 1;
    private List<String> modelNames;
    private List<String> modelNicknames;
    private List<URL> modelImagePaths;
    private List<ModelDescriptor> models;
    private Runner runner;

    private ModelCard prevModelPanel;
    private ModelCard selectedModelPanel;
    private ModelCard nextModelPanel;
    private SearchBar searchBar;
    private JPanel modelSelectionPanel;
    private JPanel modelCarouselPanel;
    private Header3 titlePanel;
    private JPanel footerPanel;
    private Layout layout = Layout.createVertical(LAYOUT_WEIGHTS);

    private JLabel exampleImageLabel;
    private JTextArea modelInfoArea;

    private static final double CARR_VRATIO = 0.34;
    private static final double SELECTION_PANE_VRATIO = 0.35;
    private static final double ARROWS_VRATIO = 0.1;
    private static final double TITLE_VRATIO = 0.15;
    private static final double TITLE_LOGO_VRATIO = 0.1;
    private static final double TITLE_LOGO_HRATIO = 1.0 / 7;
    private static final double MODEL_VRATIO = 0.4;
    private static final double FOOTER_VRATIO = 0.1;
    private static final double[] LAYOUT_WEIGHTS = new double[] {0.1, 0.05, 0.75, 0.1};

    protected static final String LOADING_STR = "loading...";
    protected static final String LOADING_GIF_PATH = "loading...";
    protected static final String DIJ_ICON_PATH = "dij_imgs/deepimagej_icon.png";
    protected static final double MAIN_CARD_RT = 1;
    protected static final double SECOND_CARD_RT = 0.8;
    

    public Gui() {
        super("DeepImageJ Plugin");
        setDefaultCardsData();
        setSize(800, 900);
        setLayout(layout);

        // Initialize UI components
        initTitlePanel();
        initSearchBar();
        initMainContentPanel();
        initFooterPanel();

        setVisible(true);
    }
    
    public Gui(List<ModelDescriptor> models) {
        super("DeepImageJ Plugin");
    	this.models = models;
        setSize(800, 900);
        setLayout(layout);

        // Initialize UI components
        initTitlePanel();
        initSearchBar();
        initMainContentPanel();
        initFooterPanel();
    	setCardsData();

        setVisible(true);
    }
    
    private void setDefaultCardsData() {
        modelNames = Arrays.asList(LOADING_STR, LOADING_STR, LOADING_STR);
        modelNicknames = Arrays.asList(LOADING_STR, LOADING_STR, LOADING_STR);
        modelImagePaths = Arrays.asList(getClass().getClassLoader().getResource(DIJ_ICON_PATH), 
    											    		getClass().getClassLoader().getResource(DIJ_ICON_PATH), 
    											    		getClass().getClassLoader().getResource(DIJ_ICON_PATH));
        models = new ArrayList<ModelDescriptor>();
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

    private void initTitlePanel() {
    	titlePanel = new Header3("deepImageJ", "The Fiji/ImageJ Plugin for AI", this.getWidth(), this.getHeight());
        add(titlePanel, layout.get(0));
    }

    private void initSearchBar() {
        // Set up the title panel
        searchBar = new SearchBar(this.getWidth(), this.getHeight());
        add(searchBar, layout.get(1));
    }

    private void initMainContentPanel() {
        // Create a main content panel with vertical BoxLayout
        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.setBackground(Color.WHITE);

        // Add the model selection panel and content panel to the main content panel
        mainContentPanel.add(initModelSelectionPanel());
        mainContentPanel.add(initContentPanel());

        // Add the main content panel to the frame's CENTER region
        add(mainContentPanel, layout.get(2));
    }

    private JPanel initModelSelectionPanel() {
        modelSelectionPanel = new JPanel();
        modelSelectionPanel.setLayout(new BoxLayout(modelSelectionPanel, BoxLayout.Y_AXIS));
        modelSelectionPanel.setBackground(new Color(236, 240, 241));
        Border lineBorder = BorderFactory.createLineBorder(Color.gray, 2, true); // 2-pixel thick line border
        Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5); // 10-pixel padding around the content
        modelSelectionPanel.setBorder(BorderFactory.createCompoundBorder(paddingBorder,lineBorder));
        modelSelectionPanel.setSize(new Dimension(getWidth(), (int) (getHeight() * SELECTION_PANE_VRATIO)));

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

        modelSelectionPanel.add(modelCarouselPanel);
        modelSelectionPanel.add(navigationPanel);

        // Return the modelSelectionPanel
        return modelSelectionPanel;
    }

    private JPanel initContentPanel() {
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * MODEL_VRATIO)));

        // Example Image Panel
        JPanel exampleImagePanel = new JPanel(new BorderLayout());
        exampleImagePanel.setBackground(Color.WHITE);

        JLabel exampleTitleLabel = new JLabel("Example Image", JLabel.CENTER);
        exampleTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = createScaledIcon(getClass().getClassLoader().getResource(DIJ_ICON_PATH), logoWidth, logoHeight);
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

        contentPanel.add(exampleImagePanel);
        contentPanel.add(modelInfoPanel);

        // Return the contentPanel
        return contentPanel;
    }

    private void initFooterPanel() {
        footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(45, 62, 80));
        footerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        footerPanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * FOOTER_VRATIO)));

        JPanel runButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        runButtonPanel.setBackground(new Color(45, 62, 80));

        JButton runOnTestButton = new JButton("Run on Test");
        runOnTestButton.addActionListener(e -> runModelOnTestImage());
        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> runModel());

        styleButton(runOnTestButton);
        styleButton(runButton);

        runButtonPanel.add(runOnTestButton);
        runButtonPanel.add(runButton);

        JLabel copyrightLabel = new JLabel("© 2024 deepImageJ - Version 1.0");
        copyrightLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        copyrightLabel.setForeground(Color.WHITE);

        footerPanel.add(runButtonPanel, BorderLayout.CENTER);
        footerPanel.add(copyrightLabel, BorderLayout.SOUTH);

        add(footerPanel, layout.get(3));
    }

    private void updateCarousel(int direction) {
        currentIndex = getWrappedIndex(currentIndex + direction);

        redrawModelCards();
        
        // Update example image and model info
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = createScaledIcon(modelImagePaths.get(currentIndex), logoWidth, logoHeight);
        exampleImageLabel.setIcon(logoIcon);
        modelInfoArea.setText("Detailed information for " + modelNames.get(currentIndex));
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
    
    private void runModel() {
    	
    }
    
    private <T extends RealType<T> & NativeType<T>> void runModelOnTestImage() {
    	if (runner == null || runner.isClosed()) {
    		runner = Runner.create(this.models.get(currentIndex));
    	}
    	try {
    		if (!runner.isLoaded())
    			runner.load();
			List<Tensor<T>> outs = runner.runOnTestImages();
			for (Tensor<T> tt : outs) {
				ImPlusRaiManager.convert(tt.getData(), tt.getAxesOrderString()).show();
			}
		} catch (ModelSpecsException | RunModelException | IOException | LoadModelException e) {
			e.printStackTrace();
		}
    		
    }

    private int getWrappedIndex(int index) {
        int size = modelNames.size();
        return (index % size + size) % size;
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(new Color(52, 152, 219));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }
    
    public void setLoadingCards() {
    	setCards(generateLoadingCards());
    }
    
    public void setCards(List<JPanel> cards) {
    	
    }
    
    private List<JPanel> generateLoadingCards() {
    	double[] cardSizes = new double[] {SECOND_CARD_RT, MAIN_CARD_RT, SECOND_CARD_RT};
    	List<JPanel> cards = new ArrayList<JPanel>();
    	//for (double size : cardSizes)
    		//cards.add(ModelCard.createModelCard(LOADING_STR, Gui.class.getResource(LOADING_GIF_PATH), LOADING_STR, size));
    	return cards;
    }
    
    protected static ImageIcon createScaledIcon(URL imagePath, int logoWidth, int logoHeight) {
        if (imagePath == null) {
            return getDefaultIcon(logoWidth, logoHeight);
        }

        try (ImageInputStream iis = ImageIO.createImageInputStream(imagePath.openStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return getDefaultIcon(logoWidth, logoHeight);
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);

            if (isAnimatedGif(reader)) {
                return createScaledAnimatedGif(imagePath, logoWidth, logoHeight);
            } else {
                return createScaledStaticImage(imagePath, logoWidth, logoHeight);
            }
        } catch (IOException e) {
            return getDefaultIcon(logoWidth, logoHeight);
        }
    }

    private static boolean isAnimatedGif(ImageReader reader) throws IOException {
        return reader.getFormatName().equalsIgnoreCase("gif") && reader.getNumImages(true) > 1;
    }

    private static ImageIcon createScaledAnimatedGif(URL imagePath, int width, int height) {
        ImageIcon originalIcon = new ImageIcon(imagePath);
        Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    private static ImageIcon createScaledStaticImage(URL imagePath, int width, int height) throws IOException {
        BufferedImage originalImage = ImageIO.read(imagePath);
        if (originalImage == null) {
            return getDefaultIcon(width, height);
        }
        Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    private static ImageIcon getDefaultIcon(int width, int height) {
        try {
            URL defaultIconUrl = Gui.class.getResource(DIJ_ICON_PATH);
            if (defaultIconUrl == null) {
                return null;
            }
            BufferedImage defaultImage = ImageIO.read(defaultIconUrl);
            Image scaledDefaultImage = defaultImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledDefaultImage);
        } catch (IOException e) {
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Gui());
    }
}

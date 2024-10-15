package deepimagej.gui;

import ij.plugin.frame.PlugInFrame;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import deepimagej.Runner;
import deepimagej.tools.ImPlusRaiManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Gui extends PlugInFrame {

    private static final long serialVersionUID = 1081914206026104187L;
    private List<ModelDescriptor> models;
    private Runner runner;
	private int currentIndex = 1;

    private SearchBar searchBar;
    private ContentPanel contentPanel;
    private ModelSelectionPanel modelSelectionPanel;
    private Header titlePanel;
    private JPanel footerPanel;
    private Layout layout = Layout.createVertical(LAYOUT_WEIGHTS);

    private static final double FOOTER_VRATIO = 0.06;
    private static final double[] LAYOUT_WEIGHTS = new double[] {0.1, 0.05, 0.8, 0.05};

    protected static final String LOADING_STR = "loading...";
    protected static final String LOADING_GIF_PATH = "loading...";
    protected static final String DIJ_ICON_PATH = "dij_imgs/deepimagej_icon.png";
    

    public Gui() {
        super("DeepImageJ Plugin");
        setSize(800, 900);
        setLayout(layout);

        // Initialize UI components
        initTitlePanel();
        initSearchBar();
        initMainContentPanel();
        initFooterPanel();

        this.pack();
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

        this.pack();
        setVisible(true);
    }

    private void initTitlePanel() {
    	titlePanel = new Header("deepImageJ", "The Fiji/ImageJ Plugin for AI", this.getWidth(), this.getHeight());
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
        Layout mainPanelLayout = Layout.createVertical(new double[] {0.45, 0.55});
        mainContentPanel.setLayout(mainPanelLayout);
        mainContentPanel.setBackground(Color.WHITE);

        // Add the model selection panel and content panel to the main content panel
        this.modelSelectionPanel = new ModelSelectionPanel(this.getWidth(), this.getHeight());
        mainContentPanel.add(this.modelSelectionPanel, mainPanelLayout.get(0));
        contentPanel = new ContentPanel(this.getWidth(), this.getHeight());
        mainContentPanel.add(contentPanel, mainPanelLayout.get(1));

        // Add the main content panel to the frame's CENTER region
        add(mainContentPanel, layout.get(2));
        
        modelSelectionPanel.prevButton.addActionListener(e -> updateCarousel(-1));
        modelSelectionPanel.nextButton.addActionListener(e -> updateCarousel(1));
    }

    private void initFooterPanel() {
        footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(45, 62, 80));
        footerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        footerPanel.setPreferredSize(new Dimension(this.getWidth(), (int) (this.getHeight() * FOOTER_VRATIO)));

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

        footerPanel.add(runButtonPanel, BorderLayout.EAST);
        footerPanel.add(copyrightLabel, BorderLayout.WEST);

        add(footerPanel, layout.get(3));
    }
    
    private void runModel() {
    	
    }
    
    private <T extends RealType<T> & NativeType<T>> void runModelOnTestImage() {
    	if (runner == null || runner.isClosed()) {
    		runner = Runner.create(this.models.get(currentIndex));
    		runner = Runner.create(this.models.get(0));
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

    private void updateCarousel(int direction) {
        currentIndex = getWrappedIndex(currentIndex + direction);

        this.modelSelectionPanel.redrawModelCards(currentIndex);
        
        // Update example image and model info
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = Gui.createScaledIcon(modelSelectionPanel.getCoverPaths().get(currentIndex), logoWidth, logoHeight);
        this.contentPanel.setIcon(logoIcon);
        this.contentPanel.setInfo("Detailed information for " + modelSelectionPanel.getModelNames().get(currentIndex));
    }

    private int getWrappedIndex(int index) {
        int size = modelSelectionPanel.getModelNames().size();
        return (index % size + size) % size;
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(new Color(52, 152, 219));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }
    
    public void setModels(List<ModelDescriptor> models) {
    	currentIndex = 0;
    	this.modelSelectionPanel.setModels(models);
        // Update example image and model info
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = Gui.createScaledIcon(modelSelectionPanel.getCoverPaths().get(currentIndex), logoWidth, logoHeight);
        this.contentPanel.setIcon(logoIcon);
        this.contentPanel.setInfo("Detailed information for " + modelSelectionPanel.getModelNames().get(currentIndex));
    }
    
    public void trackEngineInstallation(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
    	this.titlePanel.trackEngineInstallation(consumersMap);
    }
    
    protected void clickedBMZ() {
    	ArrayList<ModelDescriptor> newModels = createArrayOfNulls(3);
    	boolean isEdt = SwingUtilities.isEventDispatchThread();
    	setModels(newModels);
    	
    	new Thread(() -> {
        	int nModels = searchBar.countBMZModels(true);
        	this.searchBar.findBMZModels();
    	}).start();
    	
    	new Thread(() -> {
    		while (searchBar.countBMZModels(false) == 0) {
    			try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return;
				}
            	ArrayList<ModelDescriptor> bmzModels = createArrayOfNulls(searchBar.countBMZModels(false));
            	searchBar.getBMZModels();
    		}
    	}).start();
    }
    
    private static ArrayList<ModelDescriptor> createArrayOfNulls(int n) {
    	ArrayList<ModelDescriptor> newModels = new ArrayList<ModelDescriptor>();
    	for (int i = 0; i < n; i++)
    		newModels.add(null);
    	return newModels;
    }
    
    protected void clickedLocal() {
    	
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
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = AffineTransform.getScaleInstance(
                (double) width / originalImage.getWidth(), 
                (double) height / originalImage.getHeight()
        );
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scaleOp.filter(originalImage, scaledImage);
        //Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    private static ImageIcon getDefaultIcon(int width, int height) {
        try {
            URL defaultIconUrl = Gui.class.getResource(DIJ_ICON_PATH);
            if (defaultIconUrl == null) {
                return null;
            }
            BufferedImage defaultImage = ImageIO.read(defaultIconUrl);
            Image scaledDefaultImage = defaultImage.getScaledInstance(width, height, Image.SCALE_FAST);
            return new ImageIcon(scaledDefaultImage);
        } catch (IOException e) {
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Gui());
    }
}

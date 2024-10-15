package deepimagej.gui;

import ij.plugin.frame.PlugInFrame;
import io.bioimage.modelrunner.bioimageio.BioimageioRepo;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import deepimagej.Runner;
import deepimagej.tools.ImPlusRaiManager;

import java.util.ArrayList;
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
    private JButton runButton;
    private JButton runOnTestButton;
    private Layout layout = Layout.createVertical(LAYOUT_WEIGHTS);

    private static final double FOOTER_VRATIO = 0.06;
    private static final double[] LAYOUT_WEIGHTS = new double[] {0.1, 0.05, 0.8, 0.05};

    protected static final String LOADING_STR = "loading...";
    protected static final String LOADING_GIF_PATH = "loading...";
    protected static final String DIJ_ICON_PATH = "dij_imgs/deepimagej_icon.png";
    protected static final String LOCAL_STR = "Local";
    protected static final String BIOIMAGEIO_STR = "Bioimage.io";
    protected static final String RUN_STR = "Run";
    protected static final String RUN_ON_TEST_STR = "Run on test";
    protected static final String INSTALL_STR = "Install model";
    

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
        searchBar.switchButton.addActionListener(ee -> switchBtnClicked());
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
        footerPanel.setBorder(new EmptyBorder(10, 5, 10, 5));
        footerPanel.setPreferredSize(new Dimension(this.getWidth(), (int) (this.getHeight() * FOOTER_VRATIO)));

        JPanel runButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        runButtonPanel.setBackground(new Color(45, 62, 80));

        runOnTestButton = new JButton(RUN_ON_TEST_STR);
        runOnTestButton.addActionListener(e -> runTestOrInstall());
        runButton = new JButton(RUN_STR);
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
    
    private void runTestOrInstall() {
    	if (this.runOnTestButton.getText().equals(INSTALL_STR)) {
    		installSelectedModel();
    	} else if (this.runOnTestButton.getText().equals(RUN_ON_TEST_STR)) {
    		runModelOnTestImage();
    	}
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
        ImageIcon logoIcon = ImageLoader.createScaledIcon(modelSelectionPanel.getCoverPaths().get(currentIndex), logoWidth, logoHeight);
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
        ImageIcon logoIcon = ImageLoader.createScaledIcon(modelSelectionPanel.getCoverPaths().get(currentIndex), logoWidth, logoHeight);
        this.contentPanel.setIcon(logoIcon);
        this.contentPanel.setInfo("Detailed information for " + modelSelectionPanel.getModelNames().get(currentIndex));
    }
    
    public void trackEngineInstallation(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
    	this.titlePanel.trackEngineInstallation(consumersMap);
    }
    
    protected void switchBtnClicked() {
    	if (this.searchBar.isBarOnLocal()) {
    		clickedBMZ();
    	} else {
    		clickedLocal();
    	}
    }
    
    protected void clickedBMZ() {
    	ArrayList<ModelDescriptor> newModels = createArrayOfNulls(3);
    	boolean isEdt = SwingUtilities.isEventDispatchThread();
    	this.searchBar.setBarEnabled(false);
    	this.searchBar.changeButtonToLocal();
    	this.contentPanel.setProgressIndeterminate(true);
    	this.contentPanel.setProgressText("Getting Bioimage.io models...");
    	this.runButton.setVisible(false);
    	this.runOnTestButton.setText(INSTALL_STR);
    	this.runOnTestButton.setEnabled(false);
    	this.modelSelectionPanel.setBMZBorder();
    	this.modelSelectionPanel.setArrowsEnabled(false);
    	setModels(newModels);
    	
    	Thread finderThread = new Thread(() -> {
    		// This line initiates the read of the bioimage.io collection
        	searchBar.countBMZModels(true);
        	this.searchBar.findBMZModels();
    	});
    	
    	Thread updaterThread = new Thread(() -> {
    		while (searchBar.countBMZModels(false) == 0) {
    			try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return;
				}
    		}
    		while (finderThread.isAlive()) {
    			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					return;
				}
    			int nModels = searchBar.countBMZModels(false);
            	List<ModelDescriptor> foundModels = new ArrayList<>(searchBar.getBMZModels());
            	foundModels.addAll(createArrayOfNulls(nModels - foundModels.size()));
            	SwingUtilities.invokeLater(() -> setModels(foundModels));
    		}
        	List<ModelDescriptor> foundModels = searchBar.getBMZModels();
        	SwingUtilities.invokeLater(() -> {
        		setModels(foundModels);
            	this.contentPanel.setProgressIndeterminate(false);
            	this.contentPanel.setProgressText("");
            	this.searchBar.setBarEnabled(true);
            	this.runOnTestButton.setEnabled(true);
            	this.modelSelectionPanel.setArrowsEnabled(true);
        	});
    	});
    	
    	finderThread.start();
    	updaterThread.start();
    }
    
    private static ArrayList<ModelDescriptor> createArrayOfNulls(int n) {
    	ArrayList<ModelDescriptor> newModels = new ArrayList<ModelDescriptor>();
    	for (int i = 0; i < n; i++)
    		newModels.add(null);
    	return newModels;
    }
    
    protected void clickedLocal() {
    	ArrayList<ModelDescriptor> newModels = createArrayOfNulls(3);
    	boolean isEdt = SwingUtilities.isEventDispatchThread();
    	this.searchBar.setBarEnabled(false);
    	this.searchBar.changeButtonToBMZ();
    	this.contentPanel.setProgressIndeterminate(true);
    	this.contentPanel.setProgressText("Getting Bioimage.io models...");
    	this.runButton.setVisible(true);
    	this.runButton.setEnabled(false);
    	this.runOnTestButton.setText(RUN_ON_TEST_STR);
    	this.runOnTestButton.setEnabled(false);
    	this.modelSelectionPanel.setLocalBorder();
    	this.modelSelectionPanel.setArrowsEnabled(false);
    	setModels(newModels);
    	
    	Thread finderThread = new Thread(() -> {
        	this.searchBar.findLocalModels(new File("models").getAbsolutePath());
    	});
    	
    	Thread updaterThread = new Thread(() -> {
    		while (finderThread.isAlive()) {
    			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					return;
				}
            	List<ModelDescriptor> foundModels = new ArrayList<>(searchBar.getBMZModels());
            	SwingUtilities.invokeLater(() -> setModels(foundModels));
    		}
        	List<ModelDescriptor> foundModels = searchBar.getBMZModels();
        	SwingUtilities.invokeLater(() -> {
        		setModels(foundModels);
            	this.contentPanel.setProgressIndeterminate(false);
            	this.contentPanel.setProgressText("");
            	this.searchBar.setBarEnabled(true);
            	this.runOnTestButton.setEnabled(true);
            	this.modelSelectionPanel.setArrowsEnabled(true);
            	this.runButton.setEnabled(true);
        	});
    	});
    	
    	finderThread.start();
    	updaterThread.start();
    }
    
    private void installSelectedModel() {
    	ModelDescriptor selectedModel = modelSelectionPanel.getModels().get(this.currentIndex);
    	TwoParameterConsumer<String, Double> progress = DownloadTracker.createConsumerProgress();
    	this.runOnTestButton.setEnabled(false);
    	this.searchBar.setBarEnabled(false);
    	this.modelSelectionPanel.setArrowsEnabled(false);
    	
    	Thread dwnlThread = new Thread(() -> {
        	try {
    			String modelFolder = BioimageioRepo.downloadModel(selectedModel, "models", progress);
    			selectedModel.addModelPath(Paths.get(modelFolder));
    		} catch (IOException | InterruptedException e) {
    			e.printStackTrace();
    			return;
    		}
    	});
    	
    	Thread reportThread = new Thread(() -> {
    		while (dwnlThread.isAlive()) {
    			try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
					return;
				}
    			Double val = progress.get().get("total");
    			long perc = Math.round((val == null ? 0.0 : val) * 100);
    			SwingUtilities.invokeLater(() -> contentPanel.setDeterminatePorgress((int) perc));
    		}
			Double val = progress.get().get("total");
			long perc = Math.round((val == null ? 0.0 : val) * 100);
			SwingUtilities.invokeLater(() -> {
				contentPanel.setDeterminatePorgress((int) perc);
				runOnTestButton.setEnabled(true);
		    	this.searchBar.setBarEnabled(true);
		    	this.modelSelectionPanel.setArrowsEnabled(true);
			});
    	});
    	dwnlThread.start();
    	reportThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Gui());
    }
}

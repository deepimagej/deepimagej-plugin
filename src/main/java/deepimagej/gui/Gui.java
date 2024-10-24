package deepimagej.gui;

import ij.ImagePlus;
import ij.plugin.frame.PlugInFrame;
import io.bioimage.modelrunner.bioimageio.BioimageioRepo;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptorFactory;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

import deepimagej.Constants;
import deepimagej.Runner;
import deepimagej.tools.ImPlusRaiManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Gui extends PlugInFrame {

    private static final long serialVersionUID = 1081914206026104187L;
    private Runner runner;
	private int currentIndex = 1;
	private final String modelsDir;
	private final String enginesDir;
	
	Thread dwnlThread;
	Thread runninThread;
	Thread finderThread;
	Thread localModelsThread;

    private SearchBar searchBar;
    private ContentPanel contentPanel;
    private ModelSelectionPanel modelSelectionPanel;
    private Header titlePanel;
    private JPanel footerPanel;
    private JButton runButton;
    private JButton runOnTestButton;
    private JButton cancelButton;
    private Layout layout = Layout.createVertical(LAYOUT_WEIGHTS);

    private static final double FOOTER_VRATIO = 0.06;
    private static final double[] LAYOUT_WEIGHTS = new double[] {0.1, 0.05, 0.8, 0.05};

    protected static final String LOADING_STR = "loading...";
    protected static final String NOT_FOUND_STR = "not found";
    protected static final String LOADING_GIF_PATH = "loading...";
    protected static final String DIJ_ICON_PATH = "dij_imgs/deepimagej_icon.png";
    protected static final String LOCAL_STR = "Local";
    protected static final String BIOIMAGEIO_STR = "Bioimage.io";
    protected static final String RUN_STR = "Run";
    protected static final String CANCEL_STR = "Cancel";
    protected static final String RUN_ON_TEST_STR = "Run on test";
    protected static final String INSTALL_STR = "Install model";
    private static final String MODELS_DEAFULT = "models";
    private static final String ENGINES_DEAFULT = "engines";


    public Gui() {
    	this(null, null);
    }

    public Gui(String modelsDir, String enginesDir) {
        super(Constants.DIJ_NAME + "-" + Constants.DIJ_VERSION);
        this.modelsDir = modelsDir != null ? modelsDir : new File(MODELS_DEAFULT).getAbsolutePath();
        this.enginesDir = enginesDir != null ? enginesDir : new File(ENGINES_DEAFULT).getAbsolutePath();
        loadLocalModels();
        setSize(800, 900);
        setLayout(layout);

        // Initialize UI components
        initTitlePanel();
        initSearchBar();
        initMainContentPanel();
        initFooterPanel();
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onClose();
            }
        });

        this.pack();
        setVisible(true);
    }
    
    private void loadLocalModels() {
	    localModelsThread = new Thread(() -> {
	        List<ModelDescriptor> models = ModelDescriptorFactory.getModelsAtLocalRepo(new File(modelsDir).getAbsolutePath());
	        while (contentPanel == null) {
	        	try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
				}
	        }
            this.setModels(models);
	    });
	    localModelsThread.start();
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
        searchBar.searchButton.addActionListener(ee -> searchModels());
        searchBar.searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                	searchModels();
            }
        });
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
        cancelButton = new JButton(CANCEL_STR);
        cancelButton.addActionListener(e -> cancel());

        styleButton(runOnTestButton, "blue");
        styleButton(runButton, "blue");
        styleButton(cancelButton, "red");

        runButtonPanel.add(cancelButton);
        runButtonPanel.add(runOnTestButton);
        runButtonPanel.add(runButton);

        JLabel copyrightLabel = new JLabel("© 2024 " + Constants.DIJ_NAME + " and JDLL");
        copyrightLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        copyrightLabel.setForeground(Color.WHITE);

        footerPanel.add(runButtonPanel, BorderLayout.EAST);
        footerPanel.add(copyrightLabel, BorderLayout.WEST);

        add(footerPanel, layout.get(3));
    }
    
    private void cancel() {
    	this.dispose();
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
    	SwingUtilities.invokeLater(() -> this.contentPanel.setProgressIndeterminate(true));
    	runninThread = new Thread(() -> {
        	if (runner == null || runner.isClosed()) {
            	SwingUtilities.invokeLater(() -> this.contentPanel.setProgressLabelText("Loading model..."));
        		runner = Runner.create(this.modelSelectionPanel.getModels().get(currentIndex));
        	}
        	try {
        		if (!runner.isLoaded() && GuiUtils.isEDTAlive())
        			runner.load();
        		else if (!GuiUtils.isEDTAlive())
        			return;
            	SwingUtilities.invokeLater(() -> this.contentPanel.setProgressLabelText("Running the model..."));
    			List<Tensor<T>> outs = runner.runOnTestImages();
    			for (Tensor<T> tt : outs) {
    				if (!GuiUtils.isEDTAlive())
            			return;
    				ImagePlus im = ImPlusRaiManager.convert(tt.getData(), tt.getAxesOrderString());
    				if (!GuiUtils.isEDTAlive())
            			return;
    				SwingUtilities.invokeLater(() -> im.show());
    			}
    		} catch (ModelSpecsException | RunModelException | IOException | LoadModelException e) {
    			e.printStackTrace();
    		}
        	SwingUtilities.invokeLater(() -> {
        		this.contentPanel.setProgressLabelText("");
        		this.contentPanel.setProgressIndeterminate(false);
        	});
    	});
    	runninThread.start();
    		
    }

    private void updateCarousel(int direction) {
    	closeModelWhenChanging();
        currentIndex = getWrappedIndex(currentIndex + direction);
    	updateProgressBar();

        this.modelSelectionPanel.redrawModelCards(currentIndex);
        
        // Update example image and model info
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = ImageLoader.createScaledIcon(modelSelectionPanel.getCoverPaths().get(currentIndex), logoWidth, logoHeight);
        this.contentPanel.setIcon(logoIcon);
        ModelDescriptor model = modelSelectionPanel.getModels().get(currentIndex);
        this.contentPanel.setInfo(model == null ? "Detailed model description..." : model.buildInfo());
    }
    
    private void updateProgressBar() {
    	if (this.searchBar.isBarOnLocal() && this.contentPanel.getProgress() != 0) {
    		contentPanel.setProgressBarText("");
    		contentPanel.setDeterminatePorgress(0);
    	} else if(!searchBar.isBarOnLocal() && this.contentPanel.getProgress() != 100 
    			&& modelSelectionPanel.getModels().get(currentIndex).isModelInLocalRepo()) {
    		contentPanel.setProgressBarText("100%");
    		contentPanel.setDeterminatePorgress(100);
    	} else if(!searchBar.isBarOnLocal() && this.contentPanel.getProgress() != 0 
    			&& !modelSelectionPanel.getModels().get(currentIndex).isModelInLocalRepo()) {
    		contentPanel.setProgressBarText("");
    		contentPanel.setDeterminatePorgress(0);
    	}
    	if (searchBar.isBarOnLocal() 
    			|| (!searchBar.isBarOnLocal() 
    					&& !modelSelectionPanel.getModels().get(currentIndex).isModelInLocalRepo() 
    					&& !contentPanel.getProgressBarText().equals(""))) {
    		contentPanel.setProgressBarText("");
    	}
    }

    private int getWrappedIndex(int index) {
        int size = modelSelectionPanel.getModelNames().size();
        return (index % size + size) % size;
    }

    private void styleButton(JButton button, String color) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (color.equals("red")) {
            button.setBackground(new Color(255, 20, 20));
        } else {
            button.setBackground(new Color(52, 152, 219));
        }
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }
    
    public void setModels(List<ModelDescriptor> models) {
    	if (models.size() == 0)
    		models = createArrayOfNulls(1);
    	this.modelSelectionPanel.setNotFound();
    	this.searchBar.setModels(models);
    	setModelsInGui(models);
    }
    
    protected void setModelsInGui(List<ModelDescriptor> models) {
    	currentIndex = 0;
    	this.modelSelectionPanel.setModels(models);
        // Update example image and model info
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = ImageLoader.createScaledIcon(modelSelectionPanel.getCoverPaths().get(currentIndex), logoWidth, logoHeight);
        this.contentPanel.setIcon(logoIcon);
        ModelDescriptor model = modelSelectionPanel.getModels().get(currentIndex);
        this.contentPanel.setInfo(model == null ? "Detailed model description..." : model.buildInfo());
    }
    
    public void trackEngineInstallation(Map<String, TwoParameterConsumer<String, Double>> consumersMap) {
    	this.titlePanel.trackEngineInstallation(consumersMap);
    }
    
    private void searchModels() {
    	List<ModelDescriptor> models = this.searchBar.performSearch();
    	if (models.size() == 0) {
    		modelSelectionPanel.setNotFound();
    		models = createArrayOfNulls(1);
    	}
    	this.setModelsInGui(models);
    }
    
    protected void switchBtnClicked() {
    	closeModelWhenChanging();
    	if (this.searchBar.isBarOnLocal()) {
    		clickedBMZ();
    	} else {
    		clickedLocal();
    	}
    }
    
    private void closeModelWhenChanging() {
    	if (runner != null && !runner.isClosed()) {
			try {
				runner.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }
    
    protected void clickedBMZ() {
    	modelSelectionPanel.setLoading();
    	ArrayList<ModelDescriptor> newModels = createArrayOfNulls(3);
    	boolean isEdt = SwingUtilities.isEventDispatchThread();
    	this.searchBar.setBarEnabled(false);
    	this.searchBar.changeButtonToLocal();
    	this.contentPanel.setProgressIndeterminate(true);
    	this.contentPanel.setProgressBarText("");
    	this.runButton.setVisible(false);
    	this.runOnTestButton.setText(INSTALL_STR);
    	this.runOnTestButton.setEnabled(false);
    	this.modelSelectionPanel.setBMZBorder();
    	this.modelSelectionPanel.setArrowsEnabled(false);
    	this.contentPanel.setProgressLabelText("Looking for models at Bioimage.io");
    	setModelsInGui(newModels);
    	
    	finderThread = new Thread(() -> {
    		// This line initiates the read of the bioimage.io collection
    		try {
	        	searchBar.countBMZModels(true);
	        	this.searchBar.findBMZModels();
			} catch (InterruptedException e) {
				return;
			}
    	});
    	
    	Thread updaterThread = new Thread(() -> {
			try {
	    		while (searchBar.countBMZModels(false) == 0 && finderThread.isAlive()) {
						Thread.sleep(100);
	    		}
			} catch (InterruptedException e) {
				return;
			}
    		while (finderThread.isAlive()) {
    			int nModels;
    			try {
					Thread.sleep(500);
	    			nModels = searchBar.countBMZModels(false);
				} catch (InterruptedException e) {
					return;
				}
            	List<ModelDescriptor> foundModels = new ArrayList<>(searchBar.getBMZModels());
            	foundModels.addAll(createArrayOfNulls(nModels - foundModels.size()));
            	SwingUtilities.invokeLater(() -> setModelsInGui(foundModels));
    		}
        	List<ModelDescriptor> foundModels = searchBar.getBMZModels();
        	SwingUtilities.invokeLater(() -> {
        		setModelsInGui(foundModels);
            	this.contentPanel.setProgressIndeterminate(false);
            	this.searchBar.setBarEnabled(true);
            	this.runOnTestButton.setEnabled(true);
            	this.modelSelectionPanel.setArrowsEnabled(true);
            	this.contentPanel.setProgressLabelText("");
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
    	modelSelectionPanel.setLoading();
    	ArrayList<ModelDescriptor> newModels = createArrayOfNulls(3);
    	boolean isEdt = SwingUtilities.isEventDispatchThread();
    	this.searchBar.setBarEnabled(false);
    	this.searchBar.changeButtonToBMZ();
    	this.contentPanel.setProgressIndeterminate(true);
    	this.contentPanel.setProgressBarText("");
    	this.contentPanel.setProgressLabelText("Looking for models locally");
    	this.runButton.setVisible(true);
    	this.runButton.setEnabled(false);
    	this.runOnTestButton.setText(RUN_ON_TEST_STR);
    	this.runOnTestButton.setEnabled(false);
    	this.modelSelectionPanel.setLocalBorder();
    	this.modelSelectionPanel.setArrowsEnabled(false);
    	setModelsInGui(newModels);
    	
    	Thread finderThread = new Thread(() -> {
        	this.searchBar.findLocalModels(new File(this.modelsDir).getAbsolutePath());
    	});
    	
    	Thread updaterThread = new Thread(() -> {
    		while (finderThread.isAlive()) {
    			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					return;
				}
            	List<ModelDescriptor> foundModels = new ArrayList<>(searchBar.getBMZModels());
            	SwingUtilities.invokeLater(() -> setModelsInGui(foundModels));
    		}
        	List<ModelDescriptor> foundModels = searchBar.getBMZModels();
        	SwingUtilities.invokeLater(() -> {
        		setModelsInGui(foundModels);
            	this.contentPanel.setProgressIndeterminate(false);
            	this.contentPanel.setDeterminatePorgress(0);
            	this.contentPanel.setProgressBarText("");
            	this.contentPanel.setProgressLabelText("");
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
    	this.contentPanel.setProgressLabelText("Installing ...");
    	
    	dwnlThread = new Thread(() -> {
        	try {
    			String modelFolder = BioimageioRepo.downloadModel(selectedModel, new File(modelsDir).getAbsolutePath(), progress);
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
		    	this.contentPanel.setProgressLabelText("");
			});
    	});
    	dwnlThread.start();
    	reportThread.start();
    }
    
    private void onClose() {
    	if (dwnlThread != null && this.dwnlThread.isAlive())
    		this.dwnlThread.interrupt();
    	if (finderThread != null && this.finderThread.isAlive())
    		this.finderThread.interrupt();
    	if (runninThread != null && this.runninThread.isAlive() && runner != null) {
			try {
				runner.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	if (runninThread != null && this.runninThread.isAlive())
    		this.runninThread.interrupt();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Gui());
    }
}

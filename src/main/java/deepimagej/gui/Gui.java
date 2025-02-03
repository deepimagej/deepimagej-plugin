package deepimagej.gui;

import ij.ImagePlus;
import ij.plugin.frame.PlugInFrame;
import io.bioimage.modelrunner.bioimageio.BioimageioRepo;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptorFactory;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.engine.installation.EngineInstall;
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
import java.net.URL;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import deepimagej.Constants;
import deepimagej.Runner;
import deepimagej.gui.adapter.ImageAdapter;
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
	private final ImageAdapter imAdapter;
    private final Object lock = new Object();

	Thread engineInstallThread;
	Thread trackEngineInstallThread;
	Thread dwnlThread;
	Thread runninThread;
	Thread finderThread;
	Thread updaterThread;
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
	private Map<String, TwoParameterConsumer<String, Double>> consumersMap;

    private static final double FOOTER_VRATIO = 0.06;
    private static final double[] LAYOUT_WEIGHTS = new double[] {0.1, 0.05, 0.8, 0.05};

    protected static final String LOADING_STR = "loading...";
    protected static final String NOT_FOUND_STR = "not found";
    protected static final String LOADING_GIF_PATH = "loading...";
    protected static final String LOCAL_STR = "Local";
    protected static final String BIOIMAGEIO_STR = "Bioimage.io";
    protected static final String RUN_STR = "Run";
    protected static final String CANCEL_STR = "Cancel";
    protected static final String RUN_ON_TEST_STR = "Run on test";
    protected static final String INSTALL_STR = "Install model";
    private static final String MODELS_DEAFULT = "models";
    private static final String ENGINES_DEAFULT = "engines";


    public Gui(ImageAdapter imAdapter) {
    	this(imAdapter, null, null);
    }

    public Gui(ImageAdapter imAdapter, String modelsDir, String enginesDir) {
        super(Constants.DIJ_NAME + "-" + Constants.DIJ_VERSION);
        long tt = System.currentTimeMillis();
        this.imAdapter = imAdapter;
        this.modelsDir = modelsDir != null ? modelsDir : new File(MODELS_DEAFULT).getAbsolutePath();
        this.enginesDir = enginesDir != null ? enginesDir : new File(ENGINES_DEAFULT).getAbsolutePath();
        loadLocalModels();
        System.out.println("Model loading: " + (System.currentTimeMillis() - tt));
        tt = System.currentTimeMillis();
        installEnginesIfNeeded();
        System.out.println("Engines loading: " + (System.currentTimeMillis() - tt));
        tt = System.currentTimeMillis();
        setSize(800, 900);
        setLayout(layout);
        System.out.println("Set size: " + (System.currentTimeMillis() - tt));
        tt = System.currentTimeMillis();

        // Initialize UI components
        initTitlePanel();
        System.out.println("Title panel: " + (System.currentTimeMillis() - tt));
        tt = System.currentTimeMillis();
        initSearchBar();
        System.out.println("Search bar: " + (System.currentTimeMillis() - tt));
        tt = System.currentTimeMillis();
        initMainContentPanel();
        System.out.println("Content panel: " + (System.currentTimeMillis() - tt));
        tt = System.currentTimeMillis();
        initFooterPanel();
        System.out.println("Footer: " + (System.currentTimeMillis() - tt));
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onClose();
            }
        });

        this.pack();
        setVisible(true);
    }
    
    private void installEnginesIfNeeded() {
    	SwingUtilities.invokeLater(() -> {
    		this.searchBar.switchButton.setEnabled(false);
    		this.runButton.setEnabled(false);
    		this.runOnTestButton.setEnabled(false);
    	});
    	engineInstallThread = new Thread(() -> {
	        EngineInstall installer = EngineInstall.createInstaller(this.enginesDir);
	        installer.checkBasicEngineInstallation();
	        consumersMap = installer.getBasicEnginesProgress();
	        installer.basicEngineInstallation();
	    	SwingUtilities.invokeLater(() -> {
	    		System.out.println("done");
	    		this.searchBar.switchButton.setEnabled(true);
	    		this.runButton.setEnabled(true);
	    		this.runOnTestButton.setEnabled(true);
	    	});
	    });
    	engineInstallThread.start();
	    
    	trackEngineInstallThread = new Thread(() -> {
	    	while (consumersMap == null || this.titlePanel == null) {
	    		try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					return;
				}
	    	}
	        this.trackEngineInstallation();
	    });
    	trackEngineInstallThread.start();
    }
    
    private void loadLocalModels() {
	    localModelsThread = new Thread(() -> {
	        List<ModelDescriptor> models = ModelDescriptorFactory.getModelsAtLocalRepo(new File(modelsDir).getAbsolutePath());
	        while (contentPanel == null) {
	        	try {
					Thread.sleep(100);
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
    
    private <T extends RealType<T> & NativeType<T>> void runModel() {
    	SwingUtilities.invokeLater(() -> this.contentPanel.setProgressIndeterminate(true));
    	runninThread = new Thread(() -> {
        	try {
            	if (runner == null || runner.isClosed()) {
                	SwingUtilities.invokeLater(() -> this.contentPanel.setProgressLabelText("Loading model..."));
            		runner = Runner.create(this.modelSelectionPanel.getModels().get(currentIndex));
            	}
        		if (!runner.isLoaded() && GuiUtils.isEDTAlive())
        			runner.load();
        		else if (!GuiUtils.isEDTAlive())
        			return;
            	SwingUtilities.invokeLater(() -> this.contentPanel.setProgressLabelText("Running the model..."));
            	List<Tensor<T>> list = imAdapter.getInputTensors(runner.getDescriptor());
    			List<Tensor<T>> outs = runner.run(list);
    			for (Tensor<T> tt : outs) {
    				if (!GuiUtils.isEDTAlive())
            			return;
    				ImagePlus im = ImPlusRaiManager.convert(tt.getData(), tt.getAxesOrderString());
    				if (!GuiUtils.isEDTAlive())
            			return;
    				SwingUtilities.invokeLater(() -> im.show());
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        	SwingUtilities.invokeLater(() -> {
        		this.contentPanel.setProgressLabelText("");
        		this.contentPanel.setProgressIndeterminate(false);
        	});
    	});
    	runninThread.start();
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
        	try {
            	if (runner == null || runner.isClosed()) {
                	SwingUtilities.invokeLater(() -> this.contentPanel.setProgressLabelText("Loading model..."));
            		runner = Runner.create(this.modelSelectionPanel.getModels().get(currentIndex));
            	}
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
    		} catch (Exception e) {
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
    	synchronized(lock) {
            currentIndex = getWrappedIndex(currentIndex + direction);
        	updateProgressBar();

            this.modelSelectionPanel.redrawModelCards(currentIndex);
            
            // Update example image and model info
            int logoHeight = (int) (getHeight() * 0.3);
            int logoWidth = getWidth() / 3;
        	URL coverPath = modelSelectionPanel.getCoverPaths().get(currentIndex);
            contentPanel.update(modelSelectionPanel.getModels().get(currentIndex), coverPath, logoWidth, logoHeight);
    	}
    }
    
    private void updateProgressBar() {
    	if (modelSelectionPanel.getModels().get(currentIndex) == null)
    		return;
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
    	URL coverPath = modelSelectionPanel.getCoverPaths().get(currentIndex);
        contentPanel.update(modelSelectionPanel.getModels().get(currentIndex), coverPath, logoWidth, logoHeight);
    }
    
    protected void setModelInGuiAt(ModelDescriptor model, int pos) {
    	this.modelSelectionPanel.setModelAt(model, pos);
    	synchronized (lock) {
            if (currentIndex  == pos || currentIndex == pos + 1 || currentIndex == pos - 1
            		|| getWrappedIndex(pos + 1) == currentIndex ) {
            	SwingUtilities.invokeLater(() -> updateCarousel(0));
            }
        }
    }
    
    public void trackEngineInstallation() {
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
    
    
    private int nParsedModels;
    protected void clickedBMZ() {
    	ArrayList<ModelDescriptor> newModels = createArrayOfNulls(3);
    	this.searchBar.setBarEnabled(false);
    	this.searchBar.changeButtonToLocal();
    	this.contentPanel.setProgressIndeterminate(true);
    	this.contentPanel.setProgressBarText("");
    	this.runButton.setVisible(false);
    	this.runOnTestButton.setText(INSTALL_STR);
    	this.runOnTestButton.setEnabled(false);
    	this.modelSelectionPanel.setBMZBorder();
    	this.contentPanel.setProgressLabelText("Looking for models at Bioimage.io");
    	setModelsInGui(newModels);
    	List<ModelDescriptor> oldModels = new ArrayList<>(searchBar.getBMZModels());
    	
    	finderThread = new Thread(() -> {
    		// This line initiates the read of the bioimage.io collection
    		try {
	        	searchBar.countBMZModels(true);
	        	this.searchBar.findBMZModels();
			} catch (InterruptedException e) {
				return;
			}
    	});
    	
    	updaterThread = new Thread(() -> {
    		try {
        		nParsedModels = 0;
	    		while (oldModels.equals(searchBar.getBMZModels()) && finderThread.isAlive()) {
						Thread.sleep(100);
	    		}
	    		ArrayList<ModelDescriptor> modelsList = createArrayOfNulls(searchBar.countBMZModels(false));
				if (finderThread.isAlive())
					SwingUtilities.invokeLater(() -> setModelsInGui(modelsList));
	    		while (finderThread.isAlive()) {
					Thread.sleep(100);
	            	List<ModelDescriptor> foundModels = new ArrayList<>(searchBar.getBMZModels());
	            	if (foundModels.size() < nParsedModels + 5)
	            		continue;
	            	for (int i = nParsedModels; i < foundModels.size(); i ++) {
	            		setModelInGuiAt(foundModels.get(i), i);
	            	}
	            	nParsedModels = foundModels.size();
	            	
	    		}
	    		if (Thread.currentThread().isInterrupted())
	    			return;
    		} catch (InterruptedException e) {
				return;
			}
    		
    		
    		
        	List<ModelDescriptor> foundModels = searchBar.getBMZModels();
        	for (int i = nParsedModels; i < foundModels.size(); i ++) {
        		int j = 0 + i;
            	SwingUtilities.invokeLater(() -> setModelInGuiAt(foundModels.get(j), j));
        	}
        	SwingUtilities.invokeLater(() -> {
            	this.contentPanel.setProgressIndeterminate(false);
            	this.searchBar.setBarEnabled(true);
            	this.runOnTestButton.setEnabled(true);
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
    	DefaultIcon.closeThreads();
    	if (dwnlThread != null && this.dwnlThread.isAlive())
    		this.dwnlThread.interrupt();
    	if (engineInstallThread != null && this.engineInstallThread.isAlive())
    		this.engineInstallThread.interrupt();
    	if (trackEngineInstallThread != null && this.trackEngineInstallThread.isAlive())
    		this.trackEngineInstallThread.interrupt();
    	if (finderThread != null && this.finderThread.isAlive())
    		this.finderThread.interrupt();
    	if (updaterThread != null && this.updaterThread.isAlive())
    		this.updaterThread.interrupt();
    	if (runninThread != null && runner != null) {
			try {
				runner.close();
				runner = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	if (runninThread != null && this.runninThread.isAlive())
    		this.runninThread.interrupt();
    }
}

/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import deepimagej.DeepImageJ;
import deepimagej.Constants;
import deepimagej.RunnerProgress;
import deepimagej.RunnerDL;
import deepimagej.DeepLearningModel;
import deepimagej.components.BorderPanel;
import deepimagej.components.Hyperlink;
import deepimagej.exceptions.MacrosError;
import deepimagej.modelrunner.EngineInstaller;
import deepimagej.processing.HeadlessProcessing;
import deepimagej.tools.DijRunnerPostprocessing;
import deepimagej.tools.DijRunnerPreprocessing;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import deepimagej.tools.ModelLoader;
import deepimagej.tools.SystemUsage;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.engine.EngineInfo;
import io.bioimage.modelrunner.engine.installation.EngineInstall;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.model.Model;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.versionmanagement.DeepLearningVersion;
import io.bioimage.modelrunner.versionmanagement.InstalledEngines;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.swing.JButton;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class DeepImageJ_Run2 implements PlugIn, ItemListener, Runnable, ActionListener {
	private GenericDialog 				dlg;
	private TextArea					info;
	private Choice[]					choices;
	private Label[]						labels;
	static private String				path		= IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
	private HashMap<String, DeepImageJ>	dps;
	private Log							log			= new Log();
	private int[]						patch;
	private DeepImageJ					dp;
	private HashMap<String, String>		fullnames	= new HashMap<String, String>();
	private String 						loadInfo 	= ""; 
	private String						cudaVersion = "noCUDA";

	private boolean 					batch		= true;
	private boolean 					loadedEngine= false;
	// Array that contains the index of all the models whose yaml is 
	// incorrect so it cannot be loaded
	private ArrayList<Integer>			ignoreModelsIndexList;
	// Array that contains the index of all the models whose yaml Sha256 
	// does not coincide with the sha256
	private ArrayList<Integer>			incorrectSha256IndexList;
	// Array that contains the index of all the models whose yaml is missing 
	private ArrayList<Integer>			missingYamlList;
	// Array containing all the models loaded by the plugin
	private String[] 					items;
	// Check if the plugin is being run from a macro or not
	private boolean 					isMacro = false;
	// Check if the plugin is being run in headless mode or nor
	private boolean 					headless = false;
	// Button used to test model on sample image
	private JButton						testBtn  = new JButton("Run on example image");
	// Whether the plugin can work only on test mode or it can fully function
	private boolean						testModeOnly = false;
	// Whether the plugin is running on test mode
	public boolean						testMode = false;
	// Number of open images, used to check whether an image
	// has been open or not for the testing
	private int nOpenImages = 0;
	
	/**
	 * List of the installed DL frameworks compatible with this OS
	 */
	private List<DeepLearningVersion> installedEngines;
	
	/**
	 * Create the String to engines directory
	 */
	private static final String JARS_DIRECTORY = new File("engines").getAbsolutePath();
	/**
	 * Track of threads that have been opened during execution and have to be closed
	 */
	private ArrayList<Thread> extraThreads = new ArrayList<Thread>();
	/**
	 * Message containing the references to the plugin
	 */
	private static final String REF_MSG = "References: Please cite the model developer and";
	/**
	 * Message containing the references to the plugin
	 */
	private static final String REF_1 = "[1] E. Gómez de Mariscal, DeepImageJ, Nature Methods, 2021";
	/**
	 * Message containing the references to the plugin
	 */
	private static final String REF_2 = "[2] C. García López de Haro, JDLL, arXiv, 2023";
	
	
	static public void main(String args[]) {
		path = new File("models").getAbsolutePath();
		ImagePlus imp = null;
		imp = IJ.openImage("/home/carlos/Downloads/export_qupath_part1_v2.tif");
		if (imp != null)
			imp.show();		WindowManager.setTempCurrentImage(imp);
		new DeepImageJ_Run2().run("");
	}

	@Override
	public void run(String arg) {
		System.out.println("engines jars directory is " + JARS_DIRECTORY);

		
		testMode = false;
		
		headless = GraphicsEnvironment.isHeadless();
//		headless = true; // true only for debug headless testing

		isMacro = IJ.isMacro();
		
		nOpenImages = WindowManager.getImageTitles().length;

		ImagePlus imp = null;
		
		// Check whether the plugin is being run with a macro or not to find
		// if there is an image open or not
		if (!isMacro && WindowManager.getCurrentImage() != null) {
			// Set the batch processing tag to false
			batch = false;
			// As there is an image open, set testModeOnly to false
			testModeOnly = false;
			// Set the image that will be used by the plugin
			imp = WindowManager.getCurrentImage();
		} else if (!isMacro) {
			// If there is no image open, the plugin can only work
			// on test mode
			testModeOnly = true;
		}
		
		// If the plugin is runnning from a macro, the test mode is not allowed,
		// thus an image is required to be open
		if (headless && WindowManager.getCurrentImage() != null) {
			// Set the batch processing tag to false
			batch = true;
			// Set the image that will be used by the plugin
			imp = WindowManager.getCurrentImage();
			System.out.println("[DEBUG] Processing image: " + imp.getTitle());
		} else if (headless && WindowManager.getTempCurrentImage() != null) {
			// Set the batch processing tag to false
			batch = true;
			// Set the image that will be used by the plugin
			imp = WindowManager.getTempCurrentImage();
			System.out.println("[DEBUG] Processing image: " + imp.getTitle());
		} else if (isMacro && WindowManager.getCurrentImage() != null) {
			// Set the batch processing tag to false
			batch = false;
			// Set the image that will be used by the plugin
			imp = WindowManager.getCurrentImage();
		} else if (isMacro && WindowManager.getTempCurrentImage() != null) {
			// Set the batch processing tag to false
			batch = true;
			// Set the image that will be used by the plugin
			imp = WindowManager.getTempCurrentImage();
		} else if (isMacro) {
			// If there is no image open in Macro mode, stop running the plugin
			// because test mode is not available
			IJ.error("There should be an image open.");
			return;
		} else if (headless) {
			// If there is no image open in headless mode, stop running the plugin
			// because test mode is not available
			System.out.println("[DEBUG] No image was provided");
			System.out.println("[DEBUG] End execution");
			return;
		}
		
		// Set the test button disabled until the models are loaded
		testBtn.setEnabled(false);
		testBtn.addActionListener(this);
		
		String[] args = null;
		if (isMacro || headless) {
			// TODO headless run
		} else {
			args = createAndShowDialog();
		}
		
		// Run model using the open image and parameters introduced
		arrangeParametersAndRunModel(imp, args);
		
		// Free memory allocated by the plugin 
		freeIJMemory(dlg, imp);
	    System.out.println("[DEBUG] End execution");
	}
	
	public String[] createAndShowDialog() {
		
		info = new TextArea("Please wait until Tensorflow and Pytorch are loaded...", 12, 65, TextArea.SCROLLBARS_BOTH);
		choices	= new Choice[3];
		labels = new Label[5];
			
		BorderPanel panel = new BorderPanel();
		panel.setLayout(new BorderLayout());
		
		info.setEditable(false);
		panel.add(info, BorderLayout.CENTER);	

		dlg = new GenericDialog("DeepImageJ Run [" + Constants.version + "]");
		
		String[] defaultSelection = new String[] {"<Select a model from this list>"};
		dlg.addChoice("Model", defaultSelection, defaultSelection[0]);
		dlg.addChoice("Format", new String[]         { "Select format" }, "Select format");
		
		dlg.addChoice("Logging", new String[] {"Mute", "Normal", "Debug" }, "Normal");
		
		dlg.addHelp(Constants.url);
		dlg.addPanel(panel);
		String msg = "Note: The output of a deep learning model strongly depends on the data and the" + System.lineSeparator()
					+ "conditions of training process. A pre-trained model may require re-training." + System.lineSeparator()
					+ "Please, check the documentation of each model: Help button";
		dlg.setInsets(-5, 23, 0);
		if (!PlatformDetection.isMacOS())
			dlg.setInsets(-5, 23, -15);
		dlg.addMessage(msg, new Font("Helvetica", Font.BOLD, 12), Color.BLACK);
		dlg.setInsets(-2, 30, 0);
		if (!PlatformDetection.isMacOS())
			dlg.setInsets(dlg.getInsets().top, 26, -5);
		dlg.addMessage(REF_MSG, new Font("Arial", Font.BOLD, 12), Color.BLACK);
		dlg.setInsets(-1, 34, 0);
		if (!PlatformDetection.isMacOS())
			dlg.setInsets(0, 30, -5);
		dlg.addMessage(REF_1, new Font("Arial", Font.BOLD, 12), Color.BLUE);
		dlg.setInsets(0, 34, 5);
		if (!PlatformDetection.isMacOS())
			dlg.setInsets(0, 30, 5);
		dlg.addMessage(REF_2, new Font("Arial", Font.BOLD, 12), Color.BLUE);
		dlg.add(testBtn);
		
		int countChoice = 0;
		int countLabels = 0;
		int countTxt = 0;
		for (Component c : dlg.getComponents()) {
			if (c instanceof Choice) {
				Choice choice = (Choice) c;
				if (countChoice == 0)
					choice.addItemListener(this);
				choices[countChoice++] = choice;
				choices[countChoice - 1].setPreferredSize(new Dimension(234, 20));
			}
			if (c instanceof Label && ((Label) c).getText().trim().length() > 1) {
				labels[countLabels++] = (Label) c;
			}
		}
		labels[8].addMouseListener(Hyperlink.createHyperlink(labels[8], dlg));
		labels[9].addMouseListener(Hyperlink.createHyperlink(labels[9], dlg));
		testMode = false;
		
		dlg.showDialog();
		
		if (dlg.wasCanceled()) {
			// Close every model that has been loaded
			if (dps == null) {
				return null;
			}
			for (String kk : dps.keySet()) {
				if (dps.get(kk).getModel() != null)
					dps.get(kk).getModel().closeModel();
			}
			this.closeAllThreads();
			return null;
		}
		String[] args = retrieveDialogParamas();
		return args;		
	}
	
	public String[] retrieveDialogParamas() {
		// Prior to retrieving the parameters, check  that a correct model has been selected
		String index = Integer.toString(choices[0].getSelectedIndex());
		if ((index.equals("-1") || index.equals("0")) && loadedEngine) {
			IJ.error("Select a valid model.");
			if (!this.isMacro && !this.headless)
				run("");
			return null;
		} else if ((choices[0].getSelectedIndex() == -1 ||choices[0].getSelectedIndex() == 0) && !loadedEngine) {
			IJ.error("Please wait until the Deep Learning engines are loaded.");
			if (!this.isMacro && !this.headless)
				run("");
			return null;
		}
		
		// Array containing all the arguments introduced in the GUI
		String[] args = new String[7];
		// If the plugin is running in test mode, do not use dlg.getNExtChoice
		// because the internal counter of the dlg object does not reset so
		// trying to run something after the test would give an error
		// In addition to this, testing is not macro recordable so it will not
		// be a problem for the macro recorder
		if (!testMode) {
			// The index is the method that is going to be used normally to select a model.
			// The plugin looks at the index of the selection of the user and retrieves the
			// directory associated with it. With this, it allows to have the same model with
			// different configurations in different folders.
			// The next lines are needed so the commands introduced can be recorded by the Macro recorder.
			dlg.getNextChoice();
			dlg.getNextChoice();
			dlg.getNextChoice();
			dlg.getNextChoice();
			dlg.getNextString();
			dlg.getNextString();
			dlg.getNextChoice();
		}
		
		// Select the model name using its index in the list
		String dirname = fullnames.get(index);
		args[0] = dirname;
		
		String format = (String) choices[1].getSelectedItem();
		args[1] = format.toLowerCase();

		String preprocessingFiles = (String) choices[2].getSelectedItem();
		args[2] = preprocessingFiles;
		String postprocessingFiles = (String) choices[3].getSelectedItem();
		args[3] = postprocessingFiles;
		// text[0] will be ignored because it corresponds to the axes and they are fixed
		String patchAxes = texts[0].getText();
		args[4] = patchAxes;
		String patchSize = texts[1].getText();
		args[5] = patchSize;
		String debugMode = (String) choices[4].getSelectedItem();
		args[6] = debugMode.toLowerCase();
		return args;
	}
	
	public void arrangeParametersAndRunModel(ImagePlus imp, String[] args) {
		// If the args are null, something wrong happened
		if (args == null && (headless || isMacro)) {
			IJ.error("Incorrect Macro call");
			return;
		} else if (args == null) {
			return;
		} else if ((headless || isMacro) && dps.keySet().size() == 0) {
			// If no models have been found, do nothing and stop execution
			return;
		}
	}
	
	/**
	 * If the plugin has had any errors during the execution of the model
	 * reset the plugin for another execution
	 * @param imp: test image
	 */
	public void closeAndReopenPlugin(ImagePlus imp) {
		if (!isMacro && !headless) {
			run("");
		}
		// If the plugin is in test mode only, just close the
		// test image. The plugin does not need to be open
		// again because it has not beeen closed
		if (testModeOnly && imp != null) {
			imp.changes = false;
			imp.close();
		}
		// If the execution was being done in testMode, set it to false,
		// as it always has to be false except when performing a test
		if (testMode)
			testMode = false;
	}
	
	/**
	 * Whenever a model is selected or changed, this method updates
	 * the user interface
	 * @param e: the event that represents a model selection
	 */
	public void updateInterface(ItemEvent e) {
		if (e.getSource() == choices[0]) {
			info.setText("");
			int ind = choices[0].getSelectedIndex();
			String fullname = Integer.toString(ind);
			String dirname = fullnames.get(fullname);
			DeepImageJ dp = dps.get(dirname);
			// check that the dp of the selected model 
			// is valid
			boolean goodDp = isGoodDp(dp, ind);
			if (!goodDp)
				return;
			// Set the DL engine
			setDLEngine(dp);
			// Set pre- and post-processing files as options
			setProcessingFiles(dp);
			// Load the model information in the textbox
			info.setText("Loading model info. Please wait...\n");
			
			// TODO generalise for several inputs
			// Get example test input size from the yaml in the case there is no image open.
			// The string is written as "64 x 64 x 3 x 1", with the axes XYCZ
			String testSize = dp.params.inputList.get(0).inputTestSize;
			// Get basic specifications for the input from the yaml
			String tensorForm = dp.params.inputList.get(0).form;
			// Minimum size if it is not fixed, 0s if it is
			int[] tensorMin = dp.params.inputList.get(0).minimum_size;
			// Step if the size is not fixed, 0s if it is
			int[] tensorStep = dp.params.inputList.get(0).step;
			float[] haloSize = ArrayOperations.findTotalPadding(dp.params.inputList.get(0), dp.params.outputList, dp.params.pyramidalNetwork);
			// Get the minimum tile size given by the yaml without batch
			int[] min = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
			// Get the step given by the yaml without batch
			int[] step = DijTensor.getWorkingDimValues(tensorForm, tensorStep);
			// Get the halo given by the yaml without batch 
			float[] haloVals = DijTensor.getWorkingDimValues(tensorForm, haloSize); 
			// Get the axes given by the yaml without batch
			String[] dim = DijTensor.getWorkingDims(tensorForm);
			String optimalPatch = ArrayOperations.optimalPatch(haloVals, dim, step, min, testSize, dp.params.allowPatching);
			// Update the info shown in the GUI
			info.setText("");
			info.setCaretPosition(0);
			// Specify the name and location of the model
			info.append("Name: " + dp.getName().toUpperCase() + "\n");
			info.append("Location: " + new File(dirname).getName() + "\n");
			// Write the author and reference info
			setAuthInfo(dp);
			// Leave a couple of lines between chunks of information
			info.append("\n\n");
			// Write information about the tiling strategy and parameters
			setTilingInfo(dim, min, step, optimalPatch);
			// Write rest of the parameters specified in the yaml file (test images, test runtime, memory...)
			setTestParameters(dp);
			// Set the axes of the model in the GUI's textbox
			setModelAxes(dim);
			// Set the model optimal tile dimensions for the open image. If there
			// is no open image, the testing tile size will be displayed
			setTileSize(dp, step, optimalPatch);
			info.setCaretPosition(0);
			// Enable testing when a correct model is selected
			testBtn.setEnabled(true);
			// If the model is in testMOdeOnly, block the 'OK' button. This button allows running models in open images
			dlg.getButtons()[0].setEnabled(!testModeOnly);
		}
	}
	
	/**
	 * Set the parameters specified in the yaml file (test images, test runtime, memory...)
	 * @param dp:model params
	 */
	public void setTestParameters(DeepImageJ dp) {
		dp.writeParameters(info);
	}
	
	/**
	 * Set in the GUI the information about the tiling. This information
	 * will help the user select their own tiling strategy
	 * @param dim: axes of the model
	 * @param min: minimum size per axis
	 * @param step: step per axis
	 * @param optimalPatch: calculated optimal patch for the image open
	 */
	public void setTilingInfo(String[] dim, int[] min, int[] step, String optimalPatch) {
		HashMap<String, String> letterDefinition = new HashMap<String, String>();
		letterDefinition.put("X", "width");
		letterDefinition.put("Y", "height");
		letterDefinition.put("C", "channels");
		letterDefinition.put("Z", "depth");
		info.append("---- TILING SPECIFICATIONS ----\n");
		String infoString = "";
		for (String dd : dim)
			infoString += dd + ": " + letterDefinition.get(dd) + ", ";
		infoString = infoString.substring(0, infoString.length() - 2);
		info.append(infoString + "\n");
		info.append("  - minimum_size: ");
		String minString = "";
		for (int i = 0; i < dim.length; i ++)
			minString += dim[i] + "=" + min[i] + ", ";
		minString = minString.substring(0, minString.length() - 2);
		info.append(minString + "\n");
		info.append("  - step: ");
		String stepString = "";
		for (int i = 0; i < dim.length; i ++)
			stepString += dim[i] + "=" + step[i] + ", ";
		stepString = stepString.substring(0, stepString.length() - 2);
		info.append(stepString + "\n");
		info.append("\n");
		info.append("Each dimension is calculated as:\n");
		info.append("  - tile_size = minimum_size + step * n, where n is any positive integer\n");
		info.append("\n");
		info.append("Default tile_size for this model: " + optimalPatch + "\n");
		info.append("\n");
		info.setEditable(false);
	}
	
	/**
	 * Set in the information textbox the data about authors and references.
	 * @param dp: model parameters
	 */
	public void setAuthInfo(DeepImageJ dp) {
		// Specify the authors of the model
		String authInfo = "[";
		for (HashMap<String, String> authorMap : dp.params.author) {
			if (!authorMap.get("name").equals(""))
				authInfo += authorMap.get("name") + ", ";
			else
				authInfo += "N/A." + ", ";
		}
		// REplace the last ", " by a "]"
		authInfo = authInfo.substring(0, authInfo.lastIndexOf(", ")) + "]";
		
		info.append("Authors: " + authInfo);
		info.append("\n");
		// Specify the references of the model
		// Create array with al the references
		String[] refs = new String[dp.params.cite.size()];
		for (int i = 0; i < dp.params.cite.size(); i ++) {
			if (dp.params.cite.get(i).get("text").equals("") && !dp.params.cite.get(i).get("doi").equals(""))
				refs[i] = dp.params.cite.get(i).get("doi");
			refs[i] = dp.params.cite.get(i).get("text");
		}
		String refInfo = "N/A.";
		if (refs != null && !Arrays.toString(refs).contentEquals("[]") && refs.length > 0)
			refInfo = Arrays.toString(refs);
		info.append("References: " + refInfo);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		try {
			updateInterface(e);
		} catch (Exception ex) {
			String modelName = choices[0].getSelectedItem();
			setGUIOriginalParameters();
			info.append("\n");
			info.setText("It seems that either the inputs or outputs of the\n"
					+ "model (" + modelName + ") are not well defined in the rdf.yaml.\n"
					+ "Please correct the rdf.yaml or select another model.");
			dlg.getButtons()[0].setEnabled(false);
		}
	}
	
	/*
	 * Free the ImageJ workspace memory by deallocating variables
	 */
	public void freeIJMemory(GenericDialog dlg, ImagePlus imp) {
		// Free memory allocated by the plugin 
		// If it is not headless, there is no GUI, no need to close it
		if (!headless && !isMacro && !testMode)
			dlg.dispose();
		if (dp != null && dp.getModel() != null) {
			dp.getModel().closeModel();
			dp.setModel(null);
		}
		this.dp = null;
		this.dps = null;
		imp = null;
	}
	
	/**
	 * Load all the models present in the models folder of IJ/Fiji
	 */
	public void loadModels() {
		loadModels(null);
	}
	
	/**
	 * Load all the models present in the models folder of IJ/Fiji
	 * @param modelDir
	 * 	directory where the wanted model is located
	 */
	public void loadModels(String modelDir) {
		// FOrmat for the date
		Date now = new Date(); 
		if (!headless && !isMacro) {
			info.setText("Looking for models at --> " + DeepImageJ.cleanPathStr(path) + "\n");
			info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- LOADING MODELS\n");
		}
		// Array that contains the index of all the models whose yaml is 
		// incorrect so it cannot be loaded
		ignoreModelsIndexList = new ArrayList<Integer>();
		incorrectSha256IndexList = new ArrayList<Integer>();
		missingYamlList = new ArrayList<Integer>();
		dps = DeepImageJ.list(path, false, info, modelDir);
		int k = 1;
		items = new String[dps.size() + 1];
		items[0] = "<Select a model from this list>";
		for (String dirname : dps.keySet()) {
			DeepImageJ dp = dps.get(dirname);
			if (dp != null) {
				String fullname = dp.getName();
				if (!dp.params.completeConfig) {
					fullname += " (unavailable)";
					ignoreModelsIndexList.add(k);
				} else if (dp.params.incorrectSha256) {
					fullname += " (wrong sha256)";
					incorrectSha256IndexList.add(k);
				} else if (!dp.presentYaml) {
					fullname += " (missing yaml)";
					missingYamlList.add(k);
				}
				items[k++] = fullname;
				int index = k - 1;
				fullnames.put(Integer.toString(index), dirname);
			}
		}
		if (!headless && !isMacro) {
			choices[0].removeAll();
			for (String item : items)
				choices[0].addItem(item);
			info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- FINISHED LOADING MODELS");
		}
	}
	
	/*
	 * Loading Tensorflow with the ImageJ-Tensorflow Manager and Pytorch with
	 * the DJL takes some time. Normally the GUI would not show until everything is loaded.
	 * In order to show the DeepImageJ Run GUI fast, Tf and Pt are loaded in a separate thread.
	 * In headless mode only loads the engine needed to avoid wasting time. In the case
	 * no input is provided, loads both engines
	 * 
	 */
	public void loadTfAndPytorch() throws IOException {
		findAvailableEngines();
	}
	
	/*
	 * Find out which CUDA version it is being used and if its use is viable toguether with
	 * Tensorflow
	 */
	public String getCudaVersionsCompatible(String engine, String version, boolean gpu) {
		String cudas = null;
		if (engine.equals(EngineInfo.getPytorchKey()) && SystemUsage.MAP_PYTORCH_CUDA.get(version) != null) {
			cudas = SystemUsage.MAP_PYTORCH_CUDA.get(version).toString();
		} else if (engine.equals(EngineInfo.getTensorflowKey()) && SystemUsage.MAP_TF_CUDA.get(version) != null) {
			cudas = SystemUsage.MAP_TF_CUDA.get(version).toString();
		} else if (engine.equals(EngineInfo.getOnnxKey()) && SystemUsage.MAP_ONNX_CUDA.get(version) != null) {
			cudas = SystemUsage.MAP_ONNX_CUDA.get(version).toString();
		} 
		if (cudas == null){
			cudas = "";
		}
		return cudas;
	}
	
	/*
	 * Find out which CUDA version it is being used and if its use is viable toguether with
	 * Tensorflow
	 */
	public void getCUDAInfo(String tfVersion, String ptVersion, String cudaVersion) {
		//  For when no Pt version has been found.
		if (ptVersion == null)
			ptVersion = "";
		
		loadInfo += "\n";
		if (!cudaVersion.contains(File.separator) && !cudaVersion.toLowerCase().equals("nocuda")) {
			loadInfo += "Currently using CUDA " + cudaVersion + ".\n";
			if (tfVersion.contains("GPU"))
				loadInfo += DeepLearningModel.TensorflowCUDACompatibility(tfVersion, cudaVersion) + ".\n";
			loadInfo += DeepLearningModel.PytorchCUDACompatibility(ptVersion, cudaVersion) + ".\n";
		} else if ((cudaVersion.contains("bin") || cudaVersion.contains("libnvvp")) && !cudaVersion.toLowerCase().equals("nocuda")) {
			String[] outputs = cudaVersion.split(";");
			loadInfo += "Found CUDA distribution " + outputs[0] + ".\n";
			if (tfVersion.contains("GPU"))
				loadInfo += DeepLearningModel.TensorflowCUDACompatibility(tfVersion, cudaVersion) + ".\n";
			loadInfo += DeepLearningModel.PytorchCUDACompatibility(ptVersion, cudaVersion) + ".\n";
			loadInfo += "Could not find environment variable:\n - " + outputs[1] + ".\n";
			if (outputs.length == 3)
				loadInfo += "Could not find environment variable:\n - " + outputs[2] + ".\n";
			loadInfo += "Please add the missing environment variables to the path.\n";
			loadInfo += "For more info, visit DeepImageJ Wiki.\n";
		} else if (cudaVersion.toLowerCase().equals("nocuda")) {
			loadInfo += "No CUDA distribution found.\n";
		}
	}
	
	/*
	 * Set the parameters for when no model is selected
	 */
	public void setGUIOriginalParameters() {
		info.setText(loadInfo);
		choices[1].removeAll();
		choices[1].addItem("Select format");
		choices[2].removeAll();
		choices[2].addItem("Select preprocessing");
		choices[3].removeAll();
		choices[3].addItem("Select postprocessing");
		texts[0].setText("");
		texts[1].setText("");
		texts[1].setEditable(false);
		info.setCaretPosition(0);
	}

	/*
	 * For a model whose rdf.yaml file does not contain the necessary information,
	 * indicate which fields have missing information or are incorrect
	 */
	private void setUnavailableModelText(ArrayList<String> fieldsMissing) {
		info.setText("\nThe selected model contains error in the rdf.yaml.\n");
		info.append("The errors are in the following fields:\n");
		for (String err : fieldsMissing)
			info.append(" - " + err + "\n");
		dlg.getButtons()[0].setEnabled(false);
		info.setCaretPosition(0);
	}

	/*
	 * For a model whose rdf.yaml file does not contain the necessary information,
	 * indicate which fields have missing information or are incorrect
	 */
	private void setIncorrectSha256Text() {
		info.setText("\nThe selected model's Sha256 checksum does not agree\n"
				+ "with the one in the rdf.yaml file.\n");
		info.append("The model file might have been modified after creation.\n");
		info.append("Run at your own risk.\n");
		dlg.getButtons()[0].setEnabled(false);
		info.setCaretPosition(0);
	}

	/*
	 * Indicate that a model folder is missing the yaml file
	 */
	private void setMissingYamlText() {
		info.setText("\nThe selected model folder does not contain a rdf.yaml file.\n");
		info.append("The rdf.yaml file contains all the info necessary to run a model.\n");
		info.append("Please select another model.\n");
		dlg.getButtons()[0].setEnabled(false);
		info.setCaretPosition(0);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int ind = choices[0].getSelectedIndex();
		String fullname = Integer.toString(ind);
		String dirname = fullnames.get(fullname);
		DeepImageJ dp = dps.get(dirname);
		// Path to the test image specified in the rdf.yaml in 
		// the >config>deepimagej>test_information part
		String imageName = "";
		if (dp.params.inputList != null && dp.params.inputList.get(0).exampleInput != null)
			imageName = dp.getPath() + new File(dp.params.inputList.get(0).exampleInput).getName();
		// Path to the test image specified in the rdf.yaml in 
		// the >sample_inputs
		String imageName2 = null;
		if (dp.params.sampleInputs != null && dp.params.sampleInputs.length != 0)
			imageName2 = dp.getPath() + new File(dp.params.sampleInputs[0]).getName();
		ImagePlus imp = null;
		// Do not try to read npy files
		boolean notNpy = !(imageName.endsWith(".npy") || imageName.endsWith(".npx") || imageName.endsWith(".np"));
		// Flag to know whether there is a test image already open or not
		boolean openTest = false;
		// Try opening the test image. First try one and if does not open, try the other
		if (new File(imageName).isFile() && notNpy) {
			try {
			imp = IJ.openImage(imageName);
			imp.show();
			openTest = true;
			} catch (Exception ex) {
				// Do nothing
			}
		}
		notNpy = imageName2 != null && !(imageName2.endsWith(".npy") || imageName2.endsWith(".npx") || imageName2.endsWith(".np"));
		if (!openTest && notNpy && imageName2 != null && new File(imageName2).isFile()) {
			try{
				imp = IJ.openImage(imageName2);
				imp.show();
			} catch (Exception ex){
				// Do nothing
			}
		}
		if (imp == null) {
			IJ.error("No test image found defined in the yaml file.");
			return;
		}
		// Simulate clicking on the button "ok" of the GUI to run the model
		Button okay = dlg.getButtons()[0];
		// Create the event of clicking ok
		ActionEvent ee = new ActionEvent(okay, ActionEvent.ACTION_PERFORMED, okay.getActionCommand());
		// Button okay only has one action listener
		// PErform the click action
		okay.getActionListeners()[0].actionPerformed(ee);
		testMode = true;
	}
	
	/**
	 * Close all the threads that have been opened during the execution
	 */
	private void closeAllThreads() {
		extraThreads.stream().forEach(t -> {
			if (t != null)
				t.interrupt();
			t = null;
		});
	}

}
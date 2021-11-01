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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.RunnerTf;
import deepimagej.RunnerProgress;
import deepimagej.RunnerPt;
import deepimagej.DeepLearningModel;
import deepimagej.components.BorderPanel;
import deepimagej.exceptions.MacrosError;
import deepimagej.processing.HeadlessProcessing;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.DijRunnerPostprocessing;
import deepimagej.tools.DijRunnerPreprocessing;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import deepimagej.tools.ModelLoader;
import deepimagej.tools.StartTensorflowService;
import deepimagej.tools.SystemUsage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JButton;


public class DeepImageJ_Run implements PlugIn, ItemListener, Runnable, ActionListener {
	private GenericDialog 				dlg;
	private TextArea					info;
	private Choice[]					choices;
	private TextField[]	    			texts;
	private Label[]						labels;
	static private String				path		= IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
	private HashMap<String, DeepImageJ>	dps;
	private String[]					processingFile = new String[2];
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
	private JButton						testBtn  = new JButton("Test model");
	// Whether the plugin can work only on test mode or it can fully function
	private boolean						testModeOnly = false;
	// Whether the plugin is running on test mode
	public boolean						testMode = false;
	
	
	static public void main(String args[]) {
		path = System.getProperty("user.home") + File.separator + "Google Drive" + File.separator + "ImageJ" + File.separator + "models" + File.separator;
		path = "C:\\Users\\Carlos(tfg)\\Pictures\\Fiji.app\\models" + File.separator;
		path = "C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models" + File.separator;
		//ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models\\Usiigaci_2.1.4\\usiigaci.tif");
		ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models\\sample_input.tif");
		//ImagePlus imp = IJ.createImage("aux", 64, 64, 1, 24);
		imp.show();
		WindowManager.setTempCurrentImage(imp);
		if (imp != null)
			imp.show();
		new DeepImageJ_Run().run("");
	}

	@Override
	public void run(String arg) {
		
		testMode = false;
		
		headless = GraphicsEnvironment.isHeadless();

		isMacro = IJ.isMacro();

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
		if (isMacro && WindowManager.getCurrentImage() != null) {
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
		}
		
		// Set the test button disabled until the models are loaded
		testBtn.setEnabled(false);
		testBtn.addActionListener(this);
		
		String[] args = null;
		if (isMacro || headless) {
			// Macro argument
			String macroArg = Macro.getOptions();
			// Names of the variables needed to run DIJ
			String[] varNames = new String[] {"model", "format", "preprocessing", "postprocessing",
												"axes", "tile", "logging"};
			try {
				args = HeadlessProcessing.retrieveArguments(macroArg, varNames);
			} catch (MacrosError e) {
				IJ.error(e.toString());
				return;
			}
			// If it is a macro, load models, tf and pt directly in the same thread.
			// If this was done in another thread, the plugin would try to execute the
			// models before everything was ready
			loadModels();
			loadTfAndPytorch();
			// Get the index of the model selected in the list of models
			String index = Integer.toString(Index.indexOf(items, args[0]));
			// Select the model name using its index in the list
			args[0] = fullnames.get(index);
		} else {
			args = createAndShowDialog();
		}
		
		// Run model using the open image and parameters introduced
		arrangeParametersAndRunModel(imp, args);
		
		// Free memory allocated by the plugin 
		freeIJMemory(dlg, imp);
	}
	
	public String[] createAndShowDialog() {
		
		info = new TextArea("Please wait until Tensorflow and Pytorch are loaded...", 14, 58, TextArea.SCROLLBARS_BOTH);
		choices	= new Choice[5];
		texts = new TextField[2];
		labels = new Label[8];
			
		BorderPanel panel = new BorderPanel();
		panel.setLayout(new BorderLayout());
		
		info.setEditable(false);
		panel.add(info, BorderLayout.CENTER);	

		dlg = new GenericDialog("DeepImageJ Run [" + Constants.version + "]");
		
		String[] defaultSelection = new String[] {"<Select a model from this list>"};
		dlg.addChoice("Model", defaultSelection, defaultSelection[0]);
		dlg.addChoice("Format", new String[]         { "Select format" }, "Select format");
		dlg.addChoice("Preprocessing ", new String[] { "Select preprocessing " }, "Select preprocessing");
		dlg.addChoice("Postprocessing", new String[] { "Select postprocessing" }, "Select postprocessing");
		
		dlg.addStringField("Axes order", "", 30);
		dlg.addStringField("Tile size", "", 30);
		
		dlg.addChoice("Logging", new String[] { "mute", "normal", "debug" }, "normal");
		
		dlg.addHelp(Constants.url);
		dlg.addPanel(panel);
		String msg = "Note: the output of a deep learning model strongly depends on the\n"
			+ "data and the conditions of the training process. A pre-trained model\n"
			+ "may require re-training. Please, check the documentation of this\n"
			+ "model to get user guidelines: Help button.";
		
		Font font = new Font("Helvetica", Font.BOLD, 12);
		dlg.addMessage(msg, font, Color.BLACK);
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
			if (c instanceof TextField) {
				texts[countTxt ++] = (TextField) c;
			}
			if (c instanceof Label && ((Label) c).getText().trim().length() > 1) {
				labels[countLabels++] = (Label) c;
			}
		}
		texts[0].setEditable(false);
		texts[1].setEditable(false);

		// Load the models and Deep Learning engines in a separate thread if 
		// th plugin is not run from a macro
		Thread thread = new Thread(this);
		thread.start();
		
		// Set the 'ok' button and the model choice
		// combo box disabled until Tf and Pt are loaded
		choices[0].setEnabled(loadedEngine);
		// Set testMode to false as the user wants to execute the model
		// on their particular image by pressing "OK"
		testMode = false;
		
		dlg.showDialog();
		
		if (dlg.wasCanceled()) {
			// Close every model that has been loaded
			if (dps == null) {
				return null;
			}
			for (String kk : dps.keySet()) {
				if (dps.get(kk).getTfModel() != null)
					dps.get(kk).getTfModel().close();
				else if (dps.get(kk).getTorchModel() != null)
					dps.get(kk).getTorchModel().close();
			}
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
			run("");
			return null;
		} else if ((choices[0].getSelectedIndex() == -1 ||choices[0].getSelectedIndex() == 0) && !loadedEngine) {
			IJ.error("Please wait until the Deep Learning engines are loaded.");
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
				}
				// Get the arguments for the model execution
				String dirname = args[0]; String format = args[1]; processingFile[0] = args[2];
				processingFile[1] = args[3]; String patchString = args[5]; String debugMode = args[6];
								
				dp = dps.get(dirname);
				
				// If the plugin is running in test mode, get the test image
				// that has just been displayed
				if (testMode && !isMacro && WindowManager.getCurrentImage() != null) {
					// Set batch mode to false
					batch = false;
					imp = WindowManager.getCurrentImage();
					// Get basic specifications for the input from the yaml
					String tensorForm = dp.params.inputList.get(0).form;
					// Minimum size if it is not fixed, 0s if it is
					int[] tensorMin = dp.params.inputList.get(0).minimum_size;
					// Step if the size is not fixed, 0s if it is
					int[] tensorStep = dp.params.inputList.get(0).step;
					int[] haloSize = ArrayOperations.findTotalPadding(dp.params.inputList.get(0), dp.params.outputList, dp.params.pyramidalNetwork);
					// Get the minimum tile size given by the yaml without batch
					int[] min = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
					// Get the step given by the yaml without batch
					int[] step = DijTensor.getWorkingDimValues(tensorForm, tensorStep);
					// Get the halo given by the yaml without batch 
					int[] haloVals = DijTensor.getWorkingDimValues(tensorForm, haloSize); 
					// Get the axes given by the yaml without batch
					String[] dim = DijTensor.getWorkingDims(tensorForm);
					patchString = ArrayOperations.optimalPatch(haloVals, dim, step, min, dp.params.allowPatching);
				} else if (testMode && !isMacro) {
					// If no image has been displayed there is an error
					IJ.error("No test image has been found in the model folder");
					run("");
					return;
				}
				// Check if the patxh size is editable or not
				boolean patchEditable = false;
				if (!headless && !isMacro && texts[1].isEditable())
					patchEditable = true;
				
				if (debugMode.equals("debug")) {
					log.setLevel(2);
				} else if (debugMode.equals("normal")) {
					log.setLevel(1);
				} else if (debugMode.equals("mute")) {
					log.setLevel(0);
				}
				
				if (log.getLevel() >= 1)
					log.print("Load model: " + dp.getName() + "(" + dirname + ")");
				
				dp.params.framework = format.toLowerCase().contains("pytorch") ? "pytorch" : "tensorflow";
				// Select the needed attachments for the version used
				if (dp.params.framework.toLowerCase().contentEquals("pytorch")) {
					dp.params.attachments = dp.params.ptAttachments;
				} else if (dp.params.framework.toLowerCase().contentEquals("tensorflow")) {
					dp.params.attachments = dp.params.tfAttachments;
				}
				
				if (!headless && !isMacro) {
					info.setText("");
					info.setCaretPosition(0);
					info.append("Loading model. Please wait...\n");
				}


				dp.params.firstPreprocessing = null;
				dp.params.secondPreprocessing = null;
				dp.params.firstPostprocessing = null;
				dp.params.secondPostprocessing = null;
				
				if (!processingFile[0].equals("no preprocessing")) {
					// Workaround for ImageJ Macros. 
					// DeepImageJ always writes the pre and post-processing between brackets,
					// however when runnning the plugin for a macro this does not happen when there is only
					// one processing file. This workaround adds the brackets
					if (isMacro && !processingFile[0].startsWith("["))
						processingFile[0] = "[" + processingFile[0];
					if (isMacro && !processingFile[0].endsWith("]"))
						processingFile[0] = processingFile[0] + "]";
					String[] preprocArray = processingFile[0].substring(processingFile[0].indexOf("[") + 1, processingFile[0].lastIndexOf("]")).split(",");
					dp.params.firstPreprocessing = dp.getPath() + File.separator + preprocArray[0].trim();
					if (preprocArray.length > 1) {
						dp.params.secondPreprocessing = dp.getPath() + File.separator + preprocArray[1].trim();
					}
				}
				
				if (!processingFile[1].equals("no postprocessing")) {
					// Workaround for ImageJ Macros. 
					if (isMacro && !processingFile[1].startsWith("["))
						processingFile[1] = "[" + processingFile[1];
					if (isMacro && !processingFile[1].endsWith("]"))
						processingFile[1] = processingFile[1] + "]";
					String[] postprocArray = processingFile[1].substring(processingFile[1].indexOf("[") + 1, processingFile[1].lastIndexOf("]")).split(",");
					dp.params.firstPostprocessing = dp.getPath() + File.separator + postprocArray[0].trim();
					if (postprocArray.length > 1) {
						dp.params.secondPostprocessing = dp.getPath() + File.separator + postprocArray[1].trim();
					}
				}

				// TODO generalise for several image inputs
				for (DijTensor inp: dp.params.inputList) {
					String tensorForm = inp.form;
					int[] tensorMin = inp.minimum_size;
					int[] min = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
					int[] tensorStep = inp.step;
					int[] step = DijTensor.getWorkingDimValues(tensorForm, tensorStep); 
					String[] dims = DijTensor.getWorkingDims(tensorForm);
	
					int[] haloSize = ArrayOperations.findTotalPadding(inp, dp.params.outputList, dp.params.pyramidalNetwork);
					
					patch = ArrayOperations.getPatchSize(dims, inp.form, patchString, patchEditable);
					if (patch == null) {
						IJ.error("Please, introduce the patch size as integers separated by commas.\n"
								+ "For the axes order 'Y,X,C' with:\n"
								+ "Y=256, X=256 and C=1, we need to introduce:\n"
								+ "'256,256,1'\n"
								+ "Note: the key 'auto' can only be used by the plugin.");
						// Relaunch the plugin
						closeAndReopenPlugin(imp);
						return;
					}
	
					for (int i = 0; i < patch.length; i ++) {
						if(haloSize[i] * 2 >= patch[i] && patch[i] != -1) {
							String errMsg = "Error: Tiles cannot be smaller or equal than 2 times the halo at any dimension.\n"
										  + "Please, either choose a bigger tile size or change the halo in the model.yaml.";
							IJ.error(errMsg);
							// Relaunch the plugin
							closeAndReopenPlugin(imp);
							return;
						}
					}
					for (int i = 0; i < inp.minimum_size.length; i ++) {
						if (inp.step[i] != 0 && (patch[i] - inp.minimum_size[i]) % inp.step[i] != 0 && patch[i] != -1 && dp.params.allowPatching) {
							int approxTileSize = ((patch[i] - inp.minimum_size[i]) / inp.step[i]) * inp.step[i] + inp.minimum_size[i];
							IJ.error("Tile size at dim: " + tensorForm.split("")[i] + " should be product of:\n  " + inp.minimum_size[i] +
									" + " + step[i] + "*N, where N can be any integer >= 0.\n"
										+ "The immediately smaller valid tile size is " + approxTileSize);
							// Relaunch the plugin
							closeAndReopenPlugin(imp);
							return;
						} else if (inp.step[i] == 0 && patch[i] != inp.minimum_size[i]) {
							IJ.error("Patch size at dim: " + tensorForm.split("")[i] + " should be " + inp.minimum_size[i]);
							// Relaunch the plugin
							closeAndReopenPlugin(imp);
							return;
						}
					}
				}
				dp.params.inputList.get(0).recommended_patch = patch;

				ExecutorService service = Executors.newFixedThreadPool(1);
				RunnerProgress rp = null;
				if (!headless) {
					rp = new RunnerProgress(dp, "load", service);
				}
				else {
					System.out.println("[DEBUG] Loading model");
				}

				if (rp!= null && dp.params.framework.contains("tensorflow") && !(new File(dp.getPath() + File.separator + "variables").exists())) {
					info.append("Unzipping Tensorflow model. Please wait...\n");
					rp.setUnzipping(true);
				}
				
				boolean iscuda = DeepLearningModel.TensorflowCUDACompatibility(loadInfo, cudaVersion).equals("");
				ModelLoader loadModel = new ModelLoader(dp, rp, loadInfo.contains("GPU"), iscuda, log.getLevel() >= 1, SystemUsage.checkFiji());

				Future<Boolean> f1 = service.submit(loadModel);
				boolean output = false;
				try {
					output = f1.get();
				} catch (InterruptedException | ExecutionException e) {
					if (rp != null && rp.getUnzipping())
						IJ.error("Unable to unzip model");
					else
						IJ.error("Unable to load model");
					e.printStackTrace();
					if (rp != null)
						rp.stop();
				}
				
				
				// If the user has pressed stop button, stop execution and return
				if (rp != null && rp.isStopped()) {
					service.shutdown();
					rp.dispose();
					// Free memory allocated by the plugin 
					freeIJMemory(dlg, imp);
					return;
				}
				
				// If the model was not loaded, run again the plugin
				if (!output) {
					IJ.error("Load model error: " + (dp.getTfModel() == null || dp.getTorchModel() == null));
					service.shutdown();
					if (!isMacro && !headless)
						run("");
					return;
				}
				
				if (rp != null)
					rp.setService(null);

				calculateImage(imp, rp, service);
				service.shutdown();
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
			int[] haloSize = ArrayOperations.findTotalPadding(dp.params.inputList.get(0), dp.params.outputList, dp.params.pyramidalNetwork);
			// Get the minimum tile size given by the yaml without batch
			int[] min = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
			// Get the step given by the yaml without batch
			int[] step = DijTensor.getWorkingDimValues(tensorForm, tensorStep);
			// Get the halo given by the yaml without batch 
			int[] haloVals = DijTensor.getWorkingDimValues(tensorForm, haloSize); 
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
			// Enable testing when a correct model is selected
			testBtn.setEnabled(true);
			// If the model is in testMOdeOnly, block the 'OK' button. This button allows running models in open images
			dlg.getButtons()[0].setEnabled(!testModeOnly);
		}
	}
	
	/**
	 * Set the model optimal tile dimensions for the open image. If there
	 * is no open image, the testing tile size will be displayed.
	 * @param step: step between allowed tile sizes per axis. If it is zero 
	 * 				for all axes, the tile size will not be able to be modified
	 * @param optimalPatch: previously calculated tile size
	 */
	public void setTileSize(DeepImageJ dp, int[] step, String optimalPatch) {
		texts[1].setText(optimalPatch);
		int auxFixed = 0;
		for (int ss : step)
			auxFixed += ss;

		texts[1].setEditable(true);
		if (!dp.params.allowPatching || dp.params.pyramidalNetwork || auxFixed == 0) {
			texts[1].setEditable(false);
		}
	}
	
	/**
	 * Set in the GUI the axes used by this model
	 * @param dim: array containing the axes used by the model
	 */
	public void setModelAxes(String[] dim) {
		String axesAux = "";
		for (String dd : dim) {axesAux += dd + ",";}
		texts[0].setText(axesAux.substring(0, axesAux.length() - 1));
		texts[0].setEditable(false);
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
	
	/**
	 * Set the pre- and post-processing files as options in the GUI
	 * @param dp: model parameters
	 */
	public void setProcessingFiles(DeepImageJ dp) {
		choices[2].removeAll();
		choices[3].removeAll();
		Set<String> preKeys = dp.params.pre.keySet();
		Set<String> postKeys = dp.params.post.keySet();
		for (String p : preKeys) {
			if (dp.params.pre.get(p) != null)
				choices[2].addItem(Arrays.toString(dp.params.pre.get(p)));
		}
		if (choices[2].getItemCount() == 0)
			choices[2].addItem("no preprocessing");
		
		for (String p : postKeys) {
			if (dp.params.post.get(p) != null)
				choices[3].addItem(Arrays.toString(dp.params.post.get(p)));
		}
		choices[3].addItem("no postprocessing");
	}
	
	/**
	 * Set the Deep Learning engine of the model in the GUI
	 */
	public void setDLEngine(DeepImageJ dp) {		
		if (dp.params.framework.toLowerCase().equals("tensorflow/pytorch")) {
			choices[1].removeAll();
			choices[1].addItem("Select format");
			choices[1].addItem("Tensorflow");
			choices[1].addItem("Pytorch");
		} else if (dp.params.framework.toLowerCase().equals("pytorch")) {
			choices[1].removeAll();
			choices[1].addItem("Pytorch");
		} else if (dp.params.framework.toLowerCase().equals("tensorflow")) {
			choices[1].removeAll();
			choices[1].addItem("Tensorflow");
		}
	}
	
	/**
	 * Method that checks if the model selected has complete information
	 * @param dp: parameters of the model selected
	 * @param ind: index of the model in teh list of models
	 * @return true if the model is fine, false if it is not
	 */
	public boolean isGoodDp(DeepImageJ dp, int ind) {
		if (dp == null) {
			// Do not allow testing of an incorrect model
			testBtn.setEnabled(false);
			setGUIOriginalParameters();
			return false;
		} else if (ignoreModelsIndexList.contains(ind)) {
			// Do not allow testing of an incorrect model
			testBtn.setEnabled(false);
			setUnavailableModelText(dp.params.fieldsMissing);
			return false;
		} else if (incorrectSha256IndexList.contains(ind)) {
			// Do not allow testing of an incorrect model
			testBtn.setEnabled(false);
			setIncorrectSha256Text();
			return false;
		} else if (missingYamlList.contains(ind)) {
			// Do not allow testing of an incorrect model
			testBtn.setEnabled(false);
			setMissingYamlText();
			return false;
		}
		return true;
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
					+ "model (" + modelName + ") are not well defined in the model.yaml.\n"
					+ "Please correct the model.yaml or select another model.");
			dlg.getButtons()[0].setEnabled(false);
		}
	}

	public void calculateImage(ImagePlus inp, RunnerProgress rp, ExecutorService service) {
		
		int runStage = 0;
		try {
			if (log.getLevel() >= 1)
				log.print("start preprocessing");
			if (headless)
				System.out.println("[DEBUG] Pre-processing the images");
			System.out.println("[DEBUG] Image name: " + inp.getTitle());
			DijRunnerPreprocessing preprocess = new DijRunnerPreprocessing(dp, rp, inp, batch, log.getLevel() >= 1);
			Future<HashMap<String, Object>> f0 = service.submit(preprocess);
			HashMap<String, Object> inputsMap = f0.get();
			
			if ((rp != null && rp.isStopped()) || inputsMap == null) {
				// Remove possible hidden images from IJ workspace
				ArrayOperations.removeProcessedInputsFromMemory(inputsMap);
			    service.shutdown();
			    if (rp != null)
			    	rp.dispose();
				return;
			}
			
			if (log.getLevel() >= 1)
				log.print("end preprocessing");
			runStage ++;

			if (headless)
				System.out.println("[DEBUG] Running inference on the tensors");
			if (log.getLevel() >= 1)
				log.print("start runner");
			HashMap<String, Object> output = null;
			if (dp.params.framework.equals("tensorflow")) {
				RunnerTf runner = new RunnerTf(dp, rp, inputsMap, log);
				if (rp != null)
					rp.setRunner(runner);
				Future<HashMap<String, Object>> f1 = service.submit(runner);
				output = f1.get();
			} else {
				RunnerPt runner = new RunnerPt(dp, rp, inputsMap, log);
				if (rp != null)
					rp.setRunner(runner);
				Future<HashMap<String, Object>> f1 = service.submit(runner);
				output = f1.get();
			}
			
			inp.changes = false;
			inp.close();
			
			if (output == null || (rp != null && rp.isStopped())) {
				// Remove possible hidden images from IJ workspace
				ArrayOperations.removeProcessedInputsFromMemory(inputsMap);
				if (rp != null) {
					rp.allowStopping(true);
					rp.stop();
					rp.dispose();
				}
			    service.shutdown();
				return;
			}
			runStage ++;
			
			if (headless)
				System.out.println("[DEBUG] Post-processing the outputs");

			Future<HashMap<String, Object>> f2 = service.submit(new DijRunnerPostprocessing(dp, rp, output));
			output = f2.get();
			
			if (rp != null) {
				rp.allowStopping(true);
				rp.stop();
				rp.dispose();
			}

			// Print the outputs of the postprocessing
			// Retrieve the opened windows and compare them to what the model has outputed
			// Display only what has not already been displayed

			String[] finalFrames = WindowManager.getNonImageTitles();
			String[] finalImages = WindowManager.getImageTitles();
			// If the plugin is running in headless mode, nothing can be displayed
			if (!headless)
				ArrayOperations.displayMissingOutputs(finalImages, finalFrames, output);

			// Remove possible hidden images from IJ workspace
			ArrayOperations.removeProcessedInputsFromMemory(inputsMap);
			
		} catch (IllegalStateException ex) {
			IJ.error("Error during the aplication of the model.\n"
					+ "Pytorch native library not found.");
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			IJ.error("Error during the aplication of the model.");
			ex.printStackTrace();
		} catch (ExecutionException ex) {
			IJ.error("Error during the aplication of the model.");
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
			if (runStage == 0){
				IJ.error("Error during preprocessing.");
			} else if (runStage == 1) {
				IJ.error("Error during the aplication of the model.");
			} else if (runStage == 2) {
				IJ.error("Error during postprocessing.");
			}
		}

		// Close the parallel processes
	    service.shutdown();
	    if (rp != null && !rp.isStopped()) {
			rp.allowStopping(true);
			rp.stop();
			rp.dispose();
	    }
	}
	
	/*
	 * Free the ImageJ workspace memory by deallocating variables
	 */
	public void freeIJMemory(GenericDialog dlg, ImagePlus imp) {
		// Free memory allocated by the plugin 
		if (!headless && !isMacro && !testMode)
			dlg.dispose();
		if (dp != null && dp.params.framework.equals("tensorflow") && dp.getTfModel() != null) {
			dp.getTfModel().session().close();
			dp.getTfModel().close();
		} else if (dp != null && dp.params.framework.equals("pytorch") && dp.getTorchModel() != null) {
			dp.getTorchModel().close();
		}
		this.dp = null;
		this.dps = null;
		imp = null;
	}
	
	/*
	 * Load all the models present in the models folder of IJ/Fiji
	 */
	public void loadModels() {
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
		dps = DeepImageJ.list(path, false, info);
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
	 * the DJL takes some time. Normally the GUI would not sho until everything is loaded.
	 *  In order to show the DeepImageJ Run GUI fast, Tf and Pt are loaded in a separate thread.
	 */
	public void loadTfAndPytorch() {
		loadInfo = "ImageJ";
		boolean isFiji = SystemUsage.checkFiji();
		// FOrmat for the date
		Date now = new Date(); 
		if (!headless && !isMacro) {
			info.append("\n\n");
			info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- LOADING TENSORFLOW JAVA (might take some time)\n");
		}
		// First load Tensorflow
		if (isFiji) {
			loadInfo = StartTensorflowService.loadTfLibrary();
		} else {
			// In order to Pytorch to work we have to set
			// the IJ ClassLoader as the ContextClassLoader
			Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
		}
		// If the version allows GPU, find if there is CUDA
		if (loadInfo.contains("GPU")) 
			cudaVersion = SystemUsage.getCUDAEnvVariables();
		
		if (loadInfo.equals("")) {
			loadInfo += "No Tensorflow library found.\n";
			loadInfo += "Please install a new Tensorflow version.\n";
		} else if (loadInfo.equals("ImageJ")) {
			loadInfo = "Currently using TensorFlow  ";
			loadInfo += DeepLearningModel.getTFVersion(false);
			if (!loadInfo.contains("GPU"))
				loadInfo += "_CPU";
			loadInfo += ".\n";
			loadInfo += "To change the version, consult the DeepImageJ Wiki.\n";
		} else {
			loadInfo += ".\n";
			loadInfo += "To change the TF version go to Edit>Options>Tensorflow.\n";
		}	
		// Then find Pytorch the Pytorch version
		if (!headless && !isMacro)
			info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- LOADING DJL PYTORCH\n");
		String ptVersion = DeepLearningModel.getPytorchVersion();
		loadInfo += "\n";
		loadInfo += "Currently using Pytorch " + ptVersion + ".\n";
		loadInfo += "Supported by Deep Java Library " + ptVersion + ".\n";

		if (!headless && !isMacro)
			info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- Looking for installed CUDA distributions");
		getCUDAInfo(loadInfo, ptVersion, cudaVersion);
		
		loadInfo += "Models' path: " + DeepImageJ.cleanPathStr(path) + "\n";
		loadInfo += "<Please select a model>\n";
		loadedEngine = true;
		if (!headless && !isMacro) {
			info.setText(loadInfo);
			// Allow selecting the wanted model
			choices[0].setEnabled(true);
		}
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
	 * For a model whose model.yaml file does not contain the necessary information,
	 * indicate which fields have missing information or are incorrect
	 */
	private void setUnavailableModelText(ArrayList<String> fieldsMissing) {
		info.setText("\nThe selected model contains error in the model.yaml.\n");
		info.append("The errors are in the following fields:\n");
		for (String err : fieldsMissing)
			info.append(" - " + err + "\n");
		dlg.getButtons()[0].setEnabled(false);
		info.setCaretPosition(0);
	}

	/*
	 * For a model whose model.yaml file does not contain the necessary information,
	 * indicate which fields have missing information or are incorrect
	 */
	private void setIncorrectSha256Text() {
		info.setText("\nThe selected model's Sha256 checksum does not agree\n"
				+ "with the one in the model.yaml file.\n");
		info.append("The model file might have been modified after creation.\n");
		info.append("Run at your own risk.\n");
		dlg.getButtons()[0].setEnabled(false);
		info.setCaretPosition(0);
	}

	/*
	 * Indicate that a model folder is missing the yaml file
	 */
	private void setMissingYamlText() {
		info.setText("\nThe selected model folder does not contain a model.yaml file.\n");
		info.append("The model.yaml file contains all the info necessary to run a model.\n");
		info.append("Please select another model.\n");
		dlg.getButtons()[0].setEnabled(false);
		info.setCaretPosition(0);
	}

	@Override
	/**
	 * Method used to run the needed parallel processes. This processes is
	 * loading the models and the Deep Learning engines (Tensorflow and Pytorch)
	 * when the plugin is started. This task is done in parallel
	 * so some information can be displayed to the user while he waits.
	 */
	public void run() {
		loadModels();
		loadTfAndPytorch();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int ind = choices[0].getSelectedIndex();
		String fullname = Integer.toString(ind);
		String dirname = fullnames.get(fullname);
		DeepImageJ dp = dps.get(dirname);
		
		String imageName = dp.getPath() + dp.params.inputList.get(0).exampleInput;
		ImagePlus imp = null;
		if (new File(imageName).isFile()) {
			imp = IJ.openImage(imageName);
			imp.show();
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

}
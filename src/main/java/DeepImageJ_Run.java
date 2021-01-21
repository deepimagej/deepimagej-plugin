/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we expect you to include adequate citations and acknowledgments whenever you 
 * present or publish results that are based on it.
 * 
 * Reference: DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, L. Donati, M. Unser, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 *
 * Corresponding authors: mamunozb@ing.uc3m.es, daniel.sage@epfl.ch
 *
 */

/*
 * Copyright 2019. Universidad Carlos III, Madrid, Spain and EPFL, Lausanne, Switzerland.
 * 
 * This file is part of DeepImageJ.
 * 
 * DeepImageJ is free software: you can redistribute it and/or modify it under the terms of 
 * the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with DeepImageJ. 
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
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
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ai.djl.pytorch.jni.LibUtils;



public class DeepImageJ_Run implements PlugIn, ItemListener, Runnable {
	private GenericDialog 				dlg;
	private TextArea					info		= new TextArea("Please wait until Tensorflow and Pytorch are loaded...", 13, 58, TextArea.SCROLLBARS_BOTH);
	private Choice[]					choices		= new Choice[5];
	private TextField[]	    			texts		= new TextField[2];
	private Label[]						labels		= new Label[8];
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
	// Array containing all the models loaded by the plugin
	private String[] 					items;
	
	static public void main(String args[]) {
		path = System.getProperty("user.home") + File.separator + "Google Drive" + File.separator + "ImageJ" + File.separator + "models" + File.separator;
		path = "C:\\Users\\Carlos(tfg)\\Pictures\\Fiji.app\\models" + File.separator;
		path = "C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models" + File.separator;
		ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models\\MRCNN\\exampleImage.tiff");
		//ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models\\unet3d\\Substack (50-100).tif");
		//ImagePlus imp = IJ.createImage("aux", 64, 64, 1, 24);
		imp.show();
		WindowManager.setTempCurrentImage(imp);
		if (imp != null)
			imp.show();
		new DeepImageJ_Run().run("");
	}

	@Override
	public void run(String arg) {

		ImagePlus imp = WindowManager.getTempCurrentImage();
		
		if (imp == null) {
			batch = false;
			imp = WindowManager.getCurrentImage();
		}
		
		if (WindowManager.getCurrentImage() == null) {
			IJ.error("There should be an image open.");
			return;
		}
		
		info.setEditable(false);
		
		BorderPanel panel = new BorderPanel();
		panel.setLayout(new BorderLayout());
		panel.add(info, BorderLayout.CENTER);

		dlg = new GenericDialog("DeepImageJ Run [" + Constants.version + "]");
		
		String[] defaultSelection = new String[] {"       <select a model from this list>       "};
		dlg.addChoice("Model", defaultSelection, defaultSelection[0]);
		dlg.addChoice("Format", new String[]         { "-----------------Select format-----------------" }, "-----------------Select format-----------------");
		dlg.addChoice("Preprocessing ", new String[] { "-----------Select preprocessing----------- " }, "-----------Select preprocessing----------- ");
		dlg.addChoice("Postprocessing", new String[] { "-----------Select postprocessing----------" }, "-----------Select postprocessing----------");
		
		dlg.addStringField("Axes order", "", 30);
		dlg.addStringField("Tile size", "", 30);
		
		dlg.addChoice("Logging", new String[] { "mute", "normal                                                       ", "verbose", "debug" }, "normal                                                       ");
		
		dlg.addHelp(Constants.url);
		dlg.addPanel(panel);
		String msg = "Note: the output of a deep learning model strongly depends on the\n"
			+ "data and the conditions of the training process. A pre-trained model\n"
			+ "may require re-training. Please, check the documentation of this\n"
			+ "model to get user guidelines: Help button.";
		
		Font font = new Font("Helvetica", Font.BOLD, 12);
		dlg.addMessage(msg, font, Color.BLACK);
		
		int countChoice = 0;
		int countLabels = 0;
		int countTxt = 0;
		for (Component c : dlg.getComponents()) {
			if (c instanceof Choice) {
				Choice choice = (Choice) c;
				if (countChoice == 0)
					choice.addItemListener(this);
				choices[countChoice++] = choice;
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

		// Load the models and Deep Learning engines in a separate thread
		Thread thread = new Thread(this);
		thread.start();
		
		// Set the 'ok' button and the model choice
		// combo box disabled until Tf and Pt are loaded
		choices[0].setEnabled(loadedEngine);
		
		dlg.showDialog();
		
		if (dlg.wasCanceled()) {
			// Close every model that has been loaded
			for (String kk : dps.keySet()) {
				if (dps.get(kk).getTfModel() != null)
					dps.get(kk).getTfModel().close();
				else if (dps.get(kk).getTorchModel() != null)
					dps.get(kk).getTorchModel().close();
			}
			return;
		}
		
		if (choices[0].getSelectedIndex() == 0 && loadedEngine) {
			IJ.error("Please select a model.");
			run("");
			return;
		} else if (choices[0].getSelectedIndex() == 0 && !loadedEngine) {
			IJ.error("Please wait until the Deep Learning engines are loaded.");
			run("");
			return;
		}
		// This is used for the macro, as in the macro, there is no selection from the list
		String fullname = (String) choices[0].getSelectedItem();
		// The index is the method that is going to be used normally to select a model.
		// The plugin looks at the index of the selection of the user and retrieves the
		// directory associated with it. With this, it allows to have the same model with
		// different configurations in different folders.
		String index = Integer.toString(choices[0].getSelectedIndex());
		// If the plugin is running from a macro, the index of the selected item
		// in the combobox will always be 0. This would yield an error.
		// In order to bypass the error, the index is set to the index the first coincidence
		// in the list of models.
		// For example, the model 'red' is selecte, and the possible models are
		// {'orange', 'red, 'blue'} --> index = "1"
		// has to be selected again 
		if (index.equals("0") == true) {
			index = Integer.toString(Index.indexOf(items, fullname));
		}
		if (index.equals("-1") || index.equals("0")) {
			IJ.error("Select a valid model.");
		}

		String dirname = fullnames.get(index);
		
		log.print("Load model: " + fullname + "(" + dirname + ")");
		dp = dps.get(dirname);
		
		String format = (String) choices[1].getSelectedItem();
		dp.params.framework = format.contains("pytorch") ? "Pytorch" : "Tensorflow";

		processingFile[0] = (String) choices[2].getSelectedItem();
		processingFile[1] = (String) choices[3].getSelectedItem();
		
		info.setText("");
		info.setCaretPosition(0);
		info.append("Loading model. Please wait...\n");


		dp.params.firstPreprocessing = null;
		dp.params.secondPreprocessing = null;
		dp.params.firstPostprocessing = null;
		dp.params.secondPostprocessing = null;
		
		if (!processingFile[0].equals("no preprocessing")) {
			String[] preprocArray = processingFile[0].substring(processingFile[0].indexOf("[") + 1, processingFile[0].lastIndexOf("]")).split(",");
			dp.params.firstPreprocessing = dp.getPath() + File.separator + preprocArray[0].trim();
			if (preprocArray.length > 1) {
				dp.params.secondPreprocessing = dp.getPath() + File.separator + preprocArray[1].trim();
			}
		}
		
		if (!processingFile[1].equals("no postprocessing")) {
			String[] postprocArray = processingFile[1].substring(processingFile[1].indexOf("[") + 1, processingFile[1].lastIndexOf("]")).split(",");
			dp.params.firstPostprocessing = dp.getPath() + File.separator + postprocArray[0].trim();
			if (postprocArray.length > 1) {
				dp.params.secondPostprocessing = dp.getPath() + File.separator + postprocArray[1].trim();
			}
		}
		
		String tensorForm = dp.params.inputList.get(0).form;
		int[] tensorMin = dp.params.inputList.get(0).minimum_size;
		int[] min = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
		int[] tensorStep = dp.params.inputList.get(0).step;
		int[] step = DijTensor.getWorkingDimValues(tensorForm, tensorStep); 
		String[] dims = DijTensor.getWorkingDims(tensorForm);

		
		patch = ArrayOperations.getPatchSize(dims, dp.params.inputList.get(0).form, texts[1].getText(), texts[1].isEditable());
		if (patch == null) {
			IJ.error("Please, introduce the patch size as integers separated by commas.\n"
					+ "For the axes order 'Y,X,C' with:\n"
					+ "Y=256, X=256 and C=1, we need to introduce:\n"
					+ "'256,256,1'\n"
					+ "Note: the key 'auto' can only be used by the plugin.");
			run("");
			return;
		}
		int level = dlg.getNextChoiceIndex();
		log.setLevel(level);
		log.reset();

		for (int i = 0; i < patch.length; i ++) {
			int p = 0 ;
			switch (tensorForm.split("")[i]) {
			case "B":
				p = 1;
				break;
			case "Y":
				p = imp.getHeight();
				break;
			case "X":
				p = imp.getWidth();
				break;
			case "Z":
				p = imp.getNSlices();
				break;
			case "C":
				p = imp.getNChannels();
				break;
		}
			if (p * 3 < patch[i]) {
				String errMsg = "Error: Tiles cannot be bigger than 3 times the image at any dimension\n";
				errMsg += " - X = " + imp.getWidth() + ", maximum tile size at X = " + (imp.getWidth() * 3 - 1) + "\n";
				errMsg += " - Y = " + imp.getHeight() + ", maximum tile size at Y = " + (imp.getHeight() * 3 - 1) + "\n";
				if (tensorForm.contains("C"))
					errMsg += " - C = " + imp.getNChannels() + ", maximum tile size at C = " + (imp.getNChannels() * 3 - 1) + "\n";
				if (tensorForm.contains("Z"))
					errMsg += " - Z = " + imp.getNSlices() + ", maximum tile size at Z = " + (imp.getNSlices() * 3 - 1) + "\n";
				IJ.error(errMsg);
				run("");
				return;
			}
		}
		for (DijTensor inp: dp.params.inputList) {
			for (int i = 0; i < min.length; i ++) {
				if (inp.step[i] != 0 && (patch[i] - inp.minimum_size[i]) % inp.step[i] != 0 && patch[i] != -1 && dp.params.allowPatching) {
					int approxTileSize = ((patch[i] - inp.minimum_size[i]) / inp.step[i]) * inp.step[i] + inp.minimum_size[i];
					IJ.error("Tile size at dim: " + dims[i] + " should be product of:\n  " + min[i] +
							" + " + step[i] + "*N, where N can be any positive integer.\n"
								+ "The immediately smaller valid tile size is " + approxTileSize);
					run("");
					return;
				} else if (inp.step[i] == 0 && patch[i] != inp.minimum_size[i]) {
					IJ.error("Patch size at dim: " + dims[i] + " should be " + min[i]);
					run("");
					return;
				}
			}
		}
		// TODO generalise for several image inputs
		dp.params.inputList.get(0).recommended_patch = patch;

		ExecutorService service = Executors.newFixedThreadPool(1);
		RunnerProgress rp = new RunnerProgress(dp, "load", service);

		if (dp.params.framework.contains("Tensorflow") && !(new File(dp.getPath() + File.separator + "variables").exists())) {
			info.append("Unzipping Tensorflow model. Please wait...\n");
			rp.setUnzipping(true);
		}
		
		ModelLoader loadModel = new ModelLoader(dp, rp, loadInfo.contains("GPU"), DeepLearningModel.TensorflowCUDACompatibility(loadInfo, cudaVersion).equals(""));

		Future<Boolean> f1 = service.submit(loadModel);
		boolean output = false;
		try {
			output = f1.get();
		} catch (InterruptedException | ExecutionException e) {
			if (rp.getUnzipping())
				IJ.error("Unable to unzip model");
			else
				IJ.error("Unable to load model");
			e.printStackTrace();
		}
		
		
		// If the user has pressed stop button, stop execution and return
		if (rp.isStopped()) {
			service.shutdown();
			rp.dispose();
			// Free memory allocated by the plugin 
			freeIJMemory(dlg, imp);
			return;
		}
		
		// If the model was not loaded, run again the plugin
		if (!output) {
			log.print("Load model error: " + (dp.getTfModel() == null || dp.getTorchModel() == null));
			service.shutdown();
			run("");
			return;
		}
		
		rp.setService(null);

		calculateImage(imp, rp, service);
		service.shutdown();
		
		// Free memory allocated by the plugin 
		freeIJMemory(dlg, imp);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == choices[0]) {
			info.setText("");
			int ind = choices[0].getSelectedIndex();
			String fullname = Integer.toString(ind);
			String dirname = fullnames.get(fullname);

			DeepImageJ dp = dps.get(dirname);
			if (dp == null) {
				setGUIOriginalParameters();
				return;
			} else if (ignoreModelsIndexList.contains(ind)) {
				setUnavailableModelText(dp.params.fieldsMissing);
				return;
			} else if (incorrectSha256IndexList.contains(ind)) {
				setIncorrectSha256Text();
				return;
			}
			if (dp.params.framework.equals("Tensorflow/Pytorch")) {
				choices[1].removeAll();
				choices[1].addItem("-----------------Select format-----------------");
				choices[1].addItem("tensorflow_saved_model_bundle");
				choices[1].addItem("pytorch_script");
			} else if (dp.params.framework.equals("Pytorch")) {
				choices[1].removeAll();
				choices[1].addItem("pytorch_script");
			} else if (dp.params.framework.equals("Tensorflow")) {
				choices[1].removeAll();
				choices[1].addItem("tensorflow_saved_model_bundle");
			}

			info.setCaretPosition(0);
			info.append("Loading model info. Please wait...\n");
			
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
				
			// Get basic information about the input from the yaml
			String tensorForm = dp.params.inputList.get(0).form;
			// Patch size if the input size is fixed, all 0s if it is not
			int[] tensorPatch = dp.params.inputList.get(0).recommended_patch;
			// Minimum size if it is not fixed, 0s if it is
			int[] tensorMin = dp.params.inputList.get(0).minimum_size;
			// Step if the size is not fixed, 0s if it is
			int[] tensorStep = dp.params.inputList.get(0).step;
			int[] haloSize = ArrayOperations.findTotalPadding(dp.params.inputList.get(0), dp.params.outputList, dp.params.pyramidalNetwork);
			int[] dimValue = DijTensor.getWorkingDimValues(tensorForm, tensorPatch); 
			int[] min = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
			int[] step = DijTensor.getWorkingDimValues(tensorForm, tensorStep); 
			int[] haloVals = DijTensor.getWorkingDimValues(tensorForm, haloSize); 
			String[] dim = DijTensor.getWorkingDims(tensorForm);
			
			HashMap<String, String> letterDefinition = new HashMap<String, String>();
			letterDefinition.put("X", "width");
			letterDefinition.put("Y", "height");
			letterDefinition.put("C", "channels");
			letterDefinition.put("Z", "depth");


			info.setText("");
			info.setCaretPosition(0);
			info.append("\n");
			info.append("SELECTED MODEL: " + dp.getName().toUpperCase());
			info.append("\n");
			info.append("\n");
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
			String optimalPatch = ArrayOperations.optimalPatch(dimValue, haloVals, dim, step, min, dp.params.allowPatching);
			info.append("\n");
			info.append("Default tile_size for this model: " + optimalPatch + "\n");
			info.append("\n");
			info.setEditable(false);

			dp.writeParameters(info);
			info.setCaretPosition(0);
			
			String axesAux = "";
			for (String dd : dim) {axesAux += dd + ",";}
			texts[0].setText(axesAux.substring(0, axesAux.length() - 1));
			texts[0].setEditable(false);
			
			texts[1].setText(optimalPatch);
			int auxFixed = 0;
			for (int ss : step)
				auxFixed += ss;

			texts[1].setEditable(true);
			if (!dp.params.allowPatching || dp.params.pyramidalNetwork || auxFixed == 0) {
				texts[1].setEditable(false);
			}
			dlg.getButtons()[0].setEnabled(true);
		}
	}

	public void calculateImage(ImagePlus inp, RunnerProgress rp, ExecutorService service) {
		//RunnerProgress rp = new RunnerProgress(dp, "CPU");
		//rp.setVisible(true);
		
		int runStage = 0;
		try {

			log.print("start preprocessing");
			DijRunnerPreprocessing preprocess = new DijRunnerPreprocessing(dp, rp, inp, batch);
			Future<HashMap<String, Object>> f0 = service.submit(preprocess);
			HashMap<String, Object> inputsMap = f0.get();
			
			if (rp.isStopped() || inputsMap == null) {
				// Remove possible hidden images from IJ workspace
				removeProcessedInputsFromMemory(inputsMap);
			    service.shutdown();
				rp.dispose();
				return;
			}
			
			log.print("end preprocessing");
			runStage ++;
			
			log.print("start runner");
			HashMap<String, Object> output = null;
			if (dp.params.framework.equals("Tensorflow")) {
				RunnerTf runner = new RunnerTf(dp, rp, inputsMap, log);
				rp.setRunner(runner);
				// TODO decide what to store at the end of the execution
				Future<HashMap<String, Object>> f1 = service.submit(runner);
				output = f1.get();
			} else {
				String lib = new File(LibUtils.getLibName()).getName();
				if (!lib.toLowerCase().contains("cpu")) {
					rp.setGPU("cpu");
				} else {
					rp.setGPU("gpu");
				}
				RunnerPt runner = new RunnerPt(dp, rp, inputsMap, log);
				rp.setRunner(runner);
				// TODO decide what to store at the end of the execution
				Future<HashMap<String, Object>> f1 = service.submit(runner);
				output = f1.get();
			}
			
			inp.changes = false;
			inp.close();
			
			if (output == null || rp.isStopped()) {
				// Remove possible hidden images from IJ workspace
				removeProcessedInputsFromMemory(inputsMap);
				rp.dispose();
			    service.shutdown();
				return;
			}
			runStage ++;
			//output = ProcessingBridge.runPostprocessing(dp.params, output);

			Future<HashMap<String, Object>> f2 = service.submit(new DijRunnerPostprocessing(dp, rp, output));
			output = f2.get();
			
			rp.allowStopping(true);
			rp.stop();
			rp.dispose();

			// Print the outputs of the postprocessing
			// Retrieve the opened windows and compare them to what the model has outputed
			// Display only what has not already been displayed

			String[] finalFrames = WindowManager.getNonImageTitles();
			String[] finalImages = WindowManager.getImageTitles();
			ArrayOperations.displayMissingOutputs(finalImages, finalFrames, output);

			// Remove possible hidden images from IJ workspace
			removeProcessedInputsFromMemory(inputsMap);
			
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
	    if (!rp.isStopped()) {
			rp.allowStopping(true);
			rp.stop();
			rp.dispose();
	    }
	}
	
	/*
	 * REmove the inputs images that result after preprocessing from the memory of
	 * ImageJ workspace
	 */
	private void removeProcessedInputsFromMemory(HashMap<String, Object> inputsMap) {
		for (String kk : inputsMap.keySet()) {
			if (inputsMap.get(kk) instanceof ImagePlus) {
				((ImagePlus) inputsMap.get(kk)).changes = false;
				((ImagePlus) inputsMap.get(kk)).close();
			}
		}
		
	}
	
	/*
	 * Free the ImageJ workspace memory by deallocating variables
	 */
	public void freeIJMemory(GenericDialog dlg, ImagePlus imp) {
		// Free memory allocated by the plugin 
		dlg.dispose();
		if (dp.params.framework.equals("Tensorflow")) {
			dp.getTfModel().session().close();
			dp.getTfModel().close();
		} else if (dp.params.framework.equals("Pytorch")) {
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
		IJ.log("Lookin for models at --> " + DeepImageJ.cleanPathStr(path));
		// FOrmat for the date
		Date now = new Date(); 
		info.setText(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- LOADING MODELS\n");
		// Array that contains the index of all the models whose yaml is 
		// incorrect so it cannot be loaded
		ignoreModelsIndexList = new ArrayList<Integer>();
		incorrectSha256IndexList = new ArrayList<Integer>();
		dps = DeepImageJ.list(path, false, info);
		int k = 1;
		items = new String[dps.size() + 1];
		items[0] = "       <select a model from this list>       ";
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
				}
				items[k++] = fullname;
				int index = k - 1;
				fullnames.put(Integer.toString(index), dirname);
			}
		}
		choices[0].removeAll();
		for (String item : items)
			choices[0].addItem(item);
		info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- FINISHED LOADING MODELS");
	}
	
	/*
	 * Loading Tensorflow with the ImageJ-Tensorflow Manager and Pytorch with
	 * the DJL takes some time. Normally the GUI would not sho until everything is loaded.
	 *  In order to show the DeepImageJ Run GUI fast, Tf and Pt are loaded in a separate thread.
	 */
	public void loadTfAndPytorch() {
		loadInfo = "ImageJ";
		boolean isFiji = SystemUsage.checkFiji();
		info.append("\n");
		info.append("\n");
		// FOrmat for the date
		Date now = new Date(); 
		info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- LOADING TENSORFLOW JAVA (might take some time)\n");
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
			loadInfo = "Using default TensorFlow version from JAR: TF ";
			loadInfo += DeepLearningModel.getTFVersion(false);
			if (!loadInfo.contains("GPU"))
				loadInfo += "_CPU";
			loadInfo += ".\n";
			loadInfo += "To change the TF version download the corresponding\n"
					  + "libtensorflow and libtensorflow_jni jars and place\n"
					  + "them in the plugins folder.";
			loadInfo += "For more info check DeepImageJ Wiki.\n";
		} else {
			loadInfo += ".\n";
			loadInfo += "To change the TF version go to Edit>Options>Tensorflow.\n";
		}	
		// Then find Pytorch the Pytorch version
		info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- LOADING DJL PYTORCH\n");
		String ptVersion = DeepLearningModel.getPytorchVersion();
		loadInfo += "\n";
		loadInfo += "Currently using Pytorch " + ptVersion + ".\n";
		loadInfo += "Supported by Deep Java Library " + ptVersion + ".\n";

		info.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- Looking for installed CUDA distributions");
		getCUDAInfo(loadInfo, ptVersion, cudaVersion);
		loadInfo += "<Please select a model>\n";
		info.setText(loadInfo);
		loadedEngine = true;
		choices[0].setEnabled(true);
	}
	
	/*
	 * Find out which CUDA version it is being used and if its use is viable toguether with
	 * Tensorflow
	 */
	public void getCUDAInfo(String tfVersion, String ptVersion, String cudaVersion) {

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
		choices[1].addItem("-----------------Select format-----------------");
		choices[2].removeAll();
		choices[2].addItem("-----------Select preprocessing----------- ");
		choices[3].removeAll();
		choices[3].addItem("-----------Select postprocessing-----------");
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
		info.setText("The selected model contains error in the model.yaml.\n");
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
		info.setText("The selected model's Sha256 checksum does not agree\n"
				+ "with the one in the model.yaml file.\n");
		info.append("The model file might have been modified after creation.\n");
		info.append("Run at your own risk.\n");
		dlg.getButtons()[0].setEnabled(false);
		info.setCaretPosition(0);
	}

	@Override
	public void run() {
		loadModels();
		loadTfAndPytorch();
		
	}

}
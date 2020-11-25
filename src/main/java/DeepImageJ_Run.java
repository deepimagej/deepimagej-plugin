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
import java.awt.Component;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.RunnerTf;
import deepimagej.RunnerProgress;
import deepimagej.RunnerPt;
import deepimagej.TensorFlowModel;
import deepimagej.components.BorderPanel;
import deepimagej.exceptions.JavaProcessingError;
import deepimagej.exceptions.MacrosError;
import deepimagej.processing.ProcessingBridge;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import deepimagej.tools.WebBrowser;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class DeepImageJ_Run implements PlugIn, ItemListener {

	private TextArea					info		= new TextArea("Information on the model", 10, 58, TextArea.SCROLLBARS_BOTH);
	private TextArea					shapeSpec	= new TextArea("Shape specifications", 6, 58, TextArea.SCROLLBARS_BOTH);
	private Choice[]					choices		= new Choice[5];
	// TODO private TextField[]	    			texts		= new TextField[1];
	private TextField	    			patchSize	= new TextField();
	// TODO remove private Label[]	    				patchLabel	= new Label[4];
	private Label[]						labels		= new Label[7];
	static private String				path		= IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
	private HashMap<String, DeepImageJ>	dps;
	private String[]					processingFile = new String[2];
	private Log							log			= new Log();
	private int[]						patch;
	private DeepImageJ					dp;
	private HashMap<String, String>		fullnames	= new HashMap<String, String>();
	

	private boolean 					batch		= true;
	
	static public void main(String args[]) {
		path = System.getProperty("user.home") + File.separator + "Google Drive" + File.separator + "ImageJ" + File.separator + "models" + File.separator;
		path = "C:\\Users\\Carlos(tfg)\\Pictures\\Fiji.app\\models" + File.separator;
		path = "C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models" + File.separator;
		ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Desktop\\0066.jpg");
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
		} else {
			
			boolean isDeveloper = false;
	
			dps = DeepImageJ.list(path, log, isDeveloper);
			if (dps.size() == 0) {
				path = path.replace(File.separator + File.separator, File.separator);
				boolean goToPage = IJ.showMessageWithCancel("no models","No available models in " + path +
						".\nPress \"Ok\" and you will be redirected to the deepImageJ models directory.");
				if (goToPage == true) {
					WebBrowser.openDeepImageJ();
				}
				return;
			}
			info.setEditable(false);

			BorderPanel specPanel = new BorderPanel();
			specPanel.setLayout(new BorderLayout());
			specPanel.add(shapeSpec, BorderLayout.CENTER);
			
			BorderPanel panel = new BorderPanel();
			panel.setLayout(new BorderLayout());
			panel.add(info, BorderLayout.CENTER);
	
			GenericDialog dlg = new GenericDialog("DeepImageJ Run [" + Constants.version + "]");
			String[] items = new String[dps.size() + 1];
			items[0] = "<select a model from this list>";
			int k = 1;
			for (String dirname : dps.keySet()) {
				DeepImageJ dp = dps.get(dirname);
				if (dp != null) {
					String fullname = dp.getName();
					items[k++] = fullname;
					int index = k - 1;
					fullnames.put(Integer.toString(index), dirname);
				}
			}

			dlg.addChoice("Model DeepImageJ", items, items[0]);
			dlg.addChoice("Weights framework", new String[] {"------------Select version------------"}, "------------Select version------------");
			dlg.addChoice("Preprocessing ", new String[] { "no preprocessing" }, "no preprocessing");
			dlg.addChoice("Postprocessing", new String[] { "no postprocessing" }, "no postprocessing");
			
			shapeSpec.setCaretPosition(0);
			shapeSpec.setText("");
			shapeSpec.append("---- TILING SPECIFICATIONS ----\n");
			shapeSpec.append("X: width, Y: height, C: channels, Z: depth\n");
			shapeSpec.append(" - minimum_size: C=1, X=8, Y=8\n");
			shapeSpec.append(" - step: C=0, X=8, Y=8\n");
			shapeSpec.append("For every dimension, we need:\n");
			shapeSpec.append(" - patch_size = minimum_size + step * n, where n is any positive integer\n");
			shapeSpec.append("Default patch_size for this model: 1,256,256\n");
			shapeSpec.append("\n");
			shapeSpec.setEditable(false);
			dlg.addPanel(specPanel);
			dlg.addMessage("Axes order -> C,Y,X");
			dlg.addStringField("Patch size (comma separated)", "1,256,256", 20);
			
			
			dlg.addChoice("Logging", new String[] { "mute", "normal", "verbose", "debug" }, "normal");
			
			dlg.addHelp(Constants.url);
			dlg.addPanel(panel);
			
			int countChoice = 0;
			int countLabels = 0;
			// TODO int numericFieldPatch = 0;
			// TODO int labelPatch = 0;
			for (Component c : dlg.getComponents()) {
				if (c instanceof Choice) {
					Choice choice = (Choice) c;
					if (countChoice == 0)
						choice.addItemListener(this);
					choices[countChoice++] = choice;
				}
				if (c instanceof TextField) {
					patchSize = (TextField) c;
				}
				if (c instanceof Label && ((Label) c).getText().trim().length() > 1) {
					labels[countLabels++] = (Label) c;
				}
			}
			
			String loadInfo = TensorFlowModel.loadLibrary();
			if (loadInfo.equals("")) {
				info.setCaretPosition(0);
				info.append("No Tensorflow library found.\n");
				info.append("Please install a new Tensorflow version.\n");
				choices[0].setEnabled(false);
			} else {
				info.setCaretPosition(0);
				info.setText("");
				info.append(loadInfo + ".\n");
				info.append("<Please select a model>\n");
			}
			
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
			// This is used for the macro, as in the macro, there is no selection from the list
			String fullname = dlg.getNextChoice();
			// The index is the method that is going to be used normally to select a model.
			// The plugin looks at the index of the selection of the user and retrieves the
			// directory associated with it. With this, it allows to have the same model with
			// different configurations in different folders.
			String index = Integer.toString(choices[0].getSelectedIndex());
			// If we are running from a macro, the user does not change the model selecting
			// it from the list. Then the selection is 0, which yields an error. So the index
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
			
			String version = dlg.getNextChoice();
			
			// TODO know exactly which file we are loading
			String ptWeightsPath = dp.getPath() + File.separatorChar + "pytorch_script.pt" ;
			
			
			for (int i = 0; i < processingFile.length; i ++)
				processingFile[i] = dlg.getNextChoice();
			
			info.setText("");
			info.setCaretPosition(0);
			info.append("Loading model. Please wait...\n");
			boolean ret = false;
			if (dp.params.framework.equals("Tensorflow")) {
				ret = dp.loadTfModel(true);
			} else if (dp.params.framework.equals("Pytorch")) {
				ret = dp.loadPtModel(ptWeightsPath);
			}
			if (ret == false && dp.params.framework.equals("Tensorflow")) {
				IJ.error("Error loading " + dp.getName() + 
						"\nTry using another Tensorflow version.");
				return;
			} else if (ret == false && dp.params.framework.equals("Pytorch")) {
				IJ.error("Error loading " + dp.getName() + 
						"\nDeepImageJ loads models until Pytorch 1.6.");
				return;
			}
			
			log.print("Load model error: " + (dp.getTfModel() == null || dp.getTorchModel() == null));
	
			if (dp == null) {
				IJ.error("No valid model.");
				return;
			}			

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

			
			patch = getPatchSize(dims, dp.params.inputList.get(0).form, patchSize);
			if (patch == null) {
				IJ.error("Please, introduce the patch size separated by commas.\n"
						+ "For the axes order 'Y,X,C' with:\n"
						+ "Y=256, X=256 and C=1, we need to introduce:\n"
						+ "'256,256,1'");
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
					// TODO add info about dimensions?
					IJ.error("Error: Tiles cannot be bigger than 3 times the image at any dimensio.\n"
							+ "Image");
					return;
				}
			}
			for (DijTensor inp: dp.params.inputList) {
				for (int i = 0; i < min.length; i ++) {
					if (inp.step[i] != 0 && patch[i] % inp.step[i] != 0 && patch[i] != -1 && dp.params.allowPatching) {
						IJ.error("Patch size at dim: " + dims[i] + " should be product of " + min[i] +
								" + " + step[i] + "*X, where X can be any positive integer.");
						return;
					} else if (inp.step[i] == 0 && patch[i] != inp.minimum_size[i]) {
						IJ.error("Patch size at dim: " + dims[i] + " should be " + min[i]);
						return;
					}
				}
			}

			calculateImage(imp);
			
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
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == choices[0]) {
			info.setText("");
			String fullname = Integer.toString(choices[0].getSelectedIndex());
			String dirname = fullnames.get(fullname);

			DeepImageJ dp = dps.get(dirname);
			if (dp == null) {
				info.setCaretPosition(0);
				info.append("<Please select a model>\n");
				info.append("<Please select a model>\n");
				shapeSpec.setCaretPosition(0);
				shapeSpec.setText("");
				shapeSpec.append("---- TILING SPECIFICATIONS ----\n");
				shapeSpec.append("X: width, Y: height, C: channels, Z: depth\n");
				shapeSpec.append("\n");
				shapeSpec.setEditable(false);
				choices[2].removeAll();
				choices[2].addItem("no preprocessing");
				choices[3].removeAll();
				choices[3].addItem("no postprocessing");
				return;
			}
			if (dp.params.framework.equals("Tensorflow/Pytorch")) {
				choices[1].removeAll();
				choices[1].addItem(" -- Select weights framework -- ");
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

			info.setText("");
			info.setCaretPosition(0);
			dp.writeParameters(info, dp.msgChecks);
			info.append("----------- LOAD INFO ------------\n");
			for (String msg : dp.msgLoads)
				info.append(msg + "\n");
			
			choices[2].removeAll();
			choices[3].removeAll();
			HashMap<String, String[]> a = dp.params.pre;
			Set<String> preKeys = dp.params.pre.keySet();
			Set<String> postKeys = dp.params.post.keySet();
			for (String p : preKeys) {
				if (dp.params.pre.get(p) != null)
					choices[2].addItem(Arrays.toString(dp.params.pre.get(p)));
			}
			for (String p : postKeys) {
				if (dp.params.post.get(p) != null)
					choices[3].addItem(Arrays.toString(dp.params.post.get(p)));
			}
			if (choices[2].getItemCount() == 0)
				choices[2].addItem("no preprocessing");
			choices[3].addItem("no postprocessing");
				
			// Get basic information about the input from the yaml
			String tensorForm = dp.params.inputList.get(0).form;
			// Patch size if the input size is fixed, all 0s if it is not
			int[] tensorPatch = dp.params.inputList.get(0).recommended_patch;
			// Minimum size if it is not fixed, 0s if it is
			int[] tensorMin = dp.params.inputList.get(0).minimum_size;
			// Step if the size is not fixed, 0s if it is
			int[] tensorStep = dp.params.inputList.get(0).step;
			int[] haloSize = findTotalPadding(dp.params.inputList.get(0), dp.params.outputList, dp.params.pyramidalNetwork);
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

			shapeSpec.setCaretPosition(0);
			shapeSpec.setText("");
			shapeSpec.append("---- TILING SPECIFICATIONS ----\n");
			String infoString = "";
			for (String dd : dim)
				infoString += dd + ": " + letterDefinition.get(dd) + ", ";
			infoString = infoString.substring(0, infoString.length() - 2);
			shapeSpec.append(infoString + "\n");
			shapeSpec.append(" - minimum_size: ");
			String minString = "";
			for (int i = 0; i < dim.length; i ++)
				minString += dim[i] + "=" + min[i] + ", ";
			minString = minString.substring(0, minString.length() - 2);
			shapeSpec.append(minString + "\n");
			// TODO remove shapeSpec.append(" - step: C=0, X=8, Y=8\n");
			String stepString = "";
			for (int i = 0; i < dim.length; i ++)
				stepString += dim[i] + "=" + step[i] + ", ";
			stepString = minString.substring(0, stepString.length() - 2);
			shapeSpec.append(stepString + "\n");
			shapeSpec.append("For every dimension, we need:\n");
			shapeSpec.append(" - patch_size = minimum_size + step * n, where n is any positive integer\n");
			String optimalPatch = optimalPatch(dimValue, haloVals, dim, step, min, dp.params.allowPatching);
			shapeSpec.append("Default patch_size for this model: " + optimalPatch + "\n");
			shapeSpec.append("\n");
			shapeSpec.setEditable(false);
			
			String axesAux = "";
			for (String dd : dim) {axesAux += dd + ",";}
			labels[4].setText("Axes order -> " + axesAux.substring(0, axesAux.length() - 1));
			
			patchSize.setText(optimalPatch);
			int auxFixed = 0;
			for (int ss : step)
				auxFixed += ss;

			patchSize.setEditable(true);
			if (!dp.params.allowPatching || dp.params.pyramidalNetwork || auxFixed == 0) {
				patchSize.setEditable(false);
			}
		}
	}

	
	public void calculateImage(ImagePlus inp) {
		// Convert RGB image into RGB stack 
		if (batch == false) {
			ImageWindow windToClose = inp.getWindow();
			windToClose.dispose();
			ImagePlus aux = ij.plugin.CompositeConverter.makeComposite(inp);
			inp = aux == null ? inp : aux;
			windToClose.setImage(inp);
			windToClose.setVisible(true);
		} else {
			ImagePlus aux = ij.plugin.CompositeConverter.makeComposite(inp);
			inp = aux == null ? inp : aux;
		}
		
		dp.params.inputList.get(0).recommended_patch = patch;
		int runStage = 0;
		// Create parallel process for calculating the image
		ExecutorService service = Executors.newFixedThreadPool(1);
		try {
			ImagePlus im = inp.duplicate();
			String correctTitle = inp.getTitle();
			im.setTitle("tmp_" + correctTitle);
			if (batch == false) {
				ImageWindow windToClose = inp.getWindow();
				windToClose.dispose();
			}
			
			WindowManager.setTempCurrentImage(inp);
			log.print("start preprocessing");
			HashMap<String, Object> inputsMap = ProcessingBridge.runPreprocessing(inp, dp.params);
			im.setTitle(correctTitle);
			runStage ++;
			if (inputsMap.keySet().size() == 0)
				throw new Exception();
			if (batch == false)
				im.show();
			WindowManager.setTempCurrentImage(null);
			log.print("end preprocessing");
			
			log.print("start progress");
			log.print("start runner");
			RunnerProgress rp = new RunnerProgress(dp);
			HashMap<String, Object> output = null;
			if (dp.params.framework.equals("Tensorflow")) {
				RunnerTf runner = new RunnerTf(dp, rp, inputsMap, log);
				rp.setRunner(runner);
				// TODO decide what to store at the end of the execution
				Future<HashMap<String, Object>> f1 = service.submit(runner);
				output = f1.get();
			} else {
				RunnerPt runner = new RunnerPt(dp, rp, inputsMap, log);
				rp.setRunner(runner);
				// TODO decide what to store at the end of the execution
				Future<HashMap<String, Object>> f1 = service.submit(runner);
				output = f1.get();
			}
			rp.dispose();
			
			inp.changes = false;
			inp.close();
			if (output == null) 
				throw new Exception();
			runStage ++;
			output = ProcessingBridge.runPostprocessing(dp.params, output);

			// Print the outputs of the postprocessing
			// Retrieve the opened windows and compare them to what the model has outputed
			// Display only what has not already been displayed

			String[] finalFrames = WindowManager.getNonImageTitles();
			String[] finalImages = WindowManager.getImageTitles();
			ArrayOperations.displayMissingOutputs(finalImages, finalFrames, output);
			removeProcessedInputsFromMemory(inputsMap);
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			// Close the parallel processes
		    service.shutdown();
			return;
		} catch(MacrosError ex) {
			if (runStage == 0) {
				IJ.error("Error during Macro preprocessing.");
			} else if (runStage == 2) {
				IJ.error("Error during Macro postprocessing.");
			}
			// Close the parallel processes
		    service.shutdown();
			return;
		
		} catch (JavaProcessingError e) {
			if (runStage == 0) {
				IJ.error("Error during Java preprocessing.");
			} else if (runStage == 2) {
				IJ.error("Error during Java postprocessing.");
			}
			// Close the parallel processes
		    service.shutdown();
			return;
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			IJ.log("Exception " + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				IJ.log("line:" + "Error during the application of the model.");
				IJ.log(ste.getClassName());
				IJ.log(ste.getMethodName());
				IJ.log("line:" + ste.getLineNumber());
			}
			// Close the parallel processes
		    service.shutdown();
			return;
		} catch (ExecutionException ex) {
			ex.printStackTrace();
			IJ.log("Exception " + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				IJ.log("line:" + "Error during the application of the model.");
				IJ.log(ste.getClassName());
				IJ.log(ste.getMethodName());
				IJ.log("line:" + ste.getLineNumber());
			}
			// Close the parallel processes
		    service.shutdown();
			return;
		} catch (Exception ex) {
			ex.printStackTrace();
			IJ.log("Exception " + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				IJ.log(ste.getClassName());
				IJ.log(ste.getMethodName());
				IJ.log("line:" + ste.getLineNumber());
			}
			IJ.log("Exception " + ex.getMessage());
			if (runStage == 0){
				IJ.error("Error during preprocessing.");
			} else if (runStage == 1) {
				IJ.error("Error during the aplication of the model.");
			} else if (runStage == 2) {
				IJ.error("Error during postprocessing.");
			}
			// Close the parallel processes
		    service.shutdown();
			return;
		}

		// Close the parallel processes
	    service.shutdown();
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

	public static String optimalPatch(int[] patchSizeArr, int[] haloArr, String[] dimCharArr, int[] stepArr, int[] minArr, boolean allowPatch) {
		// This method looks for the optimal patch size regarding the
		// minimum patch constraint and image size. This is then suggested
		// to the user
		String patch = "";
		for (int ii = 0; ii < patchSizeArr.length; ii ++) {
			String dimChar = dimCharArr[ii];
			int halo = haloArr[ii];
			int min = minArr[ii];
			int patchSize = patchSizeArr[ii];
			int step = stepArr[ii];
			ImagePlus imp = null;
			if (imp == null) {
				imp = WindowManager.getCurrentImage();
			}
			if (imp == null) {
				patch += "100,";
			}
			
			int size = 0;
			switch (dimChar) {
				case "Y":
					size = imp.getHeight();
					break;
				case "X":
					size = imp.getWidth();
					break;
				case "Z":
					size = imp.getNSlices();
					break;
				case "C":
					size = imp.getNChannels();
					break;
			}
			
			if (step != 0 && allowPatch) {
				int optimalMult = (int)Math.ceil((double)(size + 2 * halo) / (double)step) * step;
				if (optimalMult > 3 * size) {
					optimalMult = optimalMult - step;
				}
				if (optimalMult > 3 * size) {
					optimalMult = (int)Math.ceil((double)size / (double)step) * step;
				}
				patch += Integer.toString(optimalMult) + ",";

			} else if (step != 0 && !allowPatch){
				patch += "-,";
			} else if (step == 0){
				patch += min + ",";
			} else if (patchSize != 0){
				patch += patchSize + ",";
			}
		}
		patch = patch.substring(0, patch.length() - 1);
		return patch;
	}
	
	public static int[] getPatchSize(String[] dim, String form, TextField sizes) {
		String[] definedSizes = sizes.getText().split(",");
		int[] patch = new int[form.split("").length]; // Dimensions of the patch: [x, y, c, z]
		int batchInd = Index.indexOf(form.split(""), "B");
		int count = 0;
		for (int c = 0; c < patch.length; c ++) {
			if (c != batchInd){
				try {
					if (definedSizes[count].trim().equals("-")) {
						patch[c] = -1;	
					} else {
						int value = Integer.parseInt(definedSizes[count].trim());
						patch[c] = value;	
					}
					count += 1;
				} catch (Exception ex) {
					return null;
				}				
			} else {
				patch[c] = 1;
			}
		}
		return patch;
	}
	
	
	public static int[] findTotalPadding(DijTensor input, List<DijTensor> outputs, boolean pyramidal) {
		// Create an object of int[] that contains the output dimensions
		// of each patch.
		// This dimensions are always in the form of the input
		String[] targetForm = input.form.split("");
		int[] padding = new int[targetForm.length];
		if (!pyramidal) {
			for (DijTensor out: outputs) {
				if (out.tensorType.contains("image") && !Arrays.equals(out.scale, new float[out.scale.length])) {
					for (int i = 0; i < targetForm.length; i ++) {
						int ind = Index.indexOf(out.form.split(""), targetForm[i]);
						if (ind != -1 && !targetForm[i].equals("b") && (out.offset[ind] + out.halo[ind]) > padding[i])  {
							padding[i] = out.offset[ind] + out.halo[ind];
						}
					}
				}
			}
		}
		return padding;
	}

}
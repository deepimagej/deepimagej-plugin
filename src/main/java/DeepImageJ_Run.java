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
import deepimagej.TensorFlowModel;
import deepimagej.components.BorderPanel;
import deepimagej.exceptions.JavaProcessingError;
import deepimagej.exceptions.MacrosError;
import deepimagej.processing.ProcessingBridge;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.DijTensor;
import deepimagej.tools.FileTools;
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
	private Choice[]					choices		= new Choice[5];
	private TextField[]	    			texts		= new TextField[1];
	private TextField[]	    			patchSize	= new TextField[5];
	private Label[]	    				patchLabel	= new Label[4];
	private Label[]						labels		= new Label[9];
	static private String				path		= IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
	private HashMap<String, DeepImageJ>	dps;
	private String[]					processingFile = new String[2];
	private Log							log			= new Log();
	private int[]						patch;
	private DeepImageJ					dp;
	private HashMap<String, String>		fullnames	= new HashMap<String, String>();
	
	private String						patchString = "Patch size [pixels]: ";

	private boolean 					batch		= true;
	
	static public void main(String args[]) {
		path = System.getProperty("user.home") + File.separator + "Google Drive" + File.separator + "ImageJ" + File.separator + "models" + File.separator;
		path = "C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models" + File.separator;
		ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models\\MRCNN\\exampleImage.tiff");
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
			dlg.addChoice("Model version", new String[] {"------------Select version------------"}, "------------Select version------------");
			dlg.addChoice("Preprocessing ", new String[] { "no preprocessing" }, "no preprocessing");
			dlg.addChoice("Postprocessing", new String[] { "no postprocessing" }, "no postprocessing");
			dlg.addMessage("Minimum size for each patch:");
			dlg.addMessage("Step:");

			dlg.addMessage(patchString);
			int[] examplePatch = new int[]{4, 128, 128, 1};
			String[] exampleDims = new String[] {"Z", "Y", "X", "C"};
			for (int i = 0; i < examplePatch.length; i ++) {
				dlg.addNumericField(exampleDims[i], examplePatch[i], 1);
			}
			
			dlg.addChoice("Logging", new String[] { "mute", "normal", "verbose", "debug" }, "normal");
			
			dlg.addHelp(Constants.url);
			dlg.addPanel(panel);
			
			int countChoice = 0;
			int countTextField = 0;
			int countLabels = 0;
			int numericFieldPatch = 0;
			int numericFieldPad = 0;
			int labelPatch = 0;
			for (Component c : dlg.getComponents()) {
				if (c instanceof Choice) {
					Choice choice = (Choice) c;
					if (countChoice == 0)
						choice.addItemListener(this);
					choices[countChoice++] = choice;
				}
				if (c instanceof TextField && numericFieldPatch < exampleDims.length) {
					patchSize[numericFieldPatch++] = (TextField) c;
				} else if (c instanceof TextField && numericFieldPad < exampleDims.length) {
					texts[countTextField++] = (TextField) c;
				}
				if (c instanceof Label && ((Label) c).getText().length() > 1) {
					labels[countLabels++] = (Label) c;
				} else if (c instanceof Label && labelPatch < exampleDims.length) {
					patchLabel[labelPatch++] = (Label) c;
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
			if (dlg.wasCanceled())
				return;
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
			
			//  Check ig the version has the extension " (default)" and remove it
			int startOfExtension = version.length() - " (default)".length();
			if (dp.params.previousVersions.keySet().size() == 1 && version.substring(startOfExtension).equals(" (default)"))
				version = version.substring(0, startOfExtension);

			if (!version.equals("default (not bioimage.io format)") && !version.contains(" (missing") && !version.contains(" (faulty)")) {
				String weightsPath = dp.getPath() + File.separatorChar + "weights_" + version + ".zip";
				try {
					info.setText("");
					info.setCaretPosition(0);
					info.append("Unzipping the weight. Please wait...\n");
					FileTools.unzipFolder(new File(weightsPath), dp.getPath() + File.separator + "variables");
				} catch (IOException e) {
					e.printStackTrace();
					IJ.error("Could not extract the weights");
					return;
				}
			} else if (version.contains(" (missing") && !version.contains(" (faulty")){
				IJ.error("Please choose a viable version.");
			} 
			
			for (int i = 0; i < processingFile.length; i ++)
				processingFile[i] = dlg.getNextChoice();
			
			info.setText("");
			info.setCaretPosition(0);
			info.append("Loading model. Please wait...\n");
			boolean ret = dp.loadModel(true);
			if (ret == false) {
				IJ.error("Error in loading " + dp.getName() + 
						"\nTry using another Tensorflow version.");
				return;
			}
			if (dp.getTfModel() == null)
				dp.loadModel(true);
			log.print("Load model error: " + (dp.getTfModel() == null));
	
			if (dp == null) {
				IJ.error("No valid model.");
				return;
			}			

			dp.params.firstPreprocessing = null;
			dp.params.secondPreprocessing = null;
			dp.params.firstPostprocessing = null;
			dp.params.secondPostprocessing = null;
			
			if (!processingFile[0].equals("no preprocessing")) {
				if (dp.params.pre.get(processingFile[0]) != null && dp.params.pre.get(processingFile[0]).length > 0) {
					dp.params.firstPreprocessing = dp.getPath() + File.separator + dp.params.pre.get(processingFile[0])[0];
				}
				if(dp.params.pre.get(processingFile[0]) != null && dp.params.pre.get(processingFile[0]).length > 1) {
					dp.params.secondPreprocessing = dp.getPath() + File.separator + dp.params.pre.get(processingFile[0])[1];
				}
			}
			if (!processingFile[1].equals("no postprocessing")) {
				if (dp.params.post.get(processingFile[1]) != null && dp.params.post.get(processingFile[1]).length > 0) {
					dp.params.firstPostprocessing = dp.getPath() + File.separator + dp.params.post.get(processingFile[1])[0];
				}
				if(dp.params.post.get(processingFile[1]) != null && dp.params.post.get(processingFile[1]).length > 1) {
					dp.params.secondPostprocessing = dp.getPath() + File.separator + dp.params.post.get(processingFile[1])[1];
				}
			}
			
			String tensorForm = dp.params.inputList.get(0).form;
			int[] tensorMin = dp.params.inputList.get(0).minimum_size;
			int[] min = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
			int[] tensorStep = dp.params.inputList.get(0).step;
			int[] step = DijTensor.getWorkingDimValues(tensorForm, tensorStep); 
			String[] dims = DijTensor.getWorkingDims(tensorForm);

			
			patch = getPatchSize(dims, dp.params.inputList.get(0).form, patchSize);
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
					if (inp.step[i] != 0 && patch[i] % inp.step[i] != 0) {
						IJ.error("Patch size at dim: " + dims[i] + " should be a multiple of " + step[i]);
						return;
					} else if (inp.step[i] == 0 && patch[i] != inp.minimum_size[i]) {
						IJ.error("Patch size at dim: " + dims[i] + " should " + min[i]);
						return;
					}
				}
			}

			calculateImage(imp);
			
			// Free memory allocated by the plugin 
			this.dp = null;
			this.dps = null;
			imp = null;
			try {
				this.finalize();
			} catch (Throwable e) {
				e.printStackTrace();
				IJ.log("Non critical error");
				IJ.log("Unable to clean memory after plugin execution");
			}
			System.gc();
			this.dp = null;
			this.dps = null;
			imp = null;
			try {
				this.finalize();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.gc();
			System.out.print("");
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
				choices[2].removeAll();
				choices[2].addItem("no preprocessing");
				choices[3].removeAll();
				choices[3].addItem("no postprocessing");
				labels[3].setText("Minimum size for each patch: ");
				labels[4].setText("Step: ");
				return;
			}
			boolean samePbModel = TensorFlowModel.checkSumSavedModel(dp.params);
			if (!samePbModel) {
				info.setCaretPosition(0);
				info.setText("");
				info.append("Model: " + dp.getName() + "\n");
				info.append("DeepImageJ Run cannot open this model.\n");
				info.append("The saved_model.pb has been modified "
						+ "after creating the Bundled Model.\n");
				info.append("Please select another option.\n");
				choices[0].select(0);
				choices[2].removeAll();
				choices[2].addItem("no preprocessing");
				choices[3].removeAll();
				choices[3].addItem("no postprocessing");
				labels[3].setText("Minimum size for each patch: ");
				labels[4].setText("Step: ");
				return;
			}

			info.setCaretPosition(0);
			info.append("Loading model info. Please wait...\n");
			
			HashMap<String, List<String>> versions = TensorFlowModel.checkSumWeighst(dp.params);
			
			if (versions.get("correct").size() == 0 && new File(dp.getPath() + File.separator + "variables").isDirectory()) {
				choices[1].removeAll();
				choices[1].addItem("default (not bioimage.io format)");
				for (String v : versions.get("faulty"))
					choices[1].addItem(v + " (faulty)");
				for (String v : versions.get("missing"))
					choices[1].addItem(v + " (missing)");
			} else if (versions.get("correct").size() == 1) {
				choices[1].removeAll();
				choices[1].addItem(versions.get("correct").get(0) + " (default)");
				for (String v : versions.get("faulty"))
					choices[1].addItem(v + " (faulty)");
				for (String v : versions.get("missing"))
					choices[1].addItem(v + " (missing)");
			} else if (versions.get("correct").size() > 1) {
				choices[1].removeAll();
				for (String v : versions.get("correct"))
					choices[1].addItem(v);
				for (String v : versions.get("faulty"))
					choices[1].addItem(v + " (faulty)");
				for (String v : versions.get("missing"))
					choices[1].addItem(v + " (missing)");
			} else if (versions.get("correct").size() == 0 && !new File(dp.getPath() + File.separator + "variables").isDirectory()) {
				choices[1].removeAll();
				for (String v : versions.get("faulty"))
					choices[1].addItem(v + " (faulty)");
				for (String v : versions.get("missing"))
					choices[1].addItem(v + " (missing)");		
				info.setText("");
				info.setCaretPosition(0);		
				info.append("Name: " + dp.getName() + "\n");
				info.append("Path: " + dp.getPath() + "\n");
				info.append("Could not find a viable version of the weights.");
				choices[2].removeAll();
				choices[2].addItem("no preprocessing");
				choices[3].removeAll();
				choices[3].addItem("no postprocessing");
				labels[3].setText("Minimum size for each patch: ");
				labels[4].setText("Step: ");
				return;
			}
			

			info.setText("");
			info.setCaretPosition(0);
			dp.writeParameters(info, dp.msgChecks);
			info.append("----------- LOAD INFO ------------\n");
			for (String msg : dp.msgLoads)
				info.append(msg + "\n");
			
			if (dp != null) {
				choices[2].removeAll();
				choices[3].removeAll();
				Set<String> preKeys = dp.params.pre.keySet();
				Set<String> postKeys = dp.params.post.keySet();
				for (String p : preKeys) 
					choices[2].addItem(p);
				for (String p : postKeys) 
					choices[3].addItem(p);
				choices[2].addItem("no preprocessing");
				choices[3].addItem("no postprocessing");
			}
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
			
			
			if (Arrays.equals(tensorStep, new int[tensorStep.length])) {
				labels[5].setText("The patch size was fixed by the developer.");
				labels[6].setText("");
			} else {
				String minSize = "Minimum size for each dimension: ";
				String stepSize = "Step for each dimension: ";
				for (int i = 0; i < dim.length; i ++) {
					String val = "" + min[i];
					String s = "" + step[i];
					if (step[i] == 0) {s = "fixed";}
					minSize = minSize + dim[i] + ":" + val + " ";
					stepSize = stepSize + dim[i] + ":" + s + " ";
				}
				labels[5].setText(minSize);
				labels[6].setText(stepSize);
			}
			// Hide all the labels to show only the necessary ones
			for (int i = 0; i < patchLabel.length; i ++) {
				patchLabel[i].setVisible(false);
				
				patchSize[i].setVisible(false);
				patchSize[i].setEditable(true);
				patchSize[i].setEnabled(true);
				
			}
			for (int i = 0; i < dim.length; i ++) {
				patchLabel[i].setVisible(true);
				patchSize[i].setVisible(true);
				patchLabel[i].setText(dim[i]);
				// Set the corresponding value and set whether the
				// text fields are editable or not
				patchSize[i].setText(optimalPatch(dimValue[i], haloVals[i], dim[i], step[i], min[i]));
				patchSize[i].setEditable(step[i] != 0 && dp.params.allowPatching);
				patchSize[i].setEnabled(step[i] != 0 && dp.params.allowPatching);
			}
		}
	}

	
	public void calculateImage(ImagePlus inp) {
		dp.params.inputList.get(0).recommended_patch = patch;
		int runStage = 0;
		// Create parallel process for calculating the image
		ExecutorService service = Executors.newFixedThreadPool(1);
		try {
			ImagePlus im = inp.duplicate();
			String correctTitle = inp.getTitle();
			im.setTitle("tmp_" + correctTitle);
			ImageWindow windToClose = inp.getWindow();
			windToClose.dispose();
			
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
			
			RunnerProgress rp = new RunnerProgress(dp);
			log.print("start tunner");
			RunnerTf runner = new RunnerTf(dp, rp, inputsMap, log);
			rp.setRunner(runner);
			Future<HashMap<String, Object>> f1 = service.submit(runner);
			HashMap<String, Object> output = null;
			output = f1.get();
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
	
	public static String optimalPatch(int patchSize, int halo, String dimChar, int step, int min) {
		// This method looks for the optimal patch size regarding the
		// minimum patch constraint and image size. This is then suggested
		// to the user
		ImagePlus imp = null;
		String patch;
		if (imp == null) {
			imp = WindowManager.getCurrentImage();
		}
		if (imp == null) {
			patch = "100";
			return patch;	
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
		
		if (step != 0) {
			int optimalMult = (int)Math.ceil((double)(size + 2 * halo) / (double)step) * step;
			if (optimalMult > 3 * size) {
				optimalMult = optimalMult - step;
			}
			if (optimalMult > 3 * size) {
				optimalMult = (int)Math.ceil((double)size / (double)step) * step;
			}
			patch = Integer.toString(optimalMult);

		} else if (patchSize != 0){
			patch = "" + patchSize;
		} else {
			patch = "" + min;
		}
		return patch;
	}
	
	public static int[] getPatchSize(String[] dim, String form, TextField[] sizes) {
		int[] patch = new int[form.split("").length]; // Dimensions of the patch: [x, y, c, z]
		int batchInd = Index.indexOf(form.split(""), "B");
		int count = 0;
		for (int c = 0; c < patch.length; c ++) {
			if (c != batchInd){
				int value = Integer.parseInt(sizes[count++].getText());
				patch[c] = value;
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
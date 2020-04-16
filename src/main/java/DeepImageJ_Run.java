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
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.Runner;
import deepimagej.RunnerProgress;
import deepimagej.components.BorderPanel;
import deepimagej.exceptions.MacrosError;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import deepimagej.tools.WebBrowser;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.macro.Interpreter;
import ij.plugin.PlugIn;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DeepImageJ_Run implements PlugIn, ItemListener {

	private TextArea					info		= new TextArea("Information on the model", 5, 48, TextArea.SCROLLBARS_VERTICAL_ONLY);
	private Choice[]					choices		= new Choice[4];
	private TextField[]	    			texts		= new TextField[1];
	private TextField[]	    			patchSize	= new TextField[4];
	private Label[]	    				patchLabel	= new Label[4];
	private TextField[]	    			padSize		= new TextField[4];
	private Label[]	    				padLabel	= new Label[4];
	private Label[]						labels		= new Label[12];
	static private String				path		= IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
	private HashMap<String, DeepImageJ>	dps;
	private String						preprocessingFile;
	private String						postprocessingFile;
	private Log							log			= new Log();
	private int[]						patch;
	private int[]						overlap;
	private DeepImageJ					dp;
	private HashMap<String, String>		fullnames	= new HashMap<String, String>();
	
	private String						patchString = "Patch size [pixels]";
	private String						padString 	= "Overlap size [pixels]";

	private boolean 					batch		= true;
	
	static public void main(String args[]) {
		path = System.getProperty("user.home") + File.separator + "Google Drive" + File.separator + "ImageJ" + File.separator + "models" + File.separator;
		path = "C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models" + File.separator;
		ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models\\ignacio\\exampleImage.tiff");
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
			dlg.addChoice("Preprocessing macro", new String[] { "no preprocessing" }, "no preprocessing");
			dlg.addChoice("Postprocessing macro", new String[] { "no postprocessing" }, "no postprocessing");
			dlg.addMessage("The patch introduced needs to be a multiple "
						+ "of a given number.");
			dlg.addMessage("Test conditions:");
			dlg.addMessage("Model was tested with a XXxYY image of " 
			+ "XxY pixel size.");
			dlg.addMessage("The test consumed XXX Mb of the memory.");
			dlg.addChoice("Logging", new String[] { "mute", "normal", "verbose", "debug" }, "normal");
			dlg.addMessage(patchString);
			int[] examplePatch = new int[]{4, 128, 128, 1};
			int[] examplePad = new int[]{1, 16, 16, 0};
			String[] exampleDims = new String[] {"D", "H", "W", "C"};
			for (int i = 0; i < examplePatch.length; i ++) {
				if (i % 2 != 0)
					dlg.addToSameRow();
				dlg.addNumericField(exampleDims[i], examplePatch[i], 1);
			}
			
			dlg.addMessage(padString);;
			for (int i = 0; i < examplePad.length; i ++) {
				if (i % 2 != 0)
					dlg.addToSameRow();
				dlg.addStringField(exampleDims[i], "" + examplePad[i], 1);
			}
			
			dlg.addHelp(Constants.url);
			dlg.addPanel(panel);
			
			int countChoice = 0;
			int countTextField = 0;
			int countLabels = 0;
			int numericFieldPatch = 0;
			int numericFieldPad = 0;
			int labelPatch = 0;
			int labelPad = 0;
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
					padSize[numericFieldPad++] = (TextField) c;
				} else if (c instanceof TextField && numericFieldPad < exampleDims.length) {
					texts[countTextField++] = (TextField) c;
				}
				if (c instanceof Label && ((Label) c).getText().length() > 1) {
					labels[countLabels++] = (Label) c;
				} else if (c instanceof Label && labelPatch < exampleDims.length) {
					patchLabel[labelPatch++] = (Label) c;
				} else if (c instanceof Label && labelPad < exampleDims.length) {
					padLabel[labelPad++] = (Label) c;
				}
			}
			
			dlg.showDialog();
			if (dlg.wasCanceled())
				return;
			// This is used for the macro, as in teh macro, there is no selection from the list
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
			preprocessingFile = dlg.getNextChoice();
			postprocessingFile = dlg.getNextChoice();
			
			String dirname = fullnames.get(index);
			log.print("Load model: " + fullname + "(" + dirname + ")");
			dp = dps.get(dirname);
			if (dp.getModel() == null)
				dp.loadModel(true);
			log.print("Load model error: " + (dp.getModel() == null));
	
			if (dp == null) {
				IJ.error("No valid model.");
				return;
			}

			String tensorForm = dp.params.inputList.get(0).form;
			int[] tensorMin = dp.params.inputList.get(0).minimum_size;
			int[] multiple = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
			String[] dims = DijTensor.getWorkingDims(tensorForm);

			
			patch = getPatchSize(dims, dp.params.inputList.get(0).form, patchSize);
			overlap = getPatchSize(dims, dp.params.inputList.get(0).form, padSize);
			int level = dlg.getNextChoiceIndex();
			log.setLevel(level);
			log.reset();
			
			for (int i = 0; i < patch.length; i ++) {
				if (overlap[i] > patch[i] * 3) {
					IJ.error("Error: The overlap (" + overlap[i] + ") should be less than 1/3 of the patch size  (" + patch[i] + ")");
					return;
				}
			}
			for (DijTensor inp: dp.params.inputList) {
				for (int i = 0; i < multiple.length; i ++) {
					if (inp.minimum_size[i] != 0 && patch[i] % multiple[i] != 0) {
						IJ.error("Patch size at dim: " + dims[i] + " should be a multiple of " + multiple[i]);
						return;
					}
				}
			}
			//addChangeListener(texts[1], e -> optimalPatch(dp));

			calculateImage(imp);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == choices[0]) {
			info.setText("");
			String fullname = Integer.toString(choices[0].getSelectedIndex());
			String dirname = fullnames.get(fullname);

			DeepImageJ dp = dps.get(dirname);
			if (dp == null)
				return;
			for (String msg : dp.msgChecks)
				info.append(msg + "\n");
			info.setCaretPosition(0);
			dp.writeParameters(info);
			boolean ret = dp.loadModel(true);
			if (ret == false) {
				IJ.error("Error in loading " + dp.getName());
				return;
			}
			for (String msg : dp.msgLoads)
				info.append(msg + "\n");

			if (dp.getModel() != null) {
				choices[1].removeAll();
				for (String p : dp.preprocessing)
					choices[1].addItem(p);
				choices[2].removeAll();
				for (String p : dp.postprocessing)
					choices[2].addItem(p);
			}
			// Get basic information about the input from the yaml
			String tensorForm = dp.params.inputList.get(0).form;
			int[] tensorPatch = dp.params.inputList.get(0).recommended_patch;
			int[] tensorMin = dp.params.inputList.get(0).minimum_size;
			int[] haloSize = findTotalPadding(dp.params.inputList.get(0), dp.params.outputList);
			int[] dimValue = DijTensor.getWorkingDimValues(tensorForm, tensorPatch); 
			int[] multiple = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
			int[] haloVals = DijTensor.getWorkingDimValues(tensorForm, haloSize); 
			String[] dim = DijTensor.getWorkingDims(tensorForm);
			
			if (dp.params.fixedPatch) {
				labels[3].setText("The patch size was fixed by the developer.");
			} else {
				String sentence = "Minimum multiple for each patch dimension: ";
				for (int i = 0; i < dim.length; i ++) {
					String val = "" + multiple[i];
					if (multiple[i] == 0) {val = "fixed";}
					sentence = sentence + dim[i] + ":" + val + " ";
				}
				labels[3].setText(sentence);
			}
			labels[5].setText("Model was tested with "
					+ dp.params.inputSize + " image of " + dp.params.inputPixelSize + " pixel size.");
			labels[6].setText("The test consumed " + dp.params.memoryPeak
					+ " of the memory.");
			
			// Hide all the labels to show only the necessary ones
			for (int i = 0; i < patchLabel.length; i ++) {
				patchLabel[i].setVisible(false);
				padLabel[i].setVisible(false);
				patchSize[i].setVisible(false);
				patchSize[i].setEditable(true);
				padSize[i].setVisible(false);
				padSize[i].setEditable(true);
			}
			for (int i = 0; i < dim.length; i ++) {
				patchLabel[i].setVisible(true);
				padLabel[i].setVisible(true);
				patchSize[i].setVisible(true);
				padSize[i].setVisible(true);
				// Set the corresponding label
				patchLabel[i].setText(dim[i]);
				padLabel[i].setText(dim[i]);
				// Set the corresponding value and set whether the
				// text fields are editable or not
				patchSize[i].setText("" + dimValue[i]);
				patchSize[i].setEditable(multiple[i] != 0);
				padSize[i].setText("" + haloVals[i]);
				// TODO decide what to do with padding
				padSize[i].setEditable(true);
			}
		}
	}

	
	public void calculateImage(ImagePlus inp) {
		dp.params.final_halo = convertXYCZ(overlap, dp.params.inputList.get(0).form);
		dp.params.inputList.get(0).recommended_patch = patch;
		String dir = dp.getPath();
		int runStage = 0;
		// Create parallel process for calculating the image
		ExecutorService service = Executors.newFixedThreadPool(1);
		ImagePlus im = inp.duplicate();
		String correctTitle = inp.getTitle();
		//inp.setTitle("temporalImage");
		im.setTitle("tmp_" + correctTitle);
		ImageWindow windToClose = inp.getWindow();
		windToClose.dispose();
		
		WindowManager.setTempCurrentImage(inp);
		ImagePlus[] out = null;
		try {
			if (preprocessingFile != null) {
				if (!preprocessingFile.trim().toLowerCase().startsWith("no")) {
					String m = dir + preprocessingFile;
					if (new File(m).exists()) {
						log.print("start preprocessing");
						inp = runMacro(m, inp);
						log.print("end preprocessing");
					}
				}
			}
			if (batch == false) {
				im.show();
			}
			WindowManager.setTempCurrentImage(null);
			
			if (inp == null) {
				IJ.error("Something failed in the preprocessing.");
				// Close the parallel processes
			    service.shutdown();
				return;
			}
			
			runStage ++;
			log.print("start progress");
			RunnerProgress rp = new RunnerProgress(dp);
			log.print("start runner");
			Runner runner = new Runner(dp, rp, inp, log);
			rp.setRunner(runner);
			Future<ImagePlus[]> f1 = service.submit(runner);
			
			try {
				out = f1.get();
				inp.changes = false;
				inp.close();
				im.setTitle(correctTitle);
			} catch (InterruptedException e) {
				IJ.error("No model loaded");
			} catch (ExecutionException e) {
				IJ.error("No model loaded");
			}

			if (out == null) {
				log.print("Error, output is null");
				IJ.error("Output is null");
				// Close the parallel processes
			    service.shutdown();
				return;
			}
			runStage ++;
			if (postprocessingFile != null) {
				if (!postprocessingFile.trim().toLowerCase().startsWith("no")) {
					String m = dir + postprocessingFile;
					if (new File(m).exists()) {
						log.print("start postprocessing");
						WindowManager.setTempCurrentImage(out[0]);
						out[0] = runMacro(m, out[0]);
						log.print("end postprocessing");
						out[0] = WindowManager.getCurrentImage();
					}
				}
			}
			//texts[0].setEnabled(dp.params.fixedPatch == false);
			//labels[7].setEnabled(dp.params.fixedPatch == false);
			out[0].show();
			out[0].setSlice(1);
			out[0].getProcessor().resetMinAndMax();
			WindowManager.setTempCurrentImage(null);
			
			// Close the parallel processes
		    service.shutdown();
		} catch(MacrosError ex) {
			if (runStage == 0) {
				IJ.error("Error applying the preprocessing macro to the image.");
			} else if (runStage == 2) {
				IJ.error("Error applying the postprocessing macro to the image.");
			}
			// Close the parallel processes
		    service.shutdown();
			return;
		
		} catch (FileNotFoundException e) {
			if (runStage == 0) {
				IJ.error("There was not preprocessing file found.");
			} else if (runStage == 2) {
				IJ.error("There was not preprocessing file found.");
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
	
	public static String optimalPatch(int minimumSize, String dimChar) {
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
			case "H":
				size = imp.getHeight();
				break;
			case "W":
				size = imp.getWidth();
				break;
			case "D":
				size = imp.getNSlices();
				break;
			case "C":
				size = imp.getNChannels();
				break;
		}
		
		int optimalMult = (int)Math.ceil((double)size / (double)minimumSize) * minimumSize;
		if (optimalMult > 3 * size) {
			optimalMult = optimalMult - minimumSize;
		}
		if (optimalMult > 3 * size) {
			optimalMult = (int)Math.ceil((double)size / (double)minimumSize) * minimumSize;
		}
		patch = Integer.toString(optimalMult);
		return patch;
	}
	
	public ImagePlus runMacro(String macroFile, ImagePlus imp) throws FileNotFoundException, MacrosError {
		String macro = "";
		try {
			macro = new Scanner(new File(macroFile)).useDelimiter("\\Z").next();
		} catch (NoSuchElementException ex) {
			macro ="";
		}
		ImagePlus result = imp;
		Interpreter interp = new Interpreter();
		if (macro.contentEquals("") == false) {
			result = interp.runBatchMacro(macro, imp);
		}
		return result;		
	}
	
	public static int[] getPatchSize(String[] dim, String form, TextField[] sizes) {
		int[] patch = new int[form.split("").length]; // Dimensions of the patch: [x, y, c, z]
		int batchInd = Index.indexOf(form.split(""), "N");
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
	
	public static int[] findTotalPadding(DijTensor input, List<DijTensor> outputs) {
		// Create an object of int[] that contains the output dimensions
		// of each patch.
		// This dimensions are always of the form [x, y, c, d]
		int[] padding = {0, 0, 0, 0};
		String[] target_form = input.form.split("");
		for (DijTensor out: outputs) {
			for (int i = 0; i < target_form.length; i ++) {
				int ind = Index.indexOf(out.form.split(""), target_form[i]);
				if (ind != -1 && (out.offset[ind] + out.halo[ind]) > padding[i] && target_form[i].equals("N") == false) {
					padding[i] = out.offset[ind] + out.halo[ind];
				}
			}
		}
		return padding;
	}
	
	public static int[] convertXYCZ(int[] dims, String form) {
		// Convert from whatever form into "WHCD"
		String[] target = "WHCD".split("");
		int[] targetDim = new int[target.length];
		for (int i = 0; i < target.length; i ++) {
			int ind = Index.indexOf(form.split(""), target[i]);
			if (ind != -1) {
				targetDim[i] = dims[ind];
			}
		}
		return targetDim;
	}

}
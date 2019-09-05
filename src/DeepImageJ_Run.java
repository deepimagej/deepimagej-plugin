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
import java.util.HashMap;

import deepimagej.Constants;
import deepimagej.DeepPlugin;
import deepimagej.Runner;
import deepimagej.RunnerProgress;
import deepimagej.components.BorderPanel;
import deepimagej.tools.Log;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class DeepImageJ_Run implements PlugIn, ItemListener, Runnable {

	private TextArea					info		= new TextArea("Information on the model", 5, 48, TextArea.SCROLLBARS_VERTICAL_ONLY);
	private Choice[]					choices		= new Choice[4];
	private TextField[]					texts		= new TextField[3];
	private Label[]						labels		= new Label[6];
	static private String				path		= IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
	private Thread						thread		= null;
	private HashMap<String, DeepPlugin>	dps;
	private String						preprocessingFile;
	private String						postprocessingFile;
	private Log							log			= new Log();
	private int							patch;
	private int							overlap;
	private DeepPlugin					dp;
	private HashMap<String, String>		fullnames	= new HashMap<String, String>();

	static public void main(String args[]) {
		// path = System.getProperty("user.home") + File.separator + "Desktop" +
		// File.separator + "ImageJ" + File.separator + "models" + File.separator;
		// path = "C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models"+ File.separator;
		path = System.getProperty("user.home") + File.separator + "Google Drive" + File.separator + "ImageJ" + File.separator + "models" + File.separator;
		// ImagePlus imp = IJ.openImage(path + "iso_reconstruction" + File.separator +
		// "exampleImage.tiff");
		//path = "C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models" + File.separator;
		ImagePlus imp = IJ.openImage(path + "b" + File.separator + "exampleImage.tiff");
		imp.show();
		new DeepImageJ_Run().run("");
	}

	@Override
	public void run(String arg) {

		boolean isDeveloper = false;

		dps = DeepPlugin.list(path, log, isDeveloper);
		if (dps.size() == 0) {
			IJ.error("No available models in " + path);
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
			DeepPlugin dp = dps.get(dirname);
			if (dp != null) {
				String fullname = dp.getName();
				items[k++] = fullname;
				fullnames.put(fullname, dirname);
			}
		}

		dlg.addChoice("Model DeepImageJ", items, items[0]);
		dlg.addChoice("Preprocessing macro", new String[] { "no preprocessing" }, "no preprocessing");
		dlg.addChoice("Postprocessing macro", new String[] { "no postprocessing" }, "no postprocessing");
		dlg.addNumericField("Patch size [pixels]", 128, 0);
		dlg.addNumericField("Overlap size [pixels]", 16, 0);
		dlg.addChoice("Logging", new String[] { "mute", "normal", "verbose", "debug" }, "normal");
		dlg.addHelp(Constants.url);
		dlg.addPanel(panel);

		int countChoice = 0;
		int countTextField = 0;
		int countLabels = 0;
		for (Component c : dlg.getComponents()) {
			if (c instanceof Choice) {
				Choice choice = (Choice) c;
				if (countChoice == 0)
					choice.addItemListener(this);
				choices[countChoice++] = choice;
			}
			if (c instanceof TextField) {
				texts[countTextField++] = (TextField) c;
			}
			if (c instanceof Label) {
				labels[countLabels++] = (Label) c;
			}
		}
		// buttons[0].setEnabled(false);
		dlg.showDialog();
		if (dlg.wasCanceled())
			return;
		String fullname = dlg.getNextChoice();
		preprocessingFile = dlg.getNextChoice();
		postprocessingFile = dlg.getNextChoice();
		patch = (int) dlg.getNextNumber();
		overlap = (int) dlg.getNextNumber();
		int level = dlg.getNextChoiceIndex();
		log.setLevel(level);
		log.reset();

		if (overlap * 3 > patch) {
			IJ.error("Error: The overlap (" + overlap + ") should be less than 1/3 of the patch size  (" + patch + ")");
			return;
		}
		String dirname = fullnames.get(fullname);
		log.print("Load model: " + fullname + "(" + dirname + ")");
		dp = dps.get(dirname);
		if (dp.getModel() == null)
			dp.loadModel();
		log.print("Load model error: " + (dp.getModel() == null));

		if (dp == null) {
			IJ.error("No valid model.");
			return;
		}

		if (thread == null) {
			thread = new Thread(this);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		else {
			IJ.error("No model loaded");
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == choices[0]) {
			info.setText("");
			String fullname = (String) choices[0].getSelectedItem();
			String dirname = fullnames.get(fullname);

			DeepPlugin dp = dps.get(dirname);
			if (dp == null)
				return;
			for (String msg : dp.msgChecks)
				info.append(msg + "\n");
			info.setCaretPosition(0);
			dp.writeParameters(info);
			boolean ret = dp.loadModel();
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
			texts[0].setEnabled(dp.params.fixedPatch == false);
			labels[3].setEnabled(dp.params.fixedPatch == false);
			texts[0].setText("" + dp.params.patch);
			texts[1].setText("" + dp.params.padding);
		}
	}

	@Override
	public void run() {
		dp.params.padding = overlap;
		dp.params.patch = patch;
		String dir = dp.getPath();
		if (preprocessingFile != null) {
			if (!preprocessingFile.trim().toLowerCase().startsWith("no")) {
				String m = dir + preprocessingFile;
				if (new File(m).exists()) {
					log.print("start preprocessing");
					IJ.runMacroFile(m);
					log.print("end preprocessing");
				}
			}
		}

		try {
			log.print("start progress");
			RunnerProgress rp = new RunnerProgress(dp);
			log.print("start runner");
			Runner runner = new Runner(dp, rp, log);
			rp.setRunner(runner);
			ImagePlus out = runner.call();

			if (out == null) {
				log.print("Error, output is null");
				IJ.error("Output is null");
				thread = null;
				return;
			}
			log.print("display " + out.getTitle());
			out.show();
			out.setSlice(1);
			out.getProcessor().resetMinAndMax();
		}
		catch (Exception ex) {
			IJ.log("Exception " + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				IJ.log(ste.getClassName());
				IJ.log(ste.getMethodName());
				IJ.log("line:" + ste.getLineNumber());
			}
			IJ.log("Exception " + ex.getMessage());
			thread = null;
			return;
		}
		if (postprocessingFile != null) {
			if (!postprocessingFile.trim().toLowerCase().startsWith("no")) {
				String m = dir + postprocessingFile;
				if (new File(m).exists()) {
					log.print("start postprocessing");
					IJ.runMacroFile(m);
					log.print("end postprocessing");
				}
			}
		}
		thread = null;
	}

}
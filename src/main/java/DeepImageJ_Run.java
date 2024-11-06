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

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.SwingUtilities;

import deepimagej.IjAdapter;
import deepimagej.Runner;
import deepimagej.gui.Gui;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.PlugIn;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptorFactory;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.tensor.Tensor;
import io.bioimage.modelrunner.utils.Constants;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class DeepImageJ_Run implements PlugIn {

	private String modelFolder;
	private String inputFolder;
	private String outputFolder;
	
	/**
	 * Keys required to run deepImageJ with a macro
	 */
	final static String[] macroKeys = new String[] {"modelFolder="};
	/**
	 * Optional keys to run deepImageJ with a macro or in headless mode
	 */
	final static String[] macroOptionalKeys = new String[] {"inputFolder=", "saveOuputFolder="};
	
	
	static public void main(String args[]) {
		new DeepImageJ_Run().run("");
	}
	@Override
	public void run(String arg) {
		File modelsDir = new File("models");
		if (!modelsDir.isDirectory() && !modelsDir.mkdir())
			throw new RuntimeException("Unable to create 'models' folder inside ImageJ/Fiji directory. Please create it yourself.");
	    boolean isMacro = IJ.isMacro();
	    boolean isHeadless = GraphicsEnvironment.isHeadless();
	    if (!isMacro) {
	    	runGUI();
	    } else if (isMacro && !isHeadless) {
	    	runMacro();
	    } else if (isHeadless) {
	    	runHeadless();
	    }
	}
	
	private void runGUI() {
		final Gui[] guiRef = new Gui[1];
	    if (SwingUtilities.isEventDispatchThread())
	    	guiRef[0] = new Gui(new IjAdapter());
	    else {
		    SwingUtilities.invokeLater(() -> {
		        guiRef[0] = new Gui(new IjAdapter());
		    });
	    }
	}
	
	/**
	 * Macro:
	 * "DeepImageJ Run"
	 */
	private <T extends RealType<T> & NativeType<T>> void runMacro() {
		parseCommand();
		Runner runner;
		try {
			ModelDescriptor model = ModelDescriptorFactory.readFromLocalFile(modelFolder + File.separator + Constants.RDF_FNAME);
			runner = Runner.create(model);
			runner.load();
			if (this.inputFolder != null && !(new File(this.inputFolder).isDirectory()))
				throw new IllegalArgumentException("The provided input folder does not exist: " + this.inputFolder);
			else if (this.inputFolder != null) {
				for (File ff : new File(this.inputFolder).listFiles()) {
					ImagePlus imp = IJ.openImage(ff.getAbsolutePath());
					List<Tensor<T>> res = runner.run(null);
				}
			} else {
				ImagePlus imp = WindowManager.getCurrentImage();
				List<Tensor<T>> res = runner.run(null);
			}
		} catch (ModelSpecsException | IOException | LoadModelException | RunModelException e) {
			e.printStackTrace();
			return;
		}
	}
	
	
	private void runHeadless() {
		
	}
	
	private void parseCommand() {
		String macroArg = Macro.getOptions();

		// macroArg = "modelFolder=NucleiSegmentationBoundaryModel";
		// macroArg = "modelFolder=NucleiSegmentationBoundaryModel saveOutputFolder=null";
		// macroArg = "modelFolder=[StarDist H&E Nuclei Segmentation] inputFolder=null saveOuputFolder=null";

		modelFolder = parseArg(macroArg, macroKeys[0], true);
		inputFolder = parseArg(macroArg, macroOptionalKeys[0], false);
		outputFolder = parseArg(macroArg, macroOptionalKeys[1], false);
	}
	
	private static String parseArg(String macroArg, String arg, boolean required) {
		int modelFolderInd = macroArg.indexOf(arg);
		if (modelFolderInd == -1 && required)
			throw new IllegalArgumentException("DeepImageJ macro requires to set the variable '" + arg + "'.");
		else if (modelFolderInd == -1)
			return null;
		int modelFolderInd2 = macroArg.indexOf(arg + "[");
		int endInd = macroArg.indexOf(" ", modelFolderInd);
		String value;
		if (modelFolderInd2 != -1) {
			endInd = macroArg.indexOf("] ", modelFolderInd2);
			value = macroArg.substring(modelFolderInd2 + arg.length() + 1, endInd);
		} else {
			value = macroArg.substring(modelFolderInd + arg.length(), endInd);
		}
		return value;
	}

}
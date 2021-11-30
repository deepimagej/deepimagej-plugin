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

package deepimagej.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.Callable;

import deepimagej.DeepImageJ;
import deepimagej.RunnerProgress;
import deepimagej.exceptions.JavaProcessingError;
import deepimagej.exceptions.MacrosError;
import deepimagej.processing.ProcessingBridge;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;

// TODO generalize for pre and post processing
public class DijRunnerPreprocessing implements Callable<HashMap<String, Object>>{
	private DeepImageJ dp;
	private RunnerProgress rp;
	private ImagePlus inp;
	private boolean batch;
	public 	String	error = "";
	private boolean composite = false;
	private boolean show;
	
	public DijRunnerPreprocessing(DeepImageJ dp, RunnerProgress rp, ImagePlus inp, boolean batch, boolean show) {
		this.dp = dp;
		this.rp = rp;
		this.inp = inp;
		this.batch = batch;
		this.show = show;
	}

	@Override
	public HashMap<String, Object> call() throws Exception  {
		System.out.println("[DEBUG] Start pre-processing");
		if (rp != null) {
			// Set tag of rp to 'preprocessing' so it shows the correct information
			rp.setInfoTag("preprocessing");
			// Show the progress window if it is not showing
			if (!rp.isVisible())
				rp.setVisible(this.show);
		}

		// Auxiliary variables for DIJ_Run
		ImagePlus im = null;
		String correctTitle = "";
		// Variable indicating whether the plugin is in Run mode or Build
		boolean dev = true;
		if (inp != null) {
			// This piece of code is only used in DIJ_Run
			// Convert RGB image into RGB stack 
			dev = false;
			// Check if an image is RGB Color (1channel). If it is the case transform it into RGB Stack (3channels).
			makeComposite();
			
			if (rp != null && rp.isStopped()) {
				return null;
			}
			
			// Create copy of input image to have it after the original image
			// has been modified
			im = inp.duplicate();
			correctTitle = inp.getTitle();
			im.setTitle("tmp_" + correctTitle);
			if (batch == false) {
				ImageWindow windToClose = inp.getWindow();
				windToClose.dispose();
			}
			WindowManager.setTempCurrentImage(inp);
			if (rp != null && rp.isStopped()) {
				return null;
			}
		}
		
		HashMap<String, Object> inputsMap = null;
		if (inp == null) {
			inp = dp.params.testImage;
			// Check if an image is RGB Color (1channel). If it is the case transform it into RGB Stack (3channels).
			makeComposite();
		}
		
		if (rp != null)
			rp.allowStopping(false);
		try {
			inputsMap = ProcessingBridge.runPreprocessing(inp, dp.params);
		}catch(MacrosError ex) {
			ex.printStackTrace();
			error = "Error during Macro preprocessing.";
			if (composite)
				error += "\nNote that the plugin is internally converting the RGB Color into RGB Stack.";
			if (lookForRunRGBStack())
				error += "\nThe command 'run(\"RGB Stack\");' has been found in macro preporcessing.\n"
						+ "Please remove it to avoid conflicts.";
			IJ.error(error);
			System.out.println("[DEBUG] " + error);
			if (rp != null)
				rp.allowStopping(true);
			removeProcessedImageAndShowOriginal(dev, im, correctTitle);
			return inputsMap;
		} catch (JavaProcessingError e) {
			e.printStackTrace();
			error = "Error during Java preprocessing.";
			error += "\n" + e.getJavaError();
			if (composite)
				error += "\nNote that the plugin is internally converting the RGB Color into RGB Stack.";
			if (lookForRunRGBStack())
				error += "\nThe command 'run(\"RGB Stack\");' has been found in the macro preporcessing.\n"
						+ "Please remove it to avoid conflicts.";
			IJ.error(error);
			System.out.println("[DEBUG] " + error);
			if (rp != null)
				rp.allowStopping(true);
			removeProcessedImageAndShowOriginal(dev, im, correctTitle);
			return inputsMap;
		} catch (Exception e) {
			e.printStackTrace();
			error = "Error during Java preprocessing.";
			if (composite)
				error += "\nNote that the plugin is internally converting the RGB Color into RGB Stack.";
			if (lookForRunRGBStack())
				error += "\nThe command 'run(\"RGB Stack\");' has been found in the macro preporcessing.\n"
						+ "Please remove it to avoid conflicts.";
			IJ.error(error);
			System.out.println("[DEBUG] " + error);
			if (rp != null)
				rp.allowStopping(true);
			removeProcessedImageAndShowOriginal(dev, im, correctTitle);
			return inputsMap;
		}
		
		showOriginalImage(dev, im, correctTitle);		

		if (rp != null)
			rp.allowStopping(true);
		// Check if the user has tried to stop the execution while loading the model
		// If they have return false and stop
		if(rp != null && rp.isStopped())
			return null;
		
		if (inputsMap.keySet().size() == 0)
			throw new Exception();

		System.out.println("[DEBUG] End pre-processing");
		return inputsMap;
	}
	
	/**
	 * When an error occurs, the image processed is left many times in the ImageJ workspace.
	 * This method checks if the image is in the workspace and not showing, and if it is,
	 * in DIJ Runit removes it and shows the original not processed image. In Build Bundled model
	 * it show it again
	 * @param dev: whether we are on Build bundled model or not
	 * @param im: the duplicate of theoriginal image
	 * @param correctTitle: the title of the original image
	 */
	public void removeProcessedImageAndShowOriginal(boolean dev, ImagePlus im,  String correctTitle) {
		if (!inp.getWindow().isShowing() && !dev) {
			inp.changes = false;
			inp.close();
			showOriginalImage(dev, im, correctTitle);
		} else if (!inp.getWindow().isShowing() && dev) {
			inp.getWindow().setVisible(true);
		}
	}
	
	/**
	 * In DIJ Run, show the original image (without the pre-processing changes,
	 * after the pre-processing is executed
	 * @param dev: whether we are on Build bundled model or not
	 * @param im: the duplicate of theoriginal image
	 * @param correctTitle: the title of the original image
	 */
	public void showOriginalImage(boolean dev, ImagePlus im,  String correctTitle) {
		if (!dev) {
			im.setTitle(correctTitle);
			if (!batch)
				im.show();
			WindowManager.setTempCurrentImage(null);
		}
		
	}
	
	/*
	 * Ckecks if the image to be processed is RGB Color and converts it into RGB Stack
	 * if it is the case
	 */
	private void makeComposite() {
		if (!batch && inp.getType() == 4) {
			ImageWindow windToClose = inp.getWindow();
			windToClose.dispose();
			ImagePlus aux = ij.plugin.CompositeConverter.makeComposite(inp);
			// If aux is not null, it means that aux contains a RGB Stack image
			if (aux != null) {
				IJ.log("Converting RGB Color image (1 channel) into RGB Stack (3 channels)");
				IJ.log("Be careful with any macro command that has conflicts with RGB Stack images, for example: 'run(\"RGB Stack\");'");
				composite = true;
			}
			inp = aux == null ? inp : aux;
			windToClose.setImage(inp);
			windToClose.setVisible(true);
		} else if (batch &&  inp.getType() == 4){
			System.out.println("[DEBUG] Pre-processing: transform the RGB Color image into RGB Stack");
			ImagePlus aux = ij.plugin.CompositeConverter.makeComposite(inp);
			// If aux is not null, it means that aux contains a RGB Stack image
			if (aux != null) {
				composite = true;
			}
			inp = aux == null ? inp : aux;
		}
		
	}

	/*
	 * Look for the command 'run("RGB Stack");' in the macro.
	 * Executing it after it has already been done by the macro might cause
	 * an error in the macro preprocessing. If the string is found in the
	 * macro files, return , else, false.
	 */
	public boolean lookForRunRGBStack() {
		String str2find = "run(\"RGB Stack\")";
		if (dp.params.firstPreprocessing != null && (dp.params.firstPreprocessing.contains(".txt") || dp.params.firstPreprocessing.contains(".ijm"))) {
			// Get the file as a String
			try {
				byte[] encoded = Files.readAllBytes(Paths.get(dp.params.firstPreprocessing));
				String fileStr =  new String(encoded, StandardCharsets.UTF_8);
				if (fileStr.contains(str2find))
					return true;
			} catch (IOException e) {
			}
		}
		if (dp.params.secondPreprocessing != null && (dp.params.secondPreprocessing.contains(".txt") || dp.params.secondPreprocessing.contains(".ijm"))) {
			// Get the file as a String
			try {
				byte[] encoded = Files.readAllBytes(Paths.get(dp.params.secondPreprocessing));
				String fileStr =  new String(encoded, StandardCharsets.UTF_8);
				if (fileStr.contains(str2find))
					return true;
			} catch (IOException e) {
			}
		}
		return false;
	}
}

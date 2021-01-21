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
package deepimagej.tools;

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
	
	public DijRunnerPreprocessing(DeepImageJ dp, RunnerProgress rp, ImagePlus inp, boolean batch) {
		this.dp = dp;
		this.rp = rp;
		this.inp = inp;
		this.batch = batch;
	}

	@Override
	public HashMap<String, Object> call() throws Exception  {
		
		// Set tag of rp to 'preprocessing' so it shows the correct information
		rp.setInfoTag("preprocessing");
		// Show the progress window if it is not showing
		if (!rp.isVisible())
			rp.setVisible(true);
		
		// Auxiliary variables for DIJ_Run
		ImagePlus im = null;
		String correctTitle = "";
		// Variable indicating whether the plugin is in Run mode or Build
		boolean dev = true;
		if (inp != null) {
			// This piece of code is only used in DIJ_Run
			// Convert RGB image into RGB stack 
			dev = false;
			if (batch == false) {
				ImageWindow windToClose = inp.getWindow();
				windToClose.dispose();
				ImagePlus aux = ij.plugin.CompositeConverter.makeComposite(inp);
				inp = aux == null ? inp : aux;
				windToClose.setImage(inp);
				windToClose.setVisible(true);
				IJ.log("Converting RGB Color image (1 channel) into RGB Stack (3 channels)");
			} else {
				ImagePlus aux = ij.plugin.CompositeConverter.makeComposite(inp);
				inp = aux == null ? inp : aux;
			}
			
			if (rp.isStopped()) {
				return null;
			}
			
	
			im = inp.duplicate();
			correctTitle = inp.getTitle();
			im.setTitle("tmp_" + correctTitle);
			if (batch == false) {
				ImageWindow windToClose = inp.getWindow();
				windToClose.dispose();
			}
			
			WindowManager.setTempCurrentImage(inp);
			
			if (rp.isStopped()) {
				return null;
			}
		}

		HashMap<String, Object> inputsMap = null;
		if (inp == null)
			inp = dp.params.testImage;
		
		rp.allowStopping(false);
		try {
			inputsMap = ProcessingBridge.runPreprocessing(inp, dp.params);
		}catch(MacrosError ex) {
			ex.printStackTrace();
			IJ.error("Error during Macro preprocessing.");
			error = "Error during Macro preprocessing.";
			rp.allowStopping(true);
			return inputsMap;
		} catch (JavaProcessingError e) {
			e.printStackTrace();
			IJ.error("Error during Java preprocessing.");
			error = "Error during Java preprocessing.";
			rp.allowStopping(true);
			return inputsMap;
		}
		
		if (!dev) {
			im.setTitle(correctTitle);
			if (batch == false)
				im.show();
			WindowManager.setTempCurrentImage(null);
		}
		

		rp.allowStopping(true);
		// Check if the user has tried to stop the execution while loading the model
		// If they have return false and stop
		if(rp.isStopped())
			return null;
		
		if (inputsMap.keySet().size() == 0)
			throw new Exception();
		
		return inputsMap;
	}

}

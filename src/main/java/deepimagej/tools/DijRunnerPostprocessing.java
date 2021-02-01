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

// TODO generalize for pre and post processing
public class DijRunnerPostprocessing implements Callable<HashMap<String, Object>>{
	private DeepImageJ dp;
	private RunnerProgress rp;
	private HashMap<String, Object> output;
	public	String	error = "";
	
	public DijRunnerPostprocessing(DeepImageJ dp, RunnerProgress rp, HashMap<String, Object> output) {
		this.dp = dp;
		this.rp = rp;
		this.output = output;
	}

	@Override
	public HashMap<String, Object> call() {

		// Set tag of rp to 'preprocessing' so it shows the correct information
		rp.setInfoTag("postprocessing");
		
		// The thread cannot be stopped while loading a model, thus block the button
		// while executing the task
		rp.allowStopping(false);
		HashMap<String, Object> fin = null;
		try {
			fin = ProcessingBridge.runPostprocessing(dp.params, output);
		} catch (MacrosError e) {
			e.printStackTrace();
			error = "Error during Macro postprocessing.";
			IJ.error(error);
			rp.allowStopping(true);
			return null;
		} catch (JavaProcessingError e) {
			e.printStackTrace();
			error = "Error during Java postprocessing.";
			error += "\n" + e.getJavaError();
			IJ.error(error);
			rp.allowStopping(true);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			error = "Error during Java postprocessing.";
			IJ.error(error);
			rp.allowStopping(true);
			return null;
		}

		rp.allowStopping(true);
		// Check if the user has tried to stop the execution while loading the model
		// If they have return false and stop
		if(rp.isStopped())
			return null;
		
		return fin;
	}

}

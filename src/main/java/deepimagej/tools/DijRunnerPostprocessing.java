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
		if (rp != null)
			rp.setInfoTag("postprocessing");
		
		// The thread cannot be stopped while loading a model, thus block the button
		// while executing the task
		if (rp != null)
			rp.allowStopping(false);
		HashMap<String, Object> fin = null;
		try {
			fin = ProcessingBridge.runPostprocessing(dp.params, output);
		} catch (MacrosError e) {
			e.printStackTrace();
			error = "Error during Macro postprocessing.";
			IJ.error(error);
			if (rp != null)
				rp.allowStopping(true);
			return null;
		} catch (JavaProcessingError e) {
			e.printStackTrace();
			error = "Error during Java postprocessing.";
			error += "\n" + e.getJavaError();
			IJ.error(error);
			if (rp != null)
				rp.allowStopping(true);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			error = "Error during Java postprocessing.";
			IJ.error(error);
			if (rp != null)
				rp.allowStopping(true);
			return null;
		}

		if (rp != null)
			rp.allowStopping(true);
		// Check if the user has tried to stop the execution while loading the model
		// If they have return false and stop
		if (rp!= null && rp.isStopped())
			return null;
		
		return fin;
	}

}

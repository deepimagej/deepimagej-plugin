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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;


import deepimagej.DeepImageJ;
import deepimagej.DeepLearningModel;
import deepimagej.RunnerProgress;
import ij.IJ;
import io.bioimage.modelrunner.model.Model;

public class ModelLoader implements Callable<Boolean>{
	private DeepImageJ dp;
	private RunnerProgress rp;
	private boolean gpu;
	private boolean cuda;
	private boolean show;
	private Model model;
	
	public ModelLoader(DeepImageJ dp, Model model, RunnerProgress rp, boolean gpu, boolean cuda, boolean show) {
		this.rp = rp;
		this.gpu = gpu;
		this.cuda = cuda;
		this.show = show;
		this.model = model;
		this.dp = dp;
	}

	@Override
	public Boolean call()  {
		if (dp.params.framework.contains("tensorflow") && !(new File(dp.getPath() + File.separator + "variables").exists())) {
			if (rp != null) {
				rp.setUnzipping(true);
				rp.setVisible(this.show);
			}
			String fileName = dp.getPath() + File.separator + dp.tfName;
			boolean unzipped = true;
			try {
				unzipped = FileTools.unzipFolder(new File(fileName), dp.getPath());
				// If the file was not unzipped correctly, stop and warn the user
				if (!unzipped) {
					IJ.error("Error unzipping the model\n"
							+ "It seems that the zipped file is corrupted");
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
				IJ.error("Error unzipping: " + fileName);
				return false;
			} catch (InterruptedException e) {
		        // Interrupted execution
		        return false;
		    }
		}
		// Set tag to write the correct message in the progress screen
		if (rp != null) {
			rp.setUnzipping(false);
			if (!rp.isVisible())
				rp.setVisible(this.show);
		}
		
		// Parameter to know if we are using GPU or not 
		ArrayList<String> initialSmi = null;
		if (gpu)
			initialSmi = SystemUsage.runNvidiaSmi();
		// The thread cannot be stopped while loading a model, thus block the button
		// while executing the task
		if (rp != null)
			rp.allowStopping(false);
		dp.setModel(model);
		boolean ret = dp.loadModel();
		if (ret == false && dp.params.framework.equals("tensorflow")) {
			IJ.error("Error loading " + dp.getName() + 
					"\nTry using another Tensorflow version.");
			return false;
		} else if (ret == false && dp.params.framework.equals("pytorch")) {
			IJ.error("Error loading Pytorch model: " + dp.getName() + 
					"\nCheck that the Pytorch version corresponds to the pytorch-native-auto jar executable."
					+ "\nIf the problem persits, please check the DeepImageJ Wiki.");
			return false;
		}
		if (rp != null) {
			rp.allowStopping(true);
			// Check if the user has tried to stop the execution while loading the model
			// If they have return false and stop
			if(rp.isStopped())
				return false;
		}
		
		
		if (rp != null && gpu && dp.params.framework.equals("tensorflow")) {
			ArrayList<String> finalSmi = SystemUsage.runNvidiaSmi();
			String GPUInfo = SystemUsage.isUsingGPU(initialSmi, finalSmi);
			if (GPUInfo.equals("noImageJProcess") && cuda) {
				rp.setGPU("???");
			} else if (GPUInfo.equals("noImageJProcess")) {
				rp.setGPU("CPU");
			} else if(GPUInfo.equals("¡RepeatedImageJGPU!")) {
				int nImageJInstances = SystemUsage.numberOfImageJInstances();
				// Get number of IJ instances using GPU
				int nGPUIJInstances = GPUInfo.split("¡RepeatedImageJGPU!").length;
				if (nImageJInstances > nGPUIJInstances) {
					rp.setGPU("???");
				} else if (nImageJInstances <= nGPUIJInstances) {
					rp.setGPU("gpu");
				}
			} else {
				rp.setGPU("gpu");
			}
		}
		return true;
	}

}

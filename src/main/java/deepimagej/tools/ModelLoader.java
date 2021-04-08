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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import ai.djl.pytorch.jni.LibUtils;
import deepimagej.DeepImageJ;
import deepimagej.RunnerProgress;
import ij.IJ;

public class ModelLoader implements Callable<Boolean>{
	private DeepImageJ dp;
	private RunnerProgress rp;
	private boolean gpu;
	private boolean cuda;
	private boolean show;
	private boolean isFiji;
	
	public ModelLoader(DeepImageJ dp, RunnerProgress rp, boolean gpu, boolean cuda, boolean show, boolean isFiji) {
		this.dp = dp;
		this.rp = rp;
		this.gpu = gpu;
		this.cuda = cuda;
		this.show = show;
		this.isFiji = isFiji;
	}

	@Override
	public Boolean call()  {
		if (dp.params.framework.contains("tensorflow") && !(new File(dp.getPath() + File.separator + "variables").exists())) {
			rp.setUnzipping(true);
			rp.setVisible(this.show);
			String fileName = dp.getPath() + File.separator + "tensorflow_saved_model_bundle.zip";
			try {
				FileTools.unzipFolder(new File(fileName), dp.getPath());
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
		rp.setUnzipping(false);
		if (!rp.isVisible())
			rp.setVisible(this.show);
		
		// Parameter to know if we are using GPU or not 
		ArrayList<String> initialSmi = null;
		if (gpu)
			initialSmi = SystemUsage.runNvidiaSmi();
		// The thread cannot be stopped while loading a model, thus block the button
		// while executing the task
		rp.allowStopping(false);
		boolean ret = false;
		if (dp.params.framework.equals("tensorflow")) {
			ret = dp.loadTfModel(true);
		} else if (dp.params.framework.equals("pytorch")) {
			String ptWeightsPath = dp.getPath() + File.separatorChar + "pytorch_script.pt" ;
			ret = dp.loadPtModel(ptWeightsPath, isFiji);
		}
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
		rp.allowStopping(true);
		// Check if the user has tried to stop the execution while loading the model
		// If they have return false and stop
		if(rp.isStopped())
			return false;
		
		
		if (gpu && dp.params.framework.equals("tensorflow")) {
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
		
		if (dp.params.framework.toLowerCase().equals("pytorch")) {
			String lib = new File(LibUtils.getLibName()).getName();
			if (!lib.toLowerCase().contains("cpu")) {
				rp.setGPU("cpu");
			} else {
				rp.setGPU("gpu");
			}
		}
		
		return true;
	}

}

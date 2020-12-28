package deepimagej.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import deepimagej.DeepImageJ;
import deepimagej.RunnerProgress;
import ij.IJ;

public class ModelLoader implements Callable<Boolean>{
	private DeepImageJ dp;
	private RunnerProgress rp;
	private boolean gpu;
	private boolean cuda;
	
	public ModelLoader(DeepImageJ dp, RunnerProgress rp, boolean gpu, boolean cuda) {
		this.dp = dp;
		this.rp = rp;
		this.gpu = gpu;
		this.cuda = cuda;
	}

	@Override
	public Boolean call()  {
		if (dp.params.framework.contains("Tensorflow") && !(new File(dp.getPath() + File.separator + "variables").exists())) {
			rp.setUnzipping(true);
			rp.setVisible(true);
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
			rp.setVisible(true);
		
		// Parameter to know if we are using GPU or not 
		ArrayList<String> initialSmi = null;
		if (gpu)
			initialSmi = SystemUsage.runNvidiaSmi();
		// The thread cannot be stopped while loading a model, thus block the button
		// while executing the task
		rp.allowStopping(false);
		boolean ret = false;
		if (dp.params.framework.equals("Tensorflow")) {
			ret = dp.loadTfModel(true);
		} else if (dp.params.framework.equals("Pytorch")) {
			String ptWeightsPath = dp.getPath() + File.separatorChar + "pytorch_script.pt" ;
			ret = dp.loadPtModel(ptWeightsPath);
		}
		if (ret == false && dp.params.framework.equals("Tensorflow")) {
			IJ.error("Error loading " + dp.getName() + 
					"\nTry using another Tensorflow version.");
			return false;
		} else if (ret == false && dp.params.framework.equals("Pytorch")) {
			IJ.error("Error loading " + dp.getName() + 
					"\nDeepImageJ loads models until Pytorch 1.6.");
			return false;
		}
		rp.allowStopping(true);
		// Check if the user has tried to stop the execution while loading the model
		// If they have return false and stop
		if(rp.isStopped())
			return false;
		
		
		if (gpu) {
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
					rp.setGPU("GPU");
				}
			} else {
				rp.setGPU("GPU");
			}
		}
		
		return true;
	}

}

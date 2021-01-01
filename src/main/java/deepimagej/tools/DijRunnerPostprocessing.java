package deepimagej.tools;

import java.io.File;
import java.io.IOException;
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
public class DijRunnerPostprocessing implements Callable<HashMap<String, Object>>{
	private DeepImageJ dp;
	private RunnerProgress rp;
	private HashMap<String, Object> output;
	
	public DijRunnerPostprocessing(DeepImageJ dp, RunnerProgress rp, HashMap<String, Object> output) {
		this.dp = dp;
		this.rp = rp;
		this.output = output;
	}

	@Override
	public HashMap<String, Object> call() throws MacrosError, JavaProcessingError  {

		// Set tag of rp to 'preprocessing' so it shows the correct information
		rp.setInfoTag("postprocessing");
		
		// The thread cannot be stopped while loading a model, thus block the button
		// while executing the task
		rp.allowStopping(false);
		output = ProcessingBridge.runPostprocessing(dp.params, output);

		rp.allowStopping(true);
		// Check if the user has tried to stop the execution while loading the model
		// If they have return false and stop
		if(rp.isStopped())
			return output;
		
		return output;
	}

}

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
public class DijRunnerPreprocessing implements Callable<HashMap<String, Object>>{
	private DeepImageJ dp;
	private RunnerProgress rp;
	private ImagePlus inp;
	private boolean batch;
	
	public DijRunnerPreprocessing(DeepImageJ dp, RunnerProgress rp, ImagePlus inp, boolean batch) {
		this.dp = dp;
		this.rp = rp;
		this.inp = inp;
		this.batch = batch;
	}

	@Override
	public HashMap<String, Object> call() throws MacrosError, JavaProcessingError, Exception  {
		
		// Set tag of rp to 'preprocessing' so it shows the correct information
		rp.setInfoTag("preprocessing");
		
		// Convert RGB image into RGB stack 
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
		

		ImagePlus im = inp.duplicate();
		String correctTitle = inp.getTitle();
		im.setTitle("tmp_" + correctTitle);
		if (batch == false) {
			ImageWindow windToClose = inp.getWindow();
			windToClose.dispose();
		}
		
		WindowManager.setTempCurrentImage(inp);
		HashMap<String, Object> inputsMap;
		
		if (rp.isStopped()) {
			return null;
		}
		
		rp.allowStopping(false);
		inputsMap = ProcessingBridge.runPreprocessing(inp, dp.params);
		im.setTitle(correctTitle);
		if (batch == false)
			im.show();
		WindowManager.setTempCurrentImage(null);
		

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

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

package deepimagej.processing;

import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import deepimagej.Parameters;
import deepimagej.exceptions.JavaProcessingError;
import deepimagej.exceptions.MacrosError;
import deepimagej.tools.DijTensor;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

public class ProcessingBridge {
	
	// TODO decide whether to allow or not more than 1 image input to the model
	public static HashMap<String, Object> runPreprocessing(ImagePlus im, Parameters params) throws MacrosError, JavaProcessingError {
		HashMap<String, Object> map = new HashMap<String, Object>();
		params.javaPreprocessingClass = new ArrayList<String>();
		// Assume that the image selected will result in the input image to the model
		// Assumes 'im' will be the input to the model
		map.put(params.inputList.get(0).name, im);
		if (params.firstPreprocessing != null && (params.firstPreprocessing.contains(".txt") || params.firstPreprocessing.contains(".ijm"))) {
			im = runProcessingMacro(im, params.firstPreprocessing, params.developer);
			map = manageInputs(map, false, params, im);
		} else if (params.firstPreprocessing != null && (params.firstPreprocessing.contains(".jar") || params.firstPreprocessing.contains(".class") || new File(params.firstPreprocessing).isDirectory())) {
			map = runPreprocessingJava(map, params.firstPreprocessing, params);
		}
		

		if (params.secondPreprocessing != null && (params.secondPreprocessing.contains(".txt") || params.secondPreprocessing.contains(".ijm"))) {
			im = runProcessingMacro(im, params.secondPreprocessing, params.developer);
			map = manageInputs(map, true,  params, im);
		} else if (params.secondPreprocessing != null && (params.secondPreprocessing.contains(".jar") || params.secondPreprocessing.contains(".class") || new File(params.secondPreprocessing).isDirectory())) {
			map = runPreprocessingJava(map, params.secondPreprocessing, params);
		} else if (params.secondPreprocessing == null && (params.firstPreprocessing == null || params.firstPreprocessing.contains(".txt") || params.firstPreprocessing.contains(".ijm"))) {
			map = manageInputs(map, true, params);
		} else if (params.secondPreprocessing == null && (params.firstPreprocessing.contains(".jar") || params.secondPreprocessing.contains(".class") || new File(params.firstPreprocessing).isDirectory())) {
			//TODO check if an input is missing. If it is missing try to recover it from the workspace.
		}
		return map;
	}
	
	private static HashMap<String, Object> manageInputs(HashMap<String, Object> map, boolean lastStep, Parameters params){
		 map = manageInputs(map, lastStep, params, null);
		 return map;
	}
	
	/*
	 * Updates the map containing the inputs. In principle, this is used only if there has not
	 * beeen Java processing before (Java processing should already output a map). 
	 * This method assumes that each model input has an ImageJ object associated. Except for
	 * the main image, where if it is not named correctly, assumes it is the originally referenced
	 * image (line 62).
	 */
	private static HashMap<String, Object> manageInputs(HashMap<String, Object> map, boolean lastStep, Parameters params, ImagePlus im) {
		for (DijTensor tensor : params.inputList) {
			if (tensor.tensorType == "image") {
				ImagePlus inputImage = WindowManager.getImage(tensor.name);
				if (inputImage != null) {
					map.put(tensor.name, inputImage);
		        } else if (im != null) {
					map.put(tensor.name, im);
		        }
			} else if (tensor.tensorType == "parameter") {
				Frame f = WindowManager.getFrame(tensor.name);
		        if (f!=null && (f instanceof TextWindow)) {
		        	 ResultsTable inputTable = ((TextWindow)f).getResultsTable();
					map.put(tensor.name, inputTable);
		        } else if (lastStep){
		        	IJ.error("There is no ResultsTable named: " + tensor.name + ".\n" +
		        			"There should be as it is one of the inputs required\n"
		        			+ "by the model.");
		        	return null;
		        }
			}
		}
		return map;
	}

	private static HashMap<String, Object> runPreprocessingJava(HashMap<String, Object> map, String processingPath, Parameters params) throws JavaProcessingError {
		boolean preprocessing = true;
		ExternalClassManager processingRunner = new ExternalClassManager (processingPath, preprocessing, params);
		map = processingRunner.javaPreprocess(map);
		return map;
	}

	private static ImagePlus runProcessingMacro(ImagePlus img, String macroPath, boolean developer) throws MacrosError {
		WindowManager.setTempCurrentImage(img);
		String aborted = "";
		try {
			aborted = IJ.runMacroFile(macroPath);
		} catch (Exception ex) {
			aborted = "[aborted]";
		}
		
		if (aborted == "[aborted]") {
			throw new MacrosError();
		}
		
		ImagePlus result = WindowManager.getCurrentImage();
		// If the macro opens the image, close it
		if (result.isVisible() && !developer)
			result.getWindow().dispose();
		return result;
	}
	
	/******************************************************************************************************************
	 * Method to run the wanted post-processing wanted on the images or tables 
	 * produced by the deep learning model
	 * @param params: parameters of the moel. It contains the path to the post-processing files
	 * @param map: hashmap containing all the outputs given by the model. The keys are the names 
	 * 	given by the model to each of the outputs. And the values are either ImagePlus or ResultsTable.
	 * @return map: map containing all the paths to the processing files
	 * @throws MacrosError is thrown if the Macro file does not work
	 * @throws JavaProcessingError 
	 */
	public static HashMap<String, Object> runPostprocessing(Parameters params, HashMap<String, Object> map) throws MacrosError, JavaProcessingError {

		params.javaPostprocessingClass = new ArrayList<String>();
		
		if (params.firstPostprocessing != null && (params.firstPostprocessing.contains(".txt") || params.firstPostprocessing.contains(".ijm"))) {
			runPostprocessingMacro(params.firstPostprocessing);
			map = manageOutputs();
		} else if (params.firstPostprocessing != null && (params.firstPostprocessing.contains(".jar") || params.firstPostprocessing.contains(".class") || new File(params.firstPostprocessing).isDirectory())) {
			map = runPostprocessingJava(map, params.firstPostprocessing, params);
		}
		

		if (params.secondPostprocessing != null && (params.secondPostprocessing.contains(".txt") || params.secondPostprocessing.contains(".ijm"))) {
			runPostprocessingMacro(params.secondPostprocessing);
		} else if (params.secondPostprocessing != null && (params.secondPostprocessing.contains(".jar") || params.secondPostprocessing.contains(".class") || new File(params.secondPostprocessing).isDirectory())) {
			map = runPostprocessingJava(map, params.secondPostprocessing, params);
		}
		return map;
	}
	
	/****************************************+
	 * Method to run a post-processing routine in Java on the images and table open in
	 * ImageJ
	 * @param map: hashmap containing all the images and tables opened in ImageJ with the keys
	 * being the title of the window
	 * @param processingPath: path to the java file that specifies the processing
	 * @return map: hashmap containing the results of the processing routine
	 * @throws JavaProcessingError 
	 */
	private static HashMap<String, Object> runPostprocessingJava(HashMap<String, Object> map, String processingPath, Parameters params) throws JavaProcessingError {
		boolean preprocessing = false;
		ExternalClassManager processingRunner = new ExternalClassManager (processingPath, preprocessing, params);
		map = processingRunner.javaPostprocess(map);
		return map;
	}

	/***************************
	 * Method to run a macro processing routine over the outputs of the model. 
	 * @param macroPath: path to the macro file
	 * @return: last image processed by the file
	 * @throws MacrosError: thrown if the macro contains errors
	 */
	private static void runPostprocessingMacro(String macroPath) throws MacrosError {

		String aborted = IJ.runMacroFile(macroPath);
		if (aborted == "[aborted]") {
			throw new MacrosError();
		}
	}
	
	/**************************
	 * Method that puts all the images and results tables with their names
	 * in a hashmap.
	 * @return map: hashmap containing all the images and results tables.
	 */
	private static HashMap<String, Object> manageOutputs() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		Frame[] nonImageWindows = WindowManager.getNonImageWindows();
		String[] imageTitles = WindowManager.getImageTitles();
		for (String title : imageTitles) {
			map.put(title, WindowManager.getImage(title));
		}
		for (Frame f : nonImageWindows) {
	        if (f!=null && (f instanceof TextWindow)) {
	        	String tableTitle = f.getTitle();
	        	ResultsTable table = ((TextWindow)f).getResultsTable();
				map.put(tableTitle, table);
	        }
		}
		return map;
	}

}

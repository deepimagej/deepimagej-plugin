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

package deepimagej;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import deepimagej.tools.DijTensor;
import deepimagej.tools.YAMLUtils;
import ij.ImagePlus;

public class Parameters {

	// Directory where the model is actually located
	public String path2Model;
	// Parameter that checks if the plugin is for developers or users
	public boolean developer;
	/*
	 * Path to the first pre-processing applied to the image, if there is any.
	 * It can be either a macro file ('.ijm' or '.txt'), java code (a '.class' file,
	 * a '.jar' file or a folder containing classes) or null if there is no processing.
	 */
	public String		firstPreprocessing = null;
	/*
	 * Class and method called to run preprocessing
	 */
	public String javaPreprocessingClass = null;
	/*
	 * Class and method called to run preprocessing, only exists if there are
	 * two java preprocessings
	 */
	public String javaAuxPreprocessingClass = null;
	/*
	 * Path to the second pre-processing applied to the image, if there is any.
	 * It can be either a macro file ('.ijm' or '.txt'), java code (a '.class' file,
	 * a '.jar' file or a folder containing classes) or null if there is no processing.
	 */
	public String		secondPreprocessing = null;
	/*
	 * Path to the first first post-processing applied to the image, if there is any.
	 * It can be either a macro file ('.ijm' or '.txt'), java code (a '.class' file,
	 * a '.jar' file or a folder containing classes) or null if there is no processing.
	 */
	public String		firstPostprocessing = null;
	/*
	 *Path to the second post-processing applied to the image, if there is any.
	 * It can be either a macro file ('.ijm' or '.txt'), java code (a '.class' file,
	 * a '.jar' file or a folder containing classes) or null if there is no processing.
	 */
	public String		secondPostprocessing = null;
	/*
	 * Class and method called to run postprocessing
	 */
	public String javaPostprocessingClass = null;
	/*
	 * Class and method called to run postprocessing, only exists if there are
	 * two java postprocessings
	 */
	public String javaAuxPostprocessingClass = null;
	/*
	 * Whether the network has a pyramidal pooling structure or not.
	 * If it has it, the way to define the model changes. By default
	 * it is false.
	 */
	public boolean pyramidalNetwork = false;
	/*
	 * Whether the model allows patching or has to use the whole image
	 * always.
	 */
	public boolean allowPatching = true;
	
	/*
	 * Image used to test the model
	 */
	public ImagePlus testImage;
	/*
	 * List of all the images and ResultsTables that
	 * make the final output of the model
	 */
	public List<HashMap<String, String>> savedOutputs = new ArrayList<HashMap<String, String>>();
	
	// Copy created to return the original image in the case
	// that the model fails after applying the macros
	public ImagePlus testImageBackup;
	// ImagePlus produced after testing the model
	public ImagePlus[] testResultImage;
	
	// Directory specified by the user to save the model
	public String saveDir;
	
	// Boolean informing if the config file contains the ModelCharacteristics
	// parameters, needed to load the model
	public boolean completeConfig = true;
		
	// in ModelInformation
	public String		name					= "";
	public List<String>	author					= new ArrayList<String>();
	public String		doi						= "";
	public String		version					= "";
	public String		reference				= "";
	
	public String		documentation			= null;
	private String[] 	deepImageJTag			= {"deepImageJ"};
	public List<String>	infoTags				= Arrays.asList(deepImageJTag);
	public String		license					= null;
	public String		language				= "Java";
	public String		framework				= "Tensorflow";
	public String		source					= null;
	public String		coverImage				= null;
	public String		description				= null;

	// in ModelTest
	public String[]		inputSize;
	public String[]		inputPixelSize;
	public String[]		outputSize;
	public String		memoryPeak				= "";
	public String		runtime					= "";

	public String		tag						= "";
	public String		graph					= "";
	public Set<String> 	graphSet;

	// Parameters for the correct execution of the model on an image.
	// They range from the needed dimensions and dimensions organization
	// to the size of the patches and overlap needed for the network to work
	// properly and not to crash because of different issues such as memory.
	// They also regard the requirements for the input image
	
	/*
	 * List of the selected input tensors to the model
	 */
	public List<DijTensor> inputList 	= new ArrayList<>();
	/*
	 * List of the selected output tensors of the model
	 */
	public List<DijTensor> outputList 	= new ArrayList<>();
	/*
	 * List of all the input tensors to the model
	 */
	public List<DijTensor> totalInputList 	= new ArrayList<>();
	/*
	 * List of all the output tensors of the model
	 */
	public List<DijTensor> totalOutputList 	= new ArrayList<>();
	/*
	 * If the input is fixed, only show the input size in the yaml file
	 */
	public boolean		fixedInput			= false;
	public boolean		fixedPadding			= true;

	// Halo used to allow the user modify the halo at DIJ run
	// TODO establish min halo, take into account scaling
	public int[]		final_halo = null;
	
	
	
	public Parameters(boolean valid, String path, boolean isDeveloper) {
		// If the model is not valid or we are in the developer plugin,
		// we cannot read the parameters from anywhere as there is no
		// config file
		developer = isDeveloper;
		if (!valid || developer)
			return;
		String yamlFile = path + File.separator + "config.yaml";
		Map<String, Object> config =  YAMLUtils.readConfig(yamlFile);

		name = config.get("name") != null ? (String) config.get("name") : "";
		//author = config.get("author") != null ? (String) config.get("author") : "";
		doi = config.get("URL") != null ? (String) config.get("URL") : "";
		version = config.get("version") != null ? (String) config.get("version") : "";
		//date = config.get("Date") != null ? (String) config.get("Date") : "";
		//reference = config.get("Reference") != null ? (String) config.get("Reference") : "";
		
		List b = (List) config.get("inputSize");
		inputSize = castListToStringArray((List) config.get("inputSize")) != null ? castListToStringArray((List) config.get("inputSize")) : new String[] {""};
		outputSize = castListToStringArray((List) config.get("outputSize")) != null ? castListToStringArray((List) config.get("outputSize")) : new String[] {""};
		
		memoryPeak = config.get("MemoryPeak") != null ? (String) config.get("MemoryPeak") : "";
		runtime = config.get("Runtime") != null ? (String) config.get("Runtime") : "";
		inputPixelSize = castListToStringArray((List) config.get("pixelSize")) != null ? castListToStringArray((List) config.get("pixelSize")) : new String[] {"Unknown"};
		tag = config.get("tag") != null ? (String) config.get("tag") : "serve";
		graph = config.get("sigDef") != null ? (String) config.get("sigDef") : "serving_default";

		inputList = (List<DijTensor>) config.get("inputList");
		outputList = (List<DijTensor>) config.get("outputList");
	}
	
	public static String[] castListToStringArray(List list) {
		String[] array = new String[list.size()];
		int c = 0;
		for (Object in : list) {
			array[c ++] = (String) in;
		}
		return array;
	}
}
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.ZooModel;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.YAMLUtils;
import ij.ImagePlus;

public class Parameters {

	/*
	 *  Directory where the model is located
	 */
	public String path2Model;
	/*
	 *  Parameter that checks if the plugin in use is DeepImageJ Run (false)
	 *  or DeepImageJ Build BundledModel (true)
	 */
	public boolean developer;
	/* TODO make the standard
	 * Path to the first pre-processing applied to the image, if there is any.
	 * It can be either a macro file ('.ijm' or '.txt'), java code (a '.class' file,
	 * a '.jar' file or a folder containing classes) or null if there is no processing.
	 */
	public String[]		preprocessing = new String[4];
	/*
	 * Path to the first pre-processing applied to the image, if there is any.
	 * It can be either a macro file ('.ijm' or '.txt'), java code (a '.class' file,
	 * a '.jar' file or a folder containing classes) or null if there is no processing.
	 */
	public String		firstPreprocessing = null;
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
	 * Class and method called to run preprocessing
	 */
	public ArrayList<String> javaPreprocessingClass = new ArrayList<String>();
	/*
	 * Class and method called to run postprocessing
	 */
	public ArrayList<String> javaPostprocessingClass = new ArrayList<String>();
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
	 * Copy created to return the original image in the case
	 * that the model fails after applying the macros
	 */
	public ImagePlus testImageBackup;
	/*
	 * List of all the images and ResultsTables that
	 * make the final output of the model
	 */
	public List<HashMap<String, String>> savedOutputs = new ArrayList<HashMap<String, String>>();
	/*
	 *  Directory specified by the user to save the model
	 */
	public String saveDir;
	
	// Boolean informing if the config file contains the ModelCharacteristics
	// parameters, needed to load the model
	public boolean completeConfig = true;
		
	/*
	 *  Parameters providing ModelInformation
	 */
	public String		name					= "";
	public List<String>	author					= new ArrayList<String>();
	//public String		version					= "";
	public String		format_version					= "";
	/*
	 * Citation: contains the reference articles and the corresponding dois used
	 * to create the model
	 */
	public List<HashMap<String, String>> cite;
	
	public String		documentation			= null;
	private String[] 	deepImageJTag			= {"deepImageJ"};
	public List<String>	infoTags				= Arrays.asList(deepImageJTag);
	public String		license					= null;
	public String		language				= "Java";
	public String		framework				= null;
	public String		source					= null;
	public String		coverImage				= null;
	public String		description				= null;
	public String		git_repo				= null;

	// in ModelTest
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
	/*
	 * List of all the existing versions of the model weights.
	 */
	//public Map<String, Object> previousVersions = new HashMap<String, Object>();
	/*
	 * Checksum of the saved_model.pb file. Only useful if
	 * we use a Bioimage Zoo model.
	 */
	// TODO public String saved_modelSha256;
	/*
	 * Specifies if the folder contains a Bioimage Zoo model
	 */
	public boolean biozoo = false;
	/*
	 * List of all the available preprocessings from
	 * the yaml file
	 */
	public HashMap<String, String[]> pre;
	/*
	 * List of all the available postprocessings from
	 * the yaml file
	 */
	public HashMap<String, String[]> post;
	/*
	 * Path to the model, in the case a Pytorch model is used. The Pytorch model
	 * is always a .pt or .pth file. In the case of a Tensorflow model, path to the 
	 * weights folder
	 */
	public String selectedModelPath;
	
	
	
	public Parameters(boolean valid, String path, boolean isDeveloper) {
		// If the model is not valid or we are in the developer plugin,
		// we cannot read the parameters from anywhere as there is no
		// config file
		String yamlFile = path + File.separator + "model.yaml";
		developer = isDeveloper;
		if (developer || !new File(yamlFile).isFile())
			return;
		Map<String, Object> obj =  YAMLUtils.readConfig(yamlFile);


		format_version = "" + obj.get("format_version");
		name = (String) obj.get("name");
		author = (List<String>) obj.get("authors");
		if (author == null) {
			author = new ArrayList<String>();
			author.add("n/a");
		}

		// Citation
		cite = (List<HashMap<String, String>>) obj.get("cite");
		if (cite == null) {
			cite = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> c = new HashMap<String, String>();
			c.put("text", "");
			c.put("doi", "");
			cite.add(c);
		}
		
		documentation = (String) obj.get("documentation");
		// TODO do we need cover?
		//String cover = (String) obj.get("cover");
		license = (String) obj.get("license");
		framework = (String) obj.get("framework");
		git_repo = (String) obj.get("git_repo");
		
		LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>> weights = (LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>>) obj.get("weights");
		// Look for the valid weights tags
		Set<String> weightFormats = weights.keySet();
		boolean tf = false;
		boolean pt = false;
		for (String format : weightFormats) {
			if (format.equals("tensorflow_saved_model_bundle"))
				tf = true;
			else if (format.equals("pytorch_script"))
				pt = true;
		}
		
		if (tf && pt) {
			framework = "Tensorflow/Pytorch";
		} else if (tf) {
			framework = "Tensorflow";
		} else if (pt) {
			framework = "Pytorch";
		} else if (!tf && !pt) {
			completeConfig = false;
			return;
		}
		
		// Model metadata
		Map<String, Object> config = (Map<String, Object>) obj.get("config");
		Map<String, Object> deepimagej = (Map<String, Object>) config.get("deepimagej");
		pyramidalNetwork = (boolean) deepimagej.get("pyramidal_model");
		allowPatching = (boolean) deepimagej.get("allow_tiling");
		// Model keys
		if (framework.contains("Tensorflow")) {
			Map<String, Object> model_keys = (Map<String, Object>) deepimagej.get("model_keys");
			tag = (String) model_keys.get("tensorflow_model_tag");
			graph = (String) model_keys.get("tensorflow_siganture_def");
		}		
		
		
		List<Map<String, Object>> inputs = (List<Map<String, Object>>) obj.get("inputs");
		// Check that the previous version field is complete
		if (inputs == null) {
			completeConfig = false;
			return;
		}
		inputList = new ArrayList<DijTensor>();
		
		Map<String, Object> test_information = (Map<String, Object>) deepimagej.get("test_information");
		List<LinkedHashMap<String, Object>> input_information = (List<LinkedHashMap<String, Object>>) test_information.get("inputs");
		int tensorCounter = 0;
		
		for (Map<String, Object> inp : inputs) {
			DijTensor inpTensor = new DijTensor((String) inp.get("name"));
			inpTensor.form = ((String) inp.get("axes")).toUpperCase();
			inpTensor.dataType = (String) inp.get("data_type");
			//TODO do we assume inputs in the yaml are always images?
			inpTensor.tensorType = "image";
			List<Object> auxDataRange = (ArrayList<Object>) inp.get("data_range");
			//TODO inpTensor.dataRange = castListToDoubleArray(auxDataRange);
			
			// Find by trial and error if the shape of the input is fixed or not
			try {
				List<Object> shape = (ArrayList<Object>) inp.get("shape");
				inpTensor.recommended_patch = castListToIntArray(shape);
				inpTensor.tensor_shape = inpTensor.recommended_patch;
				inpTensor.minimum_size = castListToIntArray(shape);
				inpTensor.step = new int[shape.size()];
				fixedInput = true;
			} catch (Exception ex) {
				Map<String, Object> shape = (Map<String, Object>) inp.get("shape");
				List auxMinimumSize = (List) shape.get("min");
				inpTensor.minimum_size = castListToIntArray(auxMinimumSize);
				List auxStepSize = (List) shape.get("step");
				inpTensor.step = castListToIntArray(auxStepSize);
				inpTensor.recommended_patch = new int[auxStepSize.size()];
				inpTensor.tensor_shape = new int[auxStepSize.size()];
				// Recreate the tensor shape of the model with the information
				// of the YAML
				for (int i = 0; i < inpTensor.step.length; i ++) {
					if (inpTensor.step[i] == 0) {
						inpTensor.tensor_shape[i] = inpTensor.minimum_size[i];
					} else {
						inpTensor.tensor_shape[i] = -1;
					}
				}
				fixedInput = false;
			}

			// Check that the output definition fields are complete
			if (inpTensor.form == null || inpTensor.dataType == null || inpTensor.minimum_size == null
					|| inpTensor.tensor_shape == null || inpTensor.step == null || inpTensor.recommended_patch == null) {
				completeConfig = false;
				return;
			}
			
			// Now find the test information of this tensor
			LinkedHashMap<String, Object> info = input_information.get(tensorCounter ++);
			inpTensor.exampleInput = (String) info.get("name");
			inpTensor.inputTestSize =  (String) info.get("size");
			Map<String, String>  pixel_size =  (Map<String, String>) info.get("pixel_size");
			inpTensor.inputPixelSizeX = (String) pixel_size.get("x");
			inpTensor.inputPixelSizeY = (String) pixel_size.get("y");
			inpTensor.inputPixelSizeZ = (String) pixel_size.get("z");
			
			inputList.add(inpTensor);
		}

		List<Map<String, Object>> outputs = (List<Map<String, Object>>) obj.get("outputs");
		// Check that the previous version field is complete
		if (outputs == null) {
			completeConfig = false;
			return;
		}
		outputList = new ArrayList<DijTensor>();
		
		for (Map<String, Object> out : outputs) {
			DijTensor outTensor = new DijTensor((String) out.get("name"));
			outTensor.form = (String) out.get("axes");
			outTensor.form = outTensor.form == null ? null : outTensor.form.toUpperCase();
			outTensor.tensorType = outTensor.form == null ? "list" : "image";
			List auxDataRange = (List) out.get("data_range");
			// TODO outTensor.dataRange = castListToDoubleArray(auxDataRange);
			outTensor.dataType = (String) out.get("data_type");
			if (outTensor.tensorType.contains("image") && !pyramidalNetwork) {
				List auxHalo = (List) out.get("halo");
				outTensor.halo = castListToIntArray(auxHalo);
			} else if (outTensor.tensorType.contains("image")) {
				outTensor.halo = new int[outTensor.form.length()];
			}
			

			// Find by trial and error if the shape of the input is fixed or not
			try {
				List<Object> shape = (ArrayList<Object>) out.get("shape");
				outTensor.recommended_patch = castListToIntArray(shape);
				outTensor.scale = new float[shape.size()];
				outTensor.offset = new int[shape.size()];
			} catch (Exception ex) {
				Map<String, Object> shape = (Map<String, Object>) out.get("shape");
				outTensor.referenceImage = (String) shape.get("reference_input");
				List auxScale = (List) shape.get("scale");
				outTensor.scale = castListToFloatArray(auxScale);
				List auxOffset = (List) shape.get("offset");
				outTensor.offset = castListToIntArray(auxOffset);
			}		
			outTensor.form = outTensor.form == null ? Table2Tensor.findTableForm(outTensor.recommended_patch) : outTensor.form;	

			// Check that the output definition fields are complete
			if (outTensor.form == null || outTensor.dataType == null || outTensor.scale == null
					|| outTensor.offset == null) {
				completeConfig = false;
				return;
			}
			
			outputList.add(outTensor);
		}
		// Output test information
		List<LinkedHashMap<String, Object>> output_information = (List<LinkedHashMap<String, Object>>) test_information.get("outputs");
		savedOutputs = new ArrayList<HashMap<String, String>>();
		for (LinkedHashMap<String, Object> out : output_information) {
			HashMap<String, String> info = new LinkedHashMap<String, String>();
			String outName =  (String) out.get("name");
			info.put("name", outName);
			String size =  (String) out.get("size");
			info.put("size", size);
			String type = (String) out.get("type");
			info.put("type", type);

			savedOutputs.add(info);
		}
		
		// Info about runtime and memory
		memoryPeak = (String) test_information.get("memory_peak");
		runtime = (String) test_information.get("runtime");

		
		// Get all the preprocessings available in the Yaml
		Map<String, Object> prediction = (Map<String, Object>) deepimagej.get("prediction");
		pre = new HashMap<String, String[]>();
		post = new HashMap<String, String[]>();
		Set<String> keys = prediction.keySet();
		for (String key : keys) {
			if (key.contains("preprocess")) {
				List<Map<String, String>> preprocess = (List<Map<String, String>>) prediction.get(key);
				// TODO convert into a list of processings
				String[] commands = new String[preprocess.size()];
				int processingCount = 0;
				for (Map<String, String> processing : preprocess) {
					String spec = processing.get("spec");
					if (spec != null && processing.containsKey("kwargs")) {
						commands[processingCount] = processing.get("kwargs");
					} else if (spec != null && !processing.containsKey("kwargs") && spec.contains(".jar")) {
						int extensionPosition = spec.indexOf(".jar");
						commands[processingCount] = spec.substring(0, extensionPosition + 4);
					} else if (spec != null && !processing.containsKey("kwargs") && spec.contains(".class")) {
						int extensionPosition = spec.indexOf(".class");
						commands[processingCount] = spec.substring(0, extensionPosition + 6);
					} else if (spec == null) {
						commands = null;
					}
					
					processingCount ++;
				}
				pre.put(key, commands);
			}
			if (key.contains("postprocess")) {
				List<Map<String, String>> postprocess = (List<Map<String, String>>) prediction.get(key);
				// TODO convert into a list of processings
				String[] commands = new String[postprocess.size()];
				int processingCount = 0;
				for (Map<String, String> processing : postprocess) {
					String spec = processing.get("spec");
					if (spec != null && processing.containsKey("kwargs")) {
						commands[processingCount] = processing.get("kwargs");
					} else if (spec != null && !processing.containsKey("kwargs") && spec.contains(".jar")) {
						int extensionPosition = spec.indexOf(".jar");
						if (extensionPosition == -1)
						commands[processingCount] = spec.substring(0, extensionPosition + 4);
					} else if (spec != null && !processing.containsKey("kwargs") && spec.contains(".class")) {
						int extensionPosition = spec.indexOf(".class");
						commands[processingCount] = spec.substring(0, extensionPosition + 6);
					} else if (spec == null) {
						commands = null;
					}
					
					processingCount ++;
				}
				post.put(key, commands);
			}
		}
		
		
		name = name != null ? (String) name : "n/a";
		documentation = documentation != null ? documentation : "n/a";
		format_version = format_version != null ? format_version : "n/a";
		license = license != null ? license : "n/a";
		memoryPeak = memoryPeak != null ? memoryPeak : "n/a";
		runtime = runtime != null ?  runtime : "n/a";
		tag = tag != null ? tag : "serve";
		graph = graph != null ? graph : "serving_default";
		completeConfig = true;
		
		
	}
	
	public static String[] castListToStringArray(List list) {
		String[] array = new String[list.size()];
		int c = 0;
		for (Object in : list) {
			array[c ++] = (String) in;
		}
		return array;
	}
	
	public static int[] castListToIntArray(List list) {
		int[] array = new int[list.size()];
		int c = 0;
		for (Object in : list) {
			array[c ++] = (int) in;
		}
		return array;
	}
	
	public static double[] castListToDoubleArray(List list) {
		try {
			double[] array = new double[list.size()];
			int c = 0;
			for (Object in : list) {
				array[c ++] = (double) in;
			}
			return array;
		} catch (Exception ex) {
			return null;
		}
	}
	
	public static float[] castListToFloatArray(List list) {
		try {
			float[] array = new float[list.size()];
			int c = 0;
			for (Object in : list) {
				array[c ++] = ((Double) in).floatValue();
			}
			return array;
		} catch (ClassCastException ex) {
			return null;			
		}
	}
}
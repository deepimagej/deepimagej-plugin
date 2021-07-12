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

package deepimagej;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import deepimagej.tools.DijTensor;
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
	 * List of dependencies needed for the Java pre-processing
	 */
	public ArrayList<String> preAttachments = new ArrayList<String>();
	/*
	 * List of dependencies needed for the Java post-processing
	 */
	public ArrayList<String> postAttachments = new ArrayList<String>();
	/*
	 * List of dependencies needed for the Java pre- and post-processing.
	 * This variable is the union of preAttachments and postAttachments
	 */
	public ArrayList<String> attachments = new ArrayList<String>();
	/*
	 * List of dependencies needed for the Java pre- and post-processing.
	 * This variable is the union of preAttachments and postAttachments
	 * For a Tensorflow model
	 */
	public ArrayList<String> tfAttachments = new ArrayList<String>();
	/*
	 * List of dependencies needed for the Java pre- and post-processing.
	 * This variable is the union of preAttachments and postAttachments.
	 * For a Pytorch model
	 */
	public ArrayList<String> ptAttachments = new ArrayList<String>();
	/*
	 * List of dependencies needed for the Java pre- and post-processing.
	 * This variable is the union of preAttachments and postAttachments
	 * that are not included in the model folder. This variable will only be
	 * used by the DIJ Run
	 * For Tensorflow
	 */
	public ArrayList<String> tfAttachmentsNotIncluded = new ArrayList<String>();
	/*
	 * List of dependencies needed for the Java pre- and post-processing.
	 * This variable is the union of preAttachments and postAttachments
	 * that are not included in the model folder. This variable will only be
	 * used by the DIJ Run
	 * For Pytorch
	 */
	public ArrayList<String> ptAttachmentsNotIncluded = new ArrayList<String>();
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
	
	/*
	 *  Boolean informing if the model file sh256 corresponds to the
	 *  one saved specified in the model.yaml
	 */
	public boolean incorrectSha256 = false;
	
	/*
	 *  Boolean informing if the config file contains the ModelCharacteristics
	 *  parameters, needed to load the model
	 */
	public boolean completeConfig = true;
	
	/*
	 * Missing fields in the yaml file
	 */
	public ArrayList<String> fieldsMissing = null;
	
	/*
	 * Version of the DJL Pytorch being used to run Pytorch
	 */
	public String pytorchVersion = "";
		
	/*
	 *  Parameters providing ModelInformation
	 */
	public String		name					= "n.a.";
	public List<String>	author					= new ArrayList<String>();
	public String		timestamp				= "";
	public String		format_version			= "0.3.1";
	/*
	 * Citation: contains the reference articles and the corresponding dois used
	 * to create the model
	 */
	public List<HashMap<String, String>> cite;
	
	public String		documentation			= null;
	private String[] 	deepImageJTag			= {"deepImageJ"};
	public List<String>	infoTags				= Arrays.asList(deepImageJTag);
	public String		license					= null;
	public String		language				= "java";
	public String		framework				= "";
	public String		tfSource				= null;
	public String		ptSource				= null;
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
	 * Checksum of the tf Bioimage model zoo file. Only useful if
	 * we use a Bioimage Zoo model that comes with a zipped model.
	 */
	public String tfSha256 = "";
	/*
	 * Checksum of the Pytorch scripts file. Only useful if
	 * there is a Pytorch model
	 */
	public String ptSha256 = "";
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
	public String selectedModelPath = "";
	
	public Parameters(boolean valid, String path, boolean isDeveloper) {
		// If the model is not valid or we are in the developer plugin,
		// we cannot read the parameters from anywhere as there is no
		// config file
		path2Model = path;
		File yamlFile = new File(path + File.separator + "model.yaml");
		developer = isDeveloper;
		if (developer || !yamlFile.isFile())
			return;
		Map<String, Object> obj =  YAMLUtils.readConfig(yamlFile.getAbsolutePath());
		// Find out if there is any missing field in the yaml
		fieldsMissing = checkYaml(obj);
		// Until every parameter is checkef complete config is false
		completeConfig = false;

		Set<String> kk = obj.keySet();

		format_version = "" + obj.get("format_version");
		if (kk.contains("name")) {
			name = (String) obj.get("name");
		} else {
			completeConfig = false;
			return;
		}
		if (obj.get("authors") instanceof List) {
			author = (List<String>) obj.get("authors");
		} else if (obj.get("authors") instanceof String) {
			String aux = "" + obj.get("authors");
			author = new ArrayList<String>();
			author.add(aux);
		} else {
			
		}
		timestamp = "" +  obj.get("timestamp");
		if (author == null) {
			author = new ArrayList<String>();
			author.add("n/a");
		}

		// Citation
		Object citation = obj.get("cite");
		if (citation instanceof List) {
			cite = (List<HashMap<String, String>>) citation;
		} else if (citation instanceof HashMap<?, ?>) {
			cite = new ArrayList<HashMap<String, String>>();
			cite.add((HashMap<String, String>) citation);
		} else {
			cite = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> c = new HashMap<String, String>();
			c.put("text", "");
			c.put("doi", "");
			cite.add(c);
		}
		
		documentation = (String) "" + obj.get("documentation");
		license = (String) "" + obj.get("license");
		framework = (String) "" + obj.get("framework");
		git_repo = (String) "" + obj.get("git_repo");
		
		LinkedHashMap<String, Object> weights = (LinkedHashMap<String, Object>) obj.get("weights");
		// Look for the valid weights tags
		Set<String> weightFormats = weights.keySet();
		boolean tf = false;
		boolean pt = false;
		for (String format : weightFormats) {
			if (format.equals("tensorflow_saved_model_bundle")) {
				tf = true;
				HashMap<String, Object> tfMap = ((HashMap<String, Object>) weights.get("tensorflow_saved_model_bundle"));
				// Look for the name of the model. The model can be called differently sometimes
				tfSource = (String) tfMap.get("source");
				
				// Retrieve the Tensorflow attachments
				ArrayList<String> attachmentsAux = null;
				if (tfMap.get("attachments") instanceof HashMap<?, ?>) {
					HashMap<String, Object> attachmentsMap = (HashMap<String, Object>) tfMap.get("attachments");
					if (attachmentsMap.get("files") instanceof ArrayList)
						attachmentsAux = (ArrayList<String>) attachmentsMap.get("files");
				}
				
				tfAttachments = new ArrayList<String>();
				tfAttachmentsNotIncluded = new ArrayList<String>();
				String defaultFlag = "Include here any plugin that might be required for pre- or post-processing";
				if (attachmentsAux != null) {
					for (String str : attachmentsAux) {
						if (new File(path2Model, str).isFile() && !str.contentEquals(""))
							tfAttachments.add(new File(path2Model, str).getAbsolutePath());
						else if (!str.contentEquals(defaultFlag))
							tfAttachmentsNotIncluded.add(str);
					}
				}
			} else if (format.equals("pytorch_script")) {
				HashMap<String, Object> ptMap = ((HashMap<String, Object>) weights.get("pytorch_script"));
				// Look for the name of the model. The model can be called differently sometimes
				ptSource = (String) ptMap.get("source");
				pt = true;
				
				// Retrieve the Pytorch attachments
				ArrayList<String> attachmentsAux = null;
				if (ptMap.get("attachments") instanceof HashMap<?, ?>) {
					HashMap<String, Object> attachmentsMap = (HashMap<String, Object>) ptMap.get("attachments");
					if (attachmentsMap.get("files") instanceof ArrayList)
						attachmentsAux = (ArrayList<String>) attachmentsMap.get("files");
				}
				
				ptAttachments = new ArrayList<String>();
				ptAttachmentsNotIncluded = new ArrayList<String>();
				String defaultFlag = "Include here any plugin that might be required for pre- or post-processing";
				if (attachmentsAux != null) {
					for (String str : attachmentsAux) {
						if (new File(path2Model, str).isFile() && !str.contentEquals(""))
							ptAttachments.add(new File(path2Model, str).getAbsolutePath());
						else if (!str.contentEquals(defaultFlag))
							ptAttachmentsNotIncluded.add(str);
					}
				}
			}
		}
		
		if (tf && pt) {
			framework = "tensorflow/pytorch";
			ptSha256 = (String) "" + ((LinkedHashMap<String, Object>) weights.get("pytorch_script")).get("sha256");
			tfSha256 = (String) "" + ((LinkedHashMap<String, Object>) weights.get("tensorflow_saved_model_bundle")).get("sha256");
		} else if (tf) {
			framework = "tensorflow";
			tfSha256 = (String) "" + ((LinkedHashMap<String, Object>) weights.get("tensorflow_saved_model_bundle")).get("sha256");
		} else if (pt) {
			framework = "pytorch";
			ptSha256 = (String) "" + ((LinkedHashMap<String, Object>) weights.get("pytorch_script")).get("sha256");
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
		if (deepimagej.keySet().contains("model_keys") && deepimagej.get("model_keys") != null) {
			Map<String, Object> model_keys = (Map<String, Object>) deepimagej.get("model_keys");
			tag = (String) "" + model_keys.get("tensorflow_model_tag");
			graph = (String) "" + model_keys.get("tensorflow_siganture_def");
		}		
		
		
		List<Map<String, Object>> inputs = (List<Map<String, Object>>) obj.get("inputs");
		// Check that the previous version field is complete
		if (inputs == null || inputs.size() == 0) {
			fieldsMissing = new ArrayList<String>();
			fieldsMissing.add("Inputs are not defined correctly");
			completeConfig = false;
			return;
		}
		inputList = new ArrayList<DijTensor>();
		
		Map<String, Object> test_information = (Map<String, Object>) deepimagej.get("test_information");

		List<LinkedHashMap<String, Object>> input_information = new ArrayList <LinkedHashMap<String, Object>>();
		if (test_information.get("inputs") instanceof LinkedHashMap) {
			LinkedHashMap<String, Object> aux = (LinkedHashMap<String, Object>) test_information.get("inputs");
			input_information.add(aux);
		} else if (test_information.get("inputs") instanceof List){
			input_information = (List<LinkedHashMap<String, Object>>) test_information.get("inputs");
		}
		
		int tensorCounter = 0;
		try {
			for (Map<String, Object> inp : inputs) {
				DijTensor inpTensor = new DijTensor((String) "" + inp.get("name"));
				inpTensor.form = ((String) "" + inp.get("axes")).toUpperCase();
				inpTensor.dataType = (String) "" + inp.get("data_type");
				//TODO do we assume inputs in the yaml are always images?
				inpTensor.tensorType = "image";
				//TODO List<Object> auxDataRange = (ArrayList<Object>) inp.get("data_range");
				//TODO inpTensor.dataRange = castListToDoubleArray(auxDataRange);
				
				// Find by trial and error if the shape of the input is fixed or not
				Object objectShape = inp.get("shape");
				if (objectShape instanceof List<?>) {
					List<Object> shape = (List<Object>) objectShape;
					inpTensor.recommended_patch = castListToIntArray(shape);
					inpTensor.tensor_shape = inpTensor.recommended_patch;
					inpTensor.minimum_size = castListToIntArray(shape);
					inpTensor.step = new int[shape.size()];
					fixedInput = true;
				} else if (objectShape instanceof Map<?, ?>) {
					Map<String, Object> shape = (Map<String, Object>) objectShape;
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
				try {
					inpTensor.exampleInput = (String) "" + info.get("name");
					inpTensor.inputTestSize =  (String) "" + info.get("size");
					Map<String, Object>  pixel_size =  (Map<String, Object>) info.get("pixel_size");
					inpTensor.inputPixelSizeX = (String) "" + pixel_size.get("x");
					inpTensor.inputPixelSizeY = (String) "" + pixel_size.get("y");
					inpTensor.inputPixelSizeZ = (String) "" + pixel_size.get("z");
				} catch (Exception ex) {
					inpTensor.exampleInput = (String) "";
					inpTensor.inputTestSize =  (String) "";
					Map<String, Object>  pixel_size =  (Map<String, Object>) info.get("pixel_size");
					inpTensor.inputPixelSizeX = (String) "";
					inpTensor.inputPixelSizeY = (String) "";
					inpTensor.inputPixelSizeZ = (String) "";
				}
				
				inputList.add(inpTensor);
			}
		} catch (Exception ex) {
			fieldsMissing = new ArrayList<String>();
			fieldsMissing.add("Inputs are not defined correctly");
			completeConfig = false;
			return;
		}

		List<Map<String, Object>> outputs = (List<Map<String, Object>>) obj.get("outputs");
		if (outputs == null || outputs.size() == 0) {
			fieldsMissing = new ArrayList<String>();
			fieldsMissing.add("outputs are not defined correctly");
			completeConfig = false;
			return;
		}
		try {
			outputList = new ArrayList<DijTensor>();
			
			for (Map<String, Object> out : outputs) {
				DijTensor outTensor = new DijTensor((String) out.get("name"));
				outTensor.form = (String) out.get("axes");
				outTensor.form = outTensor.form == null ? null : outTensor.form.toUpperCase();
				outTensor.tensorType = outTensor.form == null ? "list" : "image";
				if (outTensor.form != null && outTensor.form.toUpperCase().contains("I"))
					outTensor.form = outTensor.form.toUpperCase().replace("I", "R");
				if (outTensor.form == null || outTensor.form.contains("R") || (outTensor.form.length() <= 2 && (outTensor.form.contains("B") || outTensor.form.contains("C"))))
					outTensor.tensorType = "list";
				// TODO List auxDataRange = (List) out.get("data_range");
				// TODO outTensor.dataRange = castListToDoubleArray(auxDataRange);
				outTensor.dataType = (String) "" + out.get("data_type");
				if (outTensor.tensorType.contains("image") && !pyramidalNetwork) {
					List auxHalo = (List) out.get("halo");
					outTensor.halo = castListToIntArray(auxHalo);
				} else if (outTensor.tensorType.contains("image")) {
					outTensor.halo = new int[outTensor.form.length()];
				}
				
	
				// Find by trial and error if the shape of the input is fixed or not
				Object objectShape = out.get("shape");
				if (objectShape instanceof List<?>) {
					List<Object> shape = (ArrayList<Object>) objectShape;
					outTensor.recommended_patch = castListToIntArray(shape);
					outTensor.scale = new float[shape.size()];
					outTensor.offset = new int[shape.size()];
					if (pyramidalNetwork)
						outTensor.sizeOutputPyramid = outTensor.recommended_patch;
				} else if (objectShape instanceof HashMap<?,?>) {
					Map<String, Object> shape = (Map<String, Object>) objectShape;
					outTensor.referenceImage = (String) shape.get("reference_input");
					List auxScale = (List) shape.get("scale");
					outTensor.scale = castListToFloatArray(auxScale);
					List auxOffset = (List) shape.get("offset");
					outTensor.offset = castListToIntArray(auxOffset);
				} else {
					
				}
				
				// Check that the output definition fields are complete
				if ((outTensor.form == null && outTensor.tensorType.contentEquals("image")) 
						|| outTensor.dataType == null || outTensor.scale == null
						|| outTensor.offset == null) {
					completeConfig = false;
					return;
				}
				
				outputList.add(outTensor);
			}
		} catch(Exception ex) {
			fieldsMissing = new ArrayList<String>();
			fieldsMissing.add("Outputs are not defined correctly");
			completeConfig = false;
			return;
		}
		// Output test information
		List<LinkedHashMap<String, Object>> output_information = new ArrayList <LinkedHashMap<String, Object>>();
		if (test_information.get("outputs") instanceof LinkedHashMap) {
			LinkedHashMap<String, Object> aux = (LinkedHashMap<String, Object>) test_information.get("outputs");
			output_information.add(aux);
		} else if (test_information.get("outputs") instanceof List){
			output_information = (List<LinkedHashMap<String, Object>>) test_information.get("outputs");
		}
		
		savedOutputs = new ArrayList<HashMap<String, String>>();
		for (LinkedHashMap<String, Object> out : output_information) {
			HashMap<String, String> info = new LinkedHashMap<String, String>();
			String outName =  (String) "" + out.get("name");
			info.put("name", outName);
			String size =  (String) "" + out.get("size");
			info.put("size", size);
			String type = (String) "" + out.get("type");
			info.put("type", type);

			savedOutputs.add(info);
		}
		
		// Info about runtime and memory
		memoryPeak = (String) test_information.get("memory_peak") + "";
		runtime = (String) "" + test_information.get("runtime");

		
		// Get all the preprocessings available in the Yaml
		Map<String, Object> prediction = (Map<String, Object>) deepimagej.get("prediction");
		if (prediction == null)
			prediction = new HashMap<String, Object>();
		pre = new HashMap<String, String[]>();
		post = new HashMap<String, String[]>();
		Set<String> keys = prediction.keySet();
		for (String key : keys) {
			if (key.contains("preprocess")) {
				List<Map<String, Object>> preprocess = (List<Map<String, Object>>) prediction.get(key);
				// TODO convert into a list of processings
				String[] commands = new String[preprocess.size()];
				int processingCount = 0;
				for (Map<String, Object> processing : preprocess) {
					String spec = "" + processing.get("spec");
					if (spec != null && processing.containsKey("kwargs")) {
						commands[processingCount] = "" + processing.get("kwargs");
					} else if (spec != null && !spec.contentEquals("null") && !processing.containsKey("kwargs") && spec.contains(".jar")) {
						int extensionPosition = spec.indexOf(".jar");
						if (extensionPosition != -1)
							commands[processingCount] = spec.substring(0, extensionPosition + 4);
					} else if (spec != null && !spec.contentEquals("null") && !processing.containsKey("kwargs") && spec.contains(".class")) {
						int extensionPosition = spec.indexOf(".class");
						if (extensionPosition != -1)
							commands[processingCount] = spec.substring(0, extensionPosition + 6);
					} else if (spec == null || spec.contentEquals("null")) {
						commands = null;
					}
					
					processingCount ++;
				}
				pre.put(key, commands);
			}
			if (key.contains("postprocess")) {
				List<Map<String, Object>> postprocess = (List<Map<String, Object>>) prediction.get(key);
				// TODO convert into a list of processings
				String[] commands = new String[postprocess.size()];
				int processingCount = 0;
				for (Map<String, Object> processing : postprocess) {
					String spec = "" + processing.get("spec");
					if (spec != null && processing.containsKey("kwargs")) {
						commands[processingCount] = "" + processing.get("kwargs");
					} else if (spec != null && !spec.contentEquals("null") && !processing.containsKey("kwargs") && spec.contains(".jar")) {
						int extensionPosition = spec.indexOf(".jar");
						if (extensionPosition != -1)
							commands[processingCount] = spec.substring(0, extensionPosition + 4);
					} else if (spec != null && !spec.contentEquals("null") && !processing.containsKey("kwargs") && spec.contains(".class")) {
						int extensionPosition = spec.indexOf(".class");
						if (extensionPosition != -1)
							commands[processingCount] = spec.substring(0, extensionPosition + 6);
					} else if (spec == null || spec.contentEquals("null")) {
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
	
	/*
	 * Method that checks which required fields of the yaml file are missing in the provided
	 * file. It returns a list with the missing fields.
	 */
	public static ArrayList<String> checkYaml(Map<String, Object> obj) {
		ArrayList<String> missingFields = new ArrayList<String>();
		// Array with all the required fields for DeepImageJ
		String[] requiredFieldsArray = new String[]{"format_version", "name", "timestamp", "description",
				"authors", "cite", "git_repo", "tags", "license", "documentation",// TODO "attachments",  "packaged_by",
				"inputs", "outputs", "covers", // TODO "dependencies",
				"weights", "config"};// TODO , "spec"};
		Set<String> yamlFields = obj.keySet();
		List<String> dictionaryFields = Arrays.asList(new String[] {"inputs", "outputs", "config", "weights"});
		for (String field : requiredFieldsArray) {
			if (!yamlFields.contains(field))
				missingFields.add(field);
			if (dictionaryFields.contains(field)) {
				missingFields = checkYamlDictionary(field, obj, missingFields);
			}
		}
		return missingFields;
	}
	
	/*
	 * Method that checks fields inside a dictionary field of the yaml
	 */
	public static ArrayList<String> checkYamlDictionary(String ogField, Map<String, Object> obj, ArrayList<String> missingFields) {
		return checkYamlDictionary(ogField, obj, missingFields, null); 
	}
	
	/*
	 * Method that checks fields inside a dictionary field of the yaml
	 */
	public static ArrayList<String> checkYamlDictionary(String ogField, Map<String, Object> obj, ArrayList<String> missingFields, String aux) {
		// List of dictionaries inside each file with its required fields
		HashMap<String, String[]> keywords = new HashMap<String, String[]>();
		keywords.put("inShape", new String[] {"min", "step"});
		keywords.put("outShape", new String[] {"reference_input", "scale", "offset"});
		keywords.put("deepimagej", new String[] {"pyramidal_model", "allow_tiling", "prediction"});
		keywords.put("weightFormat", new String[] {"source", "sha256", "test_inputs",
				"test_outputs", "sample_inputs", "sample_outputs"});
		keywords.put("weights", new String[] {"pytorch_script", "tensorflow_saved_model_bundle"});
		keywords.put("config", new String[] {"deepimagej"});
		Set<String> yamlFields = null;
		HashMap<String, Object> dict = null;

		if (ogField.contentEquals("inputs") || ogField.contentEquals("outputs")) {
			missingFields = checkInputsOutputsField(ogField, obj, missingFields);
		} else if (ogField.contentEquals("weights")) {
			boolean format = false;
			if (obj.get(ogField) instanceof HashMap<?,?>) {
				dict = (HashMap<String, Object>) obj.get(ogField);
				List<String> possibleWeights = Arrays.asList(keywords.get(ogField));
				for (String weightFormat : dict.keySet()) {
					if (possibleWeights.contains(weightFormat)) {
						format = true;
						// In the case that any weightformat is present, proceed to check everything is in order
						if (dict.get(weightFormat) != null && dict.get(weightFormat) instanceof HashMap<?,?>)
							missingFields = checkYamlDictionary("weightFormat", (HashMap<String, Object>) dict.get(weightFormat), missingFields, "weights::" + weightFormat);
						else
							missingFields.add("weights::" + weightFormat);
					}		
				}
			}
			if (!format)
				missingFields.add("weights");
		} else if (ogField.contentEquals("weightFormat") || ogField.contentEquals("inShape") ||
					ogField.contentEquals("outShape") || ogField.contentEquals("deepimagej")) {
			// Check that all the keys are there
			String[] list = keywords.get(ogField);
			yamlFields = obj.keySet();
			for (String str : list) {
				if (!yamlFields.contains(str))
					missingFields.add(aux + "::" + str);
			}
		} else if (ogField.contentEquals("config")) {
			String[] configField = keywords.get(ogField);
			// The config yaml is organised as a dictionary. Check
			// if in our case it corresponds to a dictionary
			if (obj.get(ogField) instanceof HashMap<?,?>) {
				dict = (HashMap<String, Object>) obj.get(ogField);
				yamlFields = dict.keySet();
				for (String str : configField) {
					if (!yamlFields.contains(str))
						missingFields.add("config::" + str);
				}
				// In the case that deepimagej is present, proceed to check everything is in order
				if (dict.get("deepimagej") != null && dict.get("deepimagej") instanceof HashMap<?,?>) {
					missingFields = checkYamlDictionary("deepimagej", (HashMap<String, Object>) dict.get("deepimagej"),
														missingFields, "config::deepimagej");
				} else {
					missingFields.add("config");
				}
			} else {
				missingFields.add("config");
			}
		}
		return missingFields;
	}
	
	/*
	 * 
	 */
	public static ArrayList<String> checkInputsOutputsField(String ogField, Map<String, Object> obj,
															ArrayList<String> missingFields) {
		String[] inputsOutputsField = new String[] {"name", "axes", "data_type", "data_range", "shape"};
		Set<String> yamlFields = null;
		// The inputs and outputs in the yaml are organised as a list of dictionaries. Check
		// at each of the possible inputs for all the keywords
		if (obj.get(ogField) instanceof List<?>) {
			for (int i = 0; i < ((List<?>) obj.get(ogField)).size(); i ++) {
				Object inp= ((List<?>) obj.get(ogField)).get(i);
				if (inp instanceof HashMap<?, ?>) {
					yamlFields  = ((HashMap<String, Object>) inp).keySet();
					for (String str : inputsOutputsField) {
						if (!yamlFields.contains(str))
							missingFields.add(ogField + "::" + str);
					}
					// In the case that the shape is not fixed, look if all the required fields are there
					if (((HashMap<String, Object>) inp).get("shape") instanceof HashMap<?,?> && ogField.equals("inputs")) {
						missingFields = checkYamlDictionary("inShape",
														(HashMap<String, Object>) ((HashMap<String, Object>) inp).get("shape"),
														missingFields, "inputs#" + i + "::shape");
					} else if (((HashMap<String, Object>) inp).get("shape") instanceof HashMap<?,?> && ogField.equals("outputs")) {
						missingFields = checkYamlDictionary("outShape",
														(HashMap<String, Object>) ((HashMap<String, Object>) inp).get("shape"),
														missingFields, "outputs#" + i + "::shape");
					}
				} else {
					// If the ith input does not corresponfd to a HashMap, it is faulty
					missingFields.add(ogField + " #" + i);
				}
			}
		}
		else {
			// If the 'inputs' field does not correspond to a List<>, set the whole field as missing
			missingFields.add(ogField);
		}
		return missingFields;
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
			array[c ++] = Integer.parseInt(in.toString());
		}
		return array;
	}
	
	public static double[] castListToDoubleArray(List list) {
		try {
			double[] array = new double[list.size()];
			int c = 0;
			for (Object in : list) {
				array[c ++] = Double.parseDouble(in.toString());
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
				array[c ++] = Float.parseFloat(in.toString());
			}
			return array;
		} catch (ClassCastException ex) {
			return null;			
		}
	}
}
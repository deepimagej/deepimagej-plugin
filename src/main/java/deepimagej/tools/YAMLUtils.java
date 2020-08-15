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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.TensorFlowModel;
import ij.IJ;

public class YAMLUtils {

	private final static String idName = "name";
	private final static String idDescription = "description";
	private final static String idAuthors = "authors";
	private final static String idNodeName = "name";
	private final static String idNodeAxes = "axes";
	private final static String idNodeDataType = "data_type";
	private final static String idNodeDataRange = "data_range";
	private final static String idNodeShapeMin = "min";
	private final static String idNodeShapeStep = "step";
	private final static String idModelTag = "tensorflow_model_tag";
	private final static String idSigDef = "tensorflow_siganture_def";
	private final static String idMemoryPeak = "memory_peak";
	private final static String idRuntime = "runtime";
	
	public static void writeYaml(DeepImageJ dp) {
		Parameters params = dp.params;

		Map<String, Object> data = new LinkedHashMap<>();
		
		List<Map<String, Object>> modelInputMapsList = new ArrayList<>();
		List<Map<String, Object>> inputTestInfoList = new ArrayList<>();
		for (DijTensor inp : params.inputList) {
			if (inp.tensorType.contains("image")) {
				// Create dictionary for each image input
				Map<String, Object> inputTensorMap = new LinkedHashMap<>();
				inputTensorMap.put(idNodeName, inp.name);
				inputTensorMap.put(idNodeAxes, inp.form.toLowerCase());

				inputTensorMap.put(idNodeDataType, "float32");
				inputTensorMap.put(idNodeDataRange, Arrays.toString(inp.dataRange));
				if (params.fixedInput) {
					inputTensorMap.put("shape", Arrays.toString(inp.recommended_patch));
				} else if (!params.fixedInput) {
					Map<String, Object> shape = new LinkedHashMap<>();
					shape.put(idNodeShapeMin, Arrays.toString(inp.minimum_size));
					int[] aux = new int[inp.minimum_size.length];
					for(int i = 0; i < aux.length; i ++) {aux[i] += inp.step[i];}
					shape.put(idNodeShapeStep, Arrays.toString(aux));
					inputTensorMap.put("shape", shape);
				}
				modelInputMapsList.add(inputTensorMap);
				
				// Now write the test data info
				Map<String, Object> inputTestInfo = new LinkedHashMap<>();
				inputTestInfo.put("name", params.testImageBackup.getTitle());
				inputTestInfo.put("size", inp.inputTestSize);
				Map<String, Object> pixelSize = new LinkedHashMap<>();
				pixelSize.put("x", inp.inputPixelSizeX);
				pixelSize.put("y", inp.inputPixelSizeY);
				pixelSize.put("z", inp.inputPixelSizeZ);
				inputTestInfo.put("pixel_size", pixelSize);
				inputTestInfoList.add(inputTestInfo);
			}
		}

		// Test output metadata
		List<Map<String, Object>> modelOutputMapsList =  new ArrayList<>();
		for (DijTensor out : params.outputList) {
			// Create dictionary for each input
			Map<String, Object> outputTensorMap = getOutput(out, params.pyramidalNetwork, params.allowPatching);
			modelOutputMapsList.add(outputTensorMap);
		}
		
		// Write the info of the outputs after postprocesing
		List<Map<String, Object>> outputTestInfoList =  new ArrayList<>();
		for (HashMap<String, String> out : params.savedOutputs) {
			
			Map<String, Object> outputTestInfo = new LinkedHashMap<>();
			outputTestInfo.put("name", out.get("name"));
			outputTestInfo.put("type", out.get("type"));
			outputTestInfo.put("size", out.get("size"));
			outputTestInfoList.add(outputTestInfo);
		}
		
		data.put(idName, params.name);
		// Short description of the model
		data.put(idDescription, params.description);
		// List of authors who trained/prepared the actual model which is being saved
		data.put(idAuthors, params.author);
		
		// Citation
		data.put("cite", params.cite);
		
		// Info relevant to DeepImageJ, see: https://github.com/bioimage-io/configuration/issues/23
		Map<String, Object> config = new LinkedHashMap<>();
		Map<String, Object> deepimagej = new LinkedHashMap<>();
		deepimagej.put("pyramidal_model", params.pyramidalNetwork);
		deepimagej.put("allow_tiling", params.allowPatching);
		
		// TF model keys
		Map<String, Object> modelKeys = new LinkedHashMap<>();
		// Model tag
		modelKeys.put(idModelTag, TensorFlowModel.returnTfTag(params.tag));
		// Model signature definition
		modelKeys.put(idSigDef, TensorFlowModel.returnTfSig(params.graph));
		deepimagej.put("model_keys", modelKeys);
		
		// Test metadata
		Map<String, Object> testInformation = new LinkedHashMap<>();
		// Test input metadata
		testInformation.put("inputs", inputTestInfoList);
		
		// Test output metadata
		testInformation.put("outputs", outputTestInfoList);
		
		// Output size of the examples used to compose the model
		testInformation.put(idMemoryPeak, params.memoryPeak);
		// Output size of the examples used to compose the model
		testInformation.put(idRuntime, params.runtime);
		// Metadata of the example used to compose the model
		deepimagej.put("test_information", testInformation);
		
		config.put("deepimagej", deepimagej);
		
		// Save the model
		// Architecture
		Map<String, Object> model = new LinkedHashMap<>();
		model.put("source", "./saved_model.pb");
		try {
			model.put("sha256", FileTools.createSHA256(params.saveDir + File.separator + "saved_model.pb"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Weights
		Map<String, Object> weights = params.previousVersions;
		// Version
		Map<String, Object> version = new LinkedHashMap<>();
		String weightsVersion = "v" + params.version.trim();
		version.put("source", "./weights_" + weightsVersion + ".zip");
		String zipFile = params.saveDir + File.separator + "weights_" + weightsVersion + ".zip";
		if (new File(zipFile).isFile())
			try {
				version.put("sha256", FileTools.createSHA256(params.saveDir + File.separator + "weights_" + weightsVersion + ".zip"));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		weights.put(weightsVersion, version);
		
		
		
		// Link to the documentation of the model, which contains info about
		// the model such as the images used or architecture
		data.put("documentation", params.documentation);
		// Path to the image that will be used as the cover picture in the Bioimage model Zoo
		data.put("cover", Arrays.asList(params.coverImage));
		// Path to the test inputs
		ArrayList<String> inputExamples = new ArrayList<String>();
		for (DijTensor in : params.inputList) {
			if (in.exampleInput != null)
				inputExamples.add("./" + in.exampleInput);
		}
		data.put("test_input", inputExamples);
		// Path to the test outputs
		ArrayList<String> outputExamples = new ArrayList<String>();
		for (HashMap<String, String> out : params.savedOutputs)
			outputExamples.add("./" + out.get("name"));
		data.put("test_output", outputExamples);
		// Tags that will be used to look for the model in the Bioimage model Zoo
		data.put("tags", params.infoTags);
		// Type of license of the model
		data.put("license", params.license);
		// Version of the model
		data.put("format_version", params.version);
		// Programming language in which the model was prepared for the Bioimage model zoo
		data.put("language", params.language);
		// Deep Learning framework with which the model was obtained
		data.put("framework", params.framework);
		// Link to a website where we can find the model
		data.put("source", params.source);
		// Link to the folder containing the architecture
		data.put("model", model);
		// Link to the folder containing the weights
		data.put("weights", weights);
		// Information relevant to deepimagej
		data.put("config", config);
		
		data.put("inputs", modelInputMapsList);
		data.put("outputs", modelOutputMapsList);
		
		// Preprocessing
		List<Map<String, String>> listPreprocess = new ArrayList<Map<String, String>>();
		if (params.firstPreprocessing == null) {
			params.firstPreprocessing = params.secondPostprocessing;
			params.secondPreprocessing = null;
		}
		
		if ((params.firstPreprocessing != null) && (params.firstPreprocessing.contains(".ijm") || params.firstPreprocessing.contains(".txt"))) {
			Map<String, String> preprocess = new LinkedHashMap<>();
			preprocess.put("spec", "ij.IJ::runMacroFile");
			preprocess.put("kwargs", new File(params.firstPreprocessing).getName());
			listPreprocess.add(preprocess);
		} else if ((params.firstPreprocessing != null) && (params.firstPreprocessing.contains(".class") || params.firstPreprocessing.contains(".jar"))) {
			String filename = new File(params.firstPreprocessing).getName();
			Map<String, String> preprocess = new LinkedHashMap<>();
			preprocess.put("spec", filename + " " + params.javaPreprocessingClass);
			listPreprocess.add(preprocess);
		} else if (params.firstPreprocessing == null && params.secondPreprocessing == null) {
			Map<String, String> preprocess = new LinkedHashMap<>();
			preprocess.put("spec", null);
			listPreprocess.add(preprocess);
		} 
		if ((params.secondPreprocessing != null) && (params.secondPreprocessing.contains(".ijm") || params.secondPreprocessing.contains(".txt"))) {
			Map<String, String> preprocess = new LinkedHashMap<>();
			preprocess.put("spec", "ij.IJ::runMacroFile");
			preprocess.put("kwargs", new File(params.secondPreprocessing).getName());
			listPreprocess.add(preprocess);
		} else if ((params.secondPreprocessing != null) && (params.secondPreprocessing.contains(".class") || params.secondPreprocessing.contains(".jar"))) {
			String filename = new File(params.secondPreprocessing).getName();
			Map<String, String> preprocess = new LinkedHashMap<>();
			preprocess.put("spec", filename + " " + params.javaPreprocessingClass);
			listPreprocess.add(preprocess);
		}

		// Postprocessing
		List<Map<String, String>> listPostprocess = new ArrayList<Map<String, String>>();
		if (params.firstPostprocessing == null) {
			params.firstPostprocessing = params.secondPostprocessing;
			params.secondPostprocessing = null;
		}
		if ((params.firstPostprocessing != null)  && (params.firstPostprocessing.contains(".ijm") || params.firstPostprocessing.contains(".txt"))) {
			Map<String, String> postprocess = new LinkedHashMap<>();
			postprocess.put("spec", "ij.IJ::runMacroFile");
			postprocess.put("kwargs", new File(params.firstPostprocessing).getName());
			listPostprocess.add(postprocess);
		} else if ((params.firstPostprocessing != null)  && (params.firstPostprocessing.contains(".class") || params.firstPostprocessing.contains(".jar"))) {
			String filename = new File(params.firstPostprocessing).getName();
			Map<String, String> postprocess = new LinkedHashMap<>();
			postprocess.put("spec", filename + " " + params.javaPostprocessingClass);
			listPostprocess.add(postprocess);
		} else if (params.firstPostprocessing == null && params.secondPostprocessing == null) {
			Map<String, String> postprocess = new LinkedHashMap<>();
			postprocess.put("spec", null);
			listPostprocess.add(postprocess);
		} 
		if ((params.secondPostprocessing != null) && (params.secondPostprocessing.contains(".ijm") || params.secondPostprocessing.contains(".txt"))) {
			Map<String, String> postprocess = new LinkedHashMap<>();
			postprocess.put("spec", "ij.IJ::runMacroFile");
			postprocess.put("kwargs", new File(params.secondPostprocessing).getName());
			listPostprocess.add(postprocess);
		} else if ((params.secondPostprocessing != null) && (params.secondPostprocessing.contains(".class") || params.secondPostprocessing.contains(".jar"))) {
			String filename = new File(params.secondPostprocessing).getName();
			Map<String, String> postprocess = new LinkedHashMap<>();
			postprocess.put("spec", filename + " " + params.javaPostprocessingClass);
			listPostprocess.add(postprocess);
		}

		// Prediction, preprocessing and postprocessing together
		Map<String, Object> prediction = new LinkedHashMap<>();
		prediction.put("preprocess", listPreprocess);
		prediction.put("postprocess", listPostprocess);
		
		data.put("prediction", prediction);

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
		options.setIndent(4);
		//options.setPrettyFlow(true);
		Yaml yaml = new Yaml(options);
		FileWriter writer = null;
		try {
			writer = new FileWriter(new File(params.saveDir, "config.yaml"));
			yaml.dump(data, writer);
			writer.close();
			removeQuotes(new File(params.saveDir, "config.yaml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Map<String, Object> readConfig(String yamlFile) {
		File initialFile = new File(yamlFile);
		InputStream targetStream = null;
	    try {
			targetStream = new FileInputStream(initialFile);
			Yaml yaml = new Yaml();
			Map<String, Object> obj = yaml.load(targetStream);
			targetStream.close();
			
			return obj;
		} catch (FileNotFoundException e) {
			IJ.error("Invalid YAML file");
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * Method to write the output of the yaml file. The fields written
	 * depend on the type of network that we are defining.
	 */
	public static Map<String, Object> getOutput(DijTensor out, boolean pyramidal, boolean allowPatching){
		Map<String, Object> outputTensorMap = new LinkedHashMap<>();
		outputTensorMap.put("name", out.name);
		
		if (!pyramidal && out.tensorType.contains("image")) {
			outputTensorMap.put("axes", out.form.toLowerCase());
			outputTensorMap.put("data_type", "float32");
			outputTensorMap.put("data_range", Arrays.toString(out.dataRange));
			outputTensorMap.put("halo",  Arrays.toString(out.halo));
			Map<String, Object> shape = new LinkedHashMap<>();
			shape.put("reference_input", out.referenceImage);
			shape.put("scale", Arrays.toString(out.scale));
			shape.put("offset", Arrays.toString(out.offset));
			outputTensorMap.put("shape", shape);
			
		} else if (pyramidal && out.tensorType.contains("image")) {
			outputTensorMap.put("axes", out.form.toLowerCase());
			outputTensorMap.put("data_type", "float32");
			outputTensorMap.put("data_range", Arrays.toString(out.dataRange));
			outputTensorMap.put("shape", Arrays.toString(out.sizeOutputPyramid));
			
		}else if (out.tensorType.contains("list")) {
			outputTensorMap.put("axes", null);
			outputTensorMap.put("shape", Arrays.toString(out.tensor_shape));
			outputTensorMap.put("data_type", "float32");
			outputTensorMap.put("data_range", Arrays.toString(out.dataRange));
		}
		return outputTensorMap;
	}
	
	public static void removeQuotes(File file) throws FileNotFoundException {

		Scanner scanner = new Scanner(file);       // create scanner to read

	    // do something with that line
	    String newLine = "";
		while(scanner.hasNextLine()){  // while there is a next line
		    String line = scanner.nextLine();  // line = that next line
		
		
		    // replace a character
		    for (int i = 0; i < line.length(); i++){
		        if (line.charAt(i) != '\'') {  // or anything other character you chose
		            newLine += line.charAt(i);
		        }
		    }
		    newLine += '\n';
		
		}
		scanner.close();
		PrintWriter writer = new PrintWriter(file.getAbsolutePath()); // create file to write to
		writer.print(newLine);
		writer.close();
	}
}
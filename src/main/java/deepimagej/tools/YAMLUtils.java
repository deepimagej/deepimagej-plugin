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
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.DeepLearningModel;
import deepimagej.stamp.TfSaveStamp;
import ij.IJ;

public class YAMLUtils {
	
	public static void writeYaml(DeepImageJ dp) throws NoSuchAlgorithmException, IOException {
		Parameters params = dp.params;

		Map<String, Object> data = new LinkedHashMap<>();
		
		List<Map<String, Object>> modelInputMapsList = new ArrayList<>();
		List<Map<String, Object>> inputTestInfoList = new ArrayList<>();
		for (DijTensor inp : params.inputList) {
			if (inp.tensorType.contains("image")) {
				// Create dictionary for each image input
				Map<String, Object> inputTensorMap = new LinkedHashMap<>();
				inputTensorMap.put("name", inp.name);
				inputTensorMap.put("axes", inp.form.toLowerCase());

				inputTensorMap.put("data_type", "float32");
				inputTensorMap.put("data_range", Arrays.toString(inp.dataRange));
				if (params.fixedInput) {
					inputTensorMap.put("shape", Arrays.toString(inp.recommended_patch));
				} else if (!params.fixedInput) {
					Map<String, Object> shape = new LinkedHashMap<>();
					shape.put("min", Arrays.toString(inp.minimum_size));
					int[] aux = new int[inp.minimum_size.length];
					for(int i = 0; i < aux.length; i ++) {aux[i] += inp.step[i];}
					shape.put("step", Arrays.toString(aux));
					inputTensorMap.put("shape", shape);
				}
				inputTensorMap.put("preprocessing", null);
				modelInputMapsList.add(inputTensorMap);
				
				// Now write the test data info
				Map<String, Object> inputTestInfo = new LinkedHashMap<>();
				if (params.testImageBackup != null)
					inputTestInfo.put("name", params.testImageBackup.getTitle().substring(4));
				else 
					inputTestInfo.put("name", null);
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

		// Version of the yaml file
		data.put("format_version", params.format_version);
		// Name of the model
		data.put("name", params.name);
		// Short description of the model
		data.put("description", params.description);
		// Timestamp of when the model was created following ISO 8601
		String thisMoment = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS").format(Calendar.getInstance().getTime());
		data.put("timestamp", thisMoment);
		
		// Citation
		if (params.cite != null && params.cite.size() == 0)
			params.cite = null;
		data.put("cite", params.cite);
		// List of authors who trained/prepared the actual model which is being saved
		data.put("authors", params.author);
		// Link to the documentation of the model, which contains info about
		// the model such as the images used or architecture
		data.put("documentation", params.documentation);
		// Path to the image that will be used as the cover picture in the Bioimage model Zoo
		ArrayList<String> covers = new ArrayList<String>();
		// TODO generalize for several input images
		if (params.testImageBackup != null) 
			covers.add("./" + TfSaveStamp.getTitleWithoutExtension(params.testImageBackup.getTitle().substring(4)) + ".tif");
		else 
			covers.add(null);
		for (HashMap<String, String> out : params.savedOutputs) {
			if (out.get("type").contains("image"))
				covers.add("./" + TfSaveStamp.getTitleWithoutExtension(out.get("name")) + ".tif");
		}
		data.put("covers", covers);
		// Tags that will be used to look for the model in the Bioimage model Zoo
		data.put("tags", params.infoTags);
		// Type of license of the model
		data.put("license", params.license);
		// Programming language in which the model was prepared for the Bioimage model zoo
		data.put("language", params.language);
		// Deep Learning framework with which the model was obtained
		data.put("framework", params.framework);
		// Git repo where info to the model can be found
		data.put("git_repo", params.git_repo);
		
		// Create field containing the weights format and info
		Map<String, Object> weights = new LinkedHashMap<>();
		// Map for a specific format containing the info for the weigths of a given format
		Map<String, Object> format_info = new LinkedHashMap<>();
		// Authors of the model. Authors of the package if this model
		// is the original model
		// TODO how to put it in the interface
		format_info.put("authors", null);
		format_info.put("parent", null);
		
		// TODO allow uploading models to github, zenodo or drive
		format_info.put("source", null);
		// For Tensorflow, if upload to biozoo is selected, calculate checksum
		// For Pytorch, always calculate checksum
		if (params.framework.equals("pytorch")) {
			format_info.put("sha256", FileTools.createSHA256(params.saveDir + File.separator + "pytorch_script.pt"));
		} else if (params.framework.equals("tensorflow") && params.biozoo) {
			format_info.put("sha256", FileTools.createSHA256(params.saveDir + File.separator + "tensorflow_saved_model_bundle.zip"));
		} else if (params.framework.equals("tensorflow") && !params.biozoo) {
			format_info.put("sha256", null);
		}
		// Path to the test inputs
		ArrayList<String> inputExamples = new ArrayList<String>();
		ArrayList<String> sampleInputs = new ArrayList<String>();
		// TODO generalize for several input images
		if (params.testImageBackup != null) {
			String title =  params.testImageBackup.getTitle().substring(4);
			inputExamples.add("./" + TfSaveStamp.getTitleWithoutExtension(title) + ".tif");
			sampleInputs.add("./" + TfSaveStamp.getTitleWithoutExtension(title) + ".npy");
		} else {
			inputExamples.add(null);
			sampleInputs.add(null);
		}
		// Path to the test outputs
		ArrayList<String> outputExamples = new ArrayList<String>();
		ArrayList<String> sampleOutputs = new ArrayList<String>();
		for (HashMap<String, String> out : params.savedOutputs) {
			if (out.get("type").contains("image"))
				outputExamples.add("./" + TfSaveStamp.getTitleWithoutExtension(out.get("name")) + ".tif");
			else if (out.get("type").contains("ResultsTable"))
				outputExamples.add("./" + TfSaveStamp.getTitleWithoutExtension(out.get("name")) + ".csv");
			sampleOutputs.add("./" + TfSaveStamp.getTitleWithoutExtension(out.get("name")) + ".npy");
		}
		
		if (params.framework.equals("pytorch")) {
			weights.put("pytorch_script", format_info);
		} else {
			weights.put("tensorflow_saved_model_bundle", format_info);
		}
		// Add the preprocessing attachments to the weights, as they are part of the model
		ArrayList<String> aux = new ArrayList<String>();
		for (String str : params.attachments) {
			aux.add(new File(str).getName());
		}
		// Add information asking the developer to add plugin requirements to the attachments list
		aux.add("Include here any plugin that might be required for pre- or post-processing");
		// List of Java files that need to be included to make the plugin run
		weights.put("attachments", aux);
		

		
		// Info relevant to DeepImageJ, see: https://github.com/bioimage-io/configuration/issues/23
		Map<String, Object> config = new LinkedHashMap<>();
		Map<String, Object> deepimagej = new LinkedHashMap<>();
		deepimagej.put("pyramidal_model", params.pyramidalNetwork);
		deepimagej.put("allow_tiling", params.allowPatching);
		
		// TF model keys
		if (params.framework.contains("tensorflow")) {
			Map<String, Object> modelKeys = new LinkedHashMap<>();
			// Model tag
			modelKeys.put("tensorflow_model_tag", DeepLearningModel.returnTfTag(params.tag));
			// Model signature definition
			modelKeys.put("tensorflow_siganture_def", DeepLearningModel.returnTfSig(params.graph));
			deepimagej.put("model_keys", modelKeys);
		} else if (params.framework.contains("pytorch")) {
			deepimagej.put("model_keys", null);
		}
		
		// Test metadata
		Map<String, Object> testInformation = new LinkedHashMap<>();
		// Test input metadata
		testInformation.put("inputs", inputTestInfoList);
		
		// Test output metadata
		testInformation.put("outputs", outputTestInfoList);
		
		// Output size of the examples used to compose the model
		testInformation.put("memory_peak", params.memoryPeak);
		// Output size of the examples used to compose the model
		testInformation.put("runtime", params.runtime);
		// Metadata of the example used to compose the model
		deepimagej.put("test_information", testInformation);
		
				
		// Put the example inputs and outputs
		data.put("sample_inputs", sampleInputs);
		data.put("sample_outputs", sampleOutputs);
		data.put("test_inputs", inputExamples);
		data.put("test_outputs", outputExamples);
		
		
		// TODO what attachments should go here?
		ArrayList<String> attachments = new ArrayList<String>();
		data.put("attachments", attachments);
		// Link to the folder containing the weights
		data.put("weights", weights);
		
		data.put("inputs", modelInputMapsList);
		data.put("outputs", modelOutputMapsList);
		
		
		// Preprocessing
		List<Map<String, String>> listPreprocess = new ArrayList<Map<String, String>>();
		if (params.firstPreprocessing == null) {
			params.firstPreprocessing = params.secondPostprocessing;
			params.secondPreprocessing = null;
		}
		
		int c = 0;
		if ((params.firstPreprocessing != null) && (params.firstPreprocessing.contains(".ijm") || params.firstPreprocessing.contains(".txt"))) {
			Map<String, String> preprocess = new LinkedHashMap<>();
			preprocess.put("spec", "ij.IJ::runMacroFile");
			preprocess.put("kwargs", new File(params.firstPreprocessing).getName());
			listPreprocess.add(preprocess);
		} else if ((params.firstPreprocessing != null) && (params.firstPreprocessing.contains(".class") || params.firstPreprocessing.contains(".jar"))) {
			String filename = new File(params.firstPreprocessing).getName();
			Map<String, String> preprocess = new LinkedHashMap<>();
			preprocess.put("spec", filename + " " + params.javaPreprocessingClass.get(c ++) + "::preProcessingRoutineUsingImage");
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
			preprocess.put("spec", filename + " " + params.javaPreprocessingClass.get(c ++) + "::preProcessingRoutineUsingImage");
			listPreprocess.add(preprocess);
		}

		// Postprocessing
		List<Map<String, String>> listPostprocess = new ArrayList<Map<String, String>>();
		if (params.firstPostprocessing == null) {
			params.firstPostprocessing = params.secondPostprocessing;
			params.secondPostprocessing = null;
		}
		c = 0;
		if ((params.firstPostprocessing != null)  && (params.firstPostprocessing.contains(".ijm") || params.firstPostprocessing.contains(".txt"))) {
			Map<String, String> postprocess = new LinkedHashMap<>();
			postprocess.put("spec", "ij.IJ::runMacroFile");
			postprocess.put("kwargs", new File(params.firstPostprocessing).getName());
			listPostprocess.add(postprocess);
		} else if ((params.firstPostprocessing != null)  && (params.firstPostprocessing.contains(".class") || params.firstPostprocessing.contains(".jar"))) {
			String filename = new File(params.firstPostprocessing).getName();
			Map<String, String> postprocess = new LinkedHashMap<>();
			postprocess.put("spec", filename + " " + params.javaPostprocessingClass.get(c ++) + "::postProcessingRoutineUsingImage");
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
			postprocess.put("spec", filename + " " + params.javaPostprocessingClass.get(c ++) + "::postProcessingRoutineUsingImage");
			listPostprocess.add(postprocess);
		}

		// Prediction, preprocessing and postprocessing together
		Map<String, Object> prediction = new LinkedHashMap<>();
		prediction.put("preprocess", listPreprocess);
		prediction.put("postprocess", listPostprocess);
		
		// Information relevant to deepimagej
		deepimagej.put("prediction", prediction);
		config.put("deepimagej", deepimagej);
		data.put("config", config);

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
		options.setIndent(4);
		//options.setPrettyFlow(true);
		Yaml yaml = new Yaml(options);
		FileWriter writer = null;
		try {
			writer = new FileWriter(new File(params.saveDir, "model.yaml"));
			yaml.dump(data, writer);
			writer.close();
			removeQuotes(new File(params.saveDir, "model.yaml"));
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
			outputTensorMap.put("axes", out.form.toLowerCase());
			outputTensorMap.put("shape", Arrays.toString(out.tensor_shape));
			outputTensorMap.put("data_type", "float32");
			outputTensorMap.put("data_range", Arrays.toString(out.dataRange));
		}
		// TODO what to do with postprocesing
		outputTensorMap.put("postprocessing", null);
		return outputTensorMap;
	}
	
	public static void removeQuotes(File file) throws FileNotFoundException {

		Scanner scanner = new Scanner(file);       // create scanner to read

	    // do something with that line
	    String newLine = "";
		while(scanner.hasNextLine()){  // while there is a next line
		    String line = scanner.nextLine();  // line = that next line
		
		    // Replace Infinity by inf
		    line = line.replace("Infinity", "inf");
		    // Replace '-   ' by '  - ' and '- ' by '  - '
		    if (line.contains("-   ")) {
			    line = line.replace("-   ", "  - ");
		    } else if (line.contains("- ")) {
		    	line = line.replace("- ", "  - ");
		    }
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
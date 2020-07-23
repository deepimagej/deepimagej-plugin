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
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.TensorFlowModel;

public class YAMLUtils {

	private final static String idName = "name";
	private final static String idDescription = "description";
	private final static String idCite = "cite";
	private final static String idCiteText = "text";
	private final static String idCiteDoi = "doi";
	private final static String idAuthors = "authors";
	private final static String idDocumentation = "documentation";
	private final static String idDate = "date";
	private final static String idTags = "tags";
	private final static String idTestInput = "test_input";
	private final static String idTestOutput = "test_output";
	private final static String idCoverImage = "cover";
	private final static String idLicense = "license";
	private final static String idFormatVersion = "format_version";
	private final static String idLanguage = "language";
	private final static String idFramework = "framework";
	private final static String idSource = "source";
	private final static String idInputs = "inputs";
	private final static String idOutputs = "outputs";
	private final static String idNodeName = "name";
	private final static String idNodeAxes = "axes";
	private final static String idNodeDataType = "data_type";
	private final static String idNodeDataRange = "data_range";
	private final static String idNodeShape = "shape";
	private final static String idNodeShapeMin = "min";
	private final static String idNodeShapeStep = "step";
	private final static String idNodeHalo = "halo";
	private final static String idNodeShapeReferenceInput = "reference_input";
	private final static String idNodeShapeScale = "scale";
	private final static String idNodeShapeOffset = "offset";
	private final static String idModelTag = "tensorflow_model_tag";
	private final static String idSigDef = "tensorflow_siganture_def";
	private final static String idMemoryPeak = "memory_peak";
	private final static String idPixelSize = "pixel_size";
	private final static String idRuntime = "runtime";
	private final static String idInputSize = "input_size";
	private final static String idOutputSize = "output_size";
	private final static String idTestMetadata = "test_information";
	private final static String idTfMetadata = "tensorflow_metadata";
	
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
				inputTensorMap.put(idNodeAxes, inp.form);
				// TODO remove
				//inputTensorMap.put(idNodeDims, Arrays.toString(inp.tensor_shape));
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
				inputTestInfo.put("name", inp.name);
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
		// Credits to the lab where it was done
		// Date when the model was produced
		data.put(idDate, params.date);
		// List of authors who trained/prepared the actual model which is being saved
		//data.put(idAuthors, listAuthors(params.author));
		data.put(idAuthors, params.author);
		
		// Citation
		List<LinkedHashMap<String, Object>> citations = new ArrayList<LinkedHashMap<String, Object>>();
		LinkedHashMap<String, Object> cite = new LinkedHashMap<String, Object>();
		// Reference to the article, Github repo or other source where the model was proposed
		cite.put("text", params.reference);
		// Url to the paper or repo where the model was proposed
		cite.put("doi", params.doi);
		citations.add(cite);
		data.put(idCite, citations);
		
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
		
		// Link to the documentation of the model, which contains info about
		// the model such as the images used or architecture
		data.put(idDocumentation, params.documentation);
		// Path to the image that will be used as the cover picture in the Bioimage model Zoo
		data.put(idCoverImage, Arrays.asList(params.coverImage));
		// Path to the test inputs
		String[] inputImString = {"." + File.separator + "exampleImage.tiff"};
		data.put(idTestInput, Arrays.asList(inputImString));
		// Path to the test outputs
		String[] outputImString = {"." + File.separator + "resultImage.tiff"};
		data.put(idTestOutput, Arrays.asList(outputImString));
		// Tags that will be used to look for the model in the Bioimage model Zoo
		data.put(idTags, params.infoTags);
		// Type of license of the model
		data.put(idLicense, params.license);
		// Version of the model
		data.put(idFormatVersion, params.version);
		// Programming language in which the model was prepared for the Bioimage model zoo
		data.put(idLanguage, params.language);
		// Deep Learning framework with which the model was obtained
		data.put(idFramework, params.framework);
		// Link to a website where we can find the model
		data.put(idSource, params.source);
		// Information relevant to deepimagej
		data.put("config", config);
		
		data.put(idInputs, modelInputMapsList);
		data.put(idOutputs, modelOutputMapsList);
		
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
	
	public static List<String> listAuthors(String authorsString) {
		String[] authorsArray = authorsString.split(",");
		List<String> authorsList = Arrays.asList(authorsArray);
		return authorsList;
	}
	
	public static Map<String, Object> readConfig(String yamlFile) {
		File initialFile = new File(yamlFile);
		InputStream targetStream = null;
	    try {
			targetStream = new FileInputStream(initialFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Yaml yaml = new Yaml();
		Map<Object, Object> obj = yaml.load(targetStream);
		
		Map<String, Object> dpParams = new LinkedHashMap<>();
		
		String name = (String) obj.get(idName);
		//List<String> authors = (List<String>) obj.get(idAuthors);
		String authors = (String) obj.get(idAuthors);

		String version = (String) obj.get(idFormatVersion);
		String date = (String) obj.get(idDate);
		
		// Citation
		Map<String, Object> cite = (Map<String, Object>) obj.get(idCite);
		String reference = castListToStringArray((List) cite.get(idCiteText))[0];
		String doi = castListToStringArray((List) cite.get(idCiteDoi))[0];
		// Model metadata
		Map<String, Object> modelMeta = (Map<String, Object>) obj.get(idTfMetadata);
		String tag = castListToStringArray((List) modelMeta.get(idModelTag))[0];
		String sigDef = castListToStringArray((List) modelMeta.get(idSigDef))[0];
		// Test metadata
		Map<String, Object> testMetadata = (Map<String, Object>) obj.get(idTestMetadata);
		String memPeak = (String) testMetadata.get(idMemoryPeak);
		String runtime = (String) testMetadata.get(idRuntime);
		List pixelSize = (List) testMetadata.get(idPixelSize);
		List inputSize = (List) testMetadata.get(idInputSize);
		List outputSize = (List) testMetadata.get(idOutputSize);

		dpParams.put("name", name);
		dpParams.put("author", authors);
		dpParams.put("tag", tag);
		dpParams.put("sigDef", sigDef);
		dpParams.put("URL", doi);
		dpParams.put("reference", reference);
		dpParams.put("version", version);
		dpParams.put("date", date);
		dpParams.put("memoryPeak", memPeak);
		dpParams.put("runtime", runtime);
		dpParams.put("pixelSize", pixelSize);
		dpParams.put("inputSize", inputSize);
		dpParams.put("outputSize", outputSize);
		
		List inputs = (List) obj.get(idInputs);
		List<Object> inputList = new ArrayList<Object>();
		for (Object inp : inputs) {
			DijTensor inpTensor = new DijTensor((String) ((Map<String, Object>) inp).get(idName));
			inpTensor.form = (String) ((Map<String, Object>) inp).get(idNodeAxes);
			List auxTensorShape = (List) ((Map<String, Object>) inp).get(idNodeDims);
			inpTensor.tensor_shape = castListToIntArray(auxTensorShape);
			//inpTensor.dataType = (String) ((Map<String, Object>) inp).get(idNodeDataType);
			List auxDataRange = (List) ((Map<String, Object>) inp).get(idNodeDataRange);
			inpTensor.dataRange = castListToDoubleArray(auxDataRange);
			Map<String, Object> shape = (Map<String, Object>) ((Map<String, Object>) inp).get(idNodeShape);
			List auxRecommendedPatch = (List) shape.get(idPatchSize);
			inpTensor.recommended_patch = castListToIntArray(auxRecommendedPatch);
			List auxMinimumSize = (List) shape.get(idNodeShapeMin);
			inpTensor.minimum_size = castListToIntArray(auxMinimumSize);
			List auxStepSize = (List) shape.get(idNodeShapeStep);
			inpTensor.step = castListToIntArray(auxStepSize);
			inputList.add(inpTensor);
		}
		dpParams.put("inputList", inputList);

		List outputs = (List) obj.get(idOutputs);
		List<Object> outputList = new ArrayList<Object>();
		for (Object out : outputs) {
			DijTensor outTensor = new DijTensor((String) ((Map<String, Object>) out).get(idName));
			outTensor.form = (String) ((Map<String, Object>) out).get(idNodeAxes);
			List auxTensorShape = (List) ((Map<String, Object>) out).get(idNodeDims);
			outTensor.tensor_shape = castListToIntArray(auxTensorShape);
			//inpTensor.dataType = (String) ((Map<String, Object>) inp).get(idNodeDataType);
			List auxDataRange = (List) ((Map<String, Object>) out).get(idNodeDataRange);
			outTensor.dataRange = castListToDoubleArray(auxDataRange);
			List auxHalo = (List) ((Map<String, Object>) out).get(idNodeHalo);
			outTensor.halo = castListToIntArray(auxHalo);
			Map<String, Object> shape = (Map<String, Object>) ((Map<String, Object>) out).get(idNodeShape);
			outTensor.referenceImage = (String) shape.get(idNodeShapeReferenceInput);
			List auxScale = (List) shape.get(idNodeShapeScale);
			outTensor.scale = castListToFloatArray(auxScale);
			List auxOffset = (List) shape.get(idNodeShapeOffset);
			outTensor.offset = castListToIntArray(auxOffset);
			outputList.add(outTensor);
		}
		dpParams.put("outputList", outputList);
		
		Map<String, Object> inputObj = (Map<String, Object>) inputs.get(0);
		String axesObj = (String) inputObj.get(idNodeAxes);
		if(axesObj == null) return null;
		return dpParams;
	}
	
	/*
	 * Method to write the output of the yaml file. The fields written
	 * depend on the type of network that we are defining.
	 */
	public static Map<String, Object> getOutput(DijTensor out, boolean pyramidal, boolean allowPatching){
		Map<String, Object> outputTensorMap = new LinkedHashMap<>();
		outputTensorMap.put(idNodeName, out.name);
		
		if (!pyramidal && out.tensorType.contains("image")) {
			outputTensorMap.put(idNodeAxes, out.form);
			// TODO remove
			//outputTensorMap.put(idNodeDims, Arrays.toString(out.tensor_shape));
			outputTensorMap.put(idNodeDataType, "float32");
			outputTensorMap.put(idNodeDataRange, Arrays.toString(out.dataRange));
			outputTensorMap.put(idNodeHalo,  Arrays.toString(out.halo));
			Map<String, Object> shape = new LinkedHashMap<>();
			shape.put(idNodeShapeReferenceInput, out.referenceImage);
			shape.put(idNodeShapeScale, Arrays.toString(out.scale));
			shape.put(idNodeShapeOffset, Arrays.toString(out.offset));
			outputTensorMap.put(idNodeShape, shape);
			
		} else if (pyramidal && out.tensorType.contains("image")) {
			outputTensorMap.put(idNodeAxes, out.form);
			outputTensorMap.put(idNodeDataType, "float32");
			outputTensorMap.put(idNodeDataRange, Arrays.toString(out.dataRange));
			outputTensorMap.put("shape", Arrays.toString(out.sizeOutputPyramid));
			
		}else if (out.tensorType.contains("list")) {
			outputTensorMap.put(idNodeAxes, null);
			outputTensorMap.put("shape", Arrays.toString(out.tensor_shape));
			outputTensorMap.put(idNodeDataType, "float32");
			outputTensorMap.put(idNodeDataRange, Arrays.toString(out.dataRange));
		}
		return outputTensorMap;
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
		double[] array = new double[list.size()];
		int c = 0;
		for (Object in : list) {
			array[c ++] = (double) in;
		}
		return array;
	}
	
	public static float[] castListToFloatArray(List list) {
		float[] array = new float[list.size()];
		int c = 0;
		for (Object in : list) {
			array[c ++] = ((Double) in).floatValue();
		}
		return array;
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
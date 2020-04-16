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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	private final static String idNodeDims = "dimensions";
	private final static String idNodeDataType = "data_type";
	private final static String idNodeDataRange = "data_range";
	private final static String idNodeShape = "shape";
	private final static String idNodeShapeMin = "min";
	private final static String idNodeShapeStep = "step";
	private final static String idNodeHalo = "halo";
	private final static String idNodeShapeReferenceInput = "reference_input";
	private final static String idNodeShapeScale = "scale";
	private final static String idNodeShapeOffset = "offset";
	private final static String idPrediction = "prediction";
	private final static String idPredictionPreprocess = "preprocess";
	private final static String idPredictionPostprocess = "postprocess";
	private final static String idPatchSize = "patch_size";
	private final static String idModelTag = "tensorflow_model_tag";
	private final static String idSigDef = "tensorflow_siganture_def";
	private final static String idMemoryPeak = "memory_peak";
	private final static String idPixelSize = "pixel_size";
	private final static String idRuntime = "runtime";
	private final static String idInputSize = "input_size";
	private final static String idOutputSize = "output_size";
	private final static String idTestMetadata = "test_metadata";
	private final static String idTfMetadata = "tensorflow_metadata";
	
	public static void writeYaml(DeepImageJ dp) {
		Parameters params = dp.params;

		Map<String, Object> data = new LinkedHashMap<>();
		
		List<Object> modelInputMapsList = new ArrayList<>();
		for (DijTensor inp : params.inputList) {
			// Create dictionary for each input
			Map<String, Object> inputTensorMap = new LinkedHashMap<>();
			inputTensorMap.put(idNodeName, inp.name);
			inputTensorMap.put(idNodeAxes, inp.form);
			inputTensorMap.put(idNodeDims, inp.tensor_shape);
			inputTensorMap.put(idNodeDataType, "float32");
			inputTensorMap.put(idNodeDataRange, inp.dataRange);
			Map<String, Object> shape = new LinkedHashMap<>();
			shape.put(idPatchSize, inp.recommended_patch);
			shape.put(idNodeShapeMin, inp.minimum_size);
			int[] aux = new int[inp.minimum_size.length];
			for(int i = 0; i < aux.length; i ++) {aux[i] += inp.step[i];}
			shape.put(idNodeShapeStep, aux);
			inputTensorMap.put(idNodeShape, shape);
			modelInputMapsList.add(inputTensorMap);
		}
		List<Object> modelOutputMapsList =  new ArrayList<>();
		for (DijTensor out : params.outputList) {
			// Create dictionary for each input
			Map<String, Object> outputTensorMap = new LinkedHashMap<>();
			outputTensorMap.put(idNodeName, out.name);
			outputTensorMap.put(idNodeAxes, out.form);
			outputTensorMap.put(idNodeDims, out.tensor_shape);
			outputTensorMap.put(idNodeDataType, "float32");
			outputTensorMap.put(idNodeDataRange, out.dataRange);
			outputTensorMap.put(idNodeHalo,  out.halo);
			Map<String, Object> shape = new LinkedHashMap<>();
			shape.put(idNodeShapeReferenceInput, out.referenceImage);
			shape.put(idNodeShapeScale, out.scale);
			shape.put(idNodeShapeOffset, out.offset);
			outputTensorMap.put(idNodeShape, shape);
			modelOutputMapsList.add(outputTensorMap);
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
		Map<String, Object> cite = new LinkedHashMap<>();
		// Reference to the article, Github repo or other source where the model was proposed
		cite.put(idCiteText, new String[] {params.reference});
		// Url to the paper or repo where the model was proposed
		cite.put(idCiteDoi, new String[] {params.doi});
		data.put(idCite, cite);
		
		// TF model metadata
		Map<String, Object> tfMetadata = new LinkedHashMap<>();
		// Model tag
		tfMetadata.put(idModelTag, new String[] {TensorFlowModel.returnTfTag(params.tag)});
		// Model signature definition
		tfMetadata.put(idSigDef, new String[] {TensorFlowModel.returnTfSig(params.graph)});

		// Test metadata
		Map<String, Object> testMetadata = new LinkedHashMap<>();
		// Input size of the examples used to compose the model
		testMetadata.put(idInputSize, params.inputSize);
		// Pixel size of the input examples used to compose the model
		testMetadata.put(idPixelSize, params.inputPixelSize);
		// Output size of the examples used to compose the model
		testMetadata.put(idOutputSize, params.outputSize);
		// Output size of the examples used to compose the model
		testMetadata.put(idMemoryPeak, params.memoryPeak);
		// Output size of the examples used to compose the model
		testMetadata.put(idRuntime, params.runtime);
		
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
		data.put(idTags, Arrays.asList(params.infoTags));
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
		// Metadata of the model used to compose the model
		data.put(idTfMetadata, tfMetadata);
		// Metadata of the example used to compose the model
		data.put(idTestMetadata, testMetadata);
		
		data.put(idInputs, modelInputMapsList);
		data.put(idOutputs, modelOutputMapsList);
		
		// Prediction
		Map<String, Object> prediction = new LinkedHashMap<>();
		// TODO find a solution for including macros more elegantly
		prediction.put(idPredictionPreprocess,  "preprocessing.txt");
		prediction.put(idPredictionPostprocess, "postprocessing.txt");
		data.put(idPrediction, prediction);
        
		Yaml yaml = new Yaml();
		FileWriter writer = null;
		try {
			writer = new FileWriter(new File(params.saveDir, "config.yaml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		yaml.dump(data, writer);
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
}
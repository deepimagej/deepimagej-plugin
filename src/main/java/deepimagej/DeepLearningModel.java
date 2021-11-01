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
import java.net.JarURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.TensorFlow;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;
import org.tensorflow.framework.TensorShapeProto.Dim;

import com.google.protobuf.InvalidProtocolBufferException;

import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import deepimagej.tools.StartTensorflowService;
import ij.IJ;
import net.imagej.tensorflow.TensorFlowVersion;

public class DeepLearningModel {

	// Same as the tag used in export_saved_model in the Python code.
	private static final String[] MODEL_TAGS = {"serve", "inference", "train", "eval", "gpu", "tpu"};
	private static final String DEFAULT_TAG = "serve";
	
	
	private static final String[] TF_MODEL_TAGS = {"tf.saved_model.tag_constants.SERVING",
											   	   "tf.saved_model.tag_constants.INFERENCE",
											   	   "tf.saved_model.tag_constants.TRAINING",
											   	   "tf.saved_model.tag_constants.EVAL",
											   	   "tf.saved_model.tag_constants.GPU",
											   	   "tf.saved_model.tag_constants.TPU"};
	
	
	private static final String[] SIGNATURE_CONSTANTS = {"serving_default",
												   	     "inputs",
												   	     "tensorflow/serving/classify",
												   	     "classes",
												   	     "scores",
												   	     "inputs",
												   	     "tensorflow/serving/predict",
												   	     "outputs",
												   	     "inputs",
												   	     "tensorflow/serving/regress",
												   	     "outputs",
												   	     "train",
												   	     "eval",
												   	     "tensorflow/supervised/training",
												   	     "tensorflow/supervised/eval"};

	private static final String[] TF_SIGNATURE_CONSTANTS = {"tf.saved_model.signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY",
												   	     "tf.saved_model.signature_constants.CLASSIFY_INPUTS",
												   	     "tf.saved_model.signature_constants.CLASSIFY_METHOD_NAME",
												   	     "tf.saved_model.signature_constants.CLASSIFY_OUTPUT_CLASSES",
												   	     "tf.saved_model.signature_constants.CLASSIFY_OUTPUT_SCORES",
												   	     "tf.saved_model.signature_constants.PREDICT_INPUTS",
												   	     "tf.saved_model.signature_constants.PREDICT_METHOD_NAME",
												   	     "tf.saved_model.signature_constants.PREDICT_OUTPUTS",
												   	     "tf.saved_model.signature_constants.REGRESS_INPUTS",
												   	     "tf.saved_model.signature_constants.REGRESS_METHOD_NAME",
												   	     "tf.saved_model.signature_constants.REGRESS_OUTPUTS",
												   	     "tf.saved_model.signature_constants.DEFAULT_TRAIN_SIGNATURE_DEF_KEY",
												   	     "tf.saved_model.signature_constants.DEFAULT_EVAL_SIGNATURE_DEF_KEY",
												   	     "tf.saved_model.signature_constants.SUPERVISED_TRAIN_METHOD_NAME",
												   	     "tf.saved_model.signature_constants.SUPERVISED_EVAL_METHOD_NAME"};
	
	// TODO remove this or the next method
	public static SavedModelBundle loadTf(String path, String tag, Log log) {
		log.print("load model from " + path);

		SavedModelBundle model = null;
		try {
			Runtime instance = Runtime.getRuntime();
			double a = instance.freeMemory() / (1024*1024.0);
			model = SavedModelBundle.load(path, tag);
			double b = instance.freeMemory() / (1024*1024.0);
			System.out.println(b-a);
		}
		catch (Exception e) {
			log.print("Exception in loading model " + path);
			log.print(e.toString());
			log.print(e.getMessage());
			return null;
		}
		log.print("Loaded");
		return model;
	}

	public static SavedModelBundle loadTfModel(String source, String modelTag) {
		// Load the model with its correspondent tag
		SavedModelBundle model;
		try {
			model = SavedModelBundle.load(source, modelTag);
		}
		catch (TensorFlowException e) {
			System.out.println("The tag was incorrect");
			model = null;
		}
		return model;
	}

	public static Object[] findTfTag(String source) {
		// Obtain the model_tag needed to load the model. If none works,
		// 'null' is returned
		Object[] info = checkTfTags(source, DEFAULT_TAG);
		return info;
	}

	public static Object[] checkTfTags(String source, String tag) {
		SavedModelBundle model = null;
		Set<String> sigKeys;
		Object[] info = new Object[3];
		try {
			model = SavedModelBundle.load(source, tag);
			sigKeys = metaGraphsSet(model);
		}
		catch (TensorFlowException e) {
			// If the tag does not work, try with the following existing tag
			int tag_ind = Index.indexOf(MODEL_TAGS, tag);
			if (tag_ind < MODEL_TAGS.length - 1) {
				Object[] info2 = checkTfTags(source, MODEL_TAGS[tag_ind + 1]);
				tag = (String) info2[0];
				sigKeys = (Set<String>) info2[1];
			}
			else {
				// tag = null, the user will need to introduce it
				tag = null;
				sigKeys = null;
			}
		}
		info[0] = tag;
		info[1] = sigKeys;
		info[2] = model;
		return info;
	}

	public static Set<String> metaGraphsSet(SavedModelBundle model) {
		byte[] byteGraph = model.metaGraphDef();
		// Obtain a mapping between the possible keys and their signature definitions
		Map<String, SignatureDef> sig = null;
		try {
			sig = MetaGraphDef.parseFrom(byteGraph).getSignatureDefMap();
		}
		catch (InvalidProtocolBufferException e) {
			System.out.println("The model is not a correct SavedModel model");
		}
		Set<String> modelKeys = sig.keySet();
		return modelKeys;
	}

	public static SignatureDef getSignatureFromGraph(SavedModelBundle model, String graph) {
		byte[] byteGraph = model.metaGraphDef();
		SignatureDef sig = null;
		try {
			sig = MetaGraphDef.parseFrom(byteGraph).getSignatureDefOrThrow(graph);
		}
		catch (InvalidProtocolBufferException e) {
			System.out.println("Invalid graph");
		}
		return sig;
	}

	public static int[] modelTfExitDimensions(SignatureDef sig, String entryName) {
		// This method returns the dimensions of the tensor defined by
		// the saved model. The method retrieves the tensor info and
		// converts it into an array of integers.
		TensorInfo entryInfo = sig.getOutputsOrThrow(entryName);
		TensorShapeProto entryShape = entryInfo.getTensorShape();
		List<Dim> listDim = entryShape.getDimList();
		int rank = listDim.size();
		int[] inputTensorSize = new int[rank];

		for (int i = 0; i < rank; i++) {
			inputTensorSize[i] = (int) listDim.get(i).getSize();
		}
		return inputTensorSize;
	}

	public static int[] modelTfEntryDimensions(SignatureDef sig, String entryName) {
		// This method returns the dimensions of the tensor defined by
		// the saved model. The method retrieves the tensor info and
		// converts it into an array of integers.
		TensorInfo entryInfo = sig.getInputsOrThrow(entryName);
		TensorShapeProto entryShape = entryInfo.getTensorShape();
		List<Dim> listDim = entryShape.getDimList();
		int rank = listDim.size();
		int[] inputTensorSize = new int[rank];

		for (int i = 0; i < rank; i++) {
			inputTensorSize[i] = (int) listDim.get(i).getSize();
		}

		return inputTensorSize;
	}

	public static String[] returnTfOutputs(SignatureDef sig) {

		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		Map<String, TensorInfo> out = sig.getOutputsMap();
		Set<String> outputKeys = out.keySet();
		String[] keysArray = outputKeys.toArray(new String[outputKeys.size()]);
		return keysArray;
	}

	public static String[] returnTfInputs(SignatureDef sig) {

		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		Map<String, TensorInfo> inp = sig.getInputsMap();
		Set<String> inputKeys = inp.keySet();
		String[] keysArray = inputKeys.toArray(new String[inputKeys.size()]);
		return keysArray;
	}

	public static int nChannelsOrSlices(DijTensor tensor, String channelsOrSlices) {
		// Find the number of channels or slices in the corresponding tensor
		String letter = "";
		if (channelsOrSlices.equals("channels")) {
			letter = "C";
		} else {
			letter = "Z";
		}
		
		int nChannels;
		String inputForm = tensor.form;
		int ind = Index.indexOf(inputForm.split(""), letter);
		if (ind == -1) {
			nChannels = 1;
		}
		else {
			nChannels = tensor.minimum_size[ind];
		}
		return nChannels;
	}
	
	public static String hSize(Parameters params, String inputForm) {
		// Find the number of channels in the input
		String nChannels;
		int ind = Index.indexOf(inputForm.split(""), "Y");
		if (ind == -1) {
			nChannels = "-1";
		}
		else {
			nChannels = Integer.toString(params.inputList.get(0).tensor_shape[ind]);
		}
		return nChannels;
	}
	
	public static String wSize(Parameters params, String inputForm) {
		// Find the number of channels in the input
		String nChannels;
		int ind = Index.indexOf(inputForm.split(""), "X");
		if (ind == -1) {
			nChannels = "-1";
		}
		else {
			nChannels = Integer.toString(params.inputList.get(0).tensor_shape[ind]);
		}
		return nChannels;
	}
	
	// Method added to allow multiple possible batch sizes
	public static String nBatch(int[] dims, String inputForm) {
		// Find the number of channels in the input
		String inBatch;
		int ind = Index.indexOf(inputForm.split(""), "B");
		if (ind == -1) {
			inBatch = "1";
		} else {
			inBatch = Integer.toString(dims[ind]);
		}
		if (inBatch.equals("-1")) {
			inBatch = "1";
		}
		return inBatch;
	}
	
	public static String returnTfTag(String tag) {
		String tfTag;
		int tagInd = Index.indexOf(MODEL_TAGS, tag);
		if (tagInd == -1) {
			tfTag = tag;
		} else {
			tfTag = TF_MODEL_TAGS[tagInd];
		}
		return tfTag;
	}
	
	public static String returnStringTag(String tfTag) {
		String tag;
		int tagInd = Index.indexOf(TF_MODEL_TAGS, tfTag);
		if (tagInd == -1) {
			tag = tfTag;
		} else {
			tag = MODEL_TAGS[tagInd];
		}
		return tag;
	}
	
	public static Set<String> returnTfSig(Set<String> sig) {
		Set<String> tfSig = new HashSet<>();
		for (int i = 0; i < TF_SIGNATURE_CONSTANTS.length; i ++) {
			if (sig.contains(SIGNATURE_CONSTANTS[i]) == true) {
				tfSig.add(TF_SIGNATURE_CONSTANTS[i]);
			}
		}
		if (tfSig.size() != sig.size()) {
			tfSig = sig;
		}
		return tfSig;
	}
	
	public static String returnStringSig(String tfSig) {
		String sig;
		int sigInd = Index.indexOf(TF_SIGNATURE_CONSTANTS, tfSig);
		if (sigInd == -1) {
			sig = tfSig;
		} else {
			sig = SIGNATURE_CONSTANTS[sigInd];
		}
		return sig;
	}
	
	public static String returnTfSig(String sig) {
		String tfSig;
		int tfSigInd = Index.indexOf(SIGNATURE_CONSTANTS, sig);
		if (tfSigInd == -1) {
			tfSig = sig;
		} else {
			tfSig = TF_SIGNATURE_CONSTANTS[tfSigInd];
		}
		return tfSig;
	}
	
	// TODO group Tf and Pytorch methods regarding versions
	/*
	 * Find if the CUDA and Tf versions are compatible
	 */
	public static String PytorchCUDACompatibility(String ptVersion, String CUDAVersion) {
		String errMessage = "";
		if (CUDAVersion.equals("nocuda")) {
				errMessage = "";
		} else if (ptVersion.contains("1.7.0") && !(CUDAVersion.contains("10.2") || CUDAVersion.contains("10.1") || CUDAVersion.contains("11.0"))) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with DJL Pytorch 1.7.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install either CUDA 10.1, CUDA 10.2 or CUDA 11.0.\n";
		} else if (ptVersion.contains("1.6.0") && !(CUDAVersion.contains("10.2") || CUDAVersion.contains("10.1"))) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with DJL Pytorch  1.6.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 10.1, CUDA 10.2.\n";
		} else if (ptVersion.contains("1.5.0") && !(CUDAVersion.contains("10.1")  || CUDAVersion.contains("10.2") || CUDAVersion.contains("9.2"))) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with DJL Pytorch  1.5.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 9.2, CUDA 10.1 or CUDA 10.2.\n";
		} else if (ptVersion.contains("1.4.0") && !(CUDAVersion.contains("10.1")  || CUDAVersion.contains("9.2"))) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with DJL Pytorch  1.4.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 9.2 or CUDA 10.1.\n";
		} else if (!CUDAVersion.toLowerCase().contains("nocuda") && !ptVersion.equals("")) {
			errMessage = "Make sure that the DJL Pytorch version is compatible with the installed CUDA version.\n"
					+ "Check the DeepImageJ Wiki for more information";
		}
		return errMessage;
	}
	
	/*
	 * Find if the CUDA and Tf versions are compatible
	 */
	public static String TensorflowCUDACompatibility(String tfVersion, String CUDAVersion) {
		String errMessage = "";
		if (tfVersion.contains("1.15.0") && !CUDAVersion.contains("10.0")) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with tf 1.15.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 10.0.\n";
		} else if (tfVersion.contains("1.14.0") && !CUDAVersion.contains("10.0")) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with tf 1.14.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 10.0.\n";
		} else if (tfVersion.contains("1.13.0") && !CUDAVersion.contains("10.0")) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with tf 1.13.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 10.0.\n";
		} else if (tfVersion.contains("1.12.0") && !CUDAVersion.contains("9.0")) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with tf 1.12.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 9.0.\n";
		} else if (!tfVersion.contains("1.15.0") && !tfVersion.contains("1.14.0") && !tfVersion.contains("1.13.0") && !tfVersion.contains("1.12.0")) {
			errMessage = "Make sure that the Tensorflow version is compatible with the installed CUDA version.\n";
		}
		return errMessage;
	}
	
	/*
	 * Get the Pytorch version number from the jar file
	 * The corresponding JAR is pytorch-native-auto-X.Y.Z.jar,
	 * where X.Y.Z is the version number
	 */
	public static String getPytorchVersion() {
		String ptJni = "";

		// TODO this only works for 1.7.0
		/*try {
			URL resource = NativeHelper.class.getResource("NativeHelper.class");
			JarURLConnection connection = null;
			connection = (JarURLConnection) resource.openConnection();
			ptJni = connection.getJarFileURL().getFile();
		} catch (Exception e) {
		}
		*/
		ptJni = getLibPytorchJar();
		if (!ptJni.contains("jar"))
			return ptJni;
		
		String ptVersion = getPytorchVersionFromJar(ptJni);
		return ptVersion;	
	}

	/*
	 * Finds the directory where the Pytorch jar is
	 */
	public static String getLibPytorchJar() {

		// Search in the plugins folder
		String ijDirectory = IJ.getDirectory("imagej") + File.separator;
		// TODO remove 
		//ijDirectory = "C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app";
		
		String pluginsDirectory = ijDirectory + File.separator + "plugins" + File.separator;
		String pluginsJar = findPytorchJar(pluginsDirectory);

		// Search in the jars folder
		String jarDirectory = ijDirectory + File.separator + "jars" + File.separator;
		String jarsJar = findPytorchJar(jarDirectory);

		// Check that there is only one jar file present in both folders
		if (jarsJar.equals(pluginsJar) && jarsJar.equals("")) {
			return "-No Pytorch version found-";
		} else if (jarsJar.toLowerCase().contains("more than 1 version") || pluginsJar.toLowerCase().contains("more than 1 version")) {
			return "-More than one Pytorch version present-";
		} else if (jarsJar.toLowerCase().contains("tensorflow") && jarsJar.toLowerCase().contains("tensorflow") && !jarsJar.equals(pluginsJar)) {
			return "-The plugins and jars directories contains a different version of Pytorch each-";
		}

		// Find which of them is actually the TF jni jar
		String pytorchJni = pluginsJar;
		if (pytorchJni.equals("") == true) {
			pytorchJni = jarsJar;
		}
		return pytorchJni;
	}

	/*
	 * Finds the file corresponding to the tf jar
	 */
	public static String findPytorchJar(String folderDir) {
		// Find the file libtensorflow_jni.jar

		// Name of the TF jni without the version
		String jarName = "pytorch-native-auto";
		// Auxiliary variable to make sure we only have one TF jni
		int nJars = 0;
		String ptJar = "";

		File folder = new File(folderDir);
		File[] listOfFiles = folder.listFiles();
		
		if (listOfFiles == null)
			return "";

		for (File file : listOfFiles) {
			if (file.isFile() == true) {
				String fileName = file.getAbsolutePath();
				if (fileName.indexOf(jarName) != -1) {
					nJars ++;
					ptJar = fileName;
				}
			}
		}

		if (nJars == 0) {
			ptJar = "";
		} else if (nJars >1) {
			ptJar = "more than 1 version";
		}

		return ptJar;
	}

	/*
	 * Get the version number from the jar file
	 */
	public static String getPytorchVersionFromJar(String jar) {
		// Name of the TF jni without the version
		jar = jar.toLowerCase();
		String flag = "pytorch-native-auto-";
		String jarExt = ".jar";
		String tfVersion = jar.substring(jar.lastIndexOf(flag) + flag.length(), jar.indexOf(jarExt));
		return tfVersion;
	}
	
	/*
	 * Get the version number from the jar file
	 */
	public static String getTFVersion(boolean fiji) {
		if (fiji) {
			TensorFlowVersion tfVersion = StartTensorflowService.getTfService().getTensorFlowVersion();
			return tfVersion.getVersionNumber();
		} else {
			return getTFVersionIJ();
		}
	}
	
	/*
	 * Retrieves the TF version that is going to be used for the plugin.
	 * In order to do that, the method searches in two locations where the 
	 *.jars might be: in the plugins folder or in the jars folder
	 */
	public static String getTFVersionIJ() {
		String tfJni = "";
		try {
			URL resource = ClassLoader.getSystemClassLoader().getResource("org/tensorflow/native");
			if (resource == null)
				resource = IJ.getClassLoader().getResource("org/tensorflow/native");
			JarURLConnection connection = null;
			connection = (JarURLConnection) resource.openConnection();
			tfJni = connection.getJarFileURL().getFile();
		} catch (Exception e) {
			tfJni = getLibTfJar();
			if (!tfJni.contains("jar"))
				return tfJni;
		}
		String tfVersion = getTfVersionFromJar(tfJni);
		
		if (tfVersion.contains("gpu")) {
			tfVersion = tfVersion.substring(tfVersion.toLowerCase().indexOf("gpu_") + 5) + " GPU";
		}
		return tfVersion;	
	}

	/*
	 * Finds the directory where the tf jar is
	 */
	public static String getLibTfJar() {

		// Search in the plugins folder
		String ijDirectory = IJ.getDirectory("imagej") + File.separator;
		// TODO remove 
		//ijDirectory = "C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app";
		String pluginsDirectory = ijDirectory + File.separator + "plugins" + File.separator;
		String pluginsJar = findTFJar(pluginsDirectory);

		// Search in the jars folder
		String jarDirectory = ijDirectory + File.separator + "jars" + File.separator;
		String jarsJar = findTFJar(jarDirectory);

		// Check that there is only one jar file present in both folders
		if (jarsJar.equals(pluginsJar) && jarsJar.equals("")) {
			return "-No Tensorflow version found-";
		} else if (jarsJar.toLowerCase().contains("more than 1 version") || pluginsJar.toLowerCase().contains("more than 1 version")) {
			return "-More than one tensorflow version present-";
		} else if (jarsJar.toLowerCase().contains("tensorflow") && pluginsJar.toLowerCase().contains("tensorflow") && !jarsJar.equals(pluginsJar)) {
			return "-The plugins and jars directories contains a different version of TF each-";
		}

		// Find which of them is actually the TF jni jar
		String tfJni = pluginsJar;
		if (tfJni.equals("") == true) {
			tfJni = jarsJar;
		}
		return tfJni;
	}

	/*
	 * Finds the file corresponding to the tf jar
	 */
	public static String findTFJar(String folderDir) {
		// Find the file libtensorflow_jni.jar

		// Name of the TF jni without the version
		String jarName = "libtensorflow_jni";
		// Auxiliary variable to make sure we only have one TF jni
		int nJars = 0;
		String tfJar = "";

		File folder = new File(folderDir);
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles == null)
			return "";

		for (File file : listOfFiles) {
			if (file.isFile() == true) {
				String fileName = file.getAbsolutePath();
				if (fileName.indexOf(jarName) != -1) {
					nJars ++;
					tfJar = fileName;
				}
			}
		}

		if (nJars == 0) {
			tfJar = "";
		} else if (nJars >1) {
			tfJar = "more than 1 version";
		}

		return tfJar;
	}

	/*
	 * Get the version number from the jar file
	 */
	public static String getTfVersionFromJar(String jar) {
		// Name of the TF jni without the version
		jar = jar.toLowerCase();
		String flag = "libtensorflow_jni";
		String jarExt = ".jar";
		String tfVersion = jar.substring(jar.lastIndexOf(flag) + flag.length() + 1, jar.indexOf(jarExt));
		return tfVersion;
	}

}

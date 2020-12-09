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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.scijava.Context;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;
import org.tensorflow.framework.TensorShapeProto.Dim;

import com.google.protobuf.InvalidProtocolBufferException;

import deepimagej.components.CustomizedColumn;
import deepimagej.components.CustomizedTable;
import deepimagej.processing.LoadJar;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import ij.IJ;
import net.imagej.tensorflow.DefaultTensorFlowService;
import net.imagej.tensorflow.TensorFlowService;
import net.imagej.tensorflow.TensorFlowVersion;
//import net.imagej.legacy.LegacyService;

public class TensorFlowModel {

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


	private static TensorFlowService tfService = new DefaultTensorFlowService();
    private static Context ctx;
    //private static LegacyService legacy;
	static {
		/*
		ClassLoader classLoader = IJ.getClassLoader();
		PluginIndex pluginInd = new PluginIndex(new DefaultPluginFinder(classLoader));
		ctx = new Context(Arrays.<Class<? extends Service>>asList(TensorFlowService.class), pluginInd, false);
		try {
			Class<?> cc = Class.forName("org.scijava.Context", true, classLoader);
			ctx = (Context) cc.newInstance();
			tfService = ctx.service(TensorFlowService.class);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ClassLoader aa = classLoader.getParent();
		LoadJar.loadClasses();
		ClassLoader bb = Thread.currentThread().getContextClassLoader();
		//LoadJar.loadClasses();
		ctx = (Context) IJ.runPlugIn("org.scijava.Context", "");
		*/
		try {
			ctx = (Context) IJ.runPlugIn("org.scijava.Context", "");
			if (ctx == null) ctx = new Context(TensorFlowService.class);
		} catch (Exception ex) {
			//PluginClassLoader classLoader = new PluginClassLoader("C:\\Users\\Carlos(tfg)\\3D Objects\\source\\plugins", true);
			//ClassLoader aa = classLoader.getParent();
			//Thread.currentThread().setContextClassLoader(aa);
			//ClassLoader bb = Thread.currentThread().getContextClassLoader();
			//LoadJar.loadClasses();
			ctx = (Context) IJ.runPlugIn("org.scijava.Context", "");
			//ctx = new Context(Arrays.<Class<? extends Service>>asList(Service.class), null, true);
			//PluginIndex a = ctx.getPluginIndex();
			//tfService.setContext(ctx);
		}
		tfService = ctx.service(TensorFlowService.class);
	
	}

	
	public static String loadLibrary() {
		if (!tfService.getStatus().isLoaded()) {
			tfService.initialize();
			tfService.loadLibrary();
			if (tfService.getStatus().isLoaded()) {
				return tfService.getStatus().getInfo();
			} else {
				IJ.log(tfService.getStatus().getInfo());
				return "";
			}
		}
		return tfService.getStatus().getInfo();
	}
	
	// TODO remove this or the next method
	public static SavedModelBundle load(String path, String tag, Log log) {
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

	public static SavedModelBundle loadModel(String source, String modelTag) {
		// Load the model with its correspondent tag
		SavedModelBundle model;
		try {
			Runtime instance = Runtime.getRuntime();
			double a = instance.freeMemory() / (1024*1024.0);
			model = SavedModelBundle.load(source, modelTag);
			double b = instance.freeMemory() / (1024*1024.0);
			System.out.println(b-a);
		}
		catch (TensorFlowException e) {
			System.out.println("The tag was incorrect");
			model = null;
		}
		return model;
	}

	public static Object[] findTag(String source) {
		// Obtain the model_tag needed to load the model. If none works,
		// 'null' is returned
		Object[] info = checkTags(source, DEFAULT_TAG);
		return info;
	}

	public static Object[] checkTags(String source, String tag) {
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
				Object[] info2 = checkTags(source, MODEL_TAGS[tag_ind + 1]);
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

	public static int[] modelExitDimensions(SignatureDef sig, String entryName) {
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

	public static int[] modelEntryDimensions(SignatureDef sig, String entryName) {
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

	public static String[] returnOutputs(SignatureDef sig) {

		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		Map<String, TensorInfo> out = sig.getOutputsMap();
		Set<String> outputKeys = out.keySet();
		String[] keysArray = outputKeys.toArray(new String[outputKeys.size()]);
		return keysArray;
	}

	public static String[] returnInputs(SignatureDef sig) {

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
	
	public static void showArchitecture(String name, ArrayList<String[]> architecture) {
		JFrame frame = new JFrame("Architecture of " + name);
		ArrayList<CustomizedColumn> columns = new ArrayList<CustomizedColumn>();
		columns.add(new CustomizedColumn("Operation", String.class, 100, false));
		columns.add(new CustomizedColumn("Name", String.class, 100, false));
		columns.add(new CustomizedColumn("Type", String.class, 40, false));
		columns.add(new CustomizedColumn("NumOutputs", String.class, 20, false));
		CustomizedTable arch = new CustomizedTable(columns, true);
		for (String[] archi : architecture)
			arch.append(archi);
		frame.add(arch.getPane(500, 500));
		frame.pack();
		frame.setVisible(true);
	}
	
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
	 * Get the version number from the jar file
	 */
	public static String getTFVersion() {
		TensorFlowVersion tfVersion = tfService.getTensorFlowVersion();
		return tfVersion.getVersionNumber();
	}

}

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
 * E. Gómez-de-Mariscal, C. García-López-de-Haro, L. Donati, M. Unser, A. Muñoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique fédérale de Lausanne (EPFL), Switzerland
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

import java.awt.TextArea;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.TensorFlowException;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;
import org.tensorflow.framework.TensorShapeProto;
import org.tensorflow.framework.TensorShapeProto.Dim;

import com.google.protobuf.InvalidProtocolBufferException;

import deepimagej.tools.FileUtils;
import deepimagej.tools.Index;
import deepimagej.Parameters;
import deepimagej.exceptions.TensorDimensionsException;
import ij.IJ;
import ij.ImagePlus;

public class DeepPlugin {

	private String				path;
	private Log 					log;
	public String				dirname;
	public Parameters			params;
	private boolean				valid;
	public ArrayList<String>		msgChecks		= new ArrayList<String>();
	public ArrayList<String>		msgLoads			= new ArrayList<String>();
	public ArrayList<String[]>	msgArchis		= new ArrayList<String[]>();
	private SavedModelBundle		model			= null;
	public ArrayList<String>		preprocessing	= new ArrayList<String>();
	public ArrayList<String>		postprocessing	= new ArrayList<String>();
	

	
	// Same as the tag used in export_saved_model in the Python code.
	private static final String[] MODEL_TAGS = {"serve", "inference",
											   "train", "eval", "gpu", "tpu"};
	private static final String DEFAULT_TAG = "serve";
	
	
	// TODO Carlos added extra parameter (boolean isDeveloper) for the method
	// 'check'. This is because original TensorFlow models do not need config.xml
	public DeepPlugin(String pathModel, String dirname, Log log, boolean isDeveloper) {
		String p = pathModel + File.separator + dirname + File.separator;
		this.path = p.replace("//", "/");
		this.log = log;
		this.dirname = dirname;
		this.valid = check(isDeveloper);
		this.params = new Parameters(valid, path, isDeveloper);
		preprocessing.add("no preprocessing");
		postprocessing.add("no postprocessing");
	}

	public String getPath() {
		return path;
	}
	
	public String getName() {
		return params.name.equals("n.a.") ? dirname : params.name;
	}
	
	public SavedModelBundle getModel() {
		return model;
	}

	public void setModel(SavedModelBundle model) {
		this.model = model;
	}

	// TODO Carlos created getter to eliminate the need of
	//  another parameter in the class parameters
	public boolean getValid() {
		return this.valid;
	}
	
	
	// TODO parameter added by Carlos, necessary because of teh change in the class constructor
	static public HashMap<String, DeepPlugin> list(String pathModels, Log log, boolean isDeveloper) {
		HashMap<String, DeepPlugin> list = new HashMap<String, DeepPlugin>();
		File models = new File(pathModels);
		File[] dirs = models.listFiles();
		if (dirs == null) {
			return list;
		}

		for (File dir : dirs) {
			if (dir.isDirectory()) {
				String name = dir.getName();
				DeepPlugin dp = new DeepPlugin(pathModels + File.separator, name, log, isDeveloper);
				if (dp.valid) {
					list.put(dp.dirname, dp);
				}
			}
		}
		return list;
	}

	public boolean loadModel() {
		File dir = new File(path);
		String[] files = dir.list();
		log.print("load model from " + path);
		for(String filename : files) {
			if (filename.toLowerCase().startsWith("preprocessing"))
				preprocessing.add(filename);
			if (filename.toLowerCase().startsWith("postprocessing"))
				postprocessing.add(filename);
		}
		
		msgLoads.add("----------------------");
		double chrono = System.nanoTime();
		SavedModelBundle model;
		try {
			model = SavedModelBundle.load(path, params.tag);
			setModel(model);
		}
		catch (Exception e) {
			IJ.log("Exception in loading model " + dirname);
			IJ.log(e.toString());
			IJ.log(e.getMessage());
			log.print("Exception in loading model " + dirname);
			return false;
		}
		chrono = (System.nanoTime() - chrono) / 1000000.0;
		Graph graph = model.graph();
		Iterator<Operation> ops = graph.operations();
		while (ops.hasNext()) {
			Operation op = ops.next();
			if (op != null)
				msgArchis.add(new String[] {op.toString(), op.name(), op.type(), ""+op.numOutputs()});
		}
		log.print("Loaded");
		msgLoads.add("Metagraph size: " + model.metaGraphDef().length);
		msgLoads.add("Graph size: " + model.graph().toGraphDef().length);
		msgLoads.add("Loading time: " + chrono + "ms");
		return true;
	}

	public void writeParameters(TextArea info) {
		if (params == null) {
			info.append("No params\n");
			return;
		}
		info.append(params.name + "\n");
		info.append(params.author + "\n");
		info.append(params.credit + "\n");
		info.append("----------------------\n");
		
		info.append("Tag: " + params.tag + "  Signature: " + params.graph + "\n");

		info.append("Dimensions: ");
		for (int dim : params.inDimensions)
			info.append(" " + dim);
		info.append(" Slices (" + params.slices + ") Channels (" + params.channels + ")\n");

		info.append("Input:");
		for (int i = 0; i < params.nInputs; i++)
			info.append(" " + params.inputs[i] + " (" + params.inputForm[i] + ")");
		info.append("\n");
		info.append("Output:");
		for (int i = 0; i < params.nOutputs; i++)
			info.append(" " + params.outputs[i] + " (" + params.outputForm[i] + ")");
		info.append("\n");
	}

	private boolean check(boolean isDeveloper) {
		msgChecks.add(path);

		File dir = new File(path);
		if (!dir.exists()) {
			msgChecks.add("Not found " + path);
			return false;
		}
		if (!dir.isDirectory()) {
			msgChecks.add("Not found " + path);
			return false;
		}
		boolean valid = true;

		// config.xml
		File configFile = new File(path + "config.xml");
		if (!configFile.exists()) {
			msgChecks.add("No 'config.xml' found in " + path);
			valid = false;
			if (isDeveloper == true) {
				valid = true;
			}
		}

		// saved_model
		File modelFile = new File(path + "saved_model.pb");
		if (!modelFile.exists()) {
			msgChecks.add("No 'saved_model.pb' found in " + path);
			valid = false;
		}

		// variable
		File variableFile = new File(path + "variables");
		if (!variableFile.exists()) {
			msgChecks.add("No 'variables' directory found in " + path);
			valid = false;
		}
		else {
			msgChecks.add("TensorFlow model " + FileUtils.getFolderSizeKb(path + "variables"));
		}
		return valid;
	}
	
	public String getInfoImage(String filename) {
		if (path == null)
			return "No image";
		File file = new File(filename);
		if (!file.exists()) 
			return "No image";
		ImagePlus imp = IJ.openImage(filename);
		if (imp == null) 
			return "Error image: " + filename;
		String name = file.getName();
		
		String nx = "" + imp.getWidth();
		String ny = "x" + imp.getHeight();
		String nz = imp.getNSlices() == 1 ? "" : "x" + imp.getNSlices();
		String nc = imp.getNChannels() == 1 ? "" : "x" + imp.getNChannels();
		String nt = imp.getNFrames() == 1 ? "" : "x" + imp.getNFrames();
		int depth = imp.getBitDepth();
		return name +" (" + nx + ny + nz + nc + nt + ") " + depth + "bits";
	}
	
	public String getInfoMacro(String filename) {
		if (path == null)
			return null;
		File file = new File(filename);
		if (!file.exists()) 
			return null;
		String name = file.getName();
		
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filename));
			int lines = 0;
			while (reader.readLine() != null) lines++;
			reader.close();
			return name +" (" + lines + " lines) ";
		}
		catch (Exception e) {
			return "Error";
		}
	}

///////////////////////////////////////////////////////////////////////////
	
// Auxiliary methods to find the characteristics of the model

	public static SavedModelBundle loadModel(String source, String modelTag){
		// Load the model with its correspondent tag
		SavedModelBundle model;
		try{
			model = SavedModelBundle.load(source, modelTag);
		} catch(TensorFlowException e) {
			System.out.println("The tag was incorrect");
			model = null;
		}
		return model;
	}
	
	public static Object[] checkModelCanLoad(String source){
		// Obtain the model_tag needed to load the model. If none works,
		// 'null' is returned
		Object[] info = checkTags(source, DEFAULT_TAG);
		return info;
		}
	
	public static Object[] checkTags(String source, String tag){
		SavedModelBundle model = null;
		Set<String> sigKeys;
		Object[] info = new Object[3];
		try{
			model = SavedModelBundle.load(source, tag);
			sigKeys = metaGraphsSet(model);
		} catch(TensorFlowException e) {
			// If the tag does not work, try with the following existing tag
			int tag_ind = Index.indexOf(MODEL_TAGS, tag);
			if (tag_ind < MODEL_TAGS.length - 1) {
				Object[] info2 = checkTags(source, MODEL_TAGS[tag_ind + 1]);
				tag = (String) info2[0]; sigKeys = (Set<String>) info2[1];
			} else {
				// 	tag = null, the user will need to introduce it
				tag = null;
				sigKeys = null;
			}
		}
		info[0] = tag; info[1] = sigKeys; info[2] = model;
		return info;
	}
	
	
	public static Set<String> metaGraphsSet(SavedModelBundle model){
		byte[] byteGraph = model.metaGraphDef();
		// Obtain a mapping between the possible keys and their signature definitions	
		Map<String, SignatureDef> sig = null;
		try {
			sig = MetaGraphDef.parseFrom(byteGraph).getSignatureDefMap();
		} catch (InvalidProtocolBufferException e) {
			System.out.println("The model is not a correct SavedModel model");
		}
		Set<String> modelKeys = sig.keySet();
		return modelKeys;
	}
		
	public static SignatureDef graph2SigDef(SavedModelBundle model, String key){
		byte[] byteGraph = model.metaGraphDef();
		
		SignatureDef sig = null;
		try {
			sig = MetaGraphDef.parseFrom(byteGraph).getSignatureDefOrThrow(key);
		} catch (InvalidProtocolBufferException e) {
			System.out.println("Invalid graph");
		}
		return sig;
	}
	
	
	public static Object[] retrieveInputOutputDims(SavedModelBundle model, String sigDef) throws TensorDimensionsException {
		// Retrieve the output tensor dimensions from the beginning
		int[] outDims = null;
		int[] inDims = null;
		SignatureDef sig = graph2SigDef(model, sigDef);
		String[] outNames = returnOutputs(sig);
		if (outNames.length == 1) {
			outDims = modelExitDimensions(sig, outNames[0]);
		}
		String[] inNames = returnInputs(sig);
		if (inNames.length == 1) {
			inDims = modelEntryDimensions(sig, inNames[0]);
		}
		
		if (inDims.length > 4) {
			throw new TensorDimensionsException();
		}
		Object[] dims = {inDims, outDims, inNames, outNames};
		return dims;
	}


	private static int[] modelExitDimensions(SignatureDef sig, String entryName) {
		// This method returns the dimensions of the tensor defined by 
		// the saved model. The method retrieves the tensor info and
		//converts it into an array of integers.
		TensorInfo entryInfo = sig.getOutputsOrThrow(entryName);
		TensorShapeProto entryShape = entryInfo.getTensorShape();
		List<Dim> listDim = entryShape.getDimList();
		int rank = listDim.size();
		int[] inputTensorSize = new int[rank];
		
		for (int i = 0; i < rank; i++) {
			inputTensorSize[i] = (int)listDim.get(i).getSize();
		}
		return inputTensorSize;
	}
	
	
	private static int[] modelEntryDimensions(SignatureDef sig, String entryName){
		// This method returns the dimensions of the tensor defined by 
		// the saved model. The method retrieves the tensor info and
		//converts it into an array of integers.
		TensorInfo entryInfo = sig.getInputsOrThrow(entryName);
		TensorShapeProto entryShape = entryInfo.getTensorShape();
		List<Dim> listDim = entryShape.getDimList();
		int rank = listDim.size();
		int[] inputTensorSize = new int[rank];
		
		for (int i = 0; i < rank; i++) {
			inputTensorSize[i] = (int)listDim.get(i).getSize();
		}
		
		return inputTensorSize;
	}
	
	
	public static String[] returnOutputs(SignatureDef sig){
	
		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		Map<String, TensorInfo> out =sig.getOutputsMap();
		Set<String> outputKeys  = out.keySet();
		String[] keysArray = outputKeys.toArray(new String[outputKeys.size()]);
		return keysArray;
		}
	
	public static String[] returnInputs(SignatureDef sig){
	
		// Extract names from the model signature.
		// The strings "input", "probabilities" and "patches" are meant to be
		// in sync with the model exporter (export_saved_model()) in Python.
		Map<String, TensorInfo> inp =sig.getInputsMap();
		Set<String> inputKeys  = inp.keySet();
		String[] keysArray = inputKeys.toArray(new String[inputKeys.size()]);
		return keysArray;
	}
	
	public static String nChannels(Parameters params, String inputForm) {
		// Find the number of channels in the input
		String nChannels;
		int ind = Index.indexOf(inputForm.split(""), "C");
		if (ind == -1) {
			nChannels = "1";
		} else {
			nChannels = Integer.toString(params.inDimensions[ind]);
		}
	return nChannels;
	}


}


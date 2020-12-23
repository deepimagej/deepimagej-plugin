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

package deepimagej.stamp;
import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.framework.SignatureDef;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.TensorFlowModel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.DijTensor;
import deepimagej.tools.FileTools;
import deepimagej.tools.Log;
import deepimagej.tools.SystemUsage;
import ij.IJ;

public class LoadTFStamp extends AbstractStamp implements Runnable {

	private ArrayList<String>	tags;
	private JComboBox<String>	cmbTags			= new JComboBox<String>();
	private JComboBox<String>	cmbGraphs		= new JComboBox<String>();
	//private ArrayList<String[]>	architecture	= new ArrayList<String[]>();
	private String				name;

	private HTMLPane			pnLoad;
	

	public LoadTFStamp(BuildDialog parent) {
		super(parent);
		tags = new ArrayList<String>();
		tags.add("Serve");
		buildPanel();
	}

	@Override
	public void buildPanel() {
		pnLoad = new HTMLPane(Constants.width, 70);

		HTMLPane pnTag = new HTMLPane(Constants.width / 2, 70);
		pnTag.append("h2", "Model Tag");
		pnTag.append("p", "Tag used to save the TensorFlow SavedModel. If the plugin cannot automatically find it, you will need to edit it.");

		HTMLPane pnGraph = new HTMLPane(2 * Constants.width / 2, 70);
		pnGraph.append("h2", "SignatureDef");
		pnGraph.append("p", "SignatureDef used to call the wanted model graph. There might be more than one in the same model folder.");

		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(pnTag.getPane());
		pn.add(cmbTags);
		pn.add(pnGraph.getPane());
		pn.add(cmbGraphs);
		JPanel main = new JPanel(new BorderLayout());
		main.add(pnLoad.getPane(), BorderLayout.CENTER);
		main.add(pn, BorderLayout.SOUTH);
		panel.add(main);
	}

	@Override
	public void init() {
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	
	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		if (params.tag == null) {
			Log log = new Log();
			String tag = (String)cmbTags.getSelectedItem();
			try {	
				double time = System.nanoTime();
				SavedModelBundle model = TensorFlowModel.load(params.path2Model, tag, log);
				time = System.nanoTime() - time;
				addLoadInfo(params, time);
				parent.getDeepPlugin().setTfModel(model);
				params.tag = tag;
				cmbTags.setEditable(false);
				parent.getDeepPlugin().setTfModel(model);
				params.graphSet = TensorFlowModel.metaGraphsSet(model);
				if (params.graphSet.size() > 0) {
					Set<String> tfGraphSet = TensorFlowModel.returnTfSig(params.graphSet);
					cmbGraphs.removeAllItems();
					for (int i = 0; i < params.graphSet.size(); i++) {
						cmbGraphs.addItem((String) tfGraphSet.toArray()[i]);
						cmbGraphs.setEditable(false);
					}
				}
				
			}
			catch (Exception e) {
				IJ.error("Incorrect ModelTag");
				params.tag = null;
				cmbTags.removeAllItems();
				cmbTags.setEditable(true);
			}
			return false;
		} else {
			// TODO put it inside run
			SavedModelBundle model = parent.getDeepPlugin().getTfModel();
			params.graph = TensorFlowModel.returnStringSig((String)cmbGraphs.getSelectedItem());
			SignatureDef sig = TensorFlowModel.getSignatureFromGraph(model, params.graph);
			params.totalInputList = new ArrayList<>();
			params.totalOutputList = new ArrayList<>();
			String[] inputs = TensorFlowModel.returnInputs(sig);
			String[] outputs = TensorFlowModel.returnOutputs(sig);
			pnLoad.append("p", "Number of outputs: " + outputs.length);
			boolean valid = true;
			try {
				for (int i = 0; i < inputs.length; i ++) {
					DijTensor inp = new DijTensor(inputs[i]);
					inp.setInDimensions(TensorFlowModel.modelEntryDimensions(sig, inputs[i]));
					params.totalInputList.add(inp);
				}
				for (int i = 0; i < outputs.length; i ++) {
					DijTensor out = new DijTensor(outputs[i]);
					out.setInDimensions(TensorFlowModel.modelExitDimensions(sig, outputs[i]));
					params.totalOutputList.add(out);
				}
				// TODO correct it for adecuate number of inputs and outputs
				//pnLoad.append("p", "Dimension of input: " + params.inDimensions.length + " and output: " + params.outDimensions.length);
	
			}
			catch (Exception ex) {
				pnLoad.append("p", "Dimension: ERROR");
				valid  = false;
				parent.setEnabledBackNext(valid);
				return false;
			}
			parent.setEnabledBackNext(valid);
			return true;
		}
	}

	// TODO separate in methods
	public void run() {
		parent.setEnabledBack(false);
		parent.setEnabledNext(false);
		pnLoad.setCaretPosition(0);
		pnLoad.setText("");
		pnLoad.append("p", "Loading available Tensorflow version.");
		String loadInfo = TensorFlowModel.loadLibrary();
		// If loadlLibrary() returns 'ImageJ', the plugin is running
		// on an ImageJ1 instance
		parent.setFiji(!loadInfo.contains("ImageJ"));
		pnLoad.setCaretPosition(0);
		pnLoad.setText("");
		if (loadInfo.equals("")) {
			pnLoad.append("p", "Unable to find any Tensorflow distribution.");
			pnLoad.append("p", "Please, install a valid Tensorflow version.");
			parent.setEnabledBack(true);
			return;
		}

		Parameters params = parent.getDeepPlugin().params;
		cmbTags.removeAllItems();
		cmbGraphs.removeAllItems();
		//architecture.clear();
		String tfVersion = TensorFlowModel.getTFVersion(parent.getFiji());
		pnLoad.clear();
		pnLoad.append("h2", "Tensorflow version");
		if (loadInfo.toLowerCase().contains("gpu"))
			tfVersion += "_GPU";
		pnLoad.append("p", "Currently using Tensorflow " + tfVersion);
		if (parent.getFiji()) {
			pnLoad.append("p", loadInfo);
		} else {
			pnLoad.append("p", "To change the Tensorflow version, download the corresponding\n"
							 + "libtensorflow and libtensorflow_jni jars and copy them into\n"
							 + "the plugins folder.");
		}
		// Run the nvidia-smi to see if it is possible to locate a GPU
		String cudaVersion = "";
		if (tfVersion.contains("GPU"))
			cudaVersion = SystemUsage.getCUDAEnvVariables();
		else
			parent.setGPU("CPU");
		// If a CUDA distribution was found, cudaVersion will be equal
		// to the CUDA version. If not it can be either 'noCuda', if CUDA 
		// is not installed, or if there is a CUDA_PATH in the environment variables
		// but the needed variables are not in the PATH, it will return the missing 
		// environment variables
		if (tfVersion.contains("GPU") && !cudaVersion.contains(File.separator)) {
			pnLoad.append("p", "Currently using CUDA " + cudaVersion);
			pnLoad.append("p", TensorFlowModel.TensorflowCUDACompatibility(tfVersion, cudaVersion));
		} else if (tfVersion.contains("GPU") && (cudaVersion.contains("bin") || cudaVersion.contains("libnvvp"))) {
			pnLoad.append("p", TensorFlowModel.TensorflowCUDACompatibility(tfVersion, cudaVersion));
			String[] outputs = cudaVersion.split(";");
			pnLoad.append("p", "Found CUDA distribution " + outputs[0] + ".\n");
			pnLoad.append("p", "Could not find environment variable:\n - " + outputs[1] + "\n");
			if (outputs.length == 3)
				pnLoad.append("p", "Could not find environment variable:\n - " + outputs[2] + "\n");
			pnLoad.append("p", "Please add the missing environment variables to the path.\n");
		} else if (tfVersion.contains("GPU") && cudaVersion.equals("noCUDA")) {
			pnLoad.append("p", "No CUDA distribution found.\n");
			parent.setGPU("CPU");
		}
		pnLoad.append("h2", "Model info");
		File file = new File(params.path2Model);
		if (file.exists())
			name = file.getName();

		pnLoad.append("h2", "Load " + name);

		String pnTxt = pnLoad.getText();
		Log log = new Log();
		params.tag = null;
		
		// Block back button while loading
		parent.setEnabledBackNext(false);
		Object[] info = null;
		double time = -1;
		pnLoad.append("<p>Loading model...");
		ArrayList<String> initialSmi = null;
		ArrayList<String> finalSmi = null;
		try {
			if (tfVersion.contains("GPU") && parent.getGPU().equals(""))
				initialSmi = SystemUsage.runNvidiaSmi();
			double chrono = System.nanoTime();
			info = TensorFlowModel.findTag(params.path2Model);
			time = System.nanoTime() - chrono;
			if (tfVersion.contains("GPU") && parent.getGPU().equals(""))
				finalSmi = SystemUsage.runNvidiaSmi();
		} catch (Exception ex) {
			pnLoad.clear();
			pnLoad.setText(pnTxt);
			ex.printStackTrace();
			IJ.error("DeepImageJ could not load the model,"
					+ "try with another Tensorflow version");
			pnLoad.append("h2", "DeepImageJ could not load the model.\n");
			pnLoad.append("h2", "Try with another Tensorflow version.\n");
			// Let the developer go back, but no forward
			parent.setEnabledBack(true);
			parent.setEnabledNext(false);
			return;
		}
		pnLoad.append(" -> Loaded!!!</p>");
		
		// Check if the model has been loaded on GPU
		if (tfVersion.contains("GPU") && !parent.getGPU().equals("GPU")) {
			String GPUInfo = SystemUsage.isUsingGPU(initialSmi, finalSmi);
			if (GPUInfo.equals("noImageJProcess") && !cudaVersion.contains(File.separator)) {
				pnLoad.append("p", "Unable to run nvidia-smi to check if the model was loaded on a GPU.\n");
				parent.setGPU("???");
			} else if (GPUInfo.equals("noImageJProcess")) {
				pnLoad.append("p", "Unable to load model on GPU.\n");
				parent.setGPU("CPU");
			} else if(GPUInfo.equals("¡RepeatedImageJGPU!")) {
				int nImageJInstances = SystemUsage.numberOfImageJInstances();
				// Get number of IJ instances using GPU
				int nGPUIJInstances = GPUInfo.split("¡RepeatedImageJGPU!").length;
				if (nImageJInstances > nGPUIJInstances) {
					pnLoad.append("p", "Found " + nGPUIJInstances + "instances of ImageJ/Fiji using GPU"
							+ " out of the " + nImageJInstances + " opened.\n");
					pnLoad.append("p", "Could not assert that the model was loaded on the <b>GPU</b>.\n");
					parent.setGPU("???");
				} else if (nImageJInstances <= nGPUIJInstances) {
					pnLoad.append("p", "Model loaded on the <b>GPU</b>.\n");
					if (cudaVersion.contains("bin") || cudaVersion.contains("libnvvp"))
						pnLoad.append("p", "Note that with missing environment variables, GPU performance might not be optimal.\n");
					parent.setGPU("GPU");
				}
			} else {
				pnLoad.append("p", "Model loaded on the <b>GPU</b>.\n");
				if (cudaVersion.contains("bin") || cudaVersion.contains("libnvvp"))
					pnLoad.append("p", "Note that due to missing environment variables, GPU performance might not be optimal.\n");
				parent.setGPU("GPU");
			}
		} else if (tfVersion.contains("GPU")) {
			pnLoad.append("p", "Model loaded on the <b>GPU</b>.\n");
			if (cudaVersion.contains("bin") || cudaVersion.contains("libnvvp"))
				pnLoad.append("p", "Note that due to missing environment variables, GPU performance might not be optimal.\n");
		}
		
		String tag = (String) info[0];
		if (tag != null) {
			params.tag = tag;
			String tfTag = TensorFlowModel.returnTfTag(tag);
			cmbTags.addItem(tfTag);
			cmbTags.setEditable(false);
			SavedModelBundle model = null;
			if (!(info[2] instanceof SavedModelBundle)) {
				model = TensorFlowModel.load(params.path2Model, params.tag, log);
			} else {
				model = (SavedModelBundle) info[2];
				addLoadInfo(params, time);
			}
			parent.getDeepPlugin().setTfModel(model);
			try {
				params.graphSet = TensorFlowModel.metaGraphsSet(model);
			} catch (Exception ex) {
				ex.printStackTrace();
				IJ.error("DeepImageJ could not load the model,\n"
						+ "try with another Tensorflow version");
				pnLoad.append("h2", "DeepImageJ could not load the model.\n");
				pnLoad.append("h2", "Try with another Tensorflow version.\n");
				// Let the developer go back, but no forward
				parent.setEnabledBack(true);
				parent.setEnabledNext(false);
				return;
			}
			if (params.graphSet.size() > 0) {
				Set<String> tfGraphSet = TensorFlowModel.returnTfSig(params.graphSet);
				for (int i = 0; i < params.graphSet.size(); i++) {
					cmbGraphs.addItem((String) tfGraphSet.toArray()[i]);
					cmbGraphs.setEditable(false);
				}
			}
		} else {
			cmbTags.addItem("");
			cmbTags.setEditable(true);
			cmbGraphs.addItem("");
			cmbGraphs.setEditable(false);
			pnLoad.append("p", "The plugin could not load the model automatically,<br>"
					+ "please introduce the needed information to load the model.");
		}
		// If we loaded either a Bioimage Zoo or Tensoflow model we continue
		parent.setEnabledBackNext(true);
	}

	/*
	 * Add load information to the panel
	 */
	private void addLoadInfo(Parameters params, double time) {
		pnLoad.append("p", "Path to model: " + params.path2Model + "\n");
		String timeStr = (time / 1000000000) + "";
		timeStr = timeStr.substring(0, timeStr.lastIndexOf(".") + 3);
		pnLoad.append("p", "Time to load model: " + timeStr + "\n");
		String modelSize = "" + FileTools.getFolderSize(params.path2Model + File.separator + "variables") / (1024*1024.0);
		modelSize = modelSize.substring(0, modelSize.lastIndexOf(".") + 3);
		pnLoad.append("p", "Size of the weights: " + modelSize + " MB");
		
	} 
}

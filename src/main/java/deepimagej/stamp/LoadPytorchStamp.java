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
import java.awt.TextField;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.framework.SignatureDef;

import ai.djl.MalformedModelException;
import ai.djl.engine.EngineException;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.TensorFlowModel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.DijTensor;
import deepimagej.tools.FileTools;
import deepimagej.tools.Log;
import ij.IJ;
import ij.gui.GenericDialog;

public class LoadPytorchStamp extends AbstractStamp implements Runnable {

	private String				name;
	private JTextField			inpNumber = new JTextField();
	private JTextField			outNumber = new JTextField();

	private HTMLPane			pnLoad;
	

	public LoadPytorchStamp(BuildDialog parent) {
		// TODO review messages
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {
		pnLoad = new HTMLPane(Constants.width, 70);

		HTMLPane pnTag = new HTMLPane(Constants.width / 2, 70);
		pnTag.append("h2", "Number of inputs");
		pnTag.append("p", "Number of inputs to the Pytorch model");

		HTMLPane pnGraph = new HTMLPane(2 * Constants.width / 2, 70);
		pnGraph.append("h2", "Number of outputs");
		pnGraph.append("p", "Number of outputs of the Pytorch model.");

		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(pnTag.getPane());
		pn.add(inpNumber);
		inpNumber.setText("0");
		inpNumber.setEnabled(false);
		pn.add(pnGraph.getPane());
		pn.add(outNumber);
		outNumber.setText("0");
		outNumber.setEnabled(false);
		
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
		params.totalInputList = new ArrayList<DijTensor>();
		params.totalOutputList = new ArrayList<DijTensor>();
		boolean inp = false;
		try {
			int nInp = Integer.parseInt(inpNumber.getText().trim());
			inp = true;
			int nOut = Integer.parseInt(outNumber.getText().trim());
			if (nOut < 1) {
				IJ.error("The number of outputs shoud be 1 or bigger");
				return false;
			} else if (nInp < 1) {
				IJ.error("The number of inputs shoud be 1 or bigger");
				return false;
			}
			for (int i = 0; i < nInp; i ++) {
				// TODO when possible add dimensions from model
				DijTensor inpT = new DijTensor("input" + i);
				params.totalInputList.add(inpT);
			}
			for (int i = 0; i < nOut; i ++) {
				// TODO when possible add dimensions from model
				DijTensor outT = new DijTensor("output" + i);
				params.totalOutputList.add(outT);
			}
			return true;
			
		} catch (Exception ex) {
			if (!inp) {
				IJ.error("Please introduce a valid integer for the number of inputs.");
			} else if (inp) {
				IJ.error("Please introduce a valid integer for the number of outputs.");
			}
			return false;
		}
	}

	public void run() {
		pnLoad.setCaretPosition(0);
		pnLoad.setText("");
		pnLoad.append("p", "Loading Deep Java Library...");
		
		Parameters params = parent.getDeepPlugin().params;
		params.selectedModelPath = findPytorchModels(params.path2Model);
		pnLoad.clear();
		pnLoad.append("h2", "Pytorch version");
		pnLoad.append("p", "Currently using Pytorch 1.6.0");
		pnLoad.append("p", "Supported by Deep Java Library 1.6.0");
		pnLoad.append("h2", "Model info");
		pnLoad.append("p", "Path: " + params.selectedModelPath);
		pnLoad.append("<p>Loading model...");
		
		// Load the model using DJL
		// TODO allow the use of translators and transforms
		URL url;
		// Block back button while loading
		parent.setEnabledBackNext(false);
		try {
			url = new File(new File(params.path2Model).getAbsolutePath()).toURI().toURL();
			
			if (params.selectedModelPath.equals("")) {
				pnLoad.append("No Pytorch model found in the directory.");
				parent.setEnabledBack(true);
			}
			String modelName = new File(params.selectedModelPath).getName();
			modelName = modelName.substring(0, modelName.indexOf(".pt"));
			long startTime = System.nanoTime();
			Criteria<NDList, NDList> criteria = Criteria.builder()
			        .setTypes(NDList.class, NDList.class)
			         // only search the model in local directory
			         // "ai.djl.localmodelzoo:{name of the model}"
			        .optModelUrls(url.toString()) // search models in specified path
			        //.optArtifactId("ai.djl.localmodelzoo:resnet_18") // defines which model to load
			        .optModelName(modelName)
			        .optProgress(new ProgressBar()).build();
			/*Criteria<NDList, NDList> criteria = Criteria.builder()
			        .setTypes(NDList.class, NDList.class)
			        .optModelUrls(url.toString()) // search models in specified path
			        .optArtifactId("ai.djl.localmodelzoo:" + modelName) // defines which model to load
			        .build();*/

			ZooModel<NDList, NDList> model = ModelZoo.loadModel(criteria);
			String defaultEngine = Engine.getInstance().getEngineName();
			parent.getDeepPlugin().setTorchModel(model);
			pnLoad.append(" -> Loaded!!!</p>");
			double torchscriptSize = new File(params.selectedModelPath).length() / (1024 * 1024.0);
			long stopTime = System.nanoTime();
			// Convert nanoseconds into seconds
			String loadingTime = "" + ((stopTime - startTime) / (float) 1000000000);
			pnLoad.append("p", "Model size: " + torchscriptSize + " Mb");
			pnLoad.append("p", "Loading time: " + loadingTime +  " s");
			
			parent.setEnabledBackNext(true);
			inpNumber.setEnabled(true);
			outNumber.setEnabled(true);
		} catch (MalformedURLException e) {
			pnLoad.append("p", "DeepImageJ could not load the model");
			pnLoad.append("p", "Check that the path provided to the model remains the same.");
			parent.setEnabledBack(true);
			e.printStackTrace();
		} catch (EngineException e) {
			String err = e.getMessage();
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win") && err.contains("https://github.com/awslabs/djl/blob/master/docs/development/troubleshooting.md")) {
				pnLoad.append("p", "DeepImageJ could not load the model");
				pnLoad.append("p", "Please install the Visual Studio 2019 redistributables and reboot\n"
							+ "your machine to be able to use Pytorch with DeepImageJ.");
				pnLoad.append("p", "For more information:\n");
				pnLoad.append("p", " - https://github.com/awslabs/djl/blob/master/docs/development/troubleshooting.md");
				pnLoad.append("p", " - https://github.com/awslabs/djl/issues/126");
				pnLoad.append("p", "If you already have installed VS2019 redistributables, the error\n"
								+ "might be caused by a non-compatible CUDA version installed.\n"
								+ "DJL only supports CUDA 10.1 and CUDA 10.2.");
				// TODO complete
				pnLoad.append("p", "It seems that you might have installed CUDA ");
				pnLoad.append("p", "If you want to use a GPU, please uninstall the non-compatible\n"
									+ "CUDA versions and install either CUDA 10.1 or CUDA 10.2");
				pnLoad.append("p", "If you do not need to use a GPU, just remove from the 'PATH'\n"
								+ "environment variable all the directories containing the word NVIDIA.");
				pnLoad.append("p", "NOTE that removing those variables from the path might make other\n"
								+ "programs using CUDA stop working.");
			} else if(err.contains("https://github.com/awslabs/djl/blob/master/docs/development/troubleshooting.md")){
				pnLoad.append("p", "DeepImageJ could not load the model");
				pnLoad.append("p", "The problem might be caused by incompatible CUDA versions installed.");
				pnLoad.append("p", "Deep Java Library is only compatible with CUDA 10.1 and CUDA 10.2.");
			}else {
				pnLoad.append("p", "DeepImageJ could not load the model");
				pnLoad.append("p", "It seems that the Torchscript model was created with Pytorch>1.6.0.");
				pnLoad.append("p", "Currently DeepImageJ only supports models created with Pytorch<=1.6.0.");
			}
			parent.setEnabledBack(true);
			e.printStackTrace();
		} catch (ModelNotFoundException e) {
			pnLoad.append("p", "DeepImageJ could not load the model");
			pnLoad.append("p", "No model was found in the path provided.");
			parent.setEnabledBack(true);
			e.printStackTrace();
		} catch (MalformedModelException e) {
			pnLoad.append("p", "DeepImageJ could not load the model");
			pnLoad.append("p", "The model provided is not a correct Torchscript model.");
			parent.setEnabledBack(true);
			e.printStackTrace();
		} catch (IOException e) {
			pnLoad.append("p", "DeepImageJ could not load the model");
			pnLoad.append("p", "Error whie accessing the model file.");
			parent.setEnabledBack(true);
			e.printStackTrace();
		} catch (Exception e) {
			pnLoad.append("p", "DeepImageJ could not load the model");
			pnLoad.append("p", "Error whie accessing the model file.");
			parent.setEnabledBack(true);
			e.printStackTrace();
		}
	}

	/*
	 * Find the Pytorch model (".pt" or ".pth") inside the folder provided.
	 * If there are more than one model, make the user decide.
	 */
	private String findPytorchModels(String modelPath) {
		
		File[] folderFiles = new File(modelPath).listFiles();
		ArrayList<File> ptModels = new ArrayList<File>();
		for (File file : folderFiles) {
			if (file.getName().contains(".pt"))
				ptModels.add(file);
		}
		
		if (ptModels.size() == 1) 
			return ptModels.get(0).getAbsolutePath();
		
		GenericDialog dlg = new GenericDialog("Choose Pytorch model");
		dlg.addMessage("The folder provided contained several Pytorch models");
		dlg.addMessage("Select which do you want to load.");
		String[] fileArray = new String[ptModels.size()];
		int c = 0;
		for (File f : ptModels)
			fileArray[c ++] = f.getName();
		dlg.addChoice("Select framework", fileArray, fileArray[0]);
		dlg.showDialog();
		if (dlg.wasCanceled()) {
			dlg.dispose();
			return "";
		}
		return modelPath + File.separator + dlg.getNextChoice();
	} 
}

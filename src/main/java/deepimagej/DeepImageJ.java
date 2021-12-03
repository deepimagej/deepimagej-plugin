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

import java.awt.TextArea;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.tensorflow.SavedModelBundle;

import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import deepimagej.tools.DijTensor;
import deepimagej.tools.FileTools;
import ij.IJ;
import ij.gui.GenericDialog;

public class DeepImageJ {

	private String					path;
	public String					dirname;
	public Parameters				params;
	private boolean					valid 			= false;
	// Specifies if the yaml is present in the model folder
	public boolean					presentYaml		= true;
	private boolean					developer		= true;
	private SavedModelBundle		tfModel			= null;
	private ZooModel<NDList, NDList>torchModel		= null;
	public String ptName = "pytorch_script.pt";
	public String tfName = "tensorflow_saved_model_bundle.zip";
	
	public DeepImageJ(String pathModel, String dirname, boolean dev) {
		String p = pathModel + File.separator + dirname + File.separator;
		this.path = p.replace(File.separator + File.separator, File.separator);
		// Remove double File separators
		this.path = cleanPathStr(p);
		this.dirname = dirname;
		this.developer = dev;
		if (!dev && !(new File(path, "model.yaml").isFile() || new File(path, "rdf.yaml").isFile())) {
			this.presentYaml = false;
			this.params = new Parameters(valid, path, dev);
			this.valid = check(p);
		} else if (dev || new File(path, "model.yaml").isFile() || new File(path, "rdf.yaml").isFile()) {
			this.params = new Parameters(valid, path, dev);
			this.params.path2Model = this.path;
			this.valid = check(p);
		}
		if (this.valid && dev && this.params.framework.equals("tensorflow/pytorch")) {
			askFrameworkGUI();
		}
	}

	/*
	 * Method that substitutes double path separators ('\\' or '//') by single ones
	 */
	public static String cleanPathStr(String p) {
		while (p.indexOf(File.separator + File.separator) != -1) {
			p = p.replace(File.separator + File.separator, File.separator);
		}
		return p;
	}

	public String getPath() {
		return path;
	}
	
	public String getName() {
		String name = params.name.equals("n.a.") ? dirname : params.name;
		return name.replace("\"", "");
	}
	
	public ZooModel<NDList, NDList> getTorchModel() {
		return torchModel;
	}

	public void setTorchModel(ZooModel<NDList, NDList> model) {
		this.torchModel = model;
	}
	
	public SavedModelBundle getTfModel() {
		return tfModel;
	}

	public void setTfModel(SavedModelBundle model) {
		this.tfModel = model;
	}

	public boolean getValid() {
		return this.valid;
	}
	
	static public HashMap<String, DeepImageJ> list(String pathModels, boolean isDeveloper, TextArea textField) {
		HashMap<String, DeepImageJ> list = new HashMap<String, DeepImageJ>();
		File models = new File(pathModels);
		File[] dirs = models.listFiles();
		if (dirs == null) {
			String err = "No models found at: " + System.lineSeparator() + " - " + pathModels;
			System.out.println("[DEBUG] " + err);
			IJ.log(err);
			return list;
		}

		// FOrmat for the date
		Date now = new Date(); 
		for (File dir : dirs) {
			if (dir.isDirectory()) {
				String name = dir.getName();
				if (textField != null)
					textField.append(" - " + new SimpleDateFormat("HH:mm:ss").format(now) + " -- Looking for a model at: " + name + "\n");
				DeepImageJ dp = new DeepImageJ(pathModels + File.separator, name, isDeveloper);
				if (dp.valid && dp.params != null) {
					list.put(dp.dirname, dp);
				}
			}
		}
		return list;
	}


	public boolean loadTfModel(boolean archi) {

		double chrono = System.nanoTime();
		SavedModelBundle model;
		try {
			model = SavedModelBundle.load(path, DeepLearningModel.returnStringTag(params.tag));
			setTfModel(model);
		}
		catch (Exception e) {
			IJ.log("Exception in loading model " + dirname);
			IJ.log(e.toString());
			IJ.log(e.getMessage());
			return false;
		}
		chrono = (System.nanoTime() - chrono) / 1000000.0;
		return true;
	}


	public boolean loadPtModel(String path, boolean isFiji) {
		try {
			if (!isFiji)
				Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
			URL url = new File(new File(path).getParent()).toURI().toURL();
			
			String modelName = new File(path).getName();
			modelName = modelName.substring(0, modelName.indexOf(".pt"));
			Criteria<NDList, NDList> criteria = Criteria.builder()
			        .setTypes(NDList.class, NDList.class)
			         // only search the model in local directory
			         // "ai.djl.localmodelzoo:{name of the model}"
			        .optModelUrls(url.toString()) // search models in specified path
			        //.optArtifactId("ai.djl.localmodelzoo:resnet_18") // defines which model to load
			        .optModelName(modelName)
			        .optProgress(new ProgressBar()).build();
	
			ZooModel<NDList, NDList> model = ModelZoo.loadModel(criteria);
			this.setTorchModel(model);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (ModelNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (MalformedModelException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			IJ.log("Model not found in the path provided:");
			IJ.log(path);
			e.printStackTrace();
			return false;
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			IJ.log("DeepImageJ could not load the Pytorch model.");
			IJ.log("This is probably because the Visual Studio 2019 Redistributables are missing.");
			IJ.log("In order to be able to load Pytorch models, download Visual Studio 2019 and ");
			IJ.log("its redistributables from teh following links.");
			IJ.log("- https://visualstudio.microsoft.com/es/downloads/");
			IJ.log("- https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads");
			IJ.log("If the problem persists visit the following link for more info:");
			IJ.log("- http://docs.djl.ai/docs/development/troubleshooting.html#13-unsatisfiedlinkerror-issue");
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void writeParameters(TextArea info) {
		if (params == null) {
			info.append("No params\n");
			return;
		}
		// If there is any external dependency, add it here
		if (params.ptAttachmentsNotIncluded.size() != 0) {
			info.append("----------- ATTENTION -----------\n");
			info.append("To use the Pytorch format, please make sure that\n"
					+ "the following plugins/jars are installed:\n");
			for (String str : params.ptAttachmentsNotIncluded)
				info.append(" - " + str + "\n");
		}
		// If there is any external dependency, add it here
		if (params.tfAttachmentsNotIncluded.size() != 0) {
			info.append("----------- ATTENTION -----------\n");
			info.append("To use the Tensorflow format, please make sure that\n"
					+ "the following plugins/jars are installed:\n");
			for (String str : params.tfAttachmentsNotIncluded)
				info.append(" - " + str + "\n");
		}
		info.append("---------- MODEL INFO ----------\n");
		info.append("Authors" + "\n");
		for (HashMap<String, String> auth : params.author) {
			String name = auth.get("name") == null ? "n/a" : auth.get("name");
			String aff = auth.get("affiliation") == null ? "n/a" : auth.get("affiliation");
			String orcid = auth.get("orcid") == null ? "n/a" : auth.get("orcid");
			info.append("  - Name: " + name + "\n");
			info.append("    Affiliation: " + aff + "\n");
			info.append("    Orcid: " + orcid + "\n");
		}
		info.append("References" + "\n");
		for (HashMap<String, String> ref : params.cite) {
			info.append("  - Article: " + ref.get("text") + "\n");
			info.append("    Doi: " + ref.get("doi") + "\n");
		}
		info.append("Framework: " + params.framework + "\n");
		
		if (params.framework.contains("tensorflow")) {
			info.append("Tag: " + params.tag + "\n");
			info.append("Signature: " + params.graph + "\n");
		}
		info.append("Allow tiling: " + params.allowPatching + "\n");

		info.append("\n");

		info.append("------------ TEST INFO -----------\n");
		info.append("Inputs:" + "\n");
		for (DijTensor inp : params.inputList) {
			info.append("  - Name: " + inp.exampleInput + "\n");
			info.append("    Size: " + inp.inputTestSize + "\n");
			info.append("      x: " + inp.inputPixelSizeX  + "\n");
			info.append("      y: " + inp.inputPixelSizeY  + "\n");
			info.append("      z: " + inp.inputPixelSizeZ  + "\n");			
		}
		info.append("Outputs:" + "\n");
		for (HashMap<String, String> out : params.savedOutputs) {
			info.append("  - Name: " + out.get("name") + "\n");
			info.append("  - Type: " + out.get("type") + "\n");
			info.append("     Size: " + out.get("size")  + "\n");		
		}
		info.append("Memory peak: " + params.memoryPeak + "\n");
		info.append("Runtime: " + params.runtime + "\n");
		String modelSize = "-1";
		
		String ptModelName = "pytorch_script.pt";
		String tfModelName = "tensorflow_saved_model_bundle.zip";
		if (params.framework.toLowerCase().contains("pytorch")) {
			String modelName = findNameFromSourceParam(this.params.ptSource, "pytorch");
			if (new File(this.getPath() + File.separator + modelName).exists())
				ptModelName = modelName;		
		}
		if (params.framework.toLowerCase().contains("tensorflow")) {
			String modelName = findNameFromSourceParam(this.params.tfSource, "tensorflow");
			if (new File(this.getPath() + File.separator + modelName).exists())
				tfModelName = modelName;	
		}
		
		if (params.framework.equals("pytorch")) {
			modelSize = "" + new File(this.getPath() + File.separator + ptModelName).length() / (1024 * 1024.0);
			modelSize = modelSize.substring(0, modelSize.lastIndexOf(".") + 3);
			info.append("Weights size: " + modelSize + " MB\n");
		} else if (params.framework.equals("tensorflow") && new File(this.getPath(), "variables").exists()) {
			modelSize = "" + FileTools.getFolderSize(this.getPath() + File.separator + "variables") / (1024 * 1024.0);
			modelSize = modelSize.substring(0, modelSize.lastIndexOf(".") + 3);
			info.append("Weights size: " + modelSize + " MB\n");
		} else if (params.framework.equals("tensorflow")) {
			modelSize = "" + new File(this.getPath() + File.separator + tfModelName).length() / (1024 * 1024.0);
			modelSize = modelSize.substring(0, modelSize.lastIndexOf(".") + 2);
			info.append("Zipped model size: " + modelSize + " MB\n");
		} else if (params.framework.equals("tensorflow/pytorch") && new File(this.getPath(), "variables").exists()) {
			modelSize = "" + new File(this.getPath() + File.separator + ptModelName).length() / (1024 * 1024.0);
			modelSize = modelSize.substring(0, modelSize.lastIndexOf(".") + 3);
			info.append("Pytorch weights size: " + modelSize + " MB\n");

			modelSize = "" + FileTools.getFolderSize(this.getPath() + File.separator + "variables") / (1024 * 1024.0);
			modelSize = modelSize.substring(0, modelSize.lastIndexOf(".") + 3);
			info.append("Tensorflow weights size: " + modelSize + " MB\n");
		} else if (params.framework.equals("tensorflow/pytorch")) {
			modelSize = "" + new File(this.getPath() + File.separator + ptModelName).length() / (1024 * 1024.0);
			modelSize = modelSize.substring(0, modelSize.lastIndexOf(".") + 3);
			info.append("Pytorch weights size: " + modelSize + " MB\n");

			modelSize = "" + new File(this.getPath() + File.separator + tfModelName).length() / (1024 * 1024.0);
			modelSize = modelSize.substring(0, modelSize.lastIndexOf(".") + 3);
			info.append("Zipped Tensorflow model size: " + modelSize + " MB\n");
		}
		
	}
	
	/**
	 * Removes the ./ in the case it exists
	 * @param sourceName: name of the file writen in the yaml
	 * @return name without ./ if it had it
	 */
	public static String findNameFromSourceParam(String sourceName, String framework) {
		String modelName = sourceName;
		//If the yaml does not specify a source model, set to default
		if (modelName == null && framework.toLowerCase().contentEquals("pytorch")) {
			modelName = "pytorch_script.pt";
		} else if (modelName == null && framework.toLowerCase().contentEquals("tensorflow")) {
			modelName = "tensorflow_saved_model_bundle.zip";
		} else if (modelName.indexOf("/") != -1 && modelName.indexOf("/") < 2) {
			modelName = modelName.substring(modelName.indexOf("/") + 1);
		}
		return modelName;
	}

	public  boolean check(String path) {
		File dir = new File(path);
		if (!dir.exists()) {
			return false;
		}
		if (!dir.isDirectory()) {
			return false;
		}
		boolean validTf = false;
		boolean validPt = false;

		File modelFile = new File(path + "saved_model.pb");
		File variableFile = new File(path + "variables");
		if (modelFile.exists() && variableFile.exists()){
			validTf = true; 
			this.params.framework = "tensorflow";
		}
		
		// If no tf model has been found. Look for a pytorch torchscript model
		if (findPytorchModel(dir)) {
			this.params.selectedModelPath = dir.getAbsolutePath();
			validPt = true;
			this.params.framework = "pytorch";
		}
		
		if (validTf && validPt)
			this.params.framework = "tensorflow/pytorch";
		
		if (!validTf && !validPt) {
			// Find zipped biozoo model
			try {
				validTf = findZippedBiozooModel(dir);
			} catch (IOException e) {
				validTf = false;
			}
		}
		
		return validTf || validPt;
	}
	
	/*
	 * Method returns true if a zipped bioimage model zoo tf model is found inside
	 * of the folder provided
	 */
	public boolean findZippedBiozooModel(File modelFolder) throws IOException {
		String modelName = this.params.tfSource;
		if (modelName == null) {
			modelName = "tensorflow_saved_model_bundle.zip";
		} else if (modelName.indexOf("/") != -1 && modelName.indexOf("/") < 2)
			modelName = modelName.substring(modelName.indexOf("/") + 1);
		String auxModelName = "tensorflow_saved_model_bundle.zip";
		boolean auxPresent = false;
		for (String file : modelFolder.list()) {
			// If we find the model and the checksum (sha256) is the same as specified in the yaml file, load the model
			if (file.equals(modelName) && !this.presentYaml) {
				tfName = modelName;
				return true;
			} else if (file.equals(modelName) && FileTools.createSHA256(modelFolder.getPath() + File.separator + file).equals(params.tfSha256)) {
				tfName = modelName;
				return true;
			} else if (file.equals(modelName)) {
				IJ.log("Zipped Bioimage Model Zoo model at:");
				IJ.log(modelFolder.getAbsolutePath() + File.separator + file);
				IJ.log("does not coincide with the one specified in the rdf.yaml (incorrect sha256).");
				IJ.log("\n");
				params.incorrectSha256 = true;
				tfName = modelName;
				return true;
			} else if (file.equals(auxModelName)) {
				auxPresent =  true;
			}				
		}
		if (auxPresent && !this.presentYaml) {
			return true;
		} else if (auxPresent && FileTools.createSHA256(modelFolder.getPath() + File.separator + auxModelName).equals(params.tfSha256)) {
			return true;
		} else if (auxPresent) {
			IJ.log("Zipped Bioimage Model Zoo model at:");
			IJ.log(modelFolder.getAbsolutePath() + File.separator + auxModelName);
			IJ.log("does not coincide with the one specified in the rdf.yaml (incorrect sha256).");
			IJ.log("\n");
			params.incorrectSha256 = true;
			return true;
		}
		return false;
	}
	
	/*
	 * Method returns true if a torchscript model is found inside
	 * of the folder provided and corresponds to the model defined in the rd.yaml
	 */
	public boolean findPytorchModel(File modelFolder) {
		String modelName = this.params.ptSource;
		//If the yaml does not specify a source model, set to default
		if (modelName == null) {
			modelName = "pytorch_script.pt";
		} else if (modelName.indexOf("/") != -1 && modelName.indexOf("/") < 2) {
			modelName = modelName.substring(modelName.indexOf("/") + 1);
		}
		String auxModelName = "pytorch_script.pt";
		boolean auxPresent = false;
		try {
			for (String file : modelFolder.list()) {
				if (!this.developer && file.contains(modelName) && !this.presentYaml) {
					ptName = modelName;
					return true;
				} else if (!this.developer && file.contains(modelName) && FileTools.createSHA256(modelFolder.getPath() + File.separator + file).equals(params.ptSha256)) {
					ptName = modelName;
					return true;
				} else if (this.developer && file.contains(".pt")) {
					ptName = file;
					return true;
				} else if (!this.developer && file.contains(modelName)) {
					IJ.log("Pytorch model at:");
					IJ.log(modelFolder.getAbsolutePath() + File.separator + file);
					IJ.log("does not coincide with the one specified in the rdf.yaml (incorrect sha256).");
					IJ.log("\n");
					params.incorrectSha256 = true;
					ptName = modelName;
					return true;
				} else if (!this.developer && file.contains(auxModelName)) {
					auxPresent = true;
				}
			}
			// If the model was not specified in the source or the source name was not found,
			// try with the default name
			if (!this.developer && auxPresent && !this.presentYaml) {
				return true;
			} else if (!this.developer && auxPresent && FileTools.createSHA256(modelFolder.getPath() + File.separator + auxModelName).equals(params.ptSha256)) {
				return true;
			} else if (!this.developer && auxPresent) {
				IJ.log("Zipped Bioimage Model Zoo model at:");
				IJ.log(modelFolder.getAbsolutePath() + File.separator + auxModelName);
				IJ.log("does not coincide with the one specified in the rdf.yaml (incorrect sha256).");
				IJ.log("\n");
				params.incorrectSha256 = true;
				return true;
			}

		} catch (IOException e) {
			// If we were not able to gnerate a checksum (sha256) return false
			return false;
		}
		return false;
	}
	
	/*
	 * Method returns true if a torchscript model is found inside
	 * of the folder provided
	 */
	public static boolean isTherePytorch(File modelFolder) {
		for (String file : modelFolder.list()) {
			if (file.contains(".pt") && (file.indexOf(".pt") == file.lastIndexOf(".") || file.indexOf(".pth") == file.lastIndexOf("."))) 
				return true;
		}
		return false;
	}
	
	public void askFrameworkGUI() {
		GenericDialog dlg = new GenericDialog("Choose model framework");
		dlg.addMessage("The folder provided contained both a Tensorflow and a Pytorch model");
		dlg.addMessage("Select which do you want to load.");
		dlg.addChoice("Select framework", new String[]{"tensorflow", "pytorch"}, "tensorflow");
		dlg.showDialog();
		if (dlg.wasCanceled()) {
			dlg.dispose();
			return;
		}
		this.params.framework = dlg.getNextChoice();
	}

}


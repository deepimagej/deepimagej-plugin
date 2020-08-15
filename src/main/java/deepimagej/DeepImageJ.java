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

import java.awt.TextArea;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.tensorflow.SavedModelBundle;

import deepimagej.tools.Log;
import deepimagej.tools.DijTensor;
import deepimagej.tools.FileTools;
import ij.IJ;
import ij.ImagePlus;

public class DeepImageJ {

	private String				path;
	private Log 					log;
	public String				dirname;
	public Parameters			params;
	private boolean				valid = true;
	public ArrayList<String>		msgChecks		= new ArrayList<String>();
	public ArrayList<String>		msgLoads			= new ArrayList<String>();
	public ArrayList<String[]>	msgArchis		= new ArrayList<String[]>();
	private SavedModelBundle		model			= null;
	
	public DeepImageJ(String pathModel, String dirname, Log log, boolean isDeveloper) {
		String p = pathModel + File.separator + dirname + File.separator;
		this.path = p.replace(File.separator + File.separator, File.separator);
		this.log = log;
		this.dirname = dirname;
		// TODO adapt to new. 
		//this.valid = isDeveloper ? TensorFlowModel.check(p, msgChecks) : check(p, msgChecks);
		if (!isDeveloper && new File(path, "config.yaml").isFile()) {
			this.params = new Parameters(valid, path, isDeveloper);
			this.params.path2Model = this.path;
		} else if (isDeveloper) {
			this.params = new Parameters(valid, path, isDeveloper);
		}
		this.valid = check(p, msgChecks);
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

	public boolean getValid() {
		return this.valid;
	}
	
	static public HashMap<String, DeepImageJ> list(String pathModels, Log log, boolean isDeveloper) {
		HashMap<String, DeepImageJ> list = new HashMap<String, DeepImageJ>();
		File models = new File(pathModels);
		File[] dirs = models.listFiles();
		if (dirs == null) {
			return list;
		}

		for (File dir : dirs) {
			if (dir.isDirectory()) {
				String name = dir.getName();
				DeepImageJ dp = new DeepImageJ(pathModels + File.separator, name, log, isDeveloper);
				if (dp.valid && dp.params.completeConfig == true) {
					list.put(dp.dirname, dp);
				} else if (dp.valid && dp.params.completeConfig != true) {
					IJ.error("Model " + dp.dirname + " could not load\n"
							+ "because its config.xml file did not correspond\n"
							+ "to this version of the plugin.");
				}
				
			}
		}
		return list;
	}


	public boolean loadModel(boolean archi) {
		log.print("load model from " + path);

		double chrono = System.nanoTime();
		SavedModelBundle model;
		try {
			model = SavedModelBundle.load(path, TensorFlowModel.returnStringTag(params.tag));
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
		log.print("Loaded");
		if (msgLoads.size() == 0) {
			msgLoads.add("Metagraph size: " + model.metaGraphDef().length);
			msgLoads.add("Graph size: " + model.graph().toGraphDef().length);
			msgLoads.add("Loading time: " + chrono + "ms");
		}
		return true;
	}

	public void writeParameters(TextArea info, ArrayList<String> checks) {
		if (params == null) {
			info.append("No params\n");
			return;
		}
		info.append("----------- BASIC INFO -----------\n");
		info.append("Name: " + params.name + "\n");
		info.append(checks.get(0) + "\n");
		info.append("Size: " + checks.get(1).substring(18) + "\n");
		info.append("Authors" + "\n");
		for (String auth : params.author)
			info.append("  - " + auth + "\n");
		info.append("References" + "\n");
		for (HashMap<String, String> ref : params.cite) {
			info.append("  - Article: " + ref.get("text") + "\n");
			info.append("    Doi: " + ref.get("doi") + "\n");
		}
		info.append("Framework:" + params.framework + "\n");
		

		info.append("------------ METADATA ------------\n");
		info.append("Tag: " + params.tag + "\n");
		info.append("Signature: " + params.graph + "\n");
		info.append("Allow tiling: " + params.allowPatching + "\n");

		info.append("Dimensions: ");
		/* TODO remove
		for (DijTensor inp : params.inputList) {
			info.append(Arrays.toString(inp.tensor_shape));
			int slices = 1;
			int zInd = Index.indexOf(inp.form.split(""), "Z");
			if (zInd != -1) {slices = inp.tensor_shape[zInd];}
			int channels = 1;
			int cInd = Index.indexOf(inp.form.split(""), "C");
			if (cInd != -1) {channels = inp.tensor_shape[cInd];}
			info.append(" Slices (" + slices + ") Channels (" + channels + ")\n");
		}
		*/
		info.append("Input:");
		for (DijTensor inp2 : params.inputList)
			info.append(" " + inp2.name + " (" + inp2.form + ")");
		info.append("\n");
		info.append("Output:");
		for (DijTensor out : params.outputList)
			info.append(" " + out.name + " (" + out.form + ")");
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
			// TODO Deicde info.append("  - Name: " + out.name + "\n");
			info.append("  - Type: " + out.get("type") + "\n");
			info.append("     Size: " + out.get("size")  + "\n");		
		}
		info.append("Memory peak: " + params.memoryPeak + "\n");
		info.append("Runtime: " + params.runtime + "\n");
		
	}

	// TODO what we do with this??
	public  boolean check(String path, ArrayList<String> msg) {
		msg.add("Path: " + path);
		File dir = new File(path);
		if (!dir.exists()) {
			msg.add("Not found " + path);
			return false;
		}
		if (!dir.isDirectory()) {
			msg.add("Not found " + path);
			return false;
		}
		boolean valid = true;
		
		File configFile = new File(path + "config.yaml");
		if (!configFile.exists()) {
			msg.add("No 'config.yaml' found in " + path);
			valid = false;
			return valid;
		}

		File modelFile = new File(path + "saved_model.pb");
		if (!modelFile.exists()) {
			msg.add("No 'saved_model.pb' found in " + path);
			valid = false;
		}
		
		Set<String> versions = this.params.previousVersions.keySet();
		boolean isThereBiozoo = false;
		for (String v : versions) {
			if (v.contains("weights_" + v + ".zip")) 
				isThereBiozoo = true;
		}
		if (isThereBiozoo) {
			valid = true;
			return valid;
		}

		File variableFile = new File(path + "variables");
		if (!variableFile.exists()) {
			msg.add("No 'variables' directory found in " + path);
			valid = false;
		}
		else {
			msg.add("TensorFlow model " + FileTools.getFolderSizeKb(path + "variables"));
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

}


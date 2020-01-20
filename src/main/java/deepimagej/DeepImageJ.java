/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we strongly encourage you to include adequate citations and acknowledgments 
 * whenever you present or publish results that are based on it.
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
 * DeepImageJ is an open source software (OSS): you can redistribute it and/or modify it under 
 * the terms of the BSD 2-Clause License.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 * You should have received a copy of the BSD 2-Clause License along with DeepImageJ. 
 * If not, see <https://opensource.org/licenses/bsd-license.php>.
 */

package deepimagej;

import java.awt.TextArea;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.SavedModelBundle;

import deepimagej.tools.Log;
import ij.IJ;
import ij.ImagePlus;

public class DeepImageJ {

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
	
	public DeepImageJ(String pathModel, String dirname, Log log, boolean isDeveloper) {
		String p = pathModel + File.separator + dirname + File.separator;
		this.path = p.replace(File.separator + File.separator, File.separator);
		this.log = log;
		this.dirname = dirname;
		this.valid = isDeveloper ? TensorFlowModel.check(p, msgChecks) : check(p, msgChecks);
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

	
	public  boolean check(String path, ArrayList<String> msg) {
		boolean valid = TensorFlowModel.check(path, msg);
		File configFile = new File(path + "config.xml");
		if (!configFile.exists()) {
			msg.add("No 'config.xml' found in " + path);
			// TODO Test (8/10/19)
			valid = false;
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


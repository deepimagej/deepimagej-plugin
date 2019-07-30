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

import deepimagej.tools.FileUtils;
import ij.IJ;
import ij.ImagePlus;

public class DeepPlugin {

	private String				path;
	public String				dirname;
	public Parameters			params;
	private boolean				valid;
	public ArrayList<String>		msgChecks		= new ArrayList<String>();
	public ArrayList<String>		msgLoads			= new ArrayList<String>();
	public ArrayList<String[]>	msgArchis		= new ArrayList<String[]>();
	private SavedModelBundle		model			= null;
	public ArrayList<String>		preprocessing	= new ArrayList<String>();
	public ArrayList<String>		postprocessing	= new ArrayList<String>();

	public DeepPlugin(String pathModel, String dirname) {
		this.path = pathModel + File.separator + dirname + File.separator;
		this.dirname = dirname;
		this.valid = check();
		this.params = new Parameters(valid, path);
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
	
	static public HashMap<String, DeepPlugin> list(String pathModels) {
		HashMap<String, DeepPlugin> list = new HashMap<String, DeepPlugin>();
		File models = new File(pathModels);
		File[] dirs = models.listFiles();
		if (dirs == null) {
			return list;
		}

		for (File dir : dirs) {
			if (dir.isDirectory()) {
				String name = dir.getName();
				DeepPlugin dp = new DeepPlugin(pathModels + File.separator, name);
				if (dp.valid) {
					list.put(name, dp);
				}
			}
		}
		return list;
	}

	public boolean loadModel() {
		File dir = new File(path);
		String[] files = dir.list();
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
		for (int dim : params.in_dimensions)
			info.append(" " + dim);
		info.append(" Slices (" + params.slices + ") Channels (" + params.channels + ")\n");

		info.append("Input:");
		for (int i = 0; i < params.n_inputs; i++)
			info.append(" " + params.inputs[i] + " (" + params.input_form[i] + ")");
		info.append("\n");
		info.append("Output:");
		for (int i = 0; i < params.n_outputs; i++)
			info.append(" " + params.outputs[i] + " (" + params.output_form[i] + ")");
		info.append("\n");
	}

	private boolean check() {
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
}

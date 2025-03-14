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

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.sun.jna.Platform;

import deepimagej.IjAdapter;
import deepimagej.Runner;
import deepimagej.gui.Gui;
import deepimagej.tools.ImPlusRaiManager;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.PlugIn;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptorFactory;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.tensor.Tensor;
import io.bioimage.modelrunner.utils.Constants;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class DeepImageJ_Run implements PlugIn {

	private String modelFolder;
	private String inputFolder;
	private String outputFolder;
	private String display;
	
	private ModelDescriptor model;
	
	/**
	 * Keys required to run deepImageJ with a macro
	 */
	final static String[] macroKeys = new String[] {"modelPath="};
	/**
	 * Optional keys to run deepImageJ with a macro or in headless mode
	 */
	final static String[] macroOptionalKeys = new String[] {"inputPath=", "outputFolder=", "displayOutput="};
	
	
	static public void main(String args[]) {
		new ImageJ();
		new DeepImageJ_Run().run("");
	}
	@Override
	public void run(String arg) {
	    boolean isMacro = IJ.isMacro();
	    boolean isHeadless = GraphicsEnvironment.isHeadless();
	    if (!isMacro) {
	    	runGUI();
	    } else if (isMacro ) { //&& !isHeadless) {
	    	runMacro();
	    } else if (isHeadless) {
	    	runHeadless();
	    }
	}
	
	private void runGUI() {
		File modelsDir = new File("models");
		if (!modelsDir.isDirectory() && !modelsDir.mkdir())
			throw new RuntimeException("Unable to create 'models' folder inside ImageJ/Fiji directory. Please create it yourself.");
		final Gui[] guiRef = new Gui[1];
	    if (SwingUtilities.isEventDispatchThread())
	    	guiRef[0] = new Gui(new IjAdapter());
	    else {
		    SwingUtilities.invokeLater(() -> {
		        guiRef[0] = new Gui(new IjAdapter());
		    });
	    }
	}
	
	/**
	 * Macro example:
	 * run("DeepImageJ Run", "modelPath=/path/to/model/LiveCellSegmentationBou 
	 *  inputPath=/path/to/image/sample_input_0.tif 
	 *  outputFolder=/path/to/ouput/folder
	 *  displayOutput=null")
	 */
	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>>  void runMacro() {
		parseCommand();

		if (this.inputFolder != null && !(new File(this.inputFolder).exists()))
			throw new IllegalArgumentException("The provided input folder does not exist: " + this.inputFolder);
		if (this.outputFolder != null && !(new File(this.outputFolder).isDirectory()) && !(new File(outputFolder).mkdirs()))
			throw new IllegalArgumentException("The provided output folder does not exist and cannot be created: " + this.inputFolder);
		
		IjAdapter adapter = new IjAdapter();

		try {
			loadDescriptor();
		} catch (ModelSpecsException | IOException e) {
			e.printStackTrace();
			return;
		}
		try (Runner runner = Runner.create(model, deepimagej.Constants.FIJI_FOLDER + File.separator + "engines")) {
			runner.load();
			if (this.inputFolder != null) {
				executeOnPath(runner, adapter);
			} else {
				executeOnImagePlus(runner, adapter);
			}
		} catch (IOException | LoadModelException | RunModelException | LoadEngineException e) {
			e.printStackTrace();
		}
	}
	
	private void loadDescriptor() throws FileNotFoundException, ModelSpecsException, IOException {
		model = ModelDescriptorFactory.readFromLocalFile(modelFolder + File.separator + Constants.RDF_FNAME);
		if (model.getInputTensors().size() > 1)
			throw new IllegalArgumentException("Selected model requires more than one input, currently only models with 1 input"
					+ " are supported.");
	}
	
	
	private void runHeadless() {
		// TODO not ready yet
	}
	
	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	void executeOnPath(Runner runner, IjAdapter adapter) throws FileNotFoundException, RunModelException, IOException {
		File ff = new File(this.inputFolder);
		if (ff.isDirectory())
			this.executeOnFolder(model, runner, adapter);
		else
			this.executeOnFile(model, runner, adapter);
	}
	
	private static <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	List<ImagePlus> executeOnFile(File ff, ModelDescriptor model, Runner runner, IjAdapter adapter) 
			throws FileNotFoundException, RunModelException, IOException {
		List<ImagePlus> outList = new ArrayList<ImagePlus>();
		ImagePlus imp = IJ.openImage(ff.getAbsolutePath());
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(model.getInputTensors().get(0).getName(), imp);
		List<Tensor<T>> inputList = adapter.convertToInputTensors(map, model);
		List<Tensor<R>> res = runner.run(inputList);
		for (Tensor<R> rr : res) {
			ImagePlus im = ImPlusRaiManager.convert(rr.getData(), rr.getAxesOrderString());
			im.setTitle(imp.getShortTitle() + "_" + rr.getName());
			outList.add(im);
		}
		return outList;
	}
	
	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	void executeOnFile(ModelDescriptor model, Runner runner, IjAdapter adapter) throws FileNotFoundException, RunModelException, IOException {
		List<ImagePlus> outs = executeOnFile(new File(this.inputFolder), model, runner, adapter);
		for (ImagePlus im : outs) {
			if (this.outputFolder != null) {
				IJ.saveAsTiff(im, this.outputFolder + File.separator + im.getTitle());
			} 
			if (display != null && this.display.equals("all")) {
				SwingUtilities.invokeLater(() -> im.show());
			}
		}
	}
	
	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	void executeOnFolder(ModelDescriptor model, Runner runner, IjAdapter adapter) throws FileNotFoundException, RunModelException, IOException {
		for (File ff : new File(this.inputFolder).listFiles()) {
			List<ImagePlus> outs = executeOnFile(ff, model, runner, adapter);
			
			for (ImagePlus im : outs) {
				if (this.outputFolder != null) {
					IJ.saveAsTiff(im, this.outputFolder + File.separator + im.getTitle());
				} 
				if (display != null && this.display.equals("all")) {
					SwingUtilities.invokeLater(() -> im.show());
				}
			}
		}
		
	}
	
	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	void executeOnImagePlus(Runner runner, IjAdapter adapter) throws FileNotFoundException, RunModelException, IOException {
		ImagePlus imp = WindowManager.getCurrentImage();
		Map<String, Object> inputMap = new HashMap<String, Object>();
		inputMap.put(model.getInputTensors().get(0).getName(), imp);
		List<Tensor<T>> inputList = adapter.convertToInputTensors(inputMap, model);
		List<Tensor<R>> res = runner.run(inputList);
		for (Tensor<R> rr : res) {
			ImagePlus im = ImPlusRaiManager.convert(rr.getData(), rr.getAxesOrderString());
			im.setTitle(imp.getShortTitle() + "_" + rr.getName());
			SwingUtilities.invokeLater(() -> im.show());
			if (this.outputFolder != null) {
				IJ.saveAsTiff(im, this.outputFolder + File.separator + im.getTitle());
			}
		}
	}
	
	public static String escapeString(String s) {
	    StringBuilder builder = new StringBuilder();
	    for (char c : s.toCharArray()) {
	        switch (c) {
	            case '\n':
	                builder.append("\\n");
	                break;
	            case '\r':
	                builder.append("\\r");
	                break;
	            case '\t':
	                builder.append("\\t");
	                break;
	            case '\"':
	                builder.append("\\\"");
	                break;
	            case '\\':
	                builder.append("\\\\");
	                break;
	            default:
	                builder.append(c);
	        }
	    }
	    return builder.toString();
	}

	
	private void parseCommand() {
		String macroArg = Macro.getOptions();
		System.out.println(escapeString(macroArg));
		if (Platform.isWindows())
			macroArg = macroArg.replaceAll(Pattern.quote(File.separator), Matcher.quoteReplacement(File.separator + File.separator));
		System.out.println(macroArg);

		// macroArg = "modelPath=NucleiSegmentationBoundaryModel";
		// macroArg = "modelPath=NucleiSegmentationBoundaryModel outputFolder=null";
		// macroArg = "modelPath=[StarDist H&E Nuclei Segmentation] inputPath=null outputFolder=null";

		modelFolder = parseArg(macroArg, macroKeys[0], true);
		if (!(new File(modelFolder).isAbsolute()))
			modelFolder = new File(deepimagej.Constants.FIJI_FOLDER + File.separator + "models", modelFolder).getAbsolutePath();
		inputFolder = parseArg(macroArg, macroOptionalKeys[0], false);
		outputFolder = parseArg(macroArg, macroOptionalKeys[1], false);
		display = parseArg(macroArg, macroOptionalKeys[2], false);
	}
	
	private static String parseArg(String macroArg, String arg, boolean required) {
		int modelFolderInd = macroArg.indexOf(arg);
		if (modelFolderInd == -1 && required)
			throw new IllegalArgumentException("DeepImageJ macro requires to set the variable '" + arg + "'.");
		else if (modelFolderInd == -1)
			return null;
		int modelFolderInd2 = macroArg.indexOf(arg + "[");
		int endInd = macroArg.indexOf(" ", modelFolderInd);
		String value;
		if (modelFolderInd2 != -1) {
			endInd = macroArg.indexOf("] ", modelFolderInd2);
			value = macroArg.substring(modelFolderInd2 + arg.length() + 1, endInd);
		} else {
			value = macroArg.substring(modelFolderInd + arg.length(), endInd);
		}
		if (value.equals("null") || value.equals(""))
			value = null;
		return value;
	}

}
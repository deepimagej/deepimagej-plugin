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

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import com.sun.jna.Platform;

import deepimagej.Runner;
import deepimagej.gui.ImageJGui;
import deepimagej.tools.ImPlusRaiManager;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import io.bioimage.modelrunner.apposed.appose.Types;
import io.bioimage.modelrunner.bioimageio.BioimageioRepo;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptorFactory;
import io.bioimage.modelrunner.bioimageio.description.TensorSpec;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.gui.Gui;
import io.bioimage.modelrunner.tensor.Tensor;
import io.bioimage.modelrunner.utils.Constants;
import net.imglib2.RandomAccessibleInterval;
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
	final static String[] macroKeys = new String[] {"model_path"};
	/**
	 * Optional keys to run deepImageJ with a macro or in headless mode
	 */
	final static String[] macroOptionalKeys = new String[] {"input_path", "output_folder", "display_output"};
	
	public final static String MACRO_INFO = "https://github.com/deepimagej/deepimagej-plugin/blob/main/README.md#macros";

	final static String MACRO_RECORD_COMMENT = ""
	        + System.lineSeparator()
	        + "// The macro recording feature will capture the command 'run(\"DeepImageJ Run\");', but executing it will have no effect." + System.lineSeparator()
	        + "// The recording will be performed once the button 'Run' is clicked." + System.lineSeparator()
	        + "// For more information, visit:" + System.lineSeparator()
	        + "// " + MACRO_INFO + System.lineSeparator();
	
	static public void main(String args[]) {
		new ImageJ();
		new DeepImageJ_Run().run("");
	}
	@Override
	public void run(String arg) {
	    boolean isMacro = IJ.isMacro();
	    if (!isMacro) {
	    	runGUI();
	    } else if (isMacro && Macro.getOptions() != null) {
	    	runMacro();
	    }
	}
	
	private void runGUI() {
		if (Recorder.record)
			Recorder.recordString(MACRO_RECORD_COMMENT);
		File modelsDir = new File(deepimagej.Constants.FIJI_FOLDER + File.separator + "models");
		if (!modelsDir.isDirectory() && !modelsDir.mkdir())
			throw new RuntimeException("Unable to create 'models' folder inside ImageJ/Fiji directory. Please create it yourself.");
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	ij.plugin.frame.PlugInFrame frame = new ij.plugin.frame.PlugInFrame("deepImageJ " + deepimagej.Constants.DIJ_VERSION);
            	Gui gui = new Gui(new ImageJGui());
                gui.setPreferredSize(new Dimension(600, 700));
    	    	Runnable callback = () -> frame.dispose();
    	    	gui.setCancelCallback(callback);
                frame.add(gui);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                    	gui.onClose();
                    }
                });
                }
           });
	}
	
	public static <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	List<RandomAccessibleInterval<R>> runModel(String model, List<RandomAccessibleInterval<T>> inputs, List<String> axesOrders) throws IOException, InterruptedException, LoadEngineException, RunModelException {
		model = identifyModel(model);
		ModelDescriptor descriptor = ModelDescriptorFactory.readFromLocalFile(model + File.separator + Constants.RDF_FNAME);
		
		Runner runner = Runner.create(descriptor, deepimagej.Constants.FIJI_FOLDER + File.separator + "engines");
		List<Tensor<T>> ins = createInputTensorList(descriptor, inputs, axesOrders);
		List<Tensor<R>> outs = runner.run(ins);
		List<RandomAccessibleInterval<R>> raiOuts = new ArrayList<RandomAccessibleInterval<R>>();
		for (int i = 0; i < outs.size(); i ++) {
			TensorSpec spec = descriptor.getOutputTensors().get(i);
			RandomAccessibleInterval<R> outRai = outs.get(i).getData();
			if (spec.isImage())
				outRai = ImPlusRaiManager.convertToAxesOrder(outRai, spec.getAxesOrder(), ImPlusRaiManager.IJ_AXES_ORDER);
			raiOuts.add(outRai);
		}
		return raiOuts;
	}
	
	private static <T extends RealType<T> & NativeType<T>> List<Tensor<T>> createInputTensorList(ModelDescriptor descriptor, List<RandomAccessibleInterval<T>> inputs, List<String> axesOrders) {
		if (inputs.size() != descriptor.getInputTensors().size())
			throw new IllegalArgumentException(String.format("The number of inputs defined in the Bioimage.io rdf.yaml specs file is not the same as the number"
					+ " of inputs provided: %s vs %s", descriptor.getInputTensors().size(), inputs.size()));
		List<Tensor<T>> ins = new ArrayList<Tensor<T>>();
		for (int i = 0; i < inputs.size(); i ++) {
			RandomAccessibleInterval<T> rai = inputs.get(i);
			TensorSpec spec = descriptor.getInputTensors().get(i);
			String axesOrder = axesOrders.get(i).toLowerCase();
			if (!axesOrder.contains("b") && axesOrder.contains("t") && !spec.getAxesOrder().contains("t"))
				axesOrder = axesOrder.replace("t", "b");
			else if (!axesOrder.contains("b") && axesOrder.contains("z") && !spec.getAxesOrder().contains("z"))
				axesOrder = axesOrder.replace("z", "b");
			if (spec.isImage())
				rai = ImPlusRaiManager.convertToAxesOrder(rai, axesOrder, spec.getAxesOrder());
			Tensor<T> tensor = Tensor.build(spec.getName(), spec.getAxesOrder(), rai);
			ins.add(tensor);
		}
		return ins;
	}
	
	/**
	 * Macro example:
	 * run("DeepImageJ Run", "modelPath=/path/to/model/LiveCellSegmentationBou 
	 *  inputPath=/path/to/image/sample_input_0.tif 
	 *  outputFolder=/path/to/ouput/folder
	 *  displayOutput=null")
	 */
	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>>
	void runMacro() {
		try {
			parseCommand();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return;
		}

		if (this.inputFolder != null && !(new File(this.inputFolder).exists()))
			throw new IllegalArgumentException("The provided input folder does not exist: " + this.inputFolder);
		if (this.outputFolder != null && !(new File(this.outputFolder).isDirectory()) && !(new File(outputFolder).mkdirs()))
			throw new IllegalArgumentException("The provided output folder does not exist and cannot be created: " + this.inputFolder);

		ImageJGui adapter = new ImageJGui();

		try {
			loadDescriptor();
		} catch (ModelSpecsException | IOException e) {
			e.printStackTrace();
			return;
		}
		try (Runner runner = Runner.create(model, deepimagej.Constants.FIJI_FOLDER + File.separator + "engines")) {
			runner.load(true);
			if (this.inputFolder != null) {
				executeOnPath(runner, adapter);
			} else {
				executeOnImagePlus(runner, adapter);
			}
		} catch (IOException | LoadModelException | RunModelException | LoadEngineException e) {
			throw new RuntimeException(Types.stackTrace(e));
		}
	}
	
	private void loadDescriptor() throws FileNotFoundException, ModelSpecsException, IOException {
		model = ModelDescriptorFactory.readFromLocalFile(modelFolder + File.separator + Constants.RDF_FNAME);
		if (model.getInputTensors().size() > 1)
			throw new IllegalArgumentException("Selected model requires more than one input, currently only models with 1 input"
					+ " are supported.");
	}
	
	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	void executeOnPath(Runner runner, ImageJGui adapter) throws FileNotFoundException, RunModelException, IOException {
		File ff = new File(this.inputFolder);
		if (ff.isDirectory())
			this.executeOnFolder(model, runner, adapter);
		else
			this.executeOnFile(model, runner, adapter);
	}
	
	private static <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	List<ImagePlus> executeOnFile(File ff, ModelDescriptor model, Runner runner, ImageJGui adapter) 
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
	void executeOnFile(ModelDescriptor model, Runner runner, ImageJGui adapter) throws FileNotFoundException, RunModelException, IOException {
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
	void executeOnFolder(ModelDescriptor model, Runner runner, ImageJGui adapter) throws FileNotFoundException, RunModelException, IOException {
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
	void executeOnImagePlus(Runner runner, ImageJGui adapter) throws FileNotFoundException, RunModelException, IOException {
		ImagePlus imp = WindowManager.getCurrentImage();
		Map<String, Object> inputMap = new HashMap<String, Object>();
		inputMap.put(model.getInputTensors().get(0).getName(), imp);
		List<Tensor<T>> inputList = adapter.convertToInputTensors(inputMap, model);
		List<Tensor<R>> res = runner.run(inputList);
		for (Tensor<R> rr : res) {
			ImagePlus im = ImPlusRaiManager.convert(rr.getData(), rr.getAxesOrderString());
			im.setTitle(imp.getShortTitle() + "_" + rr.getName());
			if (display != null)
				SwingUtilities.invokeLater(() -> im.show());
			if (this.outputFolder != null) {
				IJ.saveAsTiff(im, this.outputFolder + File.separator + im.getTitle());
			}
		}
	}

	
	private void parseCommand() throws IOException, InterruptedException {
		String macroArg = Macro.getOptions();
		if (Platform.isWindows())
			System.err.println("[WARNING] On Windows, you must use double "
					+ "backslashes ('\\\\') in file and folder paths. "
					+ "For example: C:\\\\path\\\\to\\\\modelFolder");

		// macroArg = "modelPath=NucleiSegmentationBoundaryModel";
		// macroArg = "modelPath=NucleiSegmentationBoundaryModel outputFolder=null";
		// macroArg = "modelPath=[StarDist H&E Nuclei Segmentation] inputPath=null outputFolder=null";

		String modelArg = parseArg(macroArg, macroKeys[0], true);
		inputFolder = parseArg(macroArg, macroOptionalKeys[0], false);
		outputFolder = parseArg(macroArg, macroOptionalKeys[1], false);
		display = parseArg(macroArg, macroOptionalKeys[2], false);
		modelFolder = identifyModel(modelArg);
	}
	
	private static String identifyModel(String modelArg) throws IOException, InterruptedException {

		if ((new File(modelArg).isAbsolute()))
			return modelArg;
		else if (new File(deepimagej.Constants.FIJI_FOLDER + File.separator + "models", modelArg).isDirectory())
			return new File(deepimagej.Constants.FIJI_FOLDER + File.separator + "models", modelArg).getAbsolutePath();
		else {
			List<ModelDescriptor> localModels = ModelDescriptorFactory.getModelsAtLocalRepo(deepimagej.Constants.FIJI_FOLDER + File.separator + "models");
			ModelDescriptor modelDes = localModels.stream().filter(mm -> mm.getNickname().equals(modelArg)).findFirst().orElse(null);
			if (modelDes != null) {
				return modelDes.getModelPath();
			}
			System.err.println("Looking for the model in the Bioimage.io repo: " + modelArg);
			Entry<String, ModelDescriptor> entry = BioimageioRepo.connect().listAllModels(false).entrySet().stream()
					.filter(ee -> ee.getValue().getNickname().equals(modelArg)).findFirst().orElse(null);
			if (entry == null)
				throw new IllegalArgumentException("Model '" + modelArg + "' not found either locally or on the Bioimage.io repo.");
			Consumer<Double> cons = (d) -> {
				System.out.println(String.format("Downloading %s: %.2f%%", modelArg, d * 100));
			};
			return BioimageioRepo.downloadModel(entry.getValue(), deepimagej.Constants.FIJI_FOLDER + File.separator + "models", cons);
		}
	}
	
	private static String parseArg(String macroArg, String arg, boolean required) {
		String value = Macro.getValue(macroArg, arg, null);
		if (value != null && value.equals(""))
			value = null;
		if (value == null && required)
			throw new IllegalArgumentException("DeepImageJ Run macro requires to the variable '" + arg + "'. "
					+ "For more info, please visit: " + DeepImageJ_Run.MACRO_INFO);
		return value;
	}

}
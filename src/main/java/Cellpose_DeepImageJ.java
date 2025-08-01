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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.apache.commons.compress.archivers.ArchiveException;

import deepimagej.gui.ImageJGui;
import deepimagej.gui.consumers.CellposeAdapter;
import deepimagej.tools.ImPlusRaiManager;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.CompositeConverter;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.apposed.appose.Types;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.gui.custom.gui.CellposeGUI;
import io.bioimage.modelrunner.gui.custom.CellposePluginUI;
import io.bioimage.modelrunner.model.special.cellpose.Cellpose;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class Cellpose_DeepImageJ implements PlugIn {
	
	private String macroModel;
	
	private String cytoColor;
	
	private String nucleiColor;   
	
	private float diameter;   
	
	private boolean displayAll = false;
	
	private static ImageJGui HELPER_CONSUMER;
    
    private static boolean INSTALLED_ENV = false;

    
	final static String MACRO_RECORD_COMMENT = ""
	        + System.lineSeparator()
	        + "// The macro recording feature will capture the command 'run(\"DeepImageJ Cellpose\");', but executing it will have no effect." + System.lineSeparator()
	        + "// The recording will be performed once the button 'Run' is clicked." + System.lineSeparator()
	        + "// For more information, visit:" + System.lineSeparator()
	        + "// " + DeepImageJ_Run.MACRO_INFO + System.lineSeparator()
	        + System.lineSeparator();
	
	static public void main(String args[]) {
		new ImageJ();
		new Cellpose_DeepImageJ().run("");
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
		CellposeAdapter adapter = new CellposeAdapter();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	ij.plugin.frame.PlugInFrame frame = new ij.plugin.frame.PlugInFrame("deepImageJ Cellpose");
            	CellposePluginUI gui = new CellposePluginUI(adapter);
                frame.add(gui);
                frame.pack();
                frame.setSize(500, 300);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                    	gui.close();
                    }
                });
    	    	gui.setCancelCallback(() -> frame.dispose());
            }
           });
	}

    private String getOutputName(String inputTitle, String tensorName) {
    	String noExtension;
    	if (inputTitle.lastIndexOf(".") != -1)
    		noExtension = inputTitle.substring(0, inputTitle.lastIndexOf("."));
    	else
    		noExtension = inputTitle;
    	String extension = ".tif";
    	return noExtension + "_" + tensorName + extension;
    }

	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>>  void runMacro() {
		try {
			parseCommand();
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
			return;
		}
		ImagePlus imp = WindowManager.getCurrentImage();
		boolean isColorRGB = imp.getType() == ImagePlus.COLOR_RGB;
		RandomAccessibleInterval<T> rai =
				ImPlusRaiManager.convert(isColorRGB ? CompositeConverter.makeComposite(imp) : imp, "xyczt");
		Map<String, RandomAccessibleInterval<T>> out = runCellpose(macroModel, rai, cytoColor, nucleiColor, diameter);
		HELPER_CONSUMER.displayRai(out.get("labels"), "xyb", getOutputName(imp.getTitle(), "labels"));
		if (!displayAll)
			return;
		HELPER_CONSUMER.displayRai(out.get("flows_0"), "xycb", getOutputName(imp.getTitle(), "flows_0"));
		HELPER_CONSUMER.displayRai(out.get("flows_1"), "cxyb", getOutputName(imp.getTitle(), "flows_1"));
		HELPER_CONSUMER.displayRai(out.get("flows_2"), "xyb", getOutputName(imp.getTitle(), "flows_2"));
		HELPER_CONSUMER.displayRai(out.get("image_dn"), "xycb", getOutputName(imp.getTitle(), "image_dn"));
	}
	
	private void parseCommand() throws NumberFormatException {
		String macroArg = Macro.getOptions();

		macroModel = parseArg(macroArg, "model", true);
		cytoColor = parseArg(macroArg, "cyto_color", true);
		nucleiColor = parseArg(macroArg, "nuclei_color", true);
		String diameterStr = parseArg(macroArg, "diameter", false);
		if (diameterStr != null) {
			diameter = Float.parseFloat(diameterStr);
		}
		String displayAllStr = parseArg(macroArg, "display_all", false);
		if (displayAllStr != null && (displayAllStr.equals("true") || displayAllStr.equals("True")))
			displayAll = true;
	}
	
	private static String parseArg(String macroArg, String arg, boolean required) {
		String value = Macro.getValue(macroArg, arg, null);
		if (value != null && value.equals(""))
			value = null;
		if (value == null && required)
			throw new IllegalArgumentException("DeepImageJ Cellpose macro requires to the variable '" + arg + "'. "
					+ "For more info, please visit: " + DeepImageJ_Run.MACRO_INFO);
		return value;
	}
	
	
	public static < T extends RealType< T > & NativeType< T > > 
	Map<String, RandomAccessibleInterval<T>> runCellpose(String modelPath, RandomAccessibleInterval<T> rai, String cytoColor, String nucleiColor) {
		return runCellpose(modelPath, rai, cytoColor, nucleiColor, null);
	}
	
	
	public static < T extends RealType< T > & NativeType< T > > 
	Map<String, RandomAccessibleInterval<T>> runCellpose(String modelPath, RandomAccessibleInterval<T> rai, String cytoColor, String nucleiColor, Float diameter) {
		checkChannels((cytoColor = cytoColor.toLowerCase()), (nucleiColor = nucleiColor.toLowerCase()));
		if (!INSTALLED_ENV) {
			Consumer<String> cons = System.out::println;
			try {
				Cellpose.installRequirements(cons);
				INSTALLED_ENV = true;
			} catch (IOException | InterruptedException | RuntimeException | MambaInstallException | ArchiveException
					| URISyntaxException e) {
				throw new RuntimeException("Error installing Cellpose. Caused by: " + Types.stackTrace(e));
			}
		}
		if (HELPER_CONSUMER == null)
			HELPER_CONSUMER = new ImageJGui();
		Cellpose model = null;
		try {
			if (new File(modelPath).isFile())
				model = Cellpose.init(modelPath);
			else if (Cellpose.fileIsCellpose(modelPath, HELPER_CONSUMER.getModelsDir()) != null)
				model = Cellpose.init(Cellpose.fileIsCellpose(modelPath, HELPER_CONSUMER.getModelsDir()));
			else {
				Consumer<Double> cons = (p) -> {
					System.out.println(String.format("Downloading %s model: %.2f%%", modelPath, p * 100));
				};
				model = Cellpose.init(Cellpose.donwloadPretrained(modelPath, HELPER_CONSUMER.getModelsDir(), cons));
			}
			model.loadModel();
	    	Map<String, RandomAccessibleInterval<T>> out = runCellposeOnFramesStack(model, rai, cytoColor, nucleiColor, diameter);
	    	model.close();
	    	return out;
		} catch (Exception e) {
			if (model != null)
				model.close();
			throw new RuntimeException("Error running the model. Caused by: " + Types.stackTrace(e));
		}
	}
    
    private static <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>>
    Map<String, RandomAccessibleInterval<T>> 
    runCellposeOnFramesStack(Cellpose model, RandomAccessibleInterval<R> rai, String cytoColor, String nucleiColor, Float diameter)
    		throws RunModelException {
    	model.setChannels(new int[] {CellposePluginUI.CHANNEL_MAP.get(cytoColor), CellposePluginUI.CHANNEL_MAP.get(nucleiColor)});
    	rai = addDimsToInput(rai, cytoColor.equals("gray") ? 1 : 3);
    	long[] inDims = rai.dimensionsAsLongArray();
    	long[] outDims = new long[] {inDims[0], inDims[1], inDims[3]};
		RandomAccessibleInterval<T> outMaskRai = Cast.unchecked(ArrayImgs.unsignedShorts(outDims));
		RandomAccessibleInterval<T> output1 = Cast.unchecked(ArrayImgs.unsignedBytes(new long[] {inDims[0], inDims[1], 3, inDims[3]}));
		RandomAccessibleInterval<T> output2 = Cast.unchecked(ArrayImgs.floats(new long[] {2, inDims[0], inDims[1], inDims[3]}));
		RandomAccessibleInterval<T> output3 = Cast.unchecked(ArrayImgs.floats(new long[] {inDims[0], inDims[1], inDims[3]}));
		RandomAccessibleInterval<T> output4 = Cast.unchecked(ArrayImgs.floats(new long[] {inDims[0], inDims[1], 3, inDims[3]}));
		RandomAccessibleInterval<T> styles = null;
		
		for (int i = 0; i < rai.dimensionsAsLongArray()[3]; i ++) {
			if (diameter != null)
				model.setDiameter(diameter);
	    	List<Tensor<R>> inList = new ArrayList<Tensor<R>>();
	    	Tensor<R> inIm = Tensor.build("input", "xyc", Views.hyperSlice(rai, 3, i));
	    	inList.add(inIm);
	    	
	    	List<Tensor<T>> outputList = new ArrayList<Tensor<T>>();
	    	Tensor<T> outMask = Tensor.build("labels", "xy", Views.hyperSlice(outMaskRai, 2, i));
	    	outputList.add(outMask);
	    	Tensor<T> flows0 = Tensor.build("flows_0", "xyc", Views.hyperSlice(output1, 3, i));
	    	outputList.add(flows0);
	    	Tensor<T> flows1 = Tensor.build("flows_1", "cxy", Views.hyperSlice(output2, 3, i));
	    	outputList.add(flows1);
	    	Tensor<T> flows2 = Tensor.build("flows_2", "xy", Views.hyperSlice(output3, 2, i));
	    	outputList.add(flows2);
	    	Tensor<T> st = Tensor.buildEmptyTensor("styles", "i");
	    	outputList.add(st);
	    	Tensor<T> dn = Tensor.build("image_dn", "xyc", Views.hyperSlice(output4, 3, i));
	    	outputList.add(dn);
	    	
	    	model.run(inList, outputList);
	    	if (styles == null) {
	    		long[] stylesDims = new long[outputList.get(4).getData().dimensionsAsLongArray().length + 1];
	    		int dd = 0;
	    		for (long dim : outputList.get(4).getData().dimensionsAsLongArray())
	    			stylesDims[dd ++] = dim;
	    		stylesDims[dd] = rai.dimensionsAsLongArray()[3];
	    		styles = new ArrayImgFactory<T>(outputList.get(4).getData().getType()).create(outDims);
	    	}
	    	RandomAccessibleInterval<T> slice = Views.hyperSlice(styles, styles.dimensionsAsLongArray().length - 1, i);
	    	slice = outputList.get(4).getData();
		}
		Map<String, RandomAccessibleInterval<T>> map = new HashMap<String, RandomAccessibleInterval<T>>();
		map.put("labels", outMaskRai);
		map.put("flows_0", output1);
		map.put("flows_1", output2);
		map.put("flows_2", output3);
		map.put("image_dn", output4);
		map.put("styles", styles);
    	return map;
    }
    
    private static <R extends RealType<R> & NativeType<R>>
    RandomAccessibleInterval<R> addDimsToInput(RandomAccessibleInterval<R> rai, int nChannels) {
    	long[] dims = rai.dimensionsAsLongArray();
    	if (dims.length == 2 && nChannels == 1)
    		return Views.addDimension(Views.addDimension(rai, 0, 0), 0, 0);
    	else if (dims.length == 2)
    		throw new IllegalArgumentException("Cyto and nuclei specified for RGB image and image provided is grayscale.");
    	else if (dims.length == 3 && dims[2] == nChannels)
    		return Views.addDimension(rai, 0, 0);
    	else if (dims.length == 3 && nChannels == 1)
    		return Views.permute(Views.addDimension(rai, 0, 0), 2, 3);
    	else if (dims.length >= 3 && dims[2] == 1 && nChannels == 3)
    		throw new IllegalArgumentException("Expected RGB (3 channels) image and got instead grayscale image (1 channel).");
    	else if (dims.length == 4 && dims[2] == nChannels)
    		return rai;
    	else if (dims.length == 5 && dims[2] == nChannels && dims[4] != 1)
    		return Views.hyperSlice(rai, 3, 0);
    	else if (dims.length == 5 && dims[2] == nChannels && dims[4] == 1)
    		return Views.hyperSlice(Views.permute(rai, 3, 4), 3, 0);
    	else if (dims.length == 4 && dims[2] != nChannels && nChannels == 1) {
    		rai = Views.hyperSlice(rai, 2, 0);
    		rai = Views.addDimension(rai, 0, 0);
    		return Views.permute(rai, 2, 3);
    	} else if (dims.length == 5 && dims[2] != nChannels)
    		throw new IllegalArgumentException("Expected grayscale (1 channel) image and got instead RGB image (3 channels).");
    	else
    		throw new IllegalArgumentException("Unsupported dimensions for Cellpose model");
    }
    
    private static void checkChannels(String cytoColor, String nucleiColor) {
    	if (!Arrays.asList(CellposeGUI.ALL_LIST).contains(cytoColor)) {
    		throw new IllegalArgumentException(String.format("Invalid 'cytoColor' (%s). Only possible options are: %s",
    				cytoColor, Arrays.asList(CellposeGUI.ALL_LIST)));
    	} else if (!Arrays.asList(CellposeGUI.ALL_LIST).contains(nucleiColor)) {
    		throw new IllegalArgumentException(String.format("Invalid 'nucleiColor' (%s). Only possible options are: %s",
    				nucleiColor, Arrays.asList(CellposeGUI.ALL_LIST)));
    	} else if ((cytoColor.equals("gray") && !nucleiColor.equals("gray")) 
    			|| (!cytoColor.equals("gray") && nucleiColor.equals("gray"))) {
    		throw new IllegalArgumentException("Invalid color combination, 'gray' can only be used for grayscale images."
    				+ " And when one of the colors is 'gray' the other needs to be 'gray' too.");
    	}
    }

}
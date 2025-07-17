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
import java.util.List;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.apache.commons.compress.archivers.ArchiveException;

import deepimagej.gui.ImageJGui;
import deepimagej.gui.consumers.StardistAdapter;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.apposed.appose.Types;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.gui.custom.StarDistPluginUI;
import io.bioimage.modelrunner.model.special.stardist.Stardist2D;
import io.bioimage.modelrunner.model.special.stardist.StardistAbstract;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class Stardist_DeepImageJ implements PlugIn {
	
	private String macroModel;
	
	private String probThresh;
	
	private String minPerc;
	
	private String maxPerc;
	
	private static ImageJGui HELPER_CONSUMER;
    
    private static boolean INSTALLED_ENV = false;

    
	final static String MACRO_RECORD_COMMENT = ""
	        + System.lineSeparator()
	        + "// The macro recording feature will capture the command 'run(\"DeepImageJ StarDist\");', but executing it will have no effect." + System.lineSeparator()
	        + "// The recording will be performed once the button 'Run' is clicked." + System.lineSeparator()
	        + "// For more information, visit:" + System.lineSeparator()
	        + "// " + DeepImageJ_Run.MACRO_INFO + System.lineSeparator()
	        + System.lineSeparator();
	
	static public void main(String args[]) {
		new ImageJ();
		new Stardist_DeepImageJ().run("");
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
		StardistAdapter adapter = new StardistAdapter();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	ij.plugin.frame.PlugInFrame frame = new ij.plugin.frame.PlugInFrame("deepImageJ StarDist");
            	StarDistPluginUI gui = new StarDistPluginUI(adapter);
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

	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>>  void runMacro() {
		parseCommand();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (HELPER_CONSUMER == null)
			HELPER_CONSUMER = new ImageJGui();
		RandomAccessibleInterval<T> out = runStarDist(macroModel, Cast.unchecked(ImageJFunctions.wrap(imp)), 
				Double.parseDouble(probThresh), Double.parseDouble(minPerc), Double.parseDouble(maxPerc));
		HELPER_CONSUMER.displayRai(out, "xycb", getOutputName(imp.getTitle(), "mask"));
	}
	
	private void parseCommand() {
		String macroArg = Macro.getOptions();

		macroModel = parseArg(macroArg, "model", true);
		probThresh = parseArg(macroArg, "prob_thresh", false);
		if (probThresh == null || (probThresh != null && probThresh.equals("")))
			probThresh = "0.5";
		minPerc = parseArg(macroArg, "min_percentile", false);
		if (minPerc == null || (minPerc != null && minPerc.equals("")))
			minPerc = "0.5";
		maxPerc = parseArg(macroArg, "max_percentile", false);
		if (maxPerc == null || (maxPerc != null && maxPerc.equals("")))
			maxPerc = "0.5";
	}
	
	private static String parseArg(String macroArg, String arg, boolean required) {
		String value = Macro.getValue(macroArg, arg, null);
		if (value != null && value.equals(""))
			value = null;
		if (value == null && required)
			throw new IllegalArgumentException("DeepImageJ StarDist macro requires to the variable '" + arg + "'. "
					+ "For more info, please visit: " + DeepImageJ_Run.MACRO_INFO);
		return value;
	}
	
	public static < T extends RealType< T > & NativeType< T > > 
	RandomAccessibleInterval<T> runStarDist(String modelPath, RandomAccessibleInterval<T> rai) {
		return runStarDist(modelPath, rai, null, 1, 99.8);
	}
	
	public static < T extends RealType< T > & NativeType< T > > 
	RandomAccessibleInterval<T> runStarDist(String modelPath, RandomAccessibleInterval<T> rai, Double probThresh) {
		return runStarDist(modelPath, rai, probThresh, 1, 99.8);
	}
	
	public static < T extends RealType< T > & NativeType< T > > 
	RandomAccessibleInterval<T> runStarDist(String modelPath, RandomAccessibleInterval<T> rai, Double probThresh, double minPerc, double maxPerc) {
		if (!INSTALLED_ENV) {
			Consumer<String> cons = System.out::println;
			try {
				StardistAbstract.installRequirements(cons);
				INSTALLED_ENV = true;
			} catch (IOException | InterruptedException | RuntimeException | MambaInstallException | ArchiveException
					| URISyntaxException e) {
				throw new RuntimeException("Error installing StarDist. Caused by: " + Types.stackTrace(e));
			}
		}
		StardistAbstract model = null;
		try {
			if (new File(modelPath).isDirectory()) {
				model = StardistAbstract.init(modelPath);
			} else if (Stardist2D.fromPretained(modelPath, HELPER_CONSUMER.getModelsDir(), false) != null){
				model = Stardist2D.fromPretained(modelPath, HELPER_CONSUMER.getModelsDir(), false);
			} else {
				Consumer<Double> cons = 
						(d) -> System.out.println(String.format("Downloading %s: %.2f%%", modelPath, d * 100));
				String path = Stardist2D.downloadPretrained(modelPath, HELPER_CONSUMER.getModelsDir(), cons);
				model = StardistAbstract.init(path);
			}
			model.loadModel();
			if (probThresh != null)
				model.setThreshold(probThresh);
			model.scaleRangeMinPercentile = minPerc;
			model.scaleRangeMaxPercentile = maxPerc;
	    	RandomAccessibleInterval<T> out = runStardistOnFramesStack(model, rai);
	    	model.close();
	    	return out;
		} catch (Exception e) {
			if (model != null)
				model.close();
			throw new RuntimeException("Error running the model. Caused by: " + Types.stackTrace(e));
		}
	}
	
    private static <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>>
    RandomAccessibleInterval<T> runStardistOnFramesStack(StardistAbstract model, RandomAccessibleInterval<R> rai) throws RunModelException {
    	rai = addDimsToInput(rai, model);
    	long[] inDims = rai.dimensionsAsLongArray();
    	long[] outDims;
    	if (model.is2D())
    		outDims = new long[] {inDims[0], inDims[1], 1, inDims[3]};
    	else
    		outDims = new long[] {inDims[0], inDims[1], 1, inDims[3], inDims[4]};
		RandomAccessibleInterval<T> outMaskRai = Cast.unchecked(ArrayImgs.floats(outDims));
		for (int i = 0; i < inDims[inDims.length - 1]; i ++) {
	    	List<Tensor<R>> inList = new ArrayList<Tensor<R>>();
	    	Tensor<R> inIm = Tensor.build("input", model.is2D() ? "xyc" : "xycz", Views.hyperSlice(rai, inDims.length - 1, i));
	    	inList.add(inIm);
	    	
	    	List<Tensor<T>> outputList = new ArrayList<Tensor<T>>();
	    	Tensor<T> outMask = Tensor.build("mask", model.is2D() ? "xyc" : "xycz", Views.hyperSlice(outMaskRai, outDims.length - 1, i));
	    	outputList.add(outMask);
	    	
	    	model.run(inList, outputList);
		}
    	return outMaskRai;
    }
    
    private static <R extends RealType<R> & NativeType<R>>
    RandomAccessibleInterval<R> addDimsToInput(RandomAccessibleInterval<R> rai, StardistAbstract model) {
    	int nChannels = model.getNChannels();
    	boolean is2d = model.is2D();
    	long[] dims = rai.dimensionsAsLongArray();
    	if (dims.length == 2 && nChannels == 1 && is2d)
    		return Views.addDimension(Views.addDimension(rai, 0, 0), 0, 0);
    	else if (dims.length == 3 && dims[2] == nChannels && is2d)
    		return Views.addDimension(rai, 0, 0);
    	else if (dims.length == 4 && dims[2] == nChannels && is2d)
    		return rai;
    	else if (dims.length == 5 && dims[2] == nChannels && is2d && dims[4] > 1)
    		return Views.hyperSlice(rai, 3, 0);
    	else if (dims.length == 5 && dims[2] == nChannels && is2d && dims[4] == 1)
    		return Views.hyperSlice(rai, 4, 0);
    	else if (dims.length == 3 && dims[2] != nChannels && nChannels == 1 && !is2d) {
    		rai = Views.permute(Views.addDimension(rai, 0, 0), 2, 3);
    		return Views.addDimension(rai, 0, 0);
    	} else if (dims.length == 4 && dims[2] != nChannels && nChannels == 1 && !is2d)
    		return Views.permute(Views.permute(Views.addDimension(rai, 0, 0), 3, 4), 2, 3);
    	else if (dims.length == 4 && dims[2] == nChannels && !is2d)
    		return Views.addDimension(rai, 0, 0);
    	else if (dims.length == 5 && dims[2] == nChannels && !is2d)
    		return rai;
    	else if (dims.length == 3 && dims[2] != nChannels && is2d)
    		throw new IllegalArgumentException(String.format("Number of channels required for this model is: %s."
    				+ " The number of channels (third dimension) in the image provided: %s.", nChannels, dims[2]));
    	else if (dims.length == 3 && dims[2] != nChannels && is2d)
    		throw new IllegalArgumentException(String.format("Number of channels required for this model is: %s."
    				+ " The number of channels (third dimension) in the image provided: %s.", nChannels, dims[2]));
    	else if (dims.length == 2 && nChannels > 1)
    		throw new IllegalArgumentException(String.format("Model requires %s channels", nChannels));
    	else if (dims.length == 2 && !is2d)
    		throw new IllegalArgumentException("Model is 3d, 2d image provided");
    	else
    		throw new IllegalArgumentException(
    				String.format("Unsupported dimensions for %s model with %s channels. Dimension order should be (X, Y, C, Z, B or T)"
    						, is2d ? "2D" : "3D", nChannels));
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
}
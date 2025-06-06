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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.apache.commons.compress.archivers.ArchiveException;

import deepimagej.gui.consumers.StardistAdapter;
import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.plugin.PlugIn;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.apposed.appose.Types;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.gui.custom.StardistGUI;
import io.bioimage.modelrunner.model.special.stardist.StardistAbstract;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.RandomAccessibleInterval;
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
public class Stardist_DeepImageJ implements PlugIn {
    
    
    private static boolean INSTALLED_ENV = false;
	
	static public void main(String args[]) {
		new ImageJ();
		new Stardist_DeepImageJ().run("");
	}
	@Override
	public void run(String arg) {
	    boolean isMacro = IJ.isMacro();
	    if (!isMacro) {
	    	runGUI();
	    } else if (isMacro ) {
	    	runMacro();
	    }
	}
	
	private void runGUI() {
		StardistAdapter adapter = new StardistAdapter();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	ij.plugin.frame.PlugInFrame frame = new ij.plugin.frame.PlugInFrame("deepImageJ StarDist");
            	StardistGUI gui = new StardistGUI(adapter);
                frame.add(gui);
                frame.pack();
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
	
	/**
	 * Macro example:
	 * run("DeepImageJ Run", "modelPath=/path/to/model/LiveCellSegmentationBou 
	 *  inputPath=/path/to/image/sample_input_0.tif 
	 *  outputFolder=/path/to/ouput/folder
	 *  displayOutput=null")
	 */
	private <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>>  void runMacro() {
		parseCommand();
	}
	
	
	public static < T extends RealType< T > & NativeType< T > > 
	RandomAccessibleInterval<T> runStarDist(String modelPath, RandomAccessibleInterval<T> rai) {
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
		try (StardistAbstract model = StardistAbstract.init(modelPath)){
			model.loadModel();
	    	return runStardistOnFramesStack(model, rai);
		} catch (RunModelException | LoadModelException | IOException e) {
			throw new RuntimeException("Error running the model. Caused by: " + Types.stackTrace(e));
		}
	}
	
    private static <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>>
    RandomAccessibleInterval<T> runStardistOnFramesStack(StardistAbstract model, RandomAccessibleInterval<R> rai) throws RunModelException {
    	long[] outDims = getOutputDims(model, rai);
    	rai = addDimsToInput(rai, outDims, model);
    	long[] inDims = rai.dimensionsAsLongArray();
		RandomAccessibleInterval<T> outMaskRai = Cast.unchecked(ArrayImgs.floats(outDims));
		for (int i = 0; i < inDims[inDims.length - 1]; i ++) {
	    	List<Tensor<R>> inList = new ArrayList<Tensor<R>>();
	    	Tensor<R> inIm = Tensor.build("input", "xyc", Views.hyperSlice(rai, inDims.length - 1, i));
	    	inList.add(inIm);
	    	
	    	List<Tensor<T>> outputList = new ArrayList<Tensor<T>>();
	    	Tensor<T> outMask = Tensor.build("mask", "xy", Views.hyperSlice(outMaskRai, outDims.length - 1, i));
	    	outputList.add(outMask);
	    	
	    	model.run(inList, outputList);
		}
    	return outMaskRai;
    }
    
    private static <R extends RealType<R> & NativeType<R>>
    RandomAccessibleInterval<R> addDimsToInput(RandomAccessibleInterval<R> rai, long[] outDims, StardistAbstract model) {
    	long[] inDims = rai.dimensionsAsLongArray();
    	if (outDims.length > inDims.length || (model.getNChannels() > 1 && outDims.length == inDims.length))
    		rai = Views.addDimension(rai, 0, 1);
    	if (outDims.length == inDims.length)
    		rai = Views.addDimension(rai, 0, 1);
    	return rai;
    }
    
    private static <R extends RealType<R> & NativeType<R>>
    long[] getOutputDims(StardistAbstract model, RandomAccessibleInterval<R> rai) {
    	int nChannels = model.getNChannels();
    	boolean is2d = model.is2D();
    	long[] dims = rai.dimensionsAsLongArray();
    	if (dims.length == 2 && nChannels == 1 && is2d)
    		return new long[] {dims[0], dims[1], 1};
    	else if (dims.length == 3 && dims[2] == 1 && nChannels == 1 && is2d)
    		return dims;
    	else if (dims.length == 2 && nChannels > 1)
    		throw new IllegalArgumentException(String.format("Model requires %s channels", nChannels));
    	else if (dims.length == 2 && !is2d)
    		throw new IllegalArgumentException("Model is 3d, 2d image provided");
    	else if (dims.length > 3 && dims[2] == nChannels && is2d)
    		return new long[] {dims[0], dims[1], dims[3]};
    	else if (dims.length == 3 && dims[2] == nChannels && is2d)
    		return new long[] {dims[0], dims[1], 1};
    	else if (dims.length >= 3 && dims[2] != nChannels && is2d)
    		throw new IllegalArgumentException(String.format("Number of channels required for this model is: %s."
    				+ " The number of channels (third dimension) in the image provided: %s.", nChannels, dims[2]));
    	else if (dims.length == 3 && dims[2] != nChannels && !is2d)
    		return new long[] {dims[0], dims[1], dims[2], 1};
    	else if (dims.length > 3 && dims[2] != nChannels && !is2d)
    		return new long[] {dims[0], dims[1], dims[2], dims[3]};
    	else if (dims.length == 4 && dims[2] == nChannels && !is2d)
    		return new long[] {dims[0], dims[1], dims[3], dims[1]};
    	else if (dims.length > 4 && dims[2] == nChannels && !is2d)
    		return new long[] {dims[0], dims[1], dims[3], dims[4]};
    	else
    		throw new IllegalArgumentException(
    				String.format("Unsupported dimensions for %s model with %s channels. Dimension order should be (X, Y, C, Z, B or T)"
    						, is2d ? "2D" : "3D", nChannels));
    }
	
	private void parseCommand() {
		String macroArg = Macro.getOptions();
		System.out.println(macroArg);

		// macroArg = "modelPath=NucleiSegmentationBoundaryModel";
		// macroArg = "modelPath=NucleiSegmentationBoundaryModel outputFolder=null";
		// macroArg = "modelPath=[StarDist H&E Nuclei Segmentation] inputPath=null outputFolder=null";

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
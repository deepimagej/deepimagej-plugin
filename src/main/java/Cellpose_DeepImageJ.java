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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

import deepimagej.gui.consumers.CellposeAdapter;
import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.plugin.PlugIn;
import io.bioimage.modelrunner.gui.CellposeGUI;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class Cellpose_DeepImageJ implements PlugIn {
	
	static public void main(String args[]) {
		new ImageJ();
		new Cellpose_DeepImageJ().run("");
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
		CellposeAdapter adapter = new CellposeAdapter();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	ij.plugin.frame.PlugInFrame frame = new ij.plugin.frame.PlugInFrame("Cellpose Plugin");
            	CellposeGUI gui = new CellposeGUI(adapter);
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
	
	
	private void runHeadless() {
		// TODO not ready yet
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
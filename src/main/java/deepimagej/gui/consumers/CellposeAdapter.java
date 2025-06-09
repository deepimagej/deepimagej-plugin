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

package deepimagej.gui.consumers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import ij.ImagePlus;
import ij.plugin.frame.Recorder;
import io.bioimage.modelrunner.gui.custom.CellposeGUI;

/**
 * @author Carlos Garcia
 */
public class CellposeAdapter extends SmallPluginAdapter {

	private JComboBox<String> cytoCbox;
	private JComboBox<String> nucleiCbox;
	
	public CellposeAdapter() {
        super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setComponents(List<JComponent> components) {
		this.componentsGui = components;
		if (varNames != null && varNames.indexOf("Cytoplasm Color:") != -1) {
			this.cytoCbox = (JComboBox<String>) componentsGui.get(varNames.indexOf("Cytoplasm Color:"));
		}
		if (varNames != null && varNames.indexOf("Nuclei Color:") != -1) {
			this.nucleiCbox = (JComboBox<String>) componentsGui.get(varNames.indexOf("Nuclei Color:"));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setVarNames(List<String> componentNames) {
		this.varNames = componentNames;
		if (componentsGui != null && varNames.indexOf("Cytoplasm Color:") != -1) {
			this.cytoCbox = (JComboBox<String>) componentsGui.get(varNames.indexOf("Cytoplasm Color:"));
		}
		if (componentsGui != null && varNames.indexOf("Nuclei Color:") != -1) {
			this.nucleiCbox = (JComboBox<String>) componentsGui.get(varNames.indexOf("Nuclei Color:"));
		}
	}

	@Override
	protected void changeOnFocusGained(ImagePlus imp) {
		updateComboBox(imp, cytoCbox);
		updateComboBox(imp, nucleiCbox);
	}
	
	private void updateComboBox(ImagePlus imp, JComboBox<String> cbox) {
		if (cbox == null) return;
		if (imp == null) {
	        cbox.setModel(new DefaultComboBoxModel<>(CellposeGUI.ALL_LIST));
		} else if ((imp.getType() == ImagePlus.COLOR_RGB || imp.getNChannels() == 3) && cbox.getItemCount() != 2) {
	        cbox.setModel(new DefaultComboBoxModel<>(CellposeGUI.RGB_LIST));
		} else if (imp.getNChannels() == 1 && imp.getType() != ImagePlus.COLOR_RGB && cbox.getItemCount() != 1) {
	        cbox.setModel(new DefaultComboBoxModel<>(CellposeGUI.GRAYSCALE_LIST));
		} else if (imp.getNChannels() != 1 && imp.getNChannels() != 3) {
	        cbox.setModel(new DefaultComboBoxModel<>(CellposeGUI.ALL_LIST));
		}
	}

	@Override
	public void notifyParams(LinkedHashMap<String, String> args) {
		if (!Recorder.record)
			return;
		
		String macro = "run(\"DeepImageJ Cellpose\", \"";
		for (Entry<String, String> ee : args.entrySet()) {
			macro += ee.getKey() + "=[" + ee.getValue() + "] " ;
		}
		macro = macro.substring(0, macro.length() - 1) + "\");" + System.lineSeparator();
		Recorder.recordString(macro);
	}

}

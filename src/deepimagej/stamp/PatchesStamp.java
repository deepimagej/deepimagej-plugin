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
 * E. Gómez-de-Mariscal, C. García-López-de-Haro, L. Donati, M. Unser, A. Muñoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique fédérale de Lausanne (EPFL), Switzerland
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
package deepimagej.stamp;

import java.util.ArrayList;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.framework.SignatureDef;

import additionaluserinterface.GridPanel;
import deepimagej.Constants;
import deepimagej.components.HTMLPane;
import deepimagej.exceptions.IncorrectPatchSize;
import deepimagej.exceptions.TensorDimensionsException;
import deepimagej.BuildDialog;
import deepimagej.Parameters;
import ij.IJ;

public class PatchesStamp extends AbstractStamp {

	private ArrayList<String> tags;
	private ArrayList<String> graphs;
	
	JTextField cmbPatchs = new JTextField();
	JTextField cmbOverlap = new JTextField();
	
	public PatchesStamp(Parameters params, BuildDialog parent, String title) {
		super(params, parent, title);
		buildPanel();
	}
	
	public void buildPanel() {
				
		HTMLPane helpPatch = new HTMLPane(Constants.width/3, 100);
		helpPatch.append("h2", "Patch Size");
		helpPatch.append("p", "The default patch size is the one indicated.");
		
		HTMLPane helpOverlap = new HTMLPane(2*Constants.width/3, 100);
		helpOverlap.append("h2", "Overlap");
		helpOverlap.append("p", "The default overlap is the one indicated. You can change it as you want.");
		
		GridPanel pn = new GridPanel(false);
		pn.place(0, 0, helpPatch);
		pn.place(0, 1, cmbPatchs);
		pn.place(1, 0, helpOverlap);
		pn.place(1, 1, cmbOverlap);

		panel.add(pn);
	}
	
	public void init() {

		
		cmbPatchs.removeAll();
		cmbOverlap.removeAll();
		
		cmbPatchs.setText(Integer.toString(params.patch));
		cmbPatchs.setEditable(true);
		if (params.fixedPatch == true) {
			cmbPatchs.setEditable(false);
		}
		
		cmbOverlap.setText(Integer.toString(params.overlap));
		cmbOverlap.setEditable(true);
		
	}
	
	
	
	public void validate() throws NumberFormatException, IncorrectPatchSize {
		String patch = (String) cmbPatchs.getText();
		String overlap = (String) cmbOverlap.getText();
		params.patch = Integer.parseInt(patch);
		params.overlap = Integer.parseInt(overlap);
		
		// Check if the patch introduced by the user is a multiple of
		// the minimum patch size
		if (params.patch % Integer.parseInt(params.minimumSize) != 0) {
			throw new IncorrectPatchSize();
		}
		
	}

	
	public JPanel getPanel() {
		return panel;
	}
}

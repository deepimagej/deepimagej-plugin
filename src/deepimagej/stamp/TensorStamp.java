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

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


import deepimagej.components.HTMLPane;
import deepimagej.exceptions.IncorrectPatchSize;
import deepimagej.exceptions.InvalidTensorForm;
import deepimagej.BuildDialog;
import deepimagej.Parameters;
import deepimagej.ArrayOperations;
import deepimagej.tools.Index;

public class TensorStamp extends AbstractStamp {

	JPanel help_pn = new JPanel();
	JComboBox<String> in[] = new JComboBox[4];
	JComboBox<String> out[] = new JComboBox[5];
	JTextField patch_size = new JTextField();
	JCheckBox fixed_patch = new JCheckBox("Fixed patch size");
	JLabel patch_info = new JLabel("Patch info");
	private String c_in[] = {"N", "H", "W", "C"};
	private String c_out[] = {"N", "H", "W", "C", "D"};
	
	public TensorStamp(Parameters params, BuildDialog parent, String title) {
		super(params, parent, title);
		buildPanel();
	}
	
	public void buildPanel() {
		for(int i=0; i<in.length; i++) {
			in[i] = new JComboBox<String>(c_in);
			in[i].setSelectedIndex(i);/*
			} else if (i == 4) {
				in[i] = new JComboBox<String>();
				in[i].setEditable(true);
			}*/
		}
		
		
		for(int i=0; i<out.length; i++) {
			out[i] = new JComboBox<String>(c_out);
			out[i].setSelectedIndex(i);
		}
		
		JPanel information = new JPanel();
		information.setLayout(new GridLayout(2, 1));
		
		JPanel aux_panel = new JPanel();
		aux_panel.setLayout(new GridLayout(3, 1));
		

		HTMLPane helpTag = new HTMLPane(400,200);
		help_pn.add(helpTag);
		JPanel input_pn = new JPanel(new GridLayout(1,5));
		JPanel output_pn = new JPanel(new GridLayout(1,5));
		JPanel patch_pn = new JPanel(new GridLayout(1,2));
		JPanel patch_aux_pn = new JPanel(new GridLayout(1,2));
		
		
		input_pn.add(new JLabel("Input"));
		output_pn.add(new JLabel("Output"));
	
		for(int i=0; i<in.length; i++) {
			if (i != 4) {
			input_pn.add(in[i]);
			output_pn.add(out[i]);
			} else if (i == 4) {
				output_pn.add(out[i]);
			}
		}

		patch_aux_pn.add(fixed_patch);
		fixed_patch.setVisible(true);
		patch_aux_pn.add(patch_size);
		patch_size.setVisible(false);
		
		patch_pn.add(patch_aux_pn);
		patch_pn.add(patch_info);
		patch_info.setVisible(false);
		

		aux_panel.add(input_pn);
		aux_panel.add(output_pn);
		aux_panel.add(patch_pn);

		information.add(help_pn);
		information.add(aux_panel);
		
		panel.add(information, BorderLayout.CENTER);

	}
	
	
	public void init() {
		adaptStamp();
		HTMLPane helpTag = new HTMLPane(400,200);
		helpTag.setSize(400, 300);
		help_pn.add(helpTag);
		String help_text = helpTag.getText();
		if (help_text.isEmpty() == true) {
			helpTag.append("h2", "Tensor Dimesions");
			String input_string = dimensions(params.inDimensions);
			String output_string = dimensions(params.outDimensions);
			helpTag.append("p", "Input Tensor Dimensions : " + input_string);
			helpTag.append("p", "Input Tensor Dimensions : " + output_string);
		}
		
	}
	
	public String dimensions(int[] vec) {
		String string = "[";
		for (int i = 0; i < vec.length - 1; i ++) {
			string = string + vec[i] + ",";
		}
		string = string +  vec[vec.length - 1] + "]";
		return string;
	}
	
	public void adaptStamp() {
		// This method removes the unnecessary squares to indicate the dimensions
		int rank = params.inDimensions.length;
		if (rank < 4) {
			for (int i = 3; i >= rank ; i --) {
				in[i].setVisible(false);
			}
		}
		rank = params.outDimensions.length;
		if (rank < 5) {
			for (int i = 4; i >= rank ; i --) {
				out[i].setVisible(false);
			}
		}
		
		
	}
	
	
	public void checkPatchType() {
		params.fixedPatch = fixed_patch.isSelected();
		fixed_patch.setEnabled(false);
		patch_size.setVisible(true);
		patch_info.setVisible(true);
		params.card --;
		if (params.fixedPatch == true) {
			patch_info.setText("Introduce the fixed patch size");
		} else {
			patch_info.setText("Introduce the fixed minimum patch size");
		}
	} 
		
	
	
	public void validate() throws IncorrectPatchSize, NumberFormatException, InvalidTensorForm {
		if (fixed_patch.isEnabled() == true) {
			checkPatchType();
		} else {
			
			params.inputForm[0] = retrieveForm("input", params.inDimensions);
			checkRepeated(params.inputForm[0]);
			params.outputForm[0] = retrieveForm("output", params.outDimensions);
			checkRepeated(params.inputForm[0]);
			
			params.minimumSize = (String)patch_size.getText();
			if (params.minimumSize == null) {
				throw new IncorrectPatchSize();
			}
			int min_patch_size = Integer.parseInt(params.minimumSize);
			if (Integer.parseInt(params.minimumSize) < 0){
				throw new IncorrectPatchSize();
			}
			int[] overlap_size = ArrayOperations.patchOverlapVerification(min_patch_size, params.fixedPatch);
			params.patch = overlap_size[0]; 
			params.overlap = overlap_size[1];
		}
	}
	
	public String retrieveForm(String in_out, int[] tensor_dims) {
		JComboBox<String>[] focus;
		if (in_out == "input") {
			focus = in;
		} else {
			focus = out;
		}
		String dims = "";
		for (int i = 0; i < tensor_dims.length; i ++) {
			dims = dims + (String)focus[i].getSelectedItem();
		}
		return dims;
	}
	
	public void checkRepeated(String form) throws InvalidTensorForm {
		// This method checks if the form given by the user 
		// has not repeated dimensions. If it has them, it throws
		// an exception to alert the user.
		for (int pos = 0; pos < form.length(); pos ++) {
			int last_index = Index.lastIndexOf(form.split(""), form.split("")[pos]);
			if (last_index != pos) {
				throw new InvalidTensorForm();
			}
		}
	}
	
	public void resetPanel() {
		fixed_patch.setEnabled(true);
		patch_size.setVisible(false);
		patch_info.setVisible(false);
		help_pn.removeAll();
		
		for (int i = 0; i < in.length; i ++) {
			in[i].setVisible(true);
			out[i].setVisible(true);
		}
		out[4].setVisible(true);
		
	}
	
	
}

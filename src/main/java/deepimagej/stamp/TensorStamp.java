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
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, L. Donati, M. Unser, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
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

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.TensorFlowModel;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import ij.IJ;

public class TensorStamp extends AbstractStamp  {

	private List<JComboBox<String>>	cmbList	= new ArrayList<>();
	private String[]				in			= { "N", "H", "W", "C", "D" };
	private String[]				out			= { "N", "H", "W", "C", "D" };
	private HTMLPane				pnDim;
	private JPanel					pn 			= new JPanel();
	private JPanel 					pnInOut 	= new JPanel();
	private int						iterateOverComboBox;
	private boolean					reinitialise = false;

	public TensorStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {
		HTMLPane info = new HTMLPane(Constants.width, 60);
		info.append("h2", "Tensor Organization");
		info.append("p", "Each dimension of input and output tensors must"
				+ " be specified so the image is processed correctly, i.e. "
				+ "the first dimension of the input tensor corresponds to the batch size, "
				+ "the second dimension to the width and so on.");

		JPanel information = new JPanel();
		information.setLayout(new GridLayout(2, 1));

		
		pnInOut.setBorder(BorderFactory.createEtchedBorder());
		//pnInOut.setLayout(new GridLayout(2, 1));
		pnDim = new HTMLPane(Constants.width, 120);

		JPanel pnInput = new JPanel(new GridLayout(1, 5));
		JPanel pnOutput = new JPanel(new GridLayout(1, 5));

		pnInOut.add(pnInput);
		pnInOut.add(pnOutput);

		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(pnDim.getPane());
		pn.add(pnInOut);
		panel.add(pn);
	}
	
	@Override
	public void init() {
		if (reinitialise == false) {
			Parameters params = parent.getDeepPlugin().params;
			pnInOut.removeAll();
			List<DijTensor> inputs = params.inputList;
			List<DijTensor> outputs = params.outputList;
			int n_tensors = inputs.size() + outputs.size();
			pnInOut.setLayout(new GridLayout(n_tensors, 1));
			// Reinitialize the list
			cmbList = new ArrayList<>();
			
			for (int i = 0; i < inputs.size(); i ++) {
				JPanel pnInput = new JPanel(new GridLayout(1, inputs.get(i).tensor_shape.length));
				pnInput.add(new JLabel(inputs.get(i).name));
				for (int j = 0; j < inputs.get(i).tensor_shape.length; j ++) {
					JComboBox<String> cmbIn = new JComboBox<String>(in);
					/*if (params.inputList.get(i).form != null) {
						cmbIn.setSelectedIndex(Index.indexOf(in, params.inputList.get(i).form[i]));
					}*/
					pnInput.add(cmbIn);
					cmbList.add(cmbIn);
				}
				pnInOut.add(pnInput);
				int a = 1+1;
			}
	
			for (int i = 0; i < outputs.size(); i ++) {
				JPanel pnOutput = new JPanel(new GridLayout(1, outputs.get(i).tensor_shape.length));
				pnOutput.add(new JLabel(outputs.get(i).name));
				for (int j = 0; j < outputs.get(i).tensor_shape.length; j ++) {
					JComboBox<String> cmbOut = new JComboBox<String>(out);
					/*if (params.outputList.get(i).form != null) {
						cmbOut.setSelectedIndex(Index.indexOf(in, params.outputList.get(i).form));
					}*/
					pnOutput.add(cmbOut);
					cmbList.add(cmbOut);
				}
				pnInOut.add(pnOutput);
			}
			
			File file = new File(parent.getDeepPlugin().params.path2Model);
			String dirname = "untitled";
			if (file.exists())
				dirname = file.getName();
	
			pnDim.clear();
			pnDim.append("h2", "Tensor organization of " + dirname);
			pnDim.append("i", "W for width (axis X), H for height (axis Y), N for batch, C for channel");
			for (int i = 0; i < inputs.size(); i ++) {
				pnDim.append("p", inputs.get(i).name + " Tensor Dimensions : " + dimensions(inputs.get(i).tensor_shape));
			}
			for (int i = 0; i < outputs.size(); i ++) {
				pnDim.append("p", outputs.get(i).name + " Tensor Dimensions : " + dimensions(outputs.get(i).tensor_shape));
			}
		}
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		List<DijTensor> inputs = params.inputList;
		iterateOverComboBox = 0;
		for (int i = 0; i < inputs.size(); i ++) {
			inputs.get(i).form = retrieveForm(inputs.get(i).name, inputs.get(i).tensor_shape, iterateOverComboBox);
			iterateOverComboBox = iterateOverComboBox + inputs.get(i).tensor_shape.length;
			if (checkRepeated(inputs.get(i).form) == false) {
				IJ.error("Repetition is not allower in input");
				return false;
			}
			if (TensorFlowModel.nBatch(inputs.get(i).tensor_shape, inputs.get(i).form).equals("1") == false){
				IJ.error("The plugin only supports models with batch size (N) = 1");
				reinitialise = true;
				return false;
			}
		}
		List<DijTensor> outputs = params.outputList;
		for (int i = 0; i < outputs.size(); i ++) {
			outputs.get(i).form = retrieveForm(outputs.get(i).name, outputs.get(i).tensor_shape, iterateOverComboBox);
			iterateOverComboBox = iterateOverComboBox + outputs.get(i).tensor_shape.length;
			if (checkRepeated(outputs.get(i).form) == false) {
				IJ.error("Repetition is not allower in input");
				return false;
			}
			if (TensorFlowModel.nBatch(outputs.get(i).tensor_shape, outputs.get(i).form).equals("1") == false){
				IJ.error("The plugin only supports models with batch size (N) = 1");
				reinitialise = true;
				return false;
			}
		}
		reinitialise = false;
		
		return true;
	}

	private String dimensions(int[] vec) {
		String string = "[";
		for (int i = 0; i < vec.length - 1; i++) {
			string = string + vec[i] + ",";
		}
		string = string + vec[vec.length - 1] + "]";
		return string;
	}

	private String retrieveForm(String in_out, int[] tensor_dims, int start) {
		
		String dims = "";
		JComboBox<String> focus = new JComboBox<String>();
		for (int i = start; i < start + tensor_dims.length; i++) {
			focus = cmbList.get(i);
			dims = dims + (String) focus.getSelectedItem();
		}
		return dims;
	}

	private boolean checkRepeated(String form) {
		// This method checks if the form given by the user
		// has not repeated dimensions. If it has them, it throws
		// an exception to alert the user.
		for (int pos = 0; pos < form.length(); pos++) {
			int last_index = Index.lastIndexOf(form.split(""), form.split("")[pos]);
			if (last_index != pos) {
				reinitialise = true;
				return false;
			}
		}
		return true;
	}
}

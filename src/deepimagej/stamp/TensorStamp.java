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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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
import deepimagej.tools.Index;
import ij.IJ;

public class TensorStamp extends AbstractStamp  {

	private JComboBox<String>	cmbIn[]		= new JComboBox[4];
	private JComboBox<String>	cmbOut[]		= new JComboBox[5];
	private String				in[]		= { "N", "H", "W", "C" };
	private String				out[]		= { "N", "H", "W", "C", "D" };
	private HTMLPane				pnDim;

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
		for (int i = 0; i < in.length; i++) {
			cmbIn[i] = new JComboBox<String>(in);
			cmbIn[i].setSelectedIndex(i);
		}

		for (int i = 0; i < out.length; i++) {
			cmbOut[i] = new JComboBox<String>(out);
			cmbOut[i].setSelectedIndex(i);
		}

		JPanel information = new JPanel();
		information.setLayout(new GridLayout(2, 1));

		JPanel pnInOut = new JPanel();
		pnInOut.setBorder(BorderFactory.createEtchedBorder());
		pnInOut.setLayout(new GridLayout(2, 1));
		pnDim = new HTMLPane(Constants.width, 120);

		JPanel pnInput = new JPanel(new GridLayout(1, 5));
		JPanel pnOutput = new JPanel(new GridLayout(1, 5));
		pnInput.add(new JLabel("Input"));
		pnOutput.add(new JLabel("Output"));
		for (int i = 0; i < in.length; i++) {
			if (i != 4) {
				pnInput.add(cmbIn[i]);
				pnOutput.add(cmbOut[i]);
			}
			else if (i == 4) {
				pnOutput.add(cmbOut[i]);
			}
		}
		pnInOut.add(pnInput);
		pnInOut.add(pnOutput);

		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(pnDim.getPane());
		pn.add(pnInOut);
		panel.add(pn);
	}
	
	@Override
	public void init() {
		Parameters params = parent.getDeepPlugin().params;
		int rank = params.inDimensions.length;
		if (rank < 4) {
			for (int i = 3; i >= rank; i--)
				cmbIn[i].setVisible(false);
		} else if (rank == 4) {
			for (int i = 0; i < rank; i ++)
				cmbIn[i].setVisible(true);
		}
		rank = params.outDimensions.length;
		if (rank < 5) {
			for (int i = 4; i >= rank; i--)
				cmbOut[i].setVisible(false);
		} else if (rank == 4) {
			for (int i = 0; i < rank; i ++)
				cmbOut[i].setVisible(true);
		}
		File file = new File(parent.getDeepPlugin().params.path2Model);
		String dirname = "untitled";
		if (file.exists())
			dirname = file.getName();

		pnDim.clear();
		pnDim.append("h2", "Tensor organization of " + dirname);
		pnDim.append("i", "W for width (axis X), H for height (axis Y), N for batch, C for channel");
		pnDim.append("p", "Input Tensor Dimensions : " + dimensions(params.inDimensions));
		pnDim.append("p", "Input Tensor Dimensions : " + dimensions(params.outDimensions));
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		params.inputForm[0] = retrieveForm("input", params.inDimensions);
		if (checkRepeated(params.inputForm[0]) == false) {
			IJ.error("Repetition is not allower in input");
			return false;
		}
		params.outputForm[0] = retrieveForm("output", params.outDimensions);
		if (checkRepeated(params.outputForm[0]) == false) {
			IJ.error("Repetition is not allower in input");
			return false;
		}
		if (TensorFlowModel.nBatch(params.inDimensions, params.inputForm[0]).equals("1") == false){
			IJ.error("The plugin only supports models with batch size (N) = 1");
			return false;
		}
		if (TensorFlowModel.nBatch(params.outDimensions, params.outputForm[0]).equals("1") == false){
			IJ.error("The plugin only supports models with batch size (N) = 1");
			return false;
		}
		
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

	private String retrieveForm(String in_out, int[] tensor_dims) {
		JComboBox<String>[] focus = in_out == "input" ? cmbIn : cmbOut;
		String dims = "";
		for (int i = 0; i < tensor_dims.length; i++)
			dims = dims + (String) focus[i].getSelectedItem();
		return dims;
	}

	private boolean checkRepeated(String form) {
		// This method checks if the form given by the user
		// has not repeated dimensions. If it has them, it throws
		// an exception to alert the user.
		for (int pos = 0; pos < form.length(); pos++) {
			int last_index = Index.lastIndexOf(form.split(""), form.split("")[pos]);
			if (last_index != pos) {
				return false;
			}
		}
		return true;
	}
}

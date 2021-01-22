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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.DeepLearningModel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import ij.IJ;

public class TensorStamp extends AbstractStamp implements ActionListener {

	private List<JComboBox<String>>	inputs;
	private static List<JComboBox<String>>	outputs;
	private List<JComboBox<String>>	inTags;
	private static List<JComboBox<String>>	outTags;
	private String[]				in			= { "B", "Y", "X", "C", "Z" };
	private String[]				outPyramidal= { "B", "Y", "X", "C", "N/i/z" };
	private String[]				inputOptions= { "image", "parameter"};
	private static String[]			outOptions	= { "image", "list", "ignore"};
	private HTMLPane				pnDim;
	private JPanel					pn 			= new JPanel();
	private JPanel 					pnInOut 	= new JPanel();
	private int						iterateOverComboBox;
	private String					model		  = "";
	private boolean					pyramidal = false;

	public TensorStamp(BuildDialog parent) {
		super(parent);
	}

	@Override
	public void buildPanel() {
		HTMLPane info = new HTMLPane(Constants.width, 100);
		info.append("h2", "Tensor Organization");
		info.append("p", "Each dimension of input and output tensors must"
				+ " be specified to process  the image correctly, i.e. "
				+ "the first dimension of the input tensor corresponds to the batch size, "
				+ "the second dimension to the width and so on.<br>"
				+ "<b>Note that for the moment DeepImageJ only supports BATCH_SIZE = 1.</b>");
		info.setMaximumSize(new Dimension(Constants.width, 100));
		
		//pnInOut.setBorder(BorderFactory.createEtchedBorder());

		Parameters params = parent.getDeepPlugin().params;
		pnInOut.removeAll();
		List<DijTensor> inputTensors = params.totalInputList;
		List<DijTensor> outputTensors = params.totalOutputList;

		// Set the correct information about each tensor
		pnDim= new HTMLPane(Constants.width, 250);
		File file = new File(parent.getDeepPlugin().params.path2Model);
		String dirname = "untitled";
		if (file.exists())
			dirname = file.getName();
		pnDim.append("h2", "Input/Output tensor dimensions of " + dirname);
		// Save the model we are using to build the interface to check if
		// we need to rebuild the panel or not. Same for pyramidal.
		model = params.path2Model;
		pyramidal = params.pyramidalNetwork;
		
		for (DijTensor tensor : inputTensors) 
			pnDim.append("<p><b>Input tensor --> </b>" +  tensor.name + " : " + Arrays.toString(tensor.tensor_shape) + "</p>");
		for (DijTensor tensor : outputTensors) 
			pnDim.append("<p><b>Output tensor --> </b>" +  tensor.name + " : " + Arrays.toString(tensor.tensor_shape) + "</p>");
		
		pnDim.append("<br>");
		pnDim.append("<p><b>Input tensor types:</b></p>");
		pnDim.append("<ul><li><p><b>Image:</b> <b>X</b> for width (axis X), <b>Y</b> for"
				+ " height (axis Y), <b>Z</b> for depth (axis Z), <b>B</b> for batch, "
				+ "<b>C</b> for channel. There can only be one input image.</p></li>");
		pnDim.append("<li><p><b>Parameter:</b> the tensor must be created using Java preprocessing,"
				+ " thus no dimension specfication is needed. The tensor is fed directly "
				+ "to the model from preprocessing.</p></li></ul>");
		
		pnDim.append("<p><b>Output tensor types:</b></p>");
		if (params.pyramidalNetwork) {
			pnDim.append("<ul><li><p><b>Image:</b> <b>X</b> for width (axis X), <b>Y</b> for"
					+ " height (axis Y), <b>N/i/z</b> for number of components/patches/objects or depth (axis Z), <b>B</b> for batch, "
					+ "<b>C</b> for channel or class</p></li>");
		} else {
			pnDim.append("<ul><li><p><b>Image:</b> <b>X</b> for width (axis X), <b>Y</b> for"
					+ " height (axis Y), <b>Z</b> for depth (axis Z), <b>B</b> for batch, "
					+ "<b>C</b> for channel</p></li>");
		}
		pnDim.append("<li><p><b>List:</b> the tensor corresponds to a batch of matrices."
				+ "<b>R</b> for rows, <b>C</b> for columns, <b>B</b> for batch. This type can be used for tensors"
				+ "with 3 dimensions at most (being one of them the batch).</p></li>");
		pnDim.append("<li><p><b>Ignore:</b> the tensor will not be fetched from the model "
				+ "by the plugin.</p></li></ul>");

		pnDim.setMaximumSize(new Dimension(Constants.width, 250));
		//JPanel pnInput = new JPanel(new GridLayout(1, 5));
		GridBagConstraints cTag = new GridBagConstraints ();
		cTag.gridwidth = 3;
		cTag.gridx = 0;
		cTag.insets = new Insets(3, 5, 3, 5);
		GridBagConstraints cLabel = new GridBagConstraints ();
		cLabel.gridwidth = 3;
		cLabel.gridx = 3;
		cLabel.insets = new Insets(3, 5, 3, 5);
		
		int nTensors = 0;
		inTags = new ArrayList<>();
		outTags = new ArrayList<>();
		inputs = new ArrayList<>();
		outputs = new ArrayList<>();
		for (DijTensor input : inputTensors) {
			// Create the panel that will contain all the elements for a tensor
			JPanel pnTensor = new JPanel(new GridBagLayout());
			// Add the combo box to decide the type of input
			JComboBox<String> cmbInType = new JComboBox<String>(inputOptions);
			cmbInType.addActionListener(this);
			pnTensor.add(cmbInType, cTag);
			inTags.add(cmbInType);
			// Add the name
			pnTensor.add(new JLabel(input.name), cLabel);
			// Now add the tensor specific dimensions
			for (int j = 0; j < input.tensor_shape.length; j ++) {
				JComboBox<String> cmbIn = new JComboBox<String>(in);
				cmbIn.setPreferredSize(new Dimension(50, 50));
				pnTensor.add(cmbIn);
				inputs.add(cmbIn);
			}
			pnInOut.add(pnTensor);
			nTensors ++;
		}
		
		
		for (DijTensor output : outputTensors) {
			// Create the panel that will contain all the elements for a tensor
			JPanel pnTensor = new JPanel(new GridBagLayout());
			// Add the combo box to decide the type of input
			JComboBox<String> cmbOutType = new JComboBox<String>(outOptions);
			cmbOutType.addActionListener(this);
			pnTensor.add(cmbOutType, cTag);
			outTags.add(cmbOutType);
			// Add the name
			pnTensor.add(new JLabel(output.name), cLabel);
			// Now add the tensor specific dimensions
			for (int j = 0; j < output.tensor_shape.length; j ++) {
				JComboBox<String> cmbOut = new JComboBox<String>(params.pyramidalNetwork ? outPyramidal : in);
				cmbOut.setPreferredSize(new Dimension(50, 50));
				pnTensor.add(cmbOut);
				outputs.add(cmbOut);
			}
			pnInOut.add(pnTensor);
			nTensors ++;
		}
		JScrollPane scroll = new JScrollPane();
		pnInOut.setPreferredSize(new Dimension(500, nTensors * 60));
        scroll.setPreferredSize(new Dimension(600, nTensors * 70 + 50));
        scroll.setViewportView(pnInOut);
		pn.removeAll();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(pnDim.getPane());
		pn.add(scroll);
		panel.add(pn);
		
		
	}
	
	@Override
	public void init() {
		String modelOfInterest = parent.getDeepPlugin().params.path2Model;
		if (!modelOfInterest.equals(model) || pyramidal != parent.getDeepPlugin().params.pyramidalNetwork) {
			buildPanel();
		}
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		// Parameter to make sure only one tensor corresponds to the
		// image type
		// TODO support several image inputs
		boolean image = false;
		// Reset 'allowPatching' parameter to its default value (true)
		if (!params.pyramidalNetwork)
			params.allowPatching = true;
		params.inputList = new ArrayList<DijTensor>();
		List<DijTensor> inputTensors = params.totalInputList;
		iterateOverComboBox = 0;
		int tagC = 0;
		for (DijTensor tensor : inputTensors) {
			tensor.form = "";
			for (int i = iterateOverComboBox; i < iterateOverComboBox + tensor.tensor_shape.length; i++)
				tensor.form = tensor.form + (String) inputs.get(i).getSelectedItem();
			tensor.tensorType = (String) inTags.get(tagC ++).getSelectedItem();
			// TODO accept more than one input image
			if (!image && tensor.tensorType.contains("image")) {
				image = true;
			} else if (tensor.tensorType.contains("image")) {
				IJ.error("The current DeepImageJ version only admits on input image tensor.");
				return false;
			}
			iterateOverComboBox += tensor.tensor_shape.length;
			if (checkRepeated(tensor.form) == false && tensor.tensorType.equals("parameter") == false) {
				IJ.error("Dimension repetition is not allowed");
				return false;
			}
			if (DeepLearningModel.nBatch(tensor.tensor_shape, tensor.form).equals("1") == false && tensor.tensorType.equals("ignore") == false){
				IJ.error("The plugin only supports models with batch size (N) = 1");
				return false;
			}
			params.inputList.add(tensor);
		}
		params.outputList = new ArrayList<DijTensor>();
		List<DijTensor> outputTensors = params.totalOutputList;
		tagC = 0;
		iterateOverComboBox = 0;
		for (DijTensor tensor : outputTensors) {
			tensor.form = "";
			for (int i = iterateOverComboBox; i < iterateOverComboBox + tensor.tensor_shape.length; i++) {
				String dimensionLetter = (String) outputs.get(i).getSelectedItem();
				dimensionLetter = dimensionLetter.toLowerCase().contains("z") ? "Z" : dimensionLetter;
				tensor.form = tensor.form + dimensionLetter;
			}
			tensor.auxForm = tensor.form;
			iterateOverComboBox += tensor.tensor_shape.length;
			tensor.tensorType = (String) outTags.get(tagC ++).getSelectedItem();
			if (tensor.tensorType.contains("list")) {
				params.allowPatching = false;
			}
			if (checkRepeated(tensor.form) == false && tensor.tensorType.equals("ignore") == false) {
				IJ.error("Dimension repetition is not allowed");
				return false;
			}
			if (DeepLearningModel.nBatch(tensor.tensor_shape, tensor.form).equals("1") == false && tensor.tensorType.equals("ignore") == false){
				IJ.error("The plugin only supports models with batch size (B) = 1");
				return false;
			}
		}
		for (Iterator<DijTensor> iter = outputTensors.listIterator(); iter.hasNext(); ) {
			DijTensor tensor = iter.next();
		    if (!tensor.tensorType.contains("ignore")) {
				params.outputList.add(tensor);
		    }
		}
		if (!image) {
			IJ.error("The model must have at least 1 input image.");
			return false;
		}
		if (params.outputList.size() < 1) {
			IJ.error("The model must have at least 1 output.");
			return false;
		}
		
		return true;
	}
	
	/*
	 * Change the letter in each Jcombobox depending on the selected tensor type.
	 */
	public void updateTensorDisplay(Parameters params) {
		// Set disabled the tensors marked as 'parameter'
		List<DijTensor> inputTensors = params.totalInputList;
		// Counter for tensors
		int cIn = 0;
		int cmbCounterIn = 0;
		for (JComboBox<String> cmbTag : inTags) {
			int indSelection = cmbTag.getSelectedIndex();
			String selection = inputOptions[indSelection];
			for (int i = cmbCounterIn; i < cmbCounterIn + inputTensors.get(cIn).tensor_shape.length; i++) {
				if (selection.contains("parameter") && inputs.get(i).getItemAt(0).equals("B")) {
					inputs.get(i).removeAllItems();
					inputs.get(i).addItem("-");
					inputs.get(i).setEnabled(false);
					String form = inputTensors.get(cIn).form;
					if (form != null && !form.contentEquals("")) {
						inputTensors.get(cIn).form = "";
					}
				} else if (selection.contains("image") && inputs.get(i).getItemAt(0).equals("-")) {
					inputs.get(i).removeAllItems();
					inputs.get(i).addItem("B");
					inputs.get(i).addItem("Y");
					inputs.get(i).addItem("X");
					inputs.get(i).addItem("C");
					inputs.get(i).addItem("Z");
					inputs.get(i).setEnabled(true);
					String form = inputTensors.get(cIn).form;
					if (form != null && !form.contentEquals("")) {
						inputTensors.get(cIn).form = "";
					}
				}
			}
			cmbCounterIn += inputTensors.get(cIn).tensor_shape.length;
			cIn ++;
		}
		// Set disabled the tensors marked as 'ignore'
		List<DijTensor> outputTensors = params.totalOutputList;
		// Counter for tensors
		int c = 0;
		int cmbCounter = 0;
		for (JComboBox<String> cmbTag : outTags) {
			int indSelection = cmbTag.getSelectedIndex();
			String selection = outOptions[indSelection];
			for (int i = cmbCounter; i < cmbCounter + outputTensors.get(c).tensor_shape.length; i++) {
				if (selection.contains("ignore")) {
					outputs.get(i).setEnabled(!selection.equals("ignore"));
				} else if (selection.contains("list") && outputs.get(i).getItemAt(1).equals("Y")) {
					outputs.get(i).removeAllItems();
					outputs.get(i).addItem("B");
					outputs.get(i).addItem("R");
					outputs.get(i).addItem("C");
					outputs.get(i).setEnabled(true);
					outputs.get(i).addActionListener(this);
					String form = outputTensors.get(c).form;
					if (form != null && !form.contentEquals(""))
						outputTensors.get(c).form = "";
				} else if (selection.contains("list") && !outputs.get(i).isEnabled()) {
					outputs.get(i).setEnabled(true);
				} else if (selection.contains("image") && outputs.get(i).getItemAt(1).equals("R") && !params.pyramidalNetwork) {
					outputs.get(i).removeAllItems();
					outputs.get(i).addItem("B");
					outputs.get(i).addItem("Y");
					outputs.get(i).addItem("X");
					outputs.get(i).addItem("C");
					outputs.get(i).addItem("Z");
					outputs.get(i).setEnabled(true);
					outputs.get(i).addActionListener(this);
					String form = outputTensors.get(c).form;
					if (form != null && !form.contentEquals(""))
						outputTensors.get(c).form = "";
				} else if (selection.contains("image") && outputs.get(i).getItemAt(1).equals("R") && !params.pyramidalNetwork) {
					outputs.get(i).removeAllItems();
					outputs.get(i).addItem("B");
					outputs.get(i).addItem("Y");
					outputs.get(i).addItem("X");
					outputs.get(i).addItem("C");
					outputs.get(i).addItem("N/i/z");
					outputs.get(i).setEnabled(true);
					outputs.get(i).addActionListener(this);
					String form = outputTensors.get(c).form;
					if (form != null && !form.contentEquals(""))
						outputTensors.get(c).form = "";
				} else if (selection.contains("image") && !outputs.get(i).isEnabled()) {
					outputs.get(i).setEnabled(true);
				}
			}
			cmbCounter += outputTensors.get(c).tensor_shape.length;
			c ++;
		}
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
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Parameters params = parent.getDeepPlugin().params;
		updateTensorDisplay(params);
	}
}

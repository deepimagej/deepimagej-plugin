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

import org.tensorflow.SavedModelBundle;
import org.tensorflow.framework.SignatureDef;

import additionaluserinterface.GridPanel;
import deepimagej.Constants;
import deepimagej.components.HTMLPane;
import deepimagej.exceptions.TensorDimensionsException;
import deepimagej.BuildDialog;
import deepimagej.DeepPlugin;
import deepimagej.Parameters;
import ij.IJ;

public class ModelStamp extends AbstractStamp {

	private ArrayList<String> tags;
	private ArrayList<String> graphs;
	
	private JComboBox<String> cmbTags = new JComboBox<String>();
	private JComboBox<String> cmbGraphs = new JComboBox<String>();
	
	public ModelStamp(Parameters params, BuildDialog parent, String title) {
		super(params, parent, title);
		tags = new ArrayList<String>();
		graphs = new ArrayList<String>();
		tags.add("Server");	
		buildPanel();
	}
	
	public void buildPanel() {
				
		HTMLPane helpTag = new HTMLPane(Constants.width/3, 100);
		helpTag.append("h2", "Model Tag Constant");
		helpTag.append("p", "Tag used to save the TensorFlow "
				+ "SavedModel. If the tag plugin cannot "
				+ "automatically find the tag, you will need to "
				+ "introduce it.");
		
		HTMLPane helpGraph = new HTMLPane(2*Constants.width/3, 100);
		helpGraph.append("h2", "SignatureDef map constant");
		helpGraph.append("p", "Tag used to call the wanted model"
				+ " graph. There might be more than one in the "
				+ "same model folder.");
		
		GridPanel pn = new GridPanel(false);
		pn.place(0, 0, helpTag);
		pn.place(0, 1, cmbTags);
		pn.place(1, 0, helpGraph);
		pn.place(1, 1, cmbGraphs);

		panel.add(pn);
	}
	
	public void init() {
		cmbTags.removeAllItems();
		cmbGraphs.removeAllItems();
		if (params.tag != null) {
			cmbTags.addItem(params.tag);
			cmbTags.setEditable(false);
			cmbGraphs.setEnabled(true);
			for (int i = 0; i < params.graphSet.size(); i ++) {
				cmbGraphs.addItem((String) params.graphSet.toArray()[i]);
				cmbGraphs.setEditable(false);
			}
		} else if (params.tag == null) {
			cmbTags.addItem("");
			cmbTags.setEditable(true);
			cmbGraphs.addItem("");
			cmbGraphs.setEditable(false);
			cmbGraphs.setEnabled(false);
		}
	}
	
	
	public void validate(DeepPlugin dp) throws TensorDimensionsException {
		// Check the dimensions of the input and output tensors
		//params.tag = (String)cmbTags.getSelectedItem();
		params.graph = (String)cmbGraphs.getSelectedItem();
		if (params.graph.equals("") == false) {
			Object[] dimensions;
			dimensions = DeepPlugin.retrieveInputOutputDims(dp.getModel(), params.graph);
			params.inDimensions = (int[]) dimensions[0];
			params.outDimensions = (int[]) dimensions[1];
			params.inputs = (String[]) dimensions[2];
			params.outputs = (String[]) dimensions[3];
		}
	}

	
	public JPanel getPanel() {
		return panel;
	}

	public void getTag(DeepPlugin dp) {
		// Method used to check if the model tag introduced by the user 
		// is correct or not
		params.tag = (String)cmbTags.getSelectedItem();
		// Try to load the model with the given tag
		dp.setModel(DeepPlugin.loadModel(params.path2Model, params.tag));
		if (dp.getModel() != null) {
			params.graphSet = DeepPlugin.metaGraphsSet(dp.getModel());
			cmbGraphs.removeAllItems();
			cmbGraphs.setEnabled(true);
			for (int i = 0; i < params.graphSet.size(); i ++) {
				cmbGraphs.addItem((String) params.graphSet.toArray()[i]);
				cmbGraphs.setEditable(false);
			}
		} else {
			IJ.error("The tag introduced was incorrect");
		}
		
	}
}

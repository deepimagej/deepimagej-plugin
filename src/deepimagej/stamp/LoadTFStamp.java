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
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.framework.SignatureDef;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepPlugin;
import deepimagej.Parameters;
import deepimagej.TensorFlowModel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.Log;
import ij.IJ;

public class LoadTFStamp extends AbstractStamp implements ActionListener, Runnable {

	private ArrayList<String>	tags;
	private JButton				bnArchi			= new JButton("Show the Network Architecture");
	private JComboBox<String>	cmbTags			= new JComboBox<String>();
	private JComboBox<String>	cmbGraphs		= new JComboBox<String>();
	private ArrayList<String[]>	architecture	= new ArrayList<String[]>();
	private String				name;

	private HTMLPane				pnLoad;

	public LoadTFStamp(BuildDialog parent) {
		super(parent);
		tags = new ArrayList<String>();
		tags.add("Serve");
		buildPanel();
	}

	@Override
	public void buildPanel() {
		pnLoad = new HTMLPane(Constants.width, 70);

		HTMLPane pnTag = new HTMLPane(Constants.width / 2, 70);
		pnTag.append("h2", "Model Tag");
		pnTag.append("p", "Tag used to save the TensorFlow SavedModel. If the tag plugin cannot automatically find , you will need to edit it.");

		HTMLPane pnGraph = new HTMLPane(2 * Constants.width / 2, 70);
		pnGraph.append("h2", "SignatureDef");
		pnGraph.append("p", "SignatureDef used to call the wanted model graph. There might be more than one in the same model folder.");

		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(pnTag.getPane());
		pn.add(cmbTags);
		pn.add(pnGraph.getPane());
		pn.add(cmbGraphs);
		pn.add(bnArchi);
		bnArchi.addActionListener(this);
		JPanel main = new JPanel(new BorderLayout());
		main.add(pnLoad.getPane(), BorderLayout.CENTER);
		main.add(pn, BorderLayout.SOUTH);
		panel.add(main);
	}

	@Override
	public void init() {
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	
	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		if (params.tag == null) {
			Log log = new Log();
			String tag = (String)cmbTags.getSelectedItem();
			try {	
				ArrayList<String> msgLoad = new ArrayList<String>();
				SavedModelBundle model = TensorFlowModel.load(params.path2Model, tag, log, msgLoad, architecture);
				parent.getDeepPlugin().setModel(model);
				params.tag = tag;
				cmbTags.setEditable(false);
				for (String m : msgLoad)
					pnLoad.append("p", m);
				parent.getDeepPlugin().setModel(model);
				pnLoad.append("p", "Architecture Network: " + architecture.size() + " ops");
				params.graphSet = TensorFlowModel.metaGraphsSet(model);
				if (params.graphSet.size() > 0) {
					Set<String> tfGraphSet = TensorFlowModel.returnTfSig(params.graphSet);
					cmbGraphs.removeAllItems();
					for (int i = 0; i < params.graphSet.size(); i++) {
						cmbGraphs.addItem((String) tfGraphSet.toArray()[i]);
						cmbGraphs.setEditable(false);
					}
				}
				
			}
			catch (Exception e) {
				IJ.error("Incorrect ModelTag");
				params.tag = null;
				cmbTags.removeAllItems();
				cmbTags.setEditable(true);
			}
			return false;
		} else {
			SavedModelBundle model = parent.getDeepPlugin().getModel();
			params.graph = TensorFlowModel.returnStringSig((String)cmbGraphs.getSelectedItem());
			SignatureDef sig = TensorFlowModel.getSignatureFromGraph(model, params.graph);
			params.outputs = TensorFlowModel.returnOutputs(sig);
			pnLoad.append("p", "Number of output: " + params.outputs.length);
			boolean valid = true;
			try {
				Object[] dimensions = TensorFlowModel.getDimensions(model, params.graph);
				params.inDimensions = (int[]) dimensions[0];
				params.outDimensions = (int[]) dimensions[1];
				params.inputs = (String[]) dimensions[2];
				params.outputs = (String[]) dimensions[3];
				pnLoad.append("p", "Dimension of input: " + params.inDimensions.length + " and output: " + params.outDimensions.length);
	
			}
			catch (Exception ex) {
				pnLoad.append("p", "Dimension: ERROR");
				valid  = false;
				parent.setEnabledBackNext(valid);
				return false;
			}
			parent.setEnabledBackNext(valid);
			return true;
		}
	}

	public void run() {
		Parameters params = parent.getDeepPlugin().params;
		cmbTags.removeAllItems();
		cmbGraphs.removeAllItems();
		architecture.clear();
		pnLoad.clear();
		File file = new File(params.path2Model);
		if (file.exists())
			name = file.getName();

		pnLoad.append("h2", "Load " + name);
		ArrayList<String> msg = new ArrayList<String>();
		boolean valid = TensorFlowModel.check(params.path2Model, msg);
		for (String m : msg)
			pnLoad.append("p", m);

		Log log = new Log();
		params.tag = null;

		if (valid) {
			Object[] info = TensorFlowModel.findTag(params.path2Model);
			String tag = (String) info[0];
			if (tag != null) {
				params.tag = tag;
				String tfTag = TensorFlowModel.returnTfTag(tag);
				cmbTags.addItem(tfTag);
				cmbTags.setEditable(false);
				ArrayList<String> msgLoad = new ArrayList<String>();
				SavedModelBundle model = TensorFlowModel.load(params.path2Model, params.tag, log, msgLoad, architecture);
				for (String m : msgLoad)
					pnLoad.append("p", m);
				parent.getDeepPlugin().setModel(model);
				pnLoad.append("p", "Architecture Network: " + architecture.size() + " ops");
				params.graphSet = TensorFlowModel.metaGraphsSet(model);
				if (params.graphSet.size() > 0) {
					Set<String> tfGraphSet = TensorFlowModel.returnTfSig(params.graphSet);
					for (int i = 0; i < params.graphSet.size(); i++) {
						cmbGraphs.addItem((String) tfGraphSet.toArray()[i]);
						cmbGraphs.setEditable(false);
					}
				}
			} else {
				cmbTags.addItem("");
				cmbTags.setEditable(true);
				cmbGraphs.addItem("");
				cmbGraphs.setEditable(false);
			}
		}
		parent.setEnabledBackNext(valid);
	} 

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnArchi)
			TensorFlowModel.showArchitecture(name, architecture);

	}
}

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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.tensorflow.SavedModelBundle;

import additionaluserinterface.GridPanel;
import deepimagej.Constants;
import deepimagej.components.HTMLPane;
import deepimagej.BuildDialog;
import deepimagej.DeepPlugin;
import deepimagej.Log;
import deepimagej.tools.FileUtils;
import deepimagej.tools.Index;
import deepimagej.Parameters;
import ij.IJ;

public class FileStamp extends AbstractStamp implements ActionListener {
	
	private JTextField txt = new JTextField("drop zone for the model", 30);
	private JLabel lbl = new JLabel("no loaded model");
	private JButton bnBrowse = new JButton("Browse");
	private Log	log	= new Log();
		
	public FileStamp(Parameters params, BuildDialog parent, String title, Log log) {
		super(params, parent, title);
		buildPanel();
		this.log = log;
	}
	
	public void buildPanel() {
		txt.setDropTarget(new LocalDropTarget());
		panel.setDropTarget(new LocalDropTarget());
		
		lbl.setBorder(BorderFactory.createEtchedBorder());
		HTMLPane info = new HTMLPane(400, 100);
		info.append("p","info on the TensorFlow model");

		GridPanel pn = new GridPanel(false);
		pn.place(0, 0, new JLabel("Model"));
		pn.place(0, 1, txt);
		pn.place(0, 2, bnBrowse);
		pn.place(1, 1, lbl);
		pn.place(2, 0, 3, 1, info);

		bnBrowse.addActionListener(this);
		panel.add(pn, BorderLayout.CENTER);

	}

	public DeepPlugin validate(DeepPlugin dp) {
		// Check if the folder actually contains a TensorFlow SavedModel. The folder must contain
		// the file "saved_model.pb and the folder "variables"
		
		// First initialise the tags that load a model
		// so if a new model is being loaded, the same tags are nor used
		params.tag = null;
		params.graph = "";
		params.graphSet = null;
		
		params.path2Model = txt.getText();
		// check that a directory has been introduced
		if (params.path2Model.equals("drop zone for the model") == false
			&& params.path2Model.equals("") == false) {
			// Retrieve the name of the directory containing the model folder
			String modelDir = params.path2Model.substring(0, Index.lastIndexOf(params.path2Model.split(""), File.separator) + 1);
			// Retrieve the name of the model ( folder containing the files)
			params.name = params.path2Model.substring(Index.lastIndexOf(params.path2Model.split(""), File.separator) + 1);
			dp = new DeepPlugin(modelDir, params.name, log, params.developer);
			if (dp.getValid() == true){
				load(params.path2Model, dp);	
			} else {
				IJ.error("The folder provided did not contain the\n"
					+ " necessary files for a TensorFlow model.");
				params.card = 0;
			}
		} else {
				IJ.error("No directory was introduced.");
				params.card = 0;
		}
		return dp;
	}

	public void load(String filename, DeepPlugin dp) {
		// Check if the model is in a zip file and decompress
		// Load the tensorflow model
		Object[] info = DeepPlugin.checkModelCanLoad(filename);
		String modelTag = (String) info[0];
		Set<String> SignatureDefs = (Set<String>) info[1];
		params.graphSet = SignatureDefs;
		SavedModelBundle model = (SavedModelBundle) info[2];
		
		// Fill the list of possible tags
		if (modelTag != null) {
			params.tag = modelTag;
			dp.setModel(model);
		} else {
			params.tag = null;
		}
	}

	public void check(File file) {
		params.path2Model = "";
		if (file.exists()) {
			String filename = file.getAbsolutePath();
			txt.setText(filename);
			txt.setCaretPosition(filename.length());
			if (file.isDirectory()) {
				String size = FileUtils.getFolderSizeKb(filename + File.separator + "variables");
				lbl.setText("File size: " + size);
				params.path2Model = file.getAbsolutePath();
			}
			else {
				lbl.setText("This is not a folder");
			}	
		}
		else {
			lbl.setText("This folder doesn't exist");	
		}
		parent.updateInterface();
	}

	public class LocalDropTarget extends DropTarget {

		@Override
		public void drop(DropTargetDropEvent e) {
			e.acceptDrop(DnDConstants.ACTION_COPY);
			e.getTransferable().getTransferDataFlavors();
			Transferable transferable = e.getTransferable();
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			for (DataFlavor flavor : flavors) {
				if (flavor.isFlavorJavaFileListType()) {
					try {
						List<File> files = (List<File>) transferable.getTransferData(flavor);
						for (File file : files) {
							check(file);
						}
					}
					catch (UnsupportedFlavorException ex) {
						ex.printStackTrace();
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
			e.dropComplete(true);
			super.drop(e);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnBrowse) {
			browse();
		}

	}
	
	private void browse() {
		JFileChooser chooser = new JFileChooser(txt.getText());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select model");
		int ret = chooser.showOpenDialog(new JFrame());
		if (ret == JFileChooser.APPROVE_OPTION) {
			check(chooser.getSelectedFile());
		}
	}
	

}

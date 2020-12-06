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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.components.HTMLPane;
import ij.IJ;
import ij.gui.GenericDialog;

public class WelcomeStamp extends AbstractStamp implements ActionListener {

	private JTextField txt = new JTextField("Drop zone TensorFlow model (protobuf)");
	private JButton bnBrowse = new JButton("Browse");

	public WelcomeStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	public void buildPanel() {
		HTMLPane pane = new HTMLPane(Constants.width, 320);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "Building Bundled Model");
		pane.append("p",
				"This wizard allows to create a bundled model for DeepImageJ in 10 steps. "
						+ "The first step will consist to load the pretrained TensorFlow or Pytorch (see documentation) model. "
						+ "At the end, the <i>DeepImageJ Bundled Model</i> is saved in a directory." 
						+ "Then, it can be easily used by the plugin 'DeepImageJ Run'");

		pane.append("p", "Before to start the building, the following material is required: <ul>");
		pane.append("li",
				"<p>A pretrained TensorFlow model version 1.15 or lower. " + "This pretrained model has to be stored in a TensorFlow SavedModel file (save_model.pb and variables)</p>");
		pane.append("li",
				"<p>A pretrained Pytorch Torchscipt model version 1.6.0 or lower. " + "This pretrained model has to be stored in a folder. The path to the folder is what needsto be provided.</p>");
		pane.append("li", "<p>General information of the pretrained model</p>");
		pane.append("li", "<p>Knowledge of tensor organization and the tiling strategy</p>");
		pane.append("li", "<p>Macro or java file of preprocessing and postprocessing</p>");
		pane.append("li", "<p>A test image</p>");
		pane.append("</ul>");
		pane.append("p", "More information: deepimagej.github.io/deepimagej");
		pane.append("p", "Reference: E. G&oacute;mez de Mariscal and C. Garc&iacute;a-L&oacute;pez-de-Haro et al. DeepImageJ: J: A user-friendly plugin to run\n" + 
				"deep learning models in ImageJ. Submitted 2019.");
		pane.append("<hr>");
		pane.append("p",
				"<small>&copy; 2019. Biomedical Imaging Group, Ecole Polytechnique F&eacute;d&eacute;rale de Lausanne (EPFL), Switzerland "
				+ "and Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain.</small>");

		txt.setFont(new Font("Arial", Font.BOLD, 14));
		txt.setForeground(Color.red);
		txt.setPreferredSize(new Dimension(Constants.width, 25));
		JPanel load = new JPanel(new BorderLayout());
		load.setBorder(BorderFactory.createEtchedBorder());
		load.add(txt, BorderLayout.CENTER);
		load.add(bnBrowse, BorderLayout.EAST);
		
		JPanel pn = new JPanel(new BorderLayout());
		pn.add(pane.getPane(), BorderLayout.CENTER);
		pn.add(load, BorderLayout.SOUTH);
		panel.add(pn);
		
		txt.setDropTarget(new LocalDropTarget());
		load.setDropTarget(new LocalDropTarget());
		bnBrowse.addActionListener(this);
	}

	@Override
	public void init() {
	}

	@Override
	public boolean finish() {
		String filename = txt.getText();
		File file = new File(filename);
		if (!file.exists()) {
			IJ.error("This directory " + filename + " doesn't exist");	
			return false;
		}
			
		// TODO for the moment only allow folder models
		if (!file.isDirectory()) {
			IJ.error("This file " + filename + " does not correspond to a Pytorch or Tensorflow model.");	
			return false;
		}

		File pb = new File(filename + File.separator + "saved_model.pb");
		if (!pb.exists() && !DeepImageJ.isTherePytorch(file)) {
			IJ.error("This directory " + filename + " is nrot a protobuf model (no saved_model.pb)"
					+ "\nmodel (no saved_model.pb) neither a Pytorch model");	
			return false;
		} 
		return true;
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
							txt.setText(file.getAbsolutePath());
							txt.setCaretPosition(1);
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
			txt.setText(chooser.getSelectedFile().getAbsolutePath());
			txt.setCaretPosition(1);
		}
	}
	
	public String getModelDir() {
		File file = new File(txt.getText());
		if (file.exists()) {
			return file.getParent();
		}
		return null;
	}
	public String getModelName() {
		File file = new File(txt.getText());
		if (file.exists()) {
			return file.getName();
		}
		return null;
		
	}
}

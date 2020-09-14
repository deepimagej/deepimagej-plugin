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
import java.awt.GridLayout;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.HTMLPane;
import ij.IJ;

public class JavaPreprocessingStamp extends AbstractStamp implements ActionListener {

	private JTextField txt1 = new JTextField("Drop zone for the first preprocessing file");
	private JTextField txt2 = new JTextField("Drop zone for the second preprocessing file");
	private JButton bnBrowse1 = new JButton("Browse");
	private JButton bnBrowse2 = new JButton("Browse");
	
	// Variable to keep track of the model being used
	private String model = "";
	
	public JavaPreprocessingStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {

		HTMLPane pane = new HTMLPane(Constants.width, 90);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "External Preprocessing");
		pane.append("p", "(Optional) Add the required preprocessing for the input image.\n"
							+ "It supports ImageJ macro routines or Java code.\n" + 
							"Macro routines allow '.txt' or '.ijm' extensions.");
		pane.append("p", "The Java code can be included with either '.class' or '.jar' files");
		
		txt1.setFont(new Font("Arial", Font.BOLD, 14));
		txt1.setForeground(Color.red);
		txt1.setPreferredSize(new Dimension(Constants.width, 25));
		JPanel load1 = new JPanel(new BorderLayout());
		load1.setBorder(BorderFactory.createEtchedBorder());
		load1.add(txt1, BorderLayout.CENTER);
		load1.add(bnBrowse1, BorderLayout.EAST);

		txt2.setFont(new Font("Arial", Font.BOLD, 14));
		txt2.setForeground(Color.red);
		txt2.setPreferredSize(new Dimension(Constants.width, 25));
		JPanel load2 = new JPanel(new BorderLayout());
		load2.setBorder(BorderFactory.createEtchedBorder());
		load2.add(txt2, BorderLayout.CENTER);
		load2.add(bnBrowse2, BorderLayout.EAST);
		
		JPanel load = new JPanel(new GridLayout(2,0));
		load.add(load1);
		load.add(load2);
		
		JPanel pn = new JPanel(new BorderLayout());
		pn.add(pane.getPane(), BorderLayout.NORTH);
		
		pn.add(load, BorderLayout.CENTER);
		
		panel.add(pn);
		
		txt1.setDropTarget(new LocalDropTarget1());
		load1.setDropTarget(new LocalDropTarget1());
		bnBrowse1.addActionListener(this);
		
		txt2.setDropTarget(new LocalDropTarget2());
		load2.setDropTarget(new LocalDropTarget2());
		bnBrowse2.addActionListener(this);
		
	}

	@Override
	public void init() {
		Parameters params = parent.getDeepPlugin().params;
		if (!model.contentEquals(params.path2Model))
			model = params.path2Model;
		if (params.firstPreprocessing == null || !model.contentEquals(params.path2Model))
			txt1.setText("Drop zone for the first preprocessing");
		if (params.secondPreprocessing == null || !model.contentEquals(params.path2Model))
			txt2.setText("Drop zone for the second preprocessing");
	}

	@Override
	public boolean finish() {
		String filename1 = txt1.getText();
		String filename2 = txt2.getText();
		parent.getDeepPlugin().params.firstPreprocessing = null;
		parent.getDeepPlugin().params.secondPreprocessing = null;
		if (filename1.contains(File.separator)) {
			File file1 = new File(filename1);
			if (!file1.exists()) {
				IJ.error("This directory " + filename1 + " doesn't exist");	
				return false;
			}
			if ((file1.isFile()) && (!file1.getAbsolutePath().contains(".txt") && !file1.getAbsolutePath().contains(".ijm")) && (!file1.getAbsolutePath().contains(".class")) && (!file1.getAbsolutePath().contains(".jar"))) {
				IJ.error("The path " + filename1 + " does not corresponf to a valid macro or Java file");	
				return false;
			}
			parent.getDeepPlugin().params.firstPreprocessing = filename1;
		}
		
		if (filename2.contains(File.separator)) {
			File file2 = new File(filename2);
			if (!file2.exists()) {
				IJ.error("This directory " + filename2 + " doesn't exist");	
				return false;
			}	
			if ((file2.isFile()) && (!file2.getAbsolutePath().contains(".txt") && !file2.getAbsolutePath().contains(".ijm")) && (!file2.getAbsolutePath().contains(".class")) && (!file2.getAbsolutePath().contains(".jar"))) {
				IJ.error("The path " + filename2 + " does not corresponf to a valid macro or Java file");	
				return false;
			}
			parent.getDeepPlugin().params.secondPreprocessing = filename2;
		}
			
		return true;
	}
	public void upadateInterface(){
	}

	public class LocalDropTarget1 extends DropTarget {

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
							txt1.setText(file.getAbsolutePath());
							txt1.setCaretPosition(1);
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

	public class LocalDropTarget2 extends DropTarget {

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
							txt2.setText(file.getAbsolutePath());
							txt2.setCaretPosition(1);
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
		if (e.getSource() == bnBrowse1) {
			browse(true);
		} else if (e.getSource() == bnBrowse2) {
			browse(false);
		}
	}
	
	private void browse(boolean firstProcessing) {
		JFileChooser chooser = new JFileChooser(txt1.getText());
		//chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setDialogTitle("Select preprocessing jar");
		int ret = chooser.showOpenDialog(new JFrame());
		if (ret == JFileChooser.APPROVE_OPTION) {
			if (firstProcessing) {
				txt1.setText(chooser.getSelectedFile().getAbsolutePath());
				txt1.setCaretPosition(1);
			} else {
				txt2.setText(chooser.getSelectedFile().getAbsolutePath());
				txt2.setCaretPosition(1);
			}
		}
	}
}

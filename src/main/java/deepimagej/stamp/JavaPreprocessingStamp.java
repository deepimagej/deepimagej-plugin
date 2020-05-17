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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.components.HTMLPane;
import ij.IJ;

public class JavaPreprocessingStamp extends AbstractStamp implements ActionListener {

	private JTextField txt = new JTextField("Drop zone for the preprocessing jar");
	private JButton bnBrowse = new JButton("Browse");
	private JCheckBox checkJavaProc = new JCheckBox("Select if you want to apply external Java preprocessing");
	private JCheckBox checkApplyBeforeMacro = new JCheckBox("Apply Java processing before the macro processing");
	
	public JavaPreprocessingStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {

		HTMLPane pane = new HTMLPane(Constants.width, 90);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "Preprocessing Java extension");
		pane.append("p", "Drop a java jar that extends the plugin and implements " 
				+ "some complex prepsrocessing. The code will be executed after the macro"
				+ " call if not stated otherwise by the user."
				+ "The provided jar will be saved in the packaged model.");
		
		txt.setFont(new Font("Arial", Font.BOLD, 14));
		txt.setForeground(Color.red);
		txt.setPreferredSize(new Dimension(Constants.width, 25));
		JPanel load = new JPanel(new BorderLayout());
		load.setBorder(BorderFactory.createEtchedBorder());
		load.add(txt, BorderLayout.CENTER);
		load.add(bnBrowse, BorderLayout.EAST);
		
		JPanel pn = new JPanel(new BorderLayout());
		pn.add(pane.getPane(), BorderLayout.NORTH);
		
		JPanel subPn = new JPanel(new BorderLayout());
		subPn.add(checkJavaProc, BorderLayout.NORTH);
		subPn.add(load, BorderLayout.CENTER);
		subPn.add(checkApplyBeforeMacro, BorderLayout.SOUTH);
		
		pn.add(subPn, BorderLayout.CENTER);
		
		panel.add(pn);
		
		txt.setDropTarget(new LocalDropTarget());
		load.setDropTarget(new LocalDropTarget());
		bnBrowse.addActionListener(this);
		checkJavaProc.addActionListener(this);
		
		// Set all the components to their initial state
		checkJavaProc.setSelected(false);
		txt.setEnabled(false);
		bnBrowse.setEnabled(false);
		checkApplyBeforeMacro.setSelected(false);
		checkApplyBeforeMacro.setEnabled(false);
	}

	@Override
	public void init() {
	}

	@Override
	public boolean finish() {
		String filename = txt.getText();
		File file = new File(filename);
		if (!file.exists() && checkJavaProc.isSelected()) {
			IJ.error("This directory " + filename + " doesn't exist");	
			return false;
		}
			
		if (!file.isFile() && checkJavaProc.isSelected()) {
			IJ.error("The path " + filename + " does not corresponf to a a file");	
			return false;
		}
		
		if (checkJavaProc.isSelected()) {
			String jarExtension = filename.substring(file.getAbsolutePath().length() - 4);
			if (!jarExtension.contains(".jar")) {
				IJ.error("This file " + filename + " is not a jar file.");	
				return false;
			}
		}
		
		parent.getDeepPlugin().params.isJavaPreprocessing = checkJavaProc.isSelected();
		parent.getDeepPlugin().params.javaPreprocessing = "";
		if (checkJavaProc.isSelected())
			parent.getDeepPlugin().params.javaPreprocessing = filename;
		parent.getDeepPlugin().params.preprocessingBeforeMacro = checkApplyBeforeMacro.isSelected();
			
		return true;
	}
	public void upadateInterface(){
		txt.setEnabled(checkJavaProc.isSelected());
		bnBrowse.setEnabled(checkJavaProc.isSelected());
		checkApplyBeforeMacro.setEnabled(checkJavaProc.isSelected());
		if (!checkJavaProc.isSelected())
			checkApplyBeforeMacro.setSelected(false);
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
		} else if (e.getSource() == checkJavaProc) {
				upadateInterface();
			}
	}
	
	private void browse() {
		JFileChooser chooser = new JFileChooser(txt.getText());
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setDialogTitle("Select preprocessing jar");
		int ret = chooser.showOpenDialog(new JFrame());
		if (ret == JFileChooser.APPROVE_OPTION) {
			txt.setText(chooser.getSelectedFile().getAbsolutePath());
			txt.setCaretPosition(1);
		}
	}
}

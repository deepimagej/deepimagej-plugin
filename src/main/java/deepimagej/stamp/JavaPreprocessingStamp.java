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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Panel;
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.BorderPanel;
import deepimagej.components.HTMLPane;
import ij.IJ;
import ij.gui.GenericDialog;

public class JavaPreprocessingStamp extends AbstractStamp implements ActionListener {

	private JTextField txt1 = new JTextField("Drop zone for the first preprocessing file");
	private JTextField txt2 = new JTextField("Drop zone for the second preprocessing file");
	private JButton bnBrowse1 = new JButton("Browse");
	private JButton bnBrowse2 = new JButton("Browse");
	
	private static JTextField depPath = new JTextField("");
	public static JList<String> dependenciesList = new JList<String>();
	private static DefaultListModel<String> dependenciesModel;
	public static JButton addBtn = new JButton("Add");
	public static JButton rmvBtn = new JButton("Remove");
	
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
		//txt1.setPreferredSize(new Dimension(Constants.width, 25));
		JPanel load1 = new JPanel(new BorderLayout());
		load1.setBorder(BorderFactory.createEtchedBorder());
		load1.add(txt1, BorderLayout.CENTER);
		load1.add(bnBrowse1, BorderLayout.EAST);

		txt2.setFont(new Font("Arial", Font.BOLD, 14));
		txt2.setForeground(Color.red);
		//txt2.setPreferredSize(new Dimension(Constants.width, 25));
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
		
		txt1.setDropTarget(new LocalDropTarget(txt1));
		load1.setDropTarget(new LocalDropTarget(txt1));
		bnBrowse1.addActionListener(this);
		
		txt2.setDropTarget(new LocalDropTarget(txt2));
		load2.setDropTarget(new LocalDropTarget(txt2));
		bnBrowse2.addActionListener(this);
		
		// Action listeners for the build path GUI.
		// This GUI only appears if a Java processing is included
		depPath.setDropTarget(new LocalDropTarget(depPath));
		//loadPath.setDropTarget(new LocalDropTarget(depPath));
		addBtn.addActionListener(this);
		rmvBtn.addActionListener(this);
		
	}

	@Override
	public void init() {
		Parameters params = parent.getDeepPlugin().params;
		if (params.firstPreprocessing == null || !model.contentEquals(params.path2Model))
			txt1.setText("Drop zone for the first preprocessing");
		if (params.secondPreprocessing == null || !model.contentEquals(params.path2Model))
			txt2.setText("Drop zone for the second preprocessing");
		if (!model.contentEquals(params.path2Model)) {
			model = params.path2Model;
		}
		params.preAttachments = new ArrayList<String>();
		dependenciesModel = new DefaultListModel<String>();
		dependenciesList.setModel(dependenciesModel);
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
		boolean result = true;
		if (filename1.endsWith(".jar") || filename1.endsWith(".class") || filename2.endsWith(".jar") || filename2.endsWith(".class")) {
			result = addJavaDependencies();
		}
		return result;
	}

	public class LocalDropTarget extends DropTarget {
		private JTextField id;
		public LocalDropTarget(JTextField id) {
			this.id = id;
		}

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
							id.setText(file.getAbsolutePath());
							id.setCaretPosition(1);
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
		} else if (e.getSource() == addBtn) {
			addDependency();
		} else if (e.getSource() == rmvBtn) {
			removeDependency();
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
	
	/**
	 * Opens GUI to link Java dependencies
	 */
	public boolean addJavaDependencies() {
		boolean result = false;
		
		GenericDialog dlg = new GenericDialog("Java Build Path and external files");
		dlg.addMessage("Add path to the Java .jar dependencies needed to run the pre-processing.");
		dlg.addMessage("You can also add files required for the execution of the Java code, such as config files.");
		dlg.addMessage("The formats allowed for Java dependencies are '.class' and '.jar'.");
		dlg.addMessage("If there are no dependencies or files needed simply press 'OK'.");

		Panel loadPath = new Panel();
		loadPath.setLayout(new FlowLayout());
		loadPath.add(depPath);
		depPath.setText("Drop file needed for pre-processing");
		depPath.setFont(new Font("Arial", Font.BOLD, 11));
		depPath.setForeground(Color.GRAY);
		depPath.setPreferredSize(new Dimension(300, 50));
		
		// Panel for buttons
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout());
		buttons.add(addBtn);
		buttons.add(rmvBtn);
		loadPath.add(buttons, BorderLayout.EAST);
		loadPath.setVisible(true);
		dlg.addPanel(loadPath, GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));
		Dimension panelSize = loadPath.getPreferredSize();
		
		BorderPanel panel = new BorderPanel();
		dependenciesModel = new DefaultListModel<String>();
		dependenciesModel.addElement("");
		dependenciesList = new JList<String>(dependenciesModel);
		dependenciesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dependenciesList.setLayoutOrientation(JList.VERTICAL);
		dependenciesList.setVisibleRowCount(2);
		JScrollPane listScroller = new JScrollPane(dependenciesList);		
		panel.add(listScroller);
		dlg.addPanel(panel, GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));

		loadPath.setPreferredSize(new Dimension((int) Math.round(panelSize.getWidth() * 1), (int) Math.round(panelSize.getHeight() * 1)));
		listScroller.setPreferredSize(new Dimension((int) Math.round(panelSize.getWidth() * 1), (int) Math.round(panelSize.getHeight() * 2)));

		dlg.showDialog();

		if (dlg.wasOKed()) {
			result = true;
		}
		return result;
	}
	
	/**
	 * Adds dependency introduced to the list. Only accept .jar file
	 */
	public void addDependency() {
		// Get the author introduced
		String tag = depPath.getText().trim();
		if (tag.equals("")) {
			IJ.error("Introduce the path to an external file.");
			// Empty the text in the text field
			depPath.setText("Drop file needed for pre-processing");
			return;
		} else if(!(new File(tag)).isFile()) {
			IJ.error("The path introduced does not correspond to an existing file.");
			// Empty the text in the text field
			depPath.setText("Drop file needed for pre-processing");
			return;
		}
		for (String dep : parent.getDeepPlugin().params.preAttachments) {
			if (dep.contentEquals(tag)) {
				IJ.error("Do not add the same file twice.");
				// Empty the text in the text field
				depPath.setText("Drop file needed for pre-processing");
				return;
			}
		}
		// Check that the name of the files introduced does not coincide 
		// with the name of a file given during pre-processing
		String preName = tag.substring(tag.lastIndexOf(File.separator) + 1);
		for (String dep : parent.getDeepPlugin().params.preAttachments) {
			String pName = dep.substring(dep.lastIndexOf(File.separator) + 1);
			if (pName.contentEquals(preName) && !tag.endsWith(".jar")) {
				IJ.error("A file called '" + preName  + "' was already added for pre-processing.\n"
						+ "Cannot add two files with the same name.");
				// Empty the text in the text field
				depPath.setText("Drop file needed for pre-processing");
				return;
			}
		}
		
		parent.getDeepPlugin().params.preAttachments.add(tag);

		dependenciesModel = new DefaultListModel<String>();
		
		// Add the elements to the list

		for (String name : parent.getDeepPlugin().params.preAttachments){
			dependenciesModel.addElement(name);
		}
		dependenciesList.setModel(dependenciesModel);
		// Empty the text in the text field
		depPath.setText("Drop file needed for pre-processing");
	}
	
	/**
	 * Remove dependency previoulsy introduced
	 */
	public void removeDependency() {
		// Get the author selected
		int tag = dependenciesList.getSelectedIndex();
		if (tag == -1) {
			IJ.error("No file selected to remove");
			return;
		}
		parent.getDeepPlugin().params.preAttachments.remove(tag);

		dependenciesModel = new DefaultListModel<String>();
		
		// Add the elements to the list

		for (String name : parent.getDeepPlugin().params.preAttachments){
			dependenciesModel.addElement(name);
		}
		dependenciesList.setModel(dependenciesModel);
	}
}

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
import java.io.PrintWriter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.components.HTMLPane;
import deepimagej.tools.FileTools;
import deepimagej.tools.YAMLUtils;
import ij.IJ;

public class SaveStamp extends AbstractStamp implements ActionListener, Runnable {

	private JTextField	txt			= new JTextField(IJ.getDirectory("imagej") + File.separator + "models"+File.separator);
	private JButton		bnBrowse	= new JButton("Browse");
	private JButton		bnSave	= new JButton("Save Bundled Model");
	private HTMLPane 	pane;
	
	public SaveStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	public void buildPanel() {
		pane = new HTMLPane(Constants.width, 320);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "Saving Bundled Model");
		DeepImageJ dp = parent.getDeepPlugin();

		if (dp != null)
			if (dp.params != null)
				if (dp.params.path2Model != null)
					txt.setText(dp.params.path2Model);
		txt.setFont(new Font("Arial", Font.BOLD, 14));
		txt.setForeground(Color.red);
		txt.setPreferredSize(new Dimension(Constants.width, 25));
		JPanel load = new JPanel(new BorderLayout());
		load.setBorder(BorderFactory.createEtchedBorder());
		load.add(txt, BorderLayout.CENTER);
		load.add(bnBrowse, BorderLayout.EAST);

		JPanel pn = new JPanel(new BorderLayout());
		pn.add(load, BorderLayout.NORTH);
		pn.add(pane.getPane(), BorderLayout.CENTER);
		pn.add(bnSave, BorderLayout.SOUTH);
		panel.add(pn);

		bnSave.addActionListener(this);
		txt.setDropTarget(new LocalDropTarget());
		load.setDropTarget(new LocalDropTarget());
		bnBrowse.addActionListener(this);
	}

	@Override
	public void init() {
	}

	@Override
	public boolean finish() {
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnBrowse) {
			browse();
		}
		if (e.getSource() == bnSave) {
			save();
		}
	}

	private void browse() {
		JFileChooser chooser = new JFileChooser(txt.getText());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select model");
		int ret = chooser.showSaveDialog(new JFrame());
		if (ret == JFileChooser.APPROVE_OPTION) {
			txt.setText(chooser.getSelectedFile().getAbsolutePath());
			txt.setCaretPosition(1);
		}
	}

	public void save() {
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
		
	}
	@Override
	public void run() {
		DeepImageJ dp = parent.getDeepPlugin();
		Parameters params = dp.params;
		params.saveDir = txt.getText() + File.separator;
		params.saveDir = params.saveDir.replace(File.separator + File.separator, File.separator);
		File dir = new File(params.saveDir);
		boolean ok = true;
		if (!dir.exists()) {
			dir.mkdir();
			pane.append("p", "Make a directory: " + params.saveDir);
		}
		dir = new File(params.saveDir);
		if (ok && !dir.exists()) {
			IJ.error("This directory is not valid to save");
			ok = false;
		}
		if (ok && !dir.isDirectory()) {
			IJ.error("This folder is not a directory");
			ok = false;
		}

		//if (ok)
		if (true)
		try {
			File source = new File(params.path2Model + "saved_model.pb");
			File dest = new File(params.saveDir  + "saved_model.pb");
			FileTools.copyFile(source, dest);
			pane.append("p", "protobuf of the model (saved_model.pb): saved");
		}
		catch (Exception e) {
			e.printStackTrace();
			pane.append("p", "protobuf of the model (saved_model.pb): not saved");
			ok = false;
		}
		
		//if (ok)
		if (true)
		try {
			File source = new File(params.path2Model + "variables");
			File dest = new File(params.saveDir + "variables");
			copyWeights(source, dest);
			pane.append("p", "weights of the network (variables): saved");
		}
		catch (Exception e) {
			e.printStackTrace();
			pane.append("p", "weights of the network (variables): not saved");
			ok = false;
		}

		//if (ok)
		if (true)
		try {
			PrintWriter preprocessing = new PrintWriter(params.saveDir + "preprocessing.txt", "UTF-8");
			preprocessing.println(params.premacro);
			preprocessing.close();
			pane.append("p", "preprocessing.txt: saved");
		}
		catch (Exception e) {
			pane.append("p", "preprocessing.txt: not saved");
			ok = false;
		}

		//if (ok)
		if (true)
		try {
			PrintWriter postprocessing = new PrintWriter(params.saveDir + "postprocessing.txt","UTF-8");
			postprocessing.println(params.postmacro);
			postprocessing.close();
			pane.append("p", "postprocessing.txt: saved");
		}
		catch (Exception e) {
			pane.append("p", "postprocessing.txt: not saved");
			ok = false;
		}

		//if (ok)
		if (true)
		try {
			YAMLUtils.writeYaml(dp);
			pane.append("p", "config.yaml: saved");
		} 
		catch(Exception ex) {
			pane.append("p", "config.yaml: not saved");
			ok = false;
		}

		//if (ok)
		if (true)
		try {
			if (params.testImageBackup != null) {
				IJ.saveAsTiff(params.testImageBackup, params.saveDir + File.separator + "exampleImage.tiff");
				pane.append("p", "exampleImage.tiff: saved");
			} else {
				throw new Exception();
			}
		} 
		catch(Exception ex) {
			pane.append("p", "exampleImage.tiff: not saved");
			ok = false;
		}

		//if (ok)
		if (true)
		try {
			if (params.testResultImage != null) {
				IJ.saveAsTiff(params.testResultImage[0], params.saveDir + File.separator + "resultImage.tiff");
				pane.append("p", "resultImage.tiff: saved");
			} else {
				throw new Exception();
			}
		} 
		catch(Exception ex) {
			pane.append("p", "resultImage.tiff:  not saved");
			ok = false;
		}

		//parent.setEnabledBackNext(ok);
	}

	private void copyWeights(File source, File dest) throws IOException {
		String source_path;
		String dest_path;
		String filename;
		File[] n_files = source.listFiles();
		for (int i = 0; i < n_files.length; i++) {
			if (n_files[i].isFile() == true) {
				filename = n_files[i].getName();
				source_path = source.getAbsolutePath() + File.separator + filename;
				dest_path = dest.getAbsolutePath() + File.separator + filename;
				FileTools.copyFile(new File(source_path), new File(dest_path));
			}
		}
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


}

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
import java.awt.Frame;
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
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.components.HTMLPane;
import deepimagej.tools.FileTools;
import deepimagej.tools.YAMLUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

public class PtSaveStamp extends AbstractStamp implements ActionListener, Runnable {

	private JTextField	txt			= new JTextField(IJ.getDirectory("imagej") + File.separator + "models" + File.separator);
	private JButton		bnBrowse	= new JButton("Browse");
	private JButton		bnSave	= new JButton("Save Bundled Model");
	private HTMLPane 	pane;
	
	public PtSaveStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	public void buildPanel() {
		pane = new HTMLPane(Constants.width, 320);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "Saving Bundled Model");
		JScrollPane infoPane = new JScrollPane(pane);
		//infoPane.setPreferredSize(new Dimension(Constants.width, pane.getPreferredSize().height));
		DeepImageJ dp = parent.getDeepPlugin();

		if (dp != null)
			if (dp.params != null)
				if (dp.params.path2Model != null)
					txt.setText(dp.params.path2Model);
		txt.setFont(new Font("Arial", Font.BOLD, 14));
		txt.setForeground(Color.red);
		//txt.setPreferredSize(new Dimension(Constants.width, 25));
		JPanel load = new JPanel(new BorderLayout());
		load.setBorder(BorderFactory.createEtchedBorder());
		load.add(txt, BorderLayout.CENTER);
		load.add(bnBrowse, BorderLayout.EAST);

		JPanel pn = new JPanel(new BorderLayout());
		pn.add(load, BorderLayout.NORTH);
		pn.add(infoPane, BorderLayout.CENTER);
		pn.add(bnSave, BorderLayout.SOUTH);
		panel.add(pn);

		bnSave.addActionListener(this);
		txt.setDropTarget(new LocalDropTarget());
		load.setDropTarget(new LocalDropTarget());
		bnBrowse.addActionListener(this);
	}

	@Override
	public void init() {
		txt.setText(IJ.getDirectory("imagej") + File.separator + "models" + File.separator);
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
		params.biozoo = true;
		params.saveDir = txt.getText() + File.separator;
		params.saveDir = params.saveDir.replace(File.separator + File.separator, File.separator);
		File dir = new File(params.saveDir);
		
		dir = new File(params.saveDir);
		
		if (dir.exists() && dir.isDirectory()) {
			pane.append("p", "Path introduced corresponded to an already existing directory.");
			pane.append("p", "Model not saved");
			IJ.error("Directory: \n" + dir.getAbsolutePath() + "\n already exists. Please introduce other name.");
			return;
		}
		
		if (!dir.exists()) {
			dir.mkdir();
			pane.append("p", "Make a directory: " + params.saveDir);
		} 
		if (!dir.exists()) {
			pane.append("p", "Model not saved");
			IJ.error("This directory is not valid to save");
			return;
		}

		// Save the model architecture
		try {
			File torchfile = new File(params.selectedModelPath);
			FileTools.copyFile(torchfile, new File(dir + File.separator + "pytorch_script" + ".pt"));
			pane.append("p", "Torchscript model (.pt or .pth): saved");
		} catch (IOException e) {
			e.printStackTrace();
			pane.append("p", "torchscript model (.pt or .pth): not saved");
			pane.append("p", "torchscript model (.pt or .pth): torchscript model was removed from the model path");
		} catch (Exception e) {
			e.printStackTrace();
			pane.append("p", "torchscript model (.pt or .pth): not saved");
		}
		
		// Save preprocessing
		if (params.firstPreprocessing != null) {
			try {
				File destFile = new File(params.saveDir + File.separator + new File(params.firstPreprocessing).getName());
				FileTools.copyFile(new File(params.firstPreprocessing), destFile);
				pane.append("p", "First preprocessing: saved");
			}
			catch (Exception e) {
				pane.append("p", "First preprocessing: not saved");
			}
		}
		if (params.secondPreprocessing != null) {
			try {
				File destFile = new File(params.saveDir + File.separator + new File(params.secondPreprocessing).getName());
				FileTools.copyFile(new File(params.secondPreprocessing), destFile);
				pane.append("p", "Second preprocessing: saved");
			}
			catch (Exception e) {
				pane.append("p", "Second preprocessing: not saved");
			}
		}

		// Save postprocessing
		if (params.firstPostprocessing != null) {
			try {
				File destFile = new File(params.saveDir + File.separator + new File(params.firstPostprocessing).getName());
				FileTools.copyFile(new File(params.firstPostprocessing), destFile);
				pane.append("p", "First postprocessing: saved");
			}
			catch (Exception e) {
				pane.append("p", "First postprocessing: not saved");
			}
		}
		if (params.secondPostprocessing != null) {
			try {
				File destFile = new File(params.saveDir + File.separator + new File(params.secondPostprocessing).getName());
				FileTools.copyFile(new File(params.secondPostprocessing), destFile);
				pane.append("p", "Second postprocessing: saved");
			}
			catch (Exception e) {
				pane.append("p", "Second postprocessing: not saved");
			}
		}

		// Save input image
		try {
			if (params.testImageBackup != null) {
				// Get name with no extension
				String title = TfSaveStamp.getTitleWithoutExtension(params.testImageBackup.getTitle().substring(4));
				IJ.saveAsTiff(params.testImageBackup, params.saveDir + File.separator + title + ".tif");
				pane.append("p", title + ".tif" + ": saved");
				if (params.biozoo) {
					TfSaveStamp.saveNpyFile(params.testImageBackup, "XYCZN", params.saveDir + File.separator + title + ".npy");
					pane.append("p", title + ".npy" + ": saved");
				}
				params.testImageBackup.setTitle("DUP_" + params.testImageBackup.getTitle());
			} else {
				throw new Exception();
			}
		} 
		catch(Exception ex) {
			pane.append("p", "exampleImage.tif: not saved");
			if (params.biozoo)
				pane.append("p", "exampleImage.npy: not saved");
		}

		// Save output images and tables (tables as saved as csv)
		for (HashMap<String, String> output : params.savedOutputs) {
			String name = output.get("name");
			String nameNoExtension= TfSaveStamp.getTitleWithoutExtension(name);
			try {
				if (output.get("type").contains("image")) {
					ImagePlus im = WindowManager.getImage(name);
					IJ.saveAsTiff(im, params.saveDir + File.separator + nameNoExtension + ".tif");
					pane.append("p", nameNoExtension + ".tif" + ": saved");
					if (params.biozoo) {
						TfSaveStamp.saveNpyFile(im, "XYCZN", params.saveDir + File.separator + nameNoExtension + ".npy");
						pane.append("p", nameNoExtension + ".npy" + ": saved");
					}
				} else if (output.get("type").contains("ResultsTable")){
					Frame f = WindowManager.getFrame(name);
			        if (f!=null && (f instanceof TextWindow)) {
			        	ResultsTable rt = ((TextWindow)f).getResultsTable();
						rt.save(params.saveDir + File.separator + nameNoExtension + ".csv");
						pane.append("p", nameNoExtension + ".csv" + ": saved");
						if (params.biozoo) {
							TfSaveStamp.saveNpyFile(rt, params.saveDir + File.separator + nameNoExtension + ".npy");
							pane.append("p", nameNoExtension + ".npy" + ": saved");
						}
					} else {
						throw new Exception();					}
				}
			} 
			catch(Exception ex) {
				pane.append("p", nameNoExtension + ".tif:  not saved");
				if (params.biozoo)
					pane.append("p", nameNoExtension + ".npy:  not saved");
			}
		}
		
		// Save yaml
		try {
			YAMLUtils.writeYaml(dp);
			pane.append("p", "model.yaml: saved");
		} 
		catch(IOException ex) {
			pane.append("p", "model.yaml: not saved");
			IJ.error("Model file was locked or does not exist anymore.");
		}
		catch(Exception ex) {
			pane.append("p", "model.yaml: not saved");
			ex.printStackTrace();
		}
		
		pane.append("p", "<b>Done!!</b>");

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

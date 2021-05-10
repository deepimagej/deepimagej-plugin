/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package deepimagej.stamp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
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
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

	    // List with processing files saved
		ArrayList<String> saved = new ArrayList<String>();
		// Save preprocessing
		if (params.firstPreprocessing != null) {
			try {
				File destFile = new File(params.saveDir + File.separator + new File(params.firstPreprocessing).getName());
				FileTools.copyFile(new File(params.firstPreprocessing), destFile);
				pane.append("p", "First preprocessing: saved");
				saved.add(params.firstPreprocessing);
			}
			catch (Exception e) {
				pane.append("p", "First preprocessing: not saved");
			}
		}
		if (params.secondPreprocessing != null && !saved.contains(params.secondPreprocessing)) {
			try {
				File destFile = new File(params.saveDir + File.separator + new File(params.secondPreprocessing).getName());
				FileTools.copyFile(new File(params.secondPreprocessing), destFile);
				pane.append("p", "Second preprocessing: saved");
				saved.add(params.secondPreprocessing);
			}
			catch (Exception e) {
				pane.append("p", "Second preprocessing: not saved");
			}
		} else if (params.secondPreprocessing != null) {
			pane.append("p", "Second preprocessing: saved");
		}

		// Save postprocessing
		if (params.firstPostprocessing != null && !saved.contains(params.firstPostprocessing)) {
			try {
				File destFile = new File(params.saveDir + File.separator + new File(params.firstPostprocessing).getName());
				FileTools.copyFile(new File(params.firstPostprocessing), destFile);
				pane.append("p", "First postprocessing: saved");
			}
			catch (Exception e) {
				pane.append("p", "First postprocessing: not saved");
			}
		} else if (params.firstPostprocessing != null) {
			pane.append("p", "First postprocessing: saved");
		}
		if (params.secondPostprocessing != null && !saved.contains(params.secondPostprocessing)) {
			try {
				File destFile = new File(params.saveDir + File.separator + new File(params.secondPostprocessing).getName());
				FileTools.copyFile(new File(params.secondPostprocessing), destFile);
				pane.append("p", "Second postprocessing: saved");
			}
			catch (Exception e) {
				pane.append("p", "Second postprocessing: not saved");
			}
		} else if (params.secondPostprocessing != null) {
			pane.append("p", "Second postprocessing: saved");
		}

		// Save input image
		try {
			if (params.testImageBackup != null) {
				// Get name with no extension
				String title = TfSaveStamp.getTitleWithoutExtension(params.testImageBackup.getTitle().substring(4));
				IJ.saveAsTiff(params.testImageBackup, params.saveDir + File.separator + title + ".tif");
				pane.append("p", title + ".tif" + ": saved");
				boolean npySaved = TfSaveStamp.saveNpyFile(params.testImageBackup, "XYCZN", params.saveDir + File.separator + title + ".npy");
				if (npySaved)
					pane.append("p", title + ".npy" + ": saved");
				else
					pane.append("p", title + ".npy: not saved");
				params.testImageBackup.setTitle("DUP_" + params.testImageBackup.getTitle());
			} else {
				throw new Exception();
			}
		} 
		catch(Exception ex) {
			pane.append("p", "exampleImage.tif: not saved");
			pane.append("p", "exampleImage.npy: not saved");
		}

		// Save output images and tables (tables as saved as csv)
		for (HashMap<String, String> output : params.savedOutputs) {
			String name = output.get("name");
			String nameNoExtension = TfSaveStamp.getTitleWithoutExtension(name);
			try {
				if (output.get("type").contains("image")) {
					ImagePlus im = WindowManager.getImage(name);
					IJ.saveAsTiff(im, params.saveDir + File.separator + nameNoExtension + ".tif");
					im.setTitle(name);
					pane.append("p", nameNoExtension + ".tif" + ": saved");
					boolean npySaved = TfSaveStamp.saveNpyFile(im, "XYCZB", params.saveDir + File.separator + nameNoExtension + ".npy");
					if (npySaved)
						pane.append("p", nameNoExtension + ".npy" + ": saved");
					else
						pane.append("p", nameNoExtension + ".npy: not saved");
				} else if (output.get("type").contains("ResultsTable")){
					Frame f = WindowManager.getFrame(name);
			        if (f!=null && (f instanceof TextWindow)) {
			        	ResultsTable rt = ((TextWindow)f).getResultsTable();
						rt.save(params.saveDir + File.separator + nameNoExtension + ".csv");
						pane.append("p", nameNoExtension + ".csv" + ": saved");
						boolean npySaved = TfSaveStamp.saveNpyFile(rt, params.saveDir + File.separator + nameNoExtension + ".npy", "RC");
						if (npySaved)
							pane.append("p", nameNoExtension + ".npy" + ": saved");
						else
							pane.append("p", nameNoExtension + ".npy: not saved");	
					} else {
						throw new Exception();					}
				}
			} 
			catch(Exception ex) {
				pane.append("p", nameNoExtension + ".tif:  not saved");
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
		
		// Finally save external dependencies
		boolean saveDeps = TfSaveStamp.saveExternalDependencies(params);
		if (saveDeps && params.attachments.size() > 0) {
			pane.append("p", "Java .jar dependencies: saved");
		} else if (!saveDeps && params.attachments.size() > 0) {
			pane.append("p", "Java .jar dependencies: not saved");
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

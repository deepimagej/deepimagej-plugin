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
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.jetbrains.bio.npy.NpyFile;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.ImagePlus2Tensor;
import deepimagej.Parameters;
import deepimagej.Table2Tensor;
import deepimagej.components.HTMLPane;
import deepimagej.tools.FileTools;
import deepimagej.tools.YAMLUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

public class TfSaveStamp extends AbstractStamp implements ActionListener, Runnable {

	private JTextField	txt			= new JTextField(IJ.getDirectory("imagej") + File.separator + "models" + File.separator);
	private JButton		bnBrowse	= new JButton("Browse");
	private JButton		bnSave	= new JButton("Save Bundled Model");
	private HTMLPane 	pane;
	
	public TfSaveStamp(BuildDialog parent) {
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
		txt.setText(IJ.getDirectory("imagej") + File.separator + "models" + File.separator);
		JPanel load = new JPanel(new BorderLayout());
		load.setBorder(BorderFactory.createEtchedBorder());
		load.add(txt, BorderLayout.CENTER);
		load.add(bnBrowse, BorderLayout.EAST);

		JPanel pn = new JPanel(new BorderLayout());
		pn.add(load, BorderLayout.NORTH);
		pn.add(infoPane, BorderLayout.CENTER);
		JPanel pnButtons = new JPanel(new GridLayout(2, 1));
		pnButtons.add(bnSave);
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
		
		if (dir.exists() && dir.isDirectory()) {
			pane.append("p", "Path introduced corresponded to an already existing directory.");
			pane.append("p", "Model not saved");
			IJ.error("Directory: \n" + dir.getAbsolutePath() + "\n already exists. Please introduce other name.");
			return;
		}

		if (!dir.exists()) {
			dir.mkdir();
			pane.append("p", "Making directory: " + params.saveDir);
		}
		
		dir = new File(params.saveDir);
		

		// Save the model architecture

		params.biozoo = true;
		try {
			String zipName = "tensorflow_saved_model_bundle.zip";
			pane.append("p", "Writting zip file...");
			FileTools.zip(new String[]{params.path2Model + File.separator + "variables", params.path2Model + File.separator  + "saved_model.pb"}, params.saveDir + File.separator + zipName);
			pane.append("p", "Tensorflow Bioimage Zoo model: saved");
		}
		catch (Exception e) {
			e.printStackTrace();
			pane.append("p", "Error zipping the varaibles folder and saved_model.pb");
			pane.append("p", "Zipped Tensorflow model: not saved");
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
				String title = getTitleWithoutExtension(params.testImageBackup.getTitle().substring(4));
				IJ.saveAsTiff(params.testImageBackup, params.saveDir + File.separator + title + ".tif");
				pane.append("p", title + ".tif" + ": saved");
				boolean npySaved = saveNpyFile(params.testImageBackup, "XYCZN", params.saveDir + File.separator + title + ".npy");
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
			String nameNoExtension= getTitleWithoutExtension(name);
			try {
				if (output.get("type").contains("image")) {
					ImagePlus im = WindowManager.getImage(name);
					IJ.saveAsTiff(im, params.saveDir + File.separator + nameNoExtension + ".tif");
					im.setTitle(name);
					pane.append("p", nameNoExtension + ".tif" + ": saved");
					boolean npySaved = saveNpyFile(im, "XYCZB", params.saveDir + File.separator + nameNoExtension + ".npy");
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
						boolean npySaved = saveNpyFile(rt, params.saveDir + File.separator + nameNoExtension + ".npy", "RC");
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
			ex.printStackTrace();
			pane.append("p", "model.yaml: not saved");
		}

		// Finally save external dependencies
		boolean saveDeps = saveExternalDependencies(params);
		if (saveDeps && params.attachments.size() > 0) {
			pane.append("p", "External dependencies: saved");
		} else if (!saveDeps && params.attachments.size() > 0) {
			pane.append("p", "External dependencies: not saved");
		}
		
		pane.append("p", "Done!!");

	}
	
	/**
	 * Save jar dependency files indicated by the developer and saved at params.attachments.
	 * Saves all types of files but '.jar' o '.class' files
	 * @param params
	 * @return true if saving was successful or false otherwise
	 */
	public static boolean saveExternalDependencies(Parameters params) {
		boolean saved = true;
		ArrayList<String> savedFiles = new ArrayList<String>();
		String errMsg = "DeepImageJ unable to save:\n";
		for (String dep : params.attachments) {
			if (savedFiles.contains(new File(dep).getName()) || (new File(dep).getName()).endsWith(".jar") || (new File(dep).getName()).endsWith(".class")) 
				continue;
			File destFile = new File(params.saveDir + File.separator + new File(dep).getName());
			try {
				FileTools.copyFile(new File(dep), destFile);
				savedFiles.add(new File(dep).getName());
			} catch (IOException e) {
				saved = false;
				errMsg += " - " + new File(dep).getName();
				e.printStackTrace();
			}
		}
		if (!saved) {
			IJ.error(errMsg);
		}
		return saved;
	}
	
	/*
	 * Gets the image title without the extension
	 */
	public static String getTitleWithoutExtension(String title) {
		int lastDot = title.lastIndexOf(".");
		if (lastDot == -1)
			return title;
		return title.substring(0, lastDot);
	}
	
	public static boolean saveNpyFile(ImagePlus im, String form, String name) {
		Path path = Paths.get(name);
		String imTitle = name.substring(name.lastIndexOf(File.separator) + 1, name.lastIndexOf("."));
		long[] imShapeLong = ImagePlus2Tensor.getTensorShape(im, form);
		int[] imShape = new int[imShapeLong.length];
		// If the number of pixels is too big let the user know that they might prefer not saving
		// the results in npy format
		String msg = "Do you want to save the image '" + imTitle + "' in .npy format.\n"
					+ "Saving it might take too long. Do you want to continue?";
		boolean accept = IJ.showMessageWithCancel("Cancel .npy file save", msg);
		if (!accept)
			return false;
		
		float[] imArray = ImagePlus2Tensor.implus2IntArray(im, form);
		NpyFile.write(path, imArray, imShape);
		return true;
	}
	
	public static boolean saveNpyFile(ResultsTable table, String name, String form) {
		Path path = Paths.get(name);
		int[] shape = Table2Tensor.getTableShape(form, table);
		// Convert the array into long
		long[] shapeLong = new long[shape.length];
		// If the number of pixels is too big let the user know that they might prefer not saving
		// the results in npy format
		String msg = "Do you want to save the table '" + table.getTitle() + "' in .npy format.\n"
						+ "Saving it might take too long. Do you want to continue?";
		boolean accept = IJ.showMessageWithCancel("Cancel .npy file save", msg);
		if (!accept)
			return false;
		// Get the array
		float[] flatRt = Table2Tensor.tableToFlatArray(table, form, shapeLong);
		NpyFile.write(path, flatRt, shape);
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


}

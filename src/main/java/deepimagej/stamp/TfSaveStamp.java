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
import java.nio.file.Path;
import java.nio.file.Paths;
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
	private JCheckBox	bnSaveBiozoo	= new JCheckBox("Save model into the Bioimage Zoo format");
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
		infoPane.setPreferredSize(new Dimension(Constants.width, pane.getPreferredSize().height));
		DeepImageJ dp = parent.getDeepPlugin();

		if (dp != null)
			if (dp.params != null)
				if (dp.params.path2Model != null)
					txt.setText(dp.params.path2Model);
		txt.setFont(new Font("Arial", Font.BOLD, 14));
		txt.setForeground(Color.red);
		txt.setPreferredSize(new Dimension(Constants.width, 25));
		txt.setText(IJ.getDirectory("imagej") + File.separator + "models" + File.separator);
		JPanel load = new JPanel(new BorderLayout());
		load.setBorder(BorderFactory.createEtchedBorder());
		load.add(txt, BorderLayout.CENTER);
		load.add(bnBrowse, BorderLayout.EAST);

		JPanel pn = new JPanel(new BorderLayout());
		pn.add(load, BorderLayout.NORTH);
		//pn.add(pane.getPane(), BorderLayout.CENTER);
		pn.add(infoPane, BorderLayout.CENTER);JPanel pnButtons = new JPanel(new GridLayout(2, 1));
		pnButtons.add(bnSave);
		pnButtons.add(bnSaveBiozoo);
		pn.add(pnButtons, BorderLayout.SOUTH);
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
		
		dir = new File(params.saveDir);
		

		// Save the model architecture
		if (!bnSaveBiozoo.isSelected()) {
			params.biozoo = false;
			try {
				File source = new File(params.path2Model + "saved_model.pb");
				File dest = new File(params.saveDir  + "saved_model.pb");
				FileTools.copyFile(source, dest);
				pane.append("p", "protobuf of the model (saved_model.pb): saved");
			} catch (IOException e) {
				e.printStackTrace();
				pane.append("p", "protobuf of the model (saved_model.pb): not saved");
				ok = false;
			} catch (Exception e) {
				e.printStackTrace();
				pane.append("p", "protobuf of the model (saved_model.pb): not saved");
				ok = false;
			}
	
			// Save the model weights
			try {
				File source = new File(params.path2Model + "variables");
				File dest = new File(params.saveDir + "variables");
				copyWeights(source, dest);
				pane.append("p", "weights of the network (variables): saved");
				// TODO add check to ensure each pair of weights is unique
			}
			catch (Exception e) {
				e.printStackTrace();
				pane.append("p", "weights of the network (variables): not saved");
				ok = false;
			}
		} else {
			params.biozoo = true;
			try {
				String zipName = "tensorflow_saved_model_bundle.zip";
				pane.append("p", "Writting zip file...");
				FileTools.zipFilesIntoFolder(new String[]{params.path2Model + File.separator + "variables", params.path2Model + File.separator  + "saved_model.pb"}, params.saveDir + File.separator + zipName);
				pane.append("p", "Tensorflow Bioimage Zoo model: saved");
			}
			catch (Exception e) {
				e.printStackTrace();
				pane.append("p", "Error zipping the varaibles folder and saved_model.pb");
				pane.append("p", "Tensorflow Bioimage Zoo model: not saved");
				ok = false;
			}
			
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
				ok = false;
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
				ok = false;
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
				ok = false;
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
				ok = false;
			}
		}

		// Save input image
		try {
			if (params.testImageBackup != null) {
				IJ.saveAsTiff(params.testImageBackup, params.saveDir + File.separator + params.testImageBackup.getTitle().substring(4));
				pane.append("p", "exampleImage.tiff: saved");
				if (params.biozoo) {
					// Get name with no extension
					String name = params.testImageBackup.getTitle();
					name = name.substring(0, name.lastIndexOf("."));
					saveNpyFile(params.testImageBackup, "XYCZN", params.saveDir + File.separator + name + ".npy");
					pane.append("p", name + ".npy" + ": saved");
				}
				params.testImageBackup.setTitle("DUP_" + params.testImageBackup.getTitle());
			} else {
				throw new Exception();
			}
		} 
		catch(Exception ex) {
			pane.append("p", "exampleImage.tiff: not saved");
			if (params.biozoo)
				pane.append("p", "exampleImage.npy: not saved");
			ok = false;
		}

		// Save output images and tables (tables as saved as csv)
		for (HashMap<String, String> output : params.savedOutputs) {
			String name = output.get("name");
			try {
				if (output.get("type").contains("image")) {
					ImagePlus im = WindowManager.getImage(name);
					IJ.saveAsTiff(im, params.saveDir + File.separator + name + ".tif");
					if (params.biozoo) {
						saveNpyFile(im, "XYCZN", params.saveDir + File.separator + name + ".npy");
						pane.append("p", name + ".npy" + ": saved");
					}
				} else if (output.get("type").contains("ResultsTable")){
					Frame f = WindowManager.getFrame(name);
			        if (f!=null && (f instanceof TextWindow)) {
			        	ResultsTable rt = ((TextWindow)f).getResultsTable();
						rt.save(params.saveDir + File.separator + name + ".csv");
						if (params.biozoo) {
							saveNpyFile(rt, params.saveDir + File.separator + name + ".npy");
							pane.append("p", name + ".npy" + ": saved");
						}
					} else {
						throw new Exception();					}
				}
				pane.append("p", name + ": saved");
			} 
			catch(Exception ex) {
				pane.append("p", name + "exampleOutput.tiff:  not saved");
				if (params.biozoo)
					pane.append("p", name + "exampleOutput.npy:  not saved");
				ok = false;
			}
		}
		
		// Save yaml
		try {
			YAMLUtils.writeYaml(dp);
			pane.append("p", "config.yaml: saved");
		} 
		catch(IOException ex) {
			pane.append("p", "config.yaml: not saved");
			ok = false;
			IJ.error("Model file was locked or does not exist anymore.");
		}
		
		catch(Exception ex) {
			ex.printStackTrace();
			pane.append("p", "config.yaml: not saved");
			ok = false;
		}
		pane.append("p", "Done!!");

		//parent.setEnabledBackNext(ok);
	}private void copyWeights(File source, File dest) throws IOException {
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
	
	public void saveNpyFile(ImagePlus im, String form, String name) {
		Path path = Paths.get(name);
		long[] imShapeLong = ImagePlus2Tensor.getTensorShape(im, form);
		int[] imShape = new int[imShapeLong.length];
		for (int i = 0; i < imShape.length; i ++)
			imShape[i] = (int) imShapeLong[i];
		float[] imArray = ImagePlus2Tensor.implus2IntArray(im, form);
		NpyFile.write(path, imArray, imShape);
	}
	
	public void saveNpyFile(ResultsTable table, String name) {
		Path path = Paths.get(name);
		long[] shapeLong = Table2Tensor.getTableShape(table);
		int[] shape = new int[shapeLong.length];
		for (int i = 0; i < shape.length; i ++)
			shape[i] = (int) shapeLong[i];
		float[] tableArray = Table2Tensor.table2IntArray(table);
		NpyFile.write(path, tableArray, shape);
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

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
import java.awt.TextField;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
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
import deepimagej.Parameters;
import deepimagej.components.HTMLPane;
import deepimagej.tools.FileTools;
import deepimagej.tools.Index;
import deepimagej.tools.YAMLUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

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
		ArrayList<String> repeatedFiles = new ArrayList<String>();
		if (!dir.exists()) {
			dir.mkdir();
			pane.append("p", "Make a directory: " + params.saveDir);
		} else {
			repeatedFiles = checkFolder(params.saveDir);
		}
		
		if (repeatedFiles.size() > 0 && repeatedFiles.size() < 3) {
			GenericDialog dlg = new GenericDialog("Overwrite existing model");
			dlg.addMessage("Part of the model was found in the save directory.");
			dlg.addMessage("Do you want to overwrite it?.");
			dlg.addMessage("If you press Ok the following files/folders will be deleted/overwritten.");
			if (new File(dir, "variables").isDirectory())
				repeatedFiles.add("variables");
			for (String f : repeatedFiles)
				dlg.addMessage("- " + f);
			dlg.showDialog();
			if (dlg.wasCanceled()) {
				pane.append("p", "Save cancelled");
				return;
			} 
			for (String f : repeatedFiles) {
				boolean deleted = true;
				if (f.contains(".")) {
					deleted = new File(f).delete();
				} else {
					deleted = FileTools.deleteDir(new File(f));
				}
				if (!deleted) {
					IJ.error("Could not remove " + f + ".\nSave cancelled.");
					pane.append("p", "Save cancelled");
				}
			}
		}
		dir = new File(params.saveDir);
		
		// Check if save directory already has a version of the same model. If it does
		// keep both models together and indicate it in the YAML file
		
		if (ok && !dir.exists()) {
			IJ.error("This directory is not valid to save");
			ok = false;
		}
		if (ok && !dir.isDirectory()) {
			IJ.error("This folder is not a directory");
			ok = false;
		}

		// Save the model architecture
		try {
			// Check that, if the save directory is already a Bioimage Model Zoo model.
			// If it is check that they have the same architecture
			boolean overwrite = true;
			if (params.biozoo) {
				int sameModel = isItTheSameModel(params.saved_modelSha256, params.saveDir + File.separator + "saved_model.pb", params.path2Model + "saved_model.pb");
				if (sameModel == 1) {
					// Do not save anything as it is the same
					overwrite = false;
				} else if (sameModel == 0) {
					GenericDialog dlg = new GenericDialog("Overwrite existing model");
					// TODO change to official name
					// TODO check if we created two different models at different times
					// TODO with the same archi, the sha256 will be the same
					dlg.addMessage("There is already a Biozoo model in the save directory.");
					dlg.addMessage("Do you want to store the current model as another version of the model already saved?.");
					dlg.addMessage("In this case the architecture from the already saved model will be kept for both the old and new weights.");
					dlg.addMessage("You can also overwrite the files with the new model?.");
					dlg.addChoice("Select", new String[] {"Overwrite",  "Same version"}, "Overwrite");
					dlg.showDialog();
					if (dlg.wasCanceled()) {
						pane.append("p", "Save cancelled");
						return;
					} 
					String choice = dlg.getNextChoice();
					if (choice.contains("Same version"))
						overwrite = false;
				} else if (sameModel == -1) {
					GenericDialog dlg = new GenericDialog("Overwrite existing model");
					dlg.addMessage("There is already a Biozoo model in the save directory.");
					dlg.addMessage("However the model seems to have been modified and it cannot longer be loaded.");
					dlg.addMessage("Do you want to overwrite it?.");
					dlg.showDialog();
					if (dlg.wasCanceled()) {
						pane.append("p", "Save cancelled");
						return;
					} 
					dlg = new GenericDialog("Overwrite existing weights");
					dlg.addMessage("What do you want to do with the weights?");
					dlg.addMessage("If they work with the architecture you are saving currently,");
					dlg.addMessage("you could keep them as another version of the model.");
					dlg.addMessage("If they correspond to another architecture, it is better to delete them.");
					dlg.addMessage("Do you want to delete them?");
					dlg.enableYesNoCancel();
					dlg.showDialog();
					if (dlg.wasCanceled()) {
						pane.append("p", "Save cancelled");
						return;
					} else if (dlg.wasOKed()) {
						for (File w : dir.listFiles()) {
							if (w.getName().contains("weights_v") && w.getName().contains(".zip")) {
								boolean deleted = w.delete();
								if (!deleted) {
									IJ.error("Could not remove " + w.getName() + ".\nSave cancelled.");
									pane.append("p", "Save cancelled");
								}
							}
						}
					} 
					
				} else if (sameModel == -2) {
					overwrite = true;
				} else if (sameModel == -3) {
					overwrite = true;
				}
			}
			if (overwrite) {
				File source = new File(params.path2Model + "saved_model.pb");
				File dest = new File(params.saveDir  + "saved_model.pb");
				FileTools.copyFile(source, dest);
			}
			pane.append("p", "protobuf of the model (saved_model.pb): saved");
		} catch (IOException e) {
			e.printStackTrace();
			pane.append("p", "protobuf of the model (saved_model.pb): not saved");
			pane.append("p", "protobuf of the model (saved_model.pb): saved_model.pb was removed from the model path");
			ok = false;
		} catch (Exception e) {
			e.printStackTrace();
			pane.append("p", "protobuf of the model (saved_model.pb): not saved");
			ok = false;
		}

		// Save the model weights
		try {
			if (params.biozoo) {
				// Check that the version string is not already in the yaml, if there is
				//  a yaml in the directory.
				boolean repeated = getModelVersion(params.saveDir, params);
				while (repeated) {
					GenericDialog dlg = new GenericDialog("Change version of the weights");
					dlg.addMessage("Version " + params.version + " already exists. Choose a different one.");
					dlg.addStringField("New weights version", "X.Y");
					dlg.showDialog();
					if (dlg.wasCanceled()) {
						pane.append("p", "Save cancelled");
						return;
					}
					Vector<TextField> strField = dlg.getStringFields();
					TextField versionField = (TextField) strField.get(0);
					params.version = versionField.getText().trim();
					repeated = getModelVersion(params.saveDir, params);
					if (repeated) {
						IJ.error("This version already exists");
					} 
					
				}
			}
			String zipName = "weights_v" + params.version + ".zip";
			File source = new File(params.path2Model + "variables");
			File dest = new File(params.saveDir + zipName);
			FileTools.zipFolder(source, dest);
			pane.append("p", "weights of the network (variables): saved");
			// TODO add check to ensure each pair of weights is unique
		}
		catch (Exception e) {
			e.printStackTrace();
			pane.append("p", "weights of the network (variables): not saved");
			ok = false;
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
		
		try {
			YAMLUtils.writeYaml(dp);
			pane.append("p", "config.yaml: saved");
		} 
		catch(Exception ex) {
			pane.append("p", "config.yaml: not saved");
			ok = false;
		}

		// Save input image
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

		// Save output images and tables (tables as saved as csv)
		for (HashMap<String, String> output : params.savedOutputs) {
			String name = output.get("name");
			try {
				if (output.get("type").contains("image")) {
					ImagePlus im = WindowManager.getImage(name);
					IJ.saveAsTiff(im, params.saveDir + File.separator + name);
				} else if (output.get("type").contains("ResultsTable")){
					Frame f = WindowManager.getFrame(name);
			        if (f!=null && (f instanceof TextWindow)) {
			        	ResultsTable rt = ((TextWindow)f).getResultsTable();
						rt.save(params.saveDir + File.separator + name + ".csv");
					} else {
						throw new Exception();					}
				}
				pane.append("p", name + ": saved");
			} 
			catch(Exception ex) {
				pane.append("p", name + ":  not saved");
				ok = false;
			}
		}

		//parent.setEnabledBackNext(ok);
	}
	
	/*
	 * Check that the architecture in the old model is correct per the yaml, and if
	 * it is the same as the new architecture we want to save
	 * Returns 1 if the model specified in he yaml is the same as the one we want to save
	 * and the one that is previously in the dir; 0 if the one specified in the yaml
	 * is the same as the one already in the folder, but not the same as the new one we
	 * want to save; -1 if none of them is the same; -2 if the one specified by the yaml
	 * is the same as the one we want to save now but not the same as the one that is already
	 * and -3 if the old model is missing from the directory.
	 */
	public static int isItTheSameModel(String originalSha256, String oldArchiDir, String newArchiDir) throws IOException{
		String oldSha256 = "";
		String newSha256 = FileTools.createSHA256(newArchiDir);
		if (new File(oldArchiDir).isFile()) {
			oldSha256 = FileTools.createSHA256(oldArchiDir);
		} else {
			return -3;
		}
		if (originalSha256.equals(oldSha256) && originalSha256.equals(newSha256)) {
			return 1;
		} else if (originalSha256.equals(oldSha256) && !originalSha256.equals(newSha256)) {
			return 0;
		} else if (!originalSha256.equals(oldSha256) && !originalSha256.equals(newSha256)) {
			return -1;
		} else {
			// !originalSha256.equals(oldSha256) && originalSha256.equals(newSha256)
			return -2;
		}
	}
	
	/*
	 * Return all the versions of the model saved in the folder.
	 * The method looks at the yaml, checks the versions that are already saved
	 * and makes sure they are correct with the sha256 
	 */
	public static boolean getModelVersion(String dir, Parameters params){
		
		//Map<String, Object> obj =  YAMLUtils.readConfig(dir + File.separator + "config.yaml");
		Set<String> keyset = params.previousVersions.keySet();
		String[] versions = new String[keyset.size()];
		int c = 0;
		// Each version name consists of 'v' + 'version string'
		boolean repeated = false;
		for (String key : keyset) {
			versions[c ++] = key.substring(1);
			if (key.substring(1).equals(params.version.trim())) {
				repeated = true;
			}
		}
		return repeated;
	}
	
	public static ArrayList<String> checkFolder(String dir){
		ArrayList<String> files = new ArrayList<String>();
		if (new File(dir, "saved_model.pb").isFile())
			files.add("saved_model.pb");
		if (new File(dir, "config.yaml").isFile()) {
			files.add("config.yaml");
		} else if (new File(dir, "config.yml").isFile()) {
			files.add("config.yml");
		}
		String[] fList = new File(dir).list();
		for (String f : fList) {
			if (f.contains("weights_v") && f.contains(".zip")) {
				files.add(f);
				break;
			}
		}
		
		return files;
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

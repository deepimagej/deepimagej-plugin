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
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

public class InputDimensionStamp extends AbstractStamp implements ActionListener {

	
	private List<JTextField>			allTxtMultiple = new ArrayList<JTextField>();
	private List<JTextField>			allTxtPatches = new ArrayList<JTextField>();
	private JCheckBox					checkParamUseIm = new JCheckBox("The processing method retrieves the parameters from the input image");
	private JCheckBox					checkParamNotUseIm = new JCheckBox("The processing method does not need the image to retrieve the parameters");
		
	private JCheckBox 					checkAllowPatching = new JCheckBox("Allow processing the image by patches.");
	
	private JComboBox<String>			cmbPatches	= new JComboBox<String>(new String[] { "Allow patch decomposition", "Predetermined input size" });
	private JLabel						lblPatches	= new JLabel("Patch size");
	private JLabel						lblMultiple	= new JLabel("Multiple factor");

	private JButton 					bnNextOutput 	= new JButton("Next Output");
	private JButton 					bnPrevOutput 	= new JButton("Previous Output");
	private GridPanel					pnInput			= new GridPanel();
	
	private JTextField 					txt = new JTextField("Drop here the corresponding .jar");
	private JButton 					bnBrowse = new JButton("Browse");
	
	private static JComboBox<String>	cmbRangeLow  = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private static JComboBox<String>	cmbRangeHigh = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private static double[] 			rangeOptions = {Double.NEGATIVE_INFINITY, (double) -1, (double) 0, (double) 1, Double.POSITIVE_INFINITY};

	private static int					inputCounter = 0;
	private String						model		  = "";
	
	public InputDimensionStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
		cmbRangeHigh.setSelectedIndex(4);
	}

	@Override
	public void buildPanel() {
		
		//Parameters params = parent.getDeepPlugin().params;
		
		HTMLPane info = new HTMLPane(Constants.width, 180);
		info.append("h2", "Input size constraints");
		info.append("p", "<b>Patch size (Q) </b>: If the network has not a predetermined input size, patch decomposition of default size <i>Q</i> is allowed.");
		info.append("p", "<b>Padding (P) </b>: To preserve the input size at the output, convolutions are calculated using zero padding boundary conditions of size <i>P</i>.");
		info.append("p", "<b>Multiple factor (m) </b>: If the network has an auto-encoder architecture, the size of each dimension of the input image, has to be multiple of a minimum size m.");
			
		GridPanel buttons = new GridPanel(true);
		buttons.setBorder(BorderFactory.createEtchedBorder());
		buttons.place(0, 0, bnPrevOutput);
		buttons.place(0, 1, bnNextOutput);
		
		// Create auxiliary DijTensor to initialise the interface 
		DijTensor auxTensor = new DijTensor("aux");
		auxTensor.tensorType = "image";
		auxTensor.tensor_shape = new int[5];
		auxTensor.form = "NDHWC";
		buildPanelForImage(auxTensor);
		
		/*if (params.inputList.get(inputCounter).tensorType.equals("image")) {
			pnInput = buildPanelForImage(params);
			updateImageInterface();
		}
		else {
			pnInput = buildPanelForParameter(params);
		}*/
		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(pnInput);
		pn.add(buttons, BorderLayout.SOUTH);
		
		panel.add(pn);	

		bnNextOutput.addActionListener(this);
		bnPrevOutput.addActionListener(this);
		cmbPatches.addActionListener(this);
		checkAllowPatching.addActionListener(this);
	}
	
	@Override
	public void init() {
		//String modelOfInterest = parent.getDeepPlugin().params.path2Model;
		Parameters params = parent.getDeepPlugin().params;
		showCorrespondingInputInterface(params);
		// Set the screen at the first input if the model changes
		String modelOfInterest = params.path2Model;
		if (!modelOfInterest.equals(model)) {
			model = modelOfInterest;
			inputCounter = 0;
		}
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		saveInputData(params);
		for (DijTensor tensor : params.inputList) {
			if (!tensor.finished){
				IJ.error("You need to fill information for every input tensor");
				return false;
			}
		}
		
		return true;
	}
	
	private void updatePatchSize(String[] dim, int[] dimValues, String[] dimChars) {
		// Look if any of he input dimensions is fixed. If it is, set the corresponding 
		// value to that dimension and make editable = false.

		boolean pat = cmbPatches.getSelectedIndex() == 1;
		lblPatches.setText(pat ? "Default patch size" : "Predetermined input size");
		
		for (int i = 0; i < dim.length; i ++) {
			if (dimValues[i] != -1) {
				allTxtMultiple.get(i).setText("" + dimValues[i]);
				allTxtMultiple.get(i).setEditable(false);
				allTxtPatches.get(i).setText("" + dimValues[i]);
				allTxtPatches.get(i).setEditable(false);
			} else if (pat == true && dimValues[i] == -1) {
				allTxtPatches.get(i).setText("" + allTxtMultiple.get(i).getText());
				allTxtMultiple.get(i).setEditable(true);
				allTxtPatches.get(i).setEditable(false);
			}  else if(pat == false) {
				allTxtMultiple.get(i).setEditable(true);
				allTxtPatches.get(i).setEditable(true);
				allTxtPatches.get(i).setText(optimalPatch(allTxtMultiple.get(i).getText(), dimChars[i]));
			}
		}
		// If every dimension is fixed, remove the possibility of editing the patch size
		if (Index.indexOf(dimValues, -1) == -1) {
			cmbPatches.setEnabled(true);
			cmbPatches.setSelectedIndex(1);
		}
	}

	public void showCorrespondingInputInterface(Parameters params) {
		
		// Check how many outputs there are to enable or not
		// the "next" and "back" buttons
		if (inputCounter == 0) {
			bnPrevOutput.setEnabled(false);
		} else {
			bnPrevOutput.setEnabled(true);
		}
		if (inputCounter < (params.inputList.size() - 1)) {
			bnNextOutput.setEnabled(true);
		} else {
			bnNextOutput.setEnabled(false);
		}

		// Reinitialise all the params
		allTxtMultiple = new ArrayList<JTextField>();
		allTxtPatches = new ArrayList<JTextField>();
		pnInput.removeAll();
		DijTensor tensor = params.inputList.get(inputCounter);
		if (tensor.tensorType.contains("image")) {
			// Build the panel
			buildPanelForImage(tensor);
			updateImageInterface(tensor);
		} else if (tensor.tensorType.contains("parameter")) {
			buildPanelForParameter(params);
		} else {
			inputCounter ++;
		}
		pnInput.revalidate();
		pnInput.repaint();
	
	}

	private void updateImageInterface(DijTensor tensor) {

		//Parameters params = parent.getDeepPlugin().params;
		String[] dim = DijTensor.getWorkingDims(tensor.form); 
		int[] dimValues = DijTensor.getWorkingDimValues(tensor.form, tensor.tensor_shape); 
		cmbPatches.setEnabled(true);
		
		if (checkAllowPatching.isSelected() == true) {
			
			updatePatchSize(dim, dimValues, dim);

		} else {
			cmbPatches.setEnabled(false);
			// Set "Predetermined input size" in order to avoid patch decomposition
			cmbPatches.setSelectedIndex(1);
			
			for (int i = 0; i < dim.length; i ++) {
				if (dimValues[i] != -1) {
					allTxtMultiple.get(i).setText("" + dimValues[i]);
					allTxtMultiple.get(i).setEditable(false);
					allTxtPatches.get(i).setText(" - ");
				} else {
					allTxtMultiple.get(i).setEditable(true);
					allTxtPatches.get(i).setEditable(false);
					allTxtPatches.get(i).setText(" - ");
				}
			}
		}
	}
	
	/*
	 * Method to retrieve from the UI the information necessary to build  
	 * whatever object is needed for the input tensor
	 */
	public boolean saveInputData(Parameters params) {
		// If the methods saving the info were successful, wasSaved=true
		boolean wasSaved = false;
		if (params.inputList.get(inputCounter).tensorType.contains("image")) {
			wasSaved = saveInputDataForImage(params);
		} else {
			wasSaved = saveInputDataForParameter(params);
		}

		int lowInd = cmbRangeLow.getSelectedIndex();
		int highInd = cmbRangeHigh.getSelectedIndex();
		if (lowInd >= highInd) {
			IJ.error("The Data Range has to go from a value to a higher one.");
			return false;
		}
		
		params.inputList.get(inputCounter).dataRange[0] = rangeOptions[lowInd];
		params.inputList.get(inputCounter).dataRange[1] = rangeOptions[highInd];
		params.inputList.get(inputCounter).finished = wasSaved;
		
		return wasSaved;
	}
	
	/*
	 * Method to retrieve from the UI the information necessary to build a 
	 * parameter from the tensor inputed to the model
	 */
	public boolean saveInputDataForParameter(Parameters params) {
		String filename = txt.getText();
		File file = new File(filename);
		if (!file.exists() || !file.isFile()) {
			IJ.error("This directory " + filename + " doesn't exist\n"
					+ "or does not corresponf to a a file");	
			return false;
		}
		String jarExtension = filename.substring(file.getAbsolutePath().length() - 4);
		if (!jarExtension.contains(".jar")) {
			IJ.error("This file " + filename + " is not a jar file.");	
			return false;
		}
		params.inputList.get(inputCounter).parameterPath = filename;
		params.inputList.get(inputCounter).useImage = checkParamNotUseIm.isSelected();
		return true;
	}
	
	/*
	 * Method to retrieve from the UI the information necessary to build an 
	 * image from the tensor inputed to the model
	 */
	public boolean saveInputDataForImage(Parameters params) {
		int[] multiple = new int[params.inputList.get(inputCounter).form.length()];
		int[] patch = new int[params.inputList.get(inputCounter).form.length()];
		int[] step = new int[params.inputList.get(inputCounter).form.length()];
		
		int batchInd = Index.indexOf(params.inputList.get(0).form.split(""), "N");
		multiple[batchInd] = 1; patch[batchInd] = 1; step[batchInd] = 0;
		//step[batchInd] = 1;

		boolean auxDetectError = true;
		try {
			int auxCount = 0;
			for (int c = 0; c < params.inputList.get(inputCounter).tensor_shape.length; c ++) {
				if (c != batchInd) {
					patch[c] = Integer.parseInt(allTxtPatches.get(auxCount).getText());
					auxDetectError = false;
					multiple[c] = Integer.parseInt(allTxtMultiple.get(auxCount).getText());
					auxDetectError = true;
					step[c] = Integer.parseInt(allTxtMultiple.get(auxCount).getText());
					
					if (!allTxtMultiple.get(auxCount).isEditable()) {
						step[c] = 0;
					}
					if (multiple[c] <= 0) {
						IJ.error("The multiple factor size should be larger than 0");
						return false;
					}
					if (patch[c] <= 0) {
						IJ.error("The patch size should be larger than 0");
						return false;
					}
					if (patch[c]%multiple[c] != 0) {
						IJ.error("At dimension " + params.inputList.get(inputCounter).form.split("")[c] + " size " +
								patch[c] + " is not a multiple of "
								+ multiple[c]);
						return false;
					}
					auxCount ++;
				}
			}
		}
		catch (Exception ex) {
			if (auxDetectError) {
				IJ.error("The patch size is not a correct integer");
			} else if (!auxDetectError) {
				IJ.error("The multiple factor size is not a correct integer");
			}
			return false;
		}

		if (cmbPatches.getSelectedIndex() == 1) {
			step = new int[step.length];
		}
		params.inputList.get(inputCounter).minimum_size = multiple;
		params.inputList.get(inputCounter).recommended_patch = patch;
		params.inputList.get(inputCounter).step = step;
		
		return true;
	}
	
	private JPanel buildPanelForParameter(Parameters params) {
		// Build panel for when the input tensor is a parameter and not an 
		// image
		Dimension dim = pnInput.getSize();
		txt.setFont(new Font("Arial", Font.BOLD, 14));
		txt.setForeground(Color.red);
		txt.setPreferredSize(new Dimension(Constants.width, 25));
		JPanel load = new JPanel(new BorderLayout());
		load.setBorder(BorderFactory.createEtchedBorder());
		load.add(txt, BorderLayout.CENTER);
		load.add(bnBrowse, BorderLayout.EAST);
		txt.setDropTarget(new LocalDropJar());
		load.setDropTarget(new LocalDropJar());
		bnBrowse.addActionListener(this);
		
		JPanel paramPn = new JPanel(new GridLayout(2, 1));
		checkParamUseIm.setEnabled(false);
		checkParamNotUseIm.setEnabled(false);
		checkParamUseIm.setSelected(false);
		checkParamNotUseIm.setSelected(false);
		paramPn.add(checkParamUseIm);
		paramPn.add(checkParamNotUseIm);
		
		
		pnInput.removeAll();
		DijTensor tensor = params.inputList.get(inputCounter);
		pnInput.place(0, 0, 1, 2, new JLabel("Input name: " + tensor.name + "\t Input type: " + tensor.tensorType));
		pnInput.place(2, 0, 1, 2, load);
		pnInput.place(4, 0, 1, 2, paramPn);
		pnInput.setPreferredSize(dim);
		
		return pnInput;		
	}
	
	private void buildPanelForImage(DijTensor tensor) {
		// Build panel for when the input tensor is an image

		allTxtMultiple = new ArrayList<JTextField>();
		allTxtPatches = new ArrayList<JTextField>();
		String[] dims = DijTensor.getWorkingDims(tensor.form); 
		
		JPanel pnMultiple = new JPanel(new GridLayout(2, dims.length));
		JPanel pnPatchSize = new JPanel(new GridLayout(2, dims.length));
		
		for (String dim: dims) {
			JLabel dimLetter1 = new JLabel(dim);
			dimLetter1.setPreferredSize( new Dimension( 10, 20 ));
			JLabel dimLetter2 = new JLabel(dim);
			dimLetter2.setPreferredSize( new Dimension( 10, 20 ));
			
			pnMultiple.add(dimLetter1);
			pnPatchSize.add(dimLetter2);
		}
		
		for (int i = 0; i < dims.length; i ++) {
			JTextField txtMultiple = new JTextField("1", 5);
			txtMultiple.setPreferredSize( new Dimension( 10, 20 ));
			JTextField txtPatches = new JTextField("100", 5);
			txtPatches.setPreferredSize( new Dimension( 10, 20 ));
			
			pnMultiple.add(txtMultiple);
			allTxtMultiple.add(txtMultiple);
			//addChangeListener(allTxtMultiple.get(i), e -> updateImageInterface());

			pnPatchSize.add(txtPatches);
			allTxtPatches.add(txtPatches);
		}
		
		pnInput.removeAll();
		pnInput.setBorder(BorderFactory.createEtchedBorder());
		checkAllowPatching.setSelected(true);
		pnInput.place(0, 0, 2, 1, new JLabel("Name: " + tensor.name + "      Input type: " + tensor.tensorType));
		pnInput.place(1, 1, checkAllowPatching);
		pnInput.place(2, 0, lblMultiple);
		pnInput.place(2, 1, pnMultiple);
		pnInput.place(3, 0, 2, 1, cmbPatches);
		pnInput.place(4, 0, lblPatches);
		pnInput.place(4, 1, pnPatchSize);
		GridPanel pnRange1 = new GridPanel(true);
		pnRange1.place(0, 0, new JLabel("Data Range lower bound"));
		pnRange1.place(0, 1, cmbRangeLow);
		pnInput.place(5, 0, pnRange1);
		GridPanel pnRange2 = new GridPanel(true);
		pnRange2.place(0, 0, new JLabel("Data Range higher bound"));
		pnRange2.place(0, 1, cmbRangeHigh);
		pnInput.place(5, 1, pnRange2);
		
		cmbRangeLow.setEditable(false);
		cmbRangeHigh.setEditable(false);
		//return pnInput;
	}
	
	public String optimalPatch(String minimumSizeString, String dimChar) {
		// This method looks for the optimal patch size regarding the
		// minimum patch constraint and image size. This is then suggested
		// to the user
		ImagePlus imp = null;
		String patch;
		int minimumSize = Integer.parseInt(minimumSizeString);
		if (imp == null) {
			imp = WindowManager.getCurrentImage();
		}
		if (imp == null) {
			patch = "100";
			return patch;	
		}
		
		int size = 0;
		switch (dimChar) {
			case "H":
				size = imp.getHeight();
				break;
			case "W":
				size = imp.getWidth();
				break;
			case "D":
				size = imp.getNSlices();
				break;
			case "C":
				size = imp.getNChannels();
				break;
		}
		
		int optimalMult = (int)Math.ceil((double)size / (double)minimumSize) * minimumSize;
		if (optimalMult > 3 * size) {
			optimalMult = optimalMult - minimumSize;
		}
		if (optimalMult > 3 * size) {
			optimalMult = (int)Math.ceil((double)size / (double)minimumSize) * minimumSize;
		}
		patch = Integer.toString(optimalMult);
		return patch;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Parameters params = parent.getDeepPlugin().params;
		if (e.getSource() == bnBrowse) {
			browse();
		}
		if (e.getSource() == bnNextOutput) {
			if (saveInputData(params)) {
				inputCounter ++;
			}
		}
		if (e.getSource() == bnPrevOutput) {
			inputCounter --;
		}
		showCorrespondingInputInterface(params);
		//updateImageInterface();
	}
	
	private void browse() {
		JFileChooser chooser = new JFileChooser(txt.getText());
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("JAR files", "jar");
	    chooser.setFileFilter(filter);
		chooser.setDialogTitle("Select .jar");
		int ret = chooser.showOpenDialog(new JFrame());
		if (ret == JFileChooser.APPROVE_OPTION) {
			txt.setText(chooser.getSelectedFile().getAbsolutePath());
			txt.setCaretPosition(1);
		}
	}
	
	public class LocalDropJar extends DropTarget {

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


	public static void addChangeListener(JTextField text, ChangeListener changeListener) {
		// Method used to "listen" the JTextFields
	    Objects.requireNonNull(text);
	    Objects.requireNonNull(changeListener);
	    DocumentListener dl = new DocumentListener() {
	        private int lastChange = 0, lastNotifiedChange = 0;

	        @Override
	        public void insertUpdate(DocumentEvent e) {
	            changedUpdate(e);
	        }

	        @Override
	        public void removeUpdate(DocumentEvent e) {
	            changedUpdate(e);
	        }

	        @Override
	        public void changedUpdate(DocumentEvent e) {
	            lastChange++;
	            SwingUtilities.invokeLater(() -> {
	                if (lastNotifiedChange != lastChange) {
	                    lastNotifiedChange = lastChange;
	                    changeListener.stateChanged(new ChangeEvent(text));
	                }
	            });
	        }
	    };
	    text.addPropertyChangeListener("document", (PropertyChangeEvent e) -> {
	        Document d1 = (Document)e.getOldValue();
	        Document d2 = (Document)e.getNewValue();
	        if (d1 != null) d1.removeDocumentListener(dl);
	        if (d2 != null) d2.addDocumentListener(dl);
	        dl.changedUpdate(null);
	    });
	    Document d = text.getDocument();
	    if (d != null) d.addDocumentListener(dl);
	}

}

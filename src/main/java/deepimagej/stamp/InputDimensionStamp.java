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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
		

	private static String 				allowPatches = "Allow patch decomposition";
	private static String 				predeterminedInput = "Predetermined input size";
	private static String 				noPatchesFixed = "Do not allow patches (fixed input size)";
	private static String 				noPatchesVariable = "Do not allow patches (variable size)";
	
	private JComboBox<String>			cmbPatches	= new JComboBox<String>(new String[] { allowPatches, predeterminedInput,
																							noPatchesFixed, noPatchesVariable});
	private JLabel						lblPatches	= new JLabel("Patch size");
	private JLabel						lblMultiple	= new JLabel("Multiple factor");

	private JButton 					bnNextOutput 	= new JButton("Next Output");
	private JButton 					bnPrevOutput 	= new JButton("Previous Output");
	private GridPanel					pnInput			= new GridPanel();
	
	private static JComboBox<String>	cmbRangeLow  = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private static JComboBox<String>	cmbRangeHigh = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private static double[] 			rangeOptions = {Double.NEGATIVE_INFINITY, (double) -1, (double) 0, (double) 1, Double.POSITIVE_INFINITY};

	private List<DijTensor> 			imageTensors;
	private static int					inputCounter = 0;
	private String						model		  = "";
	
	private String						shortForm;
	
	// Whether we need to add or not action listener to the text fields
	private boolean						listenTxtField = false;
	
	public InputDimensionStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
		cmbRangeHigh.setSelectedIndex(4);
	}

	@Override
	public void buildPanel() {
		
		HTMLPane info = new HTMLPane(Constants.width, 180);
		info.append("h", "<b>Input size constraints</b>");
		info.append("p", "<b>Patch size (Q) </b>: patch size used to process the image with teh current model. "
				+ "If <i>Allow patch decomposition</i> is selected, changes in <i>Q</i> will be allowed in inference.");
		info.append("p", "<b>Multiple factor (m) </b>: If the network has an auto-encoder architecture, the size of each dimension of the input image, has to be multiple of a minimum size m.");
			
		info.append("h", "<b>Patching strategies</b>");
		info.append("p", "<b>Allow patch decomposition</b>: <i>Q</i> will be editable by the user as long as it fulfils the <i>multiple factor</i> constraint.");
		info.append("p", "<b>Predetermined input size</b>: <i>Q</i> will be fixed. If the image is bigger than <i>Q</i> a tiling approach will be followed.");
		info.append("p", "<b>Do not allow patches (fixed input size)</b>: <i>Q</i> will be fixed. Only images with the same size as <i>Q</i> will be accepted.");
		info.append("p", "<b>Do not allow patches (variable size)</b>: we cannot select <i>Q</i>. The input image will be processed as a whole (no tiling) taking into account the <i>multiple factor</i> constraint.");
		
		GridPanel buttons = new GridPanel(true);
		buttons.setBorder(BorderFactory.createEtchedBorder());
		buttons.place(0, 0, bnPrevOutput);
		buttons.place(0, 1, bnNextOutput);
		
		// Create auxiliary DijTensor to initialise the interface 
		DijTensor auxTensor = new DijTensor("aux");
		auxTensor.tensorType = "image";
		auxTensor.tensor_shape = new int[5];
		auxTensor.form = "BZYXC";
		boolean start = true;
		buildPanelForImage(auxTensor, start);
		
		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(pnInput);
		pn.add(buttons, BorderLayout.SOUTH);
		
		panel.add(pn);	

		bnNextOutput.addActionListener(this);
		bnPrevOutput.addActionListener(this);
	}
	
	@Override
	public void init() {
		//String modelOfInterest = parent.getDeepPlugin().params.path2Model;
		Parameters params = parent.getDeepPlugin().params;
		imageTensors = DijTensor.getImageTensors(params.inputList);
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
		return true;
	}public void showCorrespondingInputInterface(Parameters params) {
		
		// Check how many outputs there are to enable or not
		// the "next" and "back" buttons
		int nImageTensors = imageTensors.size();
		if (inputCounter == 0) {
			bnPrevOutput.setEnabled(false);
		} else {
			//bnPrevOutput.setEnabled(true);
			bnPrevOutput.setEnabled(false);
		}
		if (inputCounter < (nImageTensors - 1)) {
			//bnNextOutput.setEnabled(true);
			bnPrevOutput.setEnabled(false);
		} else {
			bnNextOutput.setEnabled(false);
		}

		// Reinitialise all the params
		allTxtMultiple = new ArrayList<JTextField>();
		allTxtPatches = new ArrayList<JTextField>();
		pnInput.removeAll();
		DijTensor tensor = imageTensors.get(inputCounter);
		if (tensor.tensorType.contains("image")) {
			// Build the panel
			// Set listenTxtField to false because we need to add new
			// listeners to the new text fields
			listenTxtField = false;
			buildPanelForImage(tensor);
			updateImageInterface(tensor);
		} else {
			inputCounter ++;
		}
		pnInput.revalidate();
		pnInput.repaint();
	
	}	

	private void updateImageInterface(DijTensor tensor) {

		String[] dim = DijTensor.getWorkingDims(tensor.form); 
		int[] dimValues = DijTensor.getWorkingDimValues(tensor.form, tensor.tensor_shape); 
		cmbPatches.setEnabled(true);
		
		String selection = (String) cmbPatches.getSelectedItem();

		// Allow patch decomposition
		if (selection.contains(allowPatches)) {
			
			for (int i = 0; i < dim.length; i ++) {
				if (dimValues[i] != -1 && !listenTxtField) {
					allTxtMultiple.get(i).setText("" + dimValues[i]);
					allTxtMultiple.get(i).setEditable(false);
					allTxtPatches.get(i).setText("" + dimValues[i]);
					allTxtPatches.get(i).setEditable(false);
				} else if (dimValues[i] == -1){
					allTxtMultiple.get(i).setEditable(true);
					allTxtPatches.get(i).setEditable(true);
					allTxtPatches.get(i).setText(optimalPatch(allTxtMultiple.get(i).getText(), dim[i]));
				}
			}

		// Predetermined input size
		} else if (selection.contains(predeterminedInput)) {

			for (int i = 0; i < dim.length; i ++) {
				if (dimValues[i] != -1 && !listenTxtField) {
					allTxtMultiple.get(i).setText("" + dimValues[i]);
					allTxtMultiple.get(i).setEditable(false);
					allTxtPatches.get(i).setText("" + dimValues[i]);
					allTxtPatches.get(i).setEditable(false);
				} else if (dimValues[i] == -1){
					allTxtPatches.get(i).setText("" + allTxtMultiple.get(i).getText());
					allTxtMultiple.get(i).setEditable(true);
					allTxtPatches.get(i).setEditable(false);
				}
			}
		
		// Do not allow patches (fixed size)
		} else if (selection.contains(noPatchesFixed)) {

			for (int i = 0; i < dim.length; i ++) {
				if (dimValues[i] != -1 && !listenTxtField) {
					allTxtMultiple.get(i).setText("" + dimValues[i]);
					allTxtPatches.get(i).setText("" + dimValues[i]);
					allTxtMultiple.get(i).setEditable(false);
					allTxtPatches.get(i).setEditable(false);
				} else if (dimValues[i] == -1) {
					allTxtMultiple.get(i).setEditable(true);
					allTxtPatches.get(i).setText(allTxtMultiple.get(i).getText());
					allTxtPatches.get(i).setEditable(false);
				}
			}
			
		// Do not allow patches (variable input size)
		} else if (selection.contains(noPatchesVariable)) {
			
			for (int i = 0; i < dim.length; i ++) {
				if (dimValues[i] != -1 && !listenTxtField) {
					allTxtMultiple.get(i).setText("" + dimValues[i]);
					allTxtMultiple.get(i).setEditable(false);
					allTxtPatches.get(i).setText("" + dimValues[i]);
				} else if (dimValues[i] == -1) {
					allTxtMultiple.get(i).setEditable(true);
					allTxtPatches.get(i).setText(" - ");
				}
				allTxtPatches.get(i).setEditable(false);
			}
		}
		if (!listenTxtField) {
			for (int i = 0; i < allTxtMultiple.size(); i ++)
				addChangeListener(allTxtMultiple.get(i), e -> updateImageInterface(tensor));
			listenTxtField = true;
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
	 * Method to retrieve from the UI the information necessary to build an 
	 * image from the tensor inputed to the model
	 */
	public boolean saveInputDataForImage(Parameters params) {
		params.fixedInput = false;
		int[] multiple = new int[params.inputList.get(inputCounter).form.length()];
		int[] patch = new int[params.inputList.get(inputCounter).form.length()];
		int[] step = new int[params.inputList.get(inputCounter).form.length()];
		
		int batchInd = Index.indexOf(params.inputList.get(0).form.split(""), "B");
		multiple[batchInd] = 1; patch[batchInd] = 1; step[batchInd] = 0;
		//step[batchInd] = 1;

		boolean auxDetectError = true;
		try {
			int auxCount = 0;
			for (int c = 0; c < params.inputList.get(inputCounter).tensor_shape.length; c ++) {
				if (c != batchInd) {
					String selectedPatch = allTxtPatches.get(auxCount).getText();
					selectedPatch = selectedPatch.equals(" - ") ? "-1" : selectedPatch;
					patch[c] = Integer.parseInt(selectedPatch);
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
					if (patch[c] <= 0 && !params.pyramidalNetwork) {
						IJ.error("The patch size should be larger than 0");
						return false;
					}
					if (patch[c]%multiple[c] != 0 && !params.pyramidalNetwork) {
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
		
		String selection = (String) cmbPatches.getSelectedItem();
		
		if (selection.contains(allowPatches)) {
			params.allowPatching = true;
		} else if (selection.contains(predeterminedInput)) {
			params.allowPatching = true;
			params.fixedInput = true;
			step = new int[step.length];
		} else if (selection.contains(noPatchesFixed)) {
			// The patch is always the same. No step because
			// no more sizes are allowed
			params.fixedInput = true;
			params.allowPatching = false;
			step = new int[step.length];
		} else if (selection.contains(noPatchesVariable)) {
			// This means that the patch will always be the biggest possible
			params.allowPatching = false;
			patch = new int[patch.length];
		}
		
		params.inputList.get(inputCounter).minimum_size = multiple;
		params.inputList.get(inputCounter).recommended_patch = patch;
		params.inputList.get(inputCounter).step = step;
		
		return true;
	}
	
	private void buildPanelForImage(DijTensor tensor) {
		buildPanelForImage(tensor, false);
	}
	
	private void buildPanelForImage(DijTensor tensor, boolean start) {
		// Build panel for when the input tensor is an image

		allTxtMultiple = new ArrayList<JTextField>();
		allTxtPatches = new ArrayList<JTextField>();
		String[] dims = DijTensor.getWorkingDims(tensor.form); 
		
		JPanel pnMultiple = new JPanel(new GridLayout(2, dims.length));
		JPanel pnPatchSize = new JPanel(new GridLayout(2, dims.length));
		
		shortForm = "";
		for (String dim: dims) {
			JLabel dimLetter1 = new JLabel(dim);
			dimLetter1.setPreferredSize( new Dimension( 10, 20 ));
			JLabel dimLetter2 = new JLabel(dim);
			dimLetter2.setPreferredSize( new Dimension( 10, 20 ));
			
			pnMultiple.add(dimLetter1);
			pnPatchSize.add(dimLetter2);
			
			shortForm += dim;
		}
		
		for (int i = 0; i < dims.length; i ++) {
			JTextField txtMultiple = new JTextField("1", 5);
			txtMultiple.setPreferredSize( new Dimension( 10, 20 ));
			JTextField txtPatches = new JTextField("100", 5);
			txtPatches.setPreferredSize( new Dimension( 10, 20 ));
			
			pnMultiple.add(txtMultiple);
			allTxtMultiple.add(txtMultiple);

			pnPatchSize.add(txtPatches);
			allTxtPatches.add(txtPatches);
		}
		
		// If we are building the screen at the start of the plugin, 
		// and there are no params defined, just start by default
		if (!start) {
			Parameters params = parent.getDeepPlugin().params;
			int[] dimValues = DijTensor.getWorkingDimValues(tensor.form, tensor.tensor_shape); 
			if ((!params.allowPatching || params.pyramidalNetwork) && Index.indexOf(dimValues, -1) != -1) {
				// If we do not allow patching, do no show the corresponding options in the combobox
				cmbPatches	= new JComboBox<String>(new String[] {noPatchesFixed, noPatchesVariable});
			} else if ((!params.allowPatching || params.pyramidalNetwork) && Index.indexOf(dimValues, -1) == -1) {
				// If we do not allow patching and the size is fixed by the model, show just the only option
				cmbPatches	= new JComboBox<String>(new String[] {"Do not allow patches (fixed input size)"});
			} else if (params.allowPatching && !params.pyramidalNetwork && Index.indexOf(dimValues, -1) == -1) {
				// If we  allow patching but the size is fixed by the model, do not show the two options that
				// allow freedom in the input image
				cmbPatches	= new JComboBox<String>(new String[] {predeterminedInput, noPatchesFixed});
			} else {
				// With no restrictions show everything
				cmbPatches	= new JComboBox<String>(new String[] { allowPatches, predeterminedInput,
																	noPatchesFixed, noPatchesVariable});
			}
		} else {
			cmbPatches	= new JComboBox<String>(new String[] { allowPatches, predeterminedInput,
																noPatchesFixed, noPatchesVariable});
		}
		
		pnInput.removeAll();
		pnInput.setBorder(BorderFactory.createEtchedBorder());
		//checkAllowPatching.setSelected(true);
		pnInput.place(0, 0, 2, 1, new JLabel("Name: " + tensor.name + "      Input type: " + tensor.tensorType));
		//pnInput.place(1, 1, checkAllowPatching);
		pnInput.place(1, 0, lblMultiple);
		pnInput.place(1, 1, pnMultiple);
		pnInput.place(2, 0, 2, 1, cmbPatches);
		pnInput.place(3, 0, lblPatches);
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
		
		cmbPatches.addActionListener(this);
	}
	
	public String optimalPatch(String minimumSizeString, String dimChar) {
		// This method looks for the optimal patch size regarding the
		// minimum patch constraint and image size. This is then suggested
		// to the user
		ImagePlus imp = null;
		int ind = shortForm.indexOf(dimChar);
		int currentSize = Integer.parseInt(allTxtPatches.get(ind).getText().trim());
		String patch;
		if (minimumSizeString.equals(""))
			return "" + currentSize;
		int minimumSize = Integer.parseInt(minimumSizeString);
		
		if (imp == null) {
			imp = WindowManager.getCurrentImage();
		}
		if (imp == null && currentSize % minimumSize == 0) {
			patch = "" + currentSize;
			return patch;	
		} else if (imp == null && currentSize % minimumSize != 0) {
			patch = "" + (((int) currentSize / minimumSize) + 1) * minimumSize;
			return patch;	
		} else if (imp == null) {
			patch = "100";
			return patch;	
		}
		
		int size = 0;
		switch (dimChar) {
			case "Y":
				size = imp.getHeight();
				break;
			case "X":
				size = imp.getWidth();
				break;
			case "Z":
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
		if (e.getSource() == bnNextOutput) {
			if (saveInputData(params)) {
				inputCounter ++;
				showCorrespondingInputInterface(params);
			}
		}
		if (e.getSource() == bnPrevOutput) {
			inputCounter --;
			showCorrespondingInputInterface(params);
		}
		if (e.getSource() == cmbPatches) {
			updateImageInterface(params.inputList.get(inputCounter));
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

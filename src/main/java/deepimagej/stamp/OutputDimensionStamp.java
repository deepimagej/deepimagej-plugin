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
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import ij.IJ;

public class OutputDimensionStamp extends AbstractStamp implements ActionListener {

	private static List<JTextField> firstRowList	= new ArrayList<JTextField>();
	private static List<JTextField> secondRowList	= new ArrayList<JTextField>();
	private static List<JTextField> thirdRowList	= new ArrayList<JTextField>();

	private static List<JComboBox<String>>  cmbRowList		= new ArrayList<JComboBox<String>>();
	
	private static GridPanel		pnOutputInfo	= new GridPanel(true);
	private static GridPanel		firstRow		= new GridPanel(true);
	private static GridPanel		secondRow		= new GridPanel(true);
	private static GridPanel		thirdRow		= new GridPanel(true);
	private static JLabel			firstLabel		= new JLabel("Scaling factor");
	private static JLabel			secondLabel		= new JLabel("Halo factor");
	private static JLabel			thirdLabel		= new JLabel("Offset factor");
	private static JPanel 			pn				 = new JPanel();

	private static JComboBox<String>referenceImage 	= new JComboBox<String>(new String[] {"aux"});
	private static JLabel			refLabel		= new JLabel("Reference input image");
	private static JLabel			lblName			= new JLabel("Name");

	private static JLabel			lblType			= new JLabel("Output type: ");
	

	private static JButton 			bnNextOutput 	= new JButton("Next Output");
	private static JButton 			bnPrevOutput 	= new JButton("Previous Output");
	
	private static int				outputCounter	= 0;
	private String					model		  = "";
	
	private static double[] 		rangeOptions = {Double.NEGATIVE_INFINITY, (double) -1, (double) 0, (double) 1, Double.POSITIVE_INFINITY};
	
	private static JComboBox<String>	cmbRangeLow = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private static JComboBox<String>	cmbRangeHigh = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	
	 // Parameter to observe if there has been any change in form or tensor type
	private static List<DijTensor>	savedInputs   = null;
	

	public OutputDimensionStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
		cmbRangeHigh.setSelectedIndex(4);
	}

	@Override
	public void buildPanel() {
		
		HTMLPane info = new HTMLPane(Constants.width, 150);
		info.append("h2", "Output size constraints");
		info.append("p", "output size = input size * scale + offset");
		info.append("p", "valid output size = input size * scale + offset - 2*halo");
		info.append("p", "<b>Scaling factor</b>: the factor by which the output image "
				+ "dimensions are rescaled. E.g. in superresolution, if the output size "
				+ "is twice the size of the input, the scaling factor should be [2,2]. See the equation.");
		info.append("p", "<b>Offset factor</b>: Difference between the input and output size. Note that "
				+ "this is different from a scaling factor. See the equation.");
		info.append("p", "<b>Halo facto</b>: Size of the receptive field of one pixel in the "
				+ "network used to avoid artifacts along the borders of the image. If the "
				+ "convolutions inside the network do not use padding, set this value to 0.");

		firstRow.place(0, 0, firstLabel);
		firstRow.place(0, 1, new JLabel("aux")); firstRow.place(1, 1, new JTextField("aux"));
		secondRow.place(0, 0, secondLabel);
		secondRow.place(0, 1, new JLabel("aux")); secondRow.place(1, 1, new JTextField("aux"));
		thirdRow.place(0, 0, thirdLabel);
		thirdRow.place(0, 1, new JLabel("aux")); thirdRow.place(1, 1, new JTextField("aux"));

		JFrame pnFr = new JFrame();
		Container cn = pnFr.getContentPane();
		cn.setLayout(new GridBagLayout()); 

		GridBagConstraints  labelC = new GridBagConstraints();
		labelC.gridwidth = 2;
		labelC.gridheight = 1;
		labelC.gridx = 0;
		labelC.gridy = 0;
		labelC.ipadx = 0;
		labelC.weightx = 1;
		labelC.insets = new Insets(10, 10, 0, 5); 
		
		// Set the output name
		lblName.setText("Output name: NAME");
		cn.add(lblName, labelC);
		// Set the output name
		labelC.insets = new Insets(5, 10, 0, 5); 
		labelC.gridy = 1;
		cn.add(lblType, labelC);
		
		// Set the reference image
		labelC.gridy = 2;
		cn.add(refLabel, labelC);
		labelC.gridwidth = 1;
		labelC.ipadx = 2;
		labelC.gridy = 2;
		labelC.gridx = 2;
		labelC.weightx = 0.2;
		labelC.insets = new Insets(0, 10, 0, 5); 
		cn.add(referenceImage, labelC);

				
		labelC.gridwidth = 1;
		labelC.gridheight = 1;
		labelC.gridx = 0;
		labelC.gridy = 3;
		labelC.ipadx = 5;
		labelC.weightx = 0.1;
		labelC.insets = new Insets(10, 20, 5, 5); 

		GridBagConstraints  infoC = new GridBagConstraints();
		infoC.gridwidth = 10;
		infoC.gridheight = 3;
		infoC.gridx = 0;
		infoC.gridy = 3;
		infoC.ipadx = 15;
		infoC.ipady = 10;
		infoC.weightx = 0.9;
		infoC.anchor = GridBagConstraints.CENTER;
	    infoC.fill = GridBagConstraints.BOTH;
	    infoC.insets = new Insets(5, 20, 5, 20); 

		// First field
		cn.add(firstRow, infoC);

		// Second field
		labelC.gridy = 6;
		infoC.gridy = 6;
		cn.add(secondRow, infoC);

		// Third field
		labelC.gridy = 9;
		infoC.gridy = 9;
		cn.add(thirdRow, infoC);
		
		// Data range combo boxes
		labelC.gridy = 13;
		infoC.gridy = 13;
		infoC.gridx = 2;
		infoC.gridwidth = 1;
		infoC.ipadx = 5;
		infoC.ipady = 2;
		labelC.insets = new Insets(5, 20, 5, 5); 
	    infoC.insets = new Insets(5, 5, 5, 15); 
		cn.add(new JLabel("Data Range: lower bound"), labelC);
		cn.add(cmbRangeLow, infoC);

		labelC.gridx = 6;
		labelC.insets = new Insets(5, 15, 5, 5); 
	    infoC.insets = new Insets(5, 5, 5, 20); 
		cn.add(new JLabel("Data Range: upper bound"), labelC);
		infoC.gridx = 8;
		cn.add(cmbRangeHigh, infoC);

		pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(cn);


		JPanel pnButtons = new JPanel(new GridLayout(1, 2));
		pnButtons.setBorder(BorderFactory.createEtchedBorder());
		pnButtons.add(bnPrevOutput);
		pnButtons.add(bnNextOutput);
		pn.add(pnButtons, BorderLayout.SOUTH);
		
		panel.add(pn);
		
		bnNextOutput.addActionListener(this);
		bnPrevOutput.addActionListener(this);
		
	}
	
	@Override
	public void init() {
		Parameters params = parent.getDeepPlugin().params;
		// Set the screen at the first input if the model changes
		String modelOfInterest = params.path2Model;
		if (!modelOfInterest.equals(model)) {
			model = modelOfInterest;
			savedInputs = DijTensor.copyTensorList(params.outputList);
			outputCounter = 0;
		} else if (params.outputList.size() != savedInputs.size()) {
			savedInputs = DijTensor.copyTensorList(params.outputList);
			outputCounter = 0;
		} else {
			// Check if any output tensor definition has changed, an if it has
			// put the attention on it
			boolean changed = false;
			for (int i = 0; i < params.outputList.size(); i ++) {
				boolean sameTensor = params.outputList.get(i).name.equals(savedInputs.get(i).name);
				boolean sameForm = params.outputList.get(i).form.equals(savedInputs.get(i).form);
				boolean sameType = params.outputList.get(i).tensorType.equals(savedInputs.get(i).tensorType) ;
				if (!sameTensor || !sameType || !sameForm) {
					// Ask the user to repeat the tensor changed
					params.outputList.get(i).finished = false;
					// Start at the first tensor changed
					if (!changed) {
						savedInputs = DijTensor.copyTensorList(params.outputList);
						outputCounter = i;
						changed = true;
					}
				}
				
			}
		}
		bnNextOutput.setEnabled(true);
		bnPrevOutput.setEnabled(true);
		referenceImage.removeAllItems();
		for (DijTensor in : params.inputList) {
			if (in.tensorType.contains("image"))
				referenceImage.addItem(in.name);
		}
		updateInterface(params);
		
	
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		saveOutputData(params);
		for (DijTensor tensor : params.outputList) {
			if (!tensor.finished){
				IJ.error("You need to fill information for every input tensor");
				return false;
			}
		}
		
		return true;
	}
	
	public static void updateInterface(Parameters params) {
		
		// Check how many outputs there are to enable or not
		// the "next" and "back" buttons
		if (outputCounter == 0) {
			bnPrevOutput.setEnabled(false);
		} else {
			bnPrevOutput.setEnabled(true);
		}
		if (outputCounter < (params.outputList.size() - 1)) {
			bnNextOutput.setEnabled(true);
		} else {
			bnNextOutput.setEnabled(false);
		}

		lblName.setText("Output name: " + params.outputList.get(outputCounter).name);
		lblType.setText("Output type: " + params.outputList.get(outputCounter).tensorType);
		// Reinitialise all the params
		pnOutputInfo.removeAll(); firstRow.removeAll(); secondRow.removeAll(); thirdRow.removeAll();
		firstRowList = new ArrayList<JTextField>(); secondRowList = new ArrayList<JTextField>(); thirdRowList = new ArrayList<JTextField>();
		pn.remove(0);
		if (params.outputList.get(outputCounter).tensorType.contains("image") && !params.pyramidalNetwork) {
			// Build panel for image
			writeInfoText("image");
			getPanelForImage(params);
		} else if (params.outputList.get(outputCounter).tensorType.contains("image") && params.pyramidalNetwork) {
			// Build panel for pyramidal net
			writeInfoText("pyramidalImage");
			getPanelForImagePyramidalNet(params);
		}else if (params.outputList.get(outputCounter).tensorType.contains("list")) {
			// Build panel for list
			writeInfoText("list");
			getPanelForList(params);
		} else {
			outputCounter ++;
		}
		pnOutputInfo.revalidate();
		pnOutputInfo.repaint();
	
	}
	
	/*
	 * Method to retrieve from the UI the information necessary to build a 
	 * list from the tensor outputed by the model
	 */
	public static boolean saveOutputDataForList(Parameters params) {
		// There is no offset or halo in the case of the outùt being a list.
		// There is also no scale, but for convenience we will set it to 1.
		DijTensor tensor = params.outputList.get(outputCounter);
		tensor.scale = new float[tensor.tensor_shape.length];
		tensor.halo = new int[tensor.tensor_shape.length];
		tensor.offset = new int[tensor.tensor_shape.length];
		// Set the scale equal to 1 for every dimension
		for (int i = 0; i < tensor.scale.length; i ++)
			tensor.scale[i] = 1;
		// Now do the important thing in this step. Change the dimension letters
		// by C if it correspond to the column, or R if it corresponds to row
		int batchInd = DijTensor.getBatchInd(tensor.form);
		// Form containing rows and cols
		String newForm = "";
		
		int cmbCount = 0;
		for (int i = 0; i < params.outputList.get(outputCounter).scale.length; i++) {
			if (i == batchInd) {
				newForm = newForm + "B";
			} else {
				String selectedItem = String.valueOf(cmbRowList.get(cmbCount).getSelectedItem());
				String letter = selectedItem.split("")[0];
				cmbCount ++;
				if (newForm.indexOf(letter) == -1) {
					newForm = newForm + letter;
				} else {
					IJ.error("You cannot select the same field in both combo boxes.");
					return false;
				}
			}
		}
		tensor.form = newForm;
		return true;
	}
	
	/*
	 * Method to retrieve from the UI the information necessary to build an 
	 * image from the tensor outputed by the model
	 */
	public static boolean saveOutputDataForImage(Parameters params) {
		// Save all the information for the output given by the variable 'outputInd'
		String ref = (String) referenceImage.getSelectedItem();
		params.outputList.get(outputCounter).referenceImage = ref;
		
		params.outputList.get(outputCounter).scale = new float[params.outputList.get(outputCounter).tensor_shape.length];
		params.outputList.get(outputCounter).halo = new int[params.outputList.get(outputCounter).tensor_shape.length];
		params.outputList.get(outputCounter).offset = new int[params.outputList.get(outputCounter).tensor_shape.length];
		int batchInd = DijTensor.getBatchInd(params.outputList.get(outputCounter).form);
		
		int textFieldInd = 0;
		for (int i = 0; i < params.outputList.get(outputCounter).scale.length; i++) {
			try {
				float scaleValue =  1; int haloValue = 0; int offsetValue = 0;
				if (i == batchInd) {
					params.outputList.get(outputCounter).scale[i] = 1;
					params.outputList.get(outputCounter).halo[i] = 0;
					params.outputList.get(outputCounter).offset[i] = 0;
				} else {
					// If the value for scale is "-" because there is no dimension in the reference image,
					// save it as -1
					scaleValue = Float.valueOf(firstRowList.get(textFieldInd).isEditable() ? firstRowList.get(textFieldInd).getText() : "-1");
					params.outputList.get(outputCounter).scale[i] = scaleValue;
					haloValue = Integer.valueOf(secondRowList.get(textFieldInd).getText());
					params.outputList.get(outputCounter).halo[i] = haloValue;
					// If the value for offset is "-" because there is no dimension in the reference image,
					// save it as -1
					offsetValue = Integer.parseInt(thirdRowList.get(textFieldInd).isEditable() ? thirdRowList.get(textFieldInd).getText() : "-1");
					params.outputList.get(outputCounter).offset[i] = offsetValue;
					textFieldInd++;
				}
			} catch( NumberFormatException ex) {
				IJ.error("Make sure that no text field is empty and\n"
						+ "that they correspond to real numbers.");
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Method to retrieve from the UI the information necessary to build an 
	 * image from the tensor outputed by the model in the case the model has
	 * a Pyramidal structure
	 */
	public static boolean saveOutputDataForImagePyramidalNet(Parameters params) {
		// Save all the information for the output given by the variable 'outputInd'
		// Get the reference tensor
		
		params.outputList.get(outputCounter).sizeOutputPyramid = new int[params.outputList.get(outputCounter).tensor_shape.length];
		int batchInd = DijTensor.getBatchInd(params.outputList.get(outputCounter).form);
		
		int textFieldInd = 0;
		for (int i = 0; i < params.outputList.get(outputCounter).sizeOutputPyramid.length; i++) {
			try {
				int sizeOutputPyramid =  1;
				if (i == batchInd) {
					params.outputList.get(outputCounter).sizeOutputPyramid[i] = 1;
				} else {
					sizeOutputPyramid = Integer.valueOf(firstRowList.get(textFieldInd ++).getText());
					params.outputList.get(outputCounter).sizeOutputPyramid[i] = sizeOutputPyramid;
				}
			} catch( NumberFormatException ex) {
				IJ.error("Make sure that no text field is empty and\n"
						+ "that they correspond to real numbers.");
				return false;
			}
		}
		return true;
	}
	
	/*
	 * Method to retrieve from the UI the information necessary to build  
	 * whatever object is needed for the output tensor
	 */
	public static boolean saveOutputData(Parameters params) {
		// If the methods saving the info were successful, wasSaved=true
		boolean wasSaved = false;
		if (params.outputList.get(outputCounter).tensorType.contains("image") && !params.pyramidalNetwork) {
			wasSaved = saveOutputDataForImage(params);
		} else if (params.outputList.get(outputCounter).tensorType.contains("image") && params.pyramidalNetwork) {
			wasSaved = saveOutputDataForImagePyramidalNet(params);
		} else {
			wasSaved = saveOutputDataForList(params);
		}

		int lowInd = cmbRangeLow.getSelectedIndex();
		int highInd = cmbRangeHigh.getSelectedIndex();
		if (lowInd >= highInd) {
			IJ.error("The Data Range has to go from a value to a higher one.");
			return false;
		}
		
		params.outputList.get(outputCounter).dataRange[0] = rangeOptions[lowInd];
		params.outputList.get(outputCounter).dataRange[1] = rangeOptions[highInd];
		params.outputList.get(outputCounter).finished = wasSaved;
		
		//completeInfo[outputCounter] = wasSaved;
		return wasSaved;
	}

	private static void getPanelForImagePyramidalNet(Parameters params) {
		
		int[] dimValues = DijTensor.getWorkingDimValues(params.outputList.get(outputCounter).form, params.outputList.get(outputCounter).tensor_shape); 
		String[] dims = DijTensor.getWorkingDims(params.outputList.get(outputCounter).form);


		firstLabel.setText("Output size");
		firstLabel.setVisible(true);
		firstRow.place(0, 0, firstLabel);
		
		for (int i = 0; i < dimValues.length; i ++) {
			JLabel dimLetter1 = new JLabel(dims[i].toLowerCase().contains("z") ? "N/i/z" : dims[i]);
			JTextField txt1;
			
			int auxInd = params.outputList.get(outputCounter).form.indexOf(dims[i]);

			txt1 = new JTextField(params.outputList.get(outputCounter).finished ? "" + params.outputList.get(outputCounter).sizeOutputPyramid[auxInd] : "1", 5);
			txt1.setEditable(true);
			if (dimValues[i] != -1) {
				txt1.setText("" + dimValues[i]);
				txt1.setEditable(false);
			} else if (dimValues[i] == -1) {
				txt1.setText("" + 0);
				txt1.setEditable(true);
			}

			firstRow.place(0, i + 1, dimLetter1);
			firstRow.place(1, i + 1, txt1);
			
			firstRowList.add(txt1);
		}

		secondRow.setVisible(false);
		thirdRow.setVisible(false);
		
		refLabel.setVisible(false);
		referenceImage.setVisible(false);
		
		firstRow.revalidate();
		firstRow.repaint();
		
	}
	
	private static void getPanelForImage(Parameters params) {
		
		int[] dimValues = DijTensor.getWorkingDimValues(params.outputList.get(outputCounter).form, params.outputList.get(outputCounter).tensor_shape); 
		String[] dims = DijTensor.getWorkingDims(params.outputList.get(outputCounter).form);
		// Get the reference tensor and its working dimensions
		DijTensor refTensor = DijTensor.retrieveByName((String) referenceImage.getSelectedItem(), params.inputList);
		String[] refDims = DijTensor.getWorkingDims(refTensor.form);

		for (int i = 0; i < dimValues.length; i ++) {
			JLabel dimLetter1 = new JLabel(dims[i]);
			JLabel dimLetter2 = new JLabel(dims[i]);
			JLabel dimLetter3 = new JLabel(dims[i]);
			JTextField txt1;
			JTextField txt2;
			JTextField txt3;
			
			int auxInd = params.outputList.get(outputCounter).form.indexOf(dims[i]);

			txt1 = new JTextField(params.outputList.get(outputCounter).finished ? "" + params.outputList.get(outputCounter).scale[auxInd] : "1", 5);
			txt2 = new JTextField(params.outputList.get(outputCounter).finished && params.allowPatching ? "" + params.outputList.get(outputCounter).halo[auxInd] : "0", 5);
			txt3 = new JTextField(params.outputList.get(outputCounter).finished ? "" + params.outputList.get(outputCounter).offset[auxInd] : "0", 5);
			// Scale and offset are always editable
			txt1.setEditable(true);
			txt3.setEditable(true);
			// If we do not allow patching, do not allow using halo
			txt2.setEditable(params.allowPatching);
			
			int inputFixedSize = findFixedInput(refTensor, dims[i]);
			
			if (dimValues[i] != -1 && inputFixedSize != -1) {
				float scale = ((float) dimValues[i]) / ((float) inputFixedSize);
				txt1.setText("" + scale);
				txt1.setEditable(true);
			} else if (!refDims.toString().contains(dims[i])) {
				// If the reference image does not contain the output
				//dimension, the output size for that dimension will just be 
				// whatever comes out of the model
				txt1.setText(" - "); txt1.setEditable(false);
				txt2.setText("0"); txt2.setEditable(false);
				txt3.setText(" - "); txt3.setEditable(false);
			}

			firstRow.place(0, i + 1, dimLetter1);
			firstRow.place(1, i + 1, txt1);
			secondRow.place(0, i + 1, dimLetter2);
			secondRow.place(1, i + 1, txt2);
			thirdRow.place(0, i + 1, dimLetter3);
			thirdRow.place(1, i + 1, txt3);
			
			firstRowList.add(txt1);
			secondRowList.add(txt2);
			thirdRowList.add(txt3);
		}
		refLabel.setVisible(true);
		referenceImage.setVisible(true);

		firstLabel.setText("Scaling factor");
		secondLabel.setText("Halo factor");
		thirdLabel.setText("Offset factor");
		
		firstRow.place(0, 0, firstLabel);		
		secondRow.place(0, 0, secondLabel);		
		thirdRow.place(0, 0, thirdLabel);
		
		secondRow.setVisible(true);
		thirdRow.setVisible(true);
		firstLabel.setVisible(true);
		secondLabel.setVisible(true);
		thirdLabel.setVisible(true);

		firstRow.revalidate();;
		firstRow.repaint();
		secondRow.revalidate();;
		secondRow.repaint();
		thirdRow.revalidate();;
		thirdRow.repaint();
	}
	
	/*
	 * Create Jpanel corresponding to list output
	 */
	private static void getPanelForList(Parameters params) {
		
		cmbRowList = new ArrayList<JComboBox<String>>();
		
		int[] dimValues = DijTensor.getWorkingDimValues(params.outputList.get(outputCounter).form, params.outputList.get(outputCounter).tensor_shape); 
		String[] dims = DijTensor.getWorkingDims(params.outputList.get(outputCounter).auxForm);
		
		String[] newDims = DijTensor.getWorkingDims(params.outputList.get(outputCounter).form);
		if (params.outputList.get(outputCounter).form.contains("R") || params.outputList.get(outputCounter).form.contains("C")) {
			newDims = DijTensor.getWorkingDims(params.outputList.get(outputCounter).form);
		}

		for (int i = 0; i < dimValues.length; i ++) {
			JLabel dimLetter1 = new JLabel(""+ dims[i] + " (size=" + dimValues[i] + ")");
			JComboBox<String> txt1;

			txt1 = new JComboBox<String>(new String[] {"Rows", "Columns"});
			if (newDims != null)
				txt1.setSelectedIndex(newDims[i].equals("R") ? 0 : 1);
			txt1.setEditable(false);
			firstRow.place(0, i + 1, dimLetter1);
			firstRow.place(1, i + 1, txt1);
			cmbRowList.add(txt1);
		}

		refLabel.setVisible(false);
		referenceImage.setVisible(false);
		
		firstLabel.setVisible(false);
		secondLabel.setVisible(false);
		thirdLabel.setVisible(false);
		secondRow.setVisible(false);
		thirdRow.setVisible(false);
		firstRow.revalidate();
		firstRow.repaint();
	}
	
	private static int findFixedInput(DijTensor referenceTensor, String dim) {
		int fixed = -1;
		if (referenceTensor != null) {
			int ind = Index.indexOf(referenceTensor.form.split(""), dim);
			if (ind != -1 && referenceTensor.step[ind] == 0) {
				fixed = referenceTensor.minimum_size[ind];
			}
		}
		return fixed;
	}
	
	public static void writeInfoText(String definition) {
		HTMLPane info = new HTMLPane(Constants.width, 150);
		if (definition.contains("image")) {
			info.append("h", "<b>Output size constraints</b><ul>");
			info.append("li", "<p>output size = input size * scale + offset</p>");
			info.append("li", "<p>valid output size = input size * scale + offset - 2*halo</p>");
			info.append("</ul>");
			info.append("p", "<b>Scaling factor</b>: the factor by which the output image "
					+ "dimensions are rescaled. E.g. in superresolution, if the output size "
					+ "is twice the size of the input, the scaling factor should be [2,2]. See the equation.");
			info.append("p", "<b>Offset factor</b>: Difference between the input and output size. Note that "
					+ "this is different from a scaling factor. See the equation.");
			info.append("p", "<b>Halo facto</b>: Size of the receptive field of one pixel in the "
					+ "network used to avoid artifacts along the borders of the image. If the "
					+ "convolutions inside the network do not use padding, set this value to 0.");
		} else if (definition.contains("pyramidalImage")) {
			info.append("h", "<b>Output size constraints</b>");
			info.append("p", "<b>Output size</b>: Fixed output size of the model");
		} else if (definition.contains("list")) {
			info.append("h", "<b>Output size constraints</b>");
			info.append("p", "<b>Choose the dimension corresponding to rows and the dimension "
					+ "corresponding to columns.");
		} 
		pn.add(info.getPane(), 0);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Parameters params = parent.getDeepPlugin().params;
		if (e.getSource() == bnNextOutput) {
			if (saveOutputData(params)) {
				outputCounter ++;
			}
		} else if (e.getSource() == bnPrevOutput) {
			outputCounter --;
		}
		updateInterface(params);
	}

}

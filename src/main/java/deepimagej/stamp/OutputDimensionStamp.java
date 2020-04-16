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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
	
	private static GridPanel		pnOutputInfo	= new GridPanel(true);
	private static GridPanel		firstRow		= new GridPanel(true);
	private static GridPanel		secondRow		= new GridPanel(true);
	private static GridPanel		thirdRow		= new GridPanel(true);
	private static JCheckBox		checkIsImage 	= new JCheckBox("Output is an image.");
	private static JComboBox<String>referenceImage 	= new JComboBox<String>();
	private static JLabel			lblName			= new JLabel("Name");
	private static JLabel			lblFirst		= new JLabel("Scaling factor");
	private static JLabel			lblSecond		= new JLabel("Halo factor");
	private static JLabel			lblThird		= new JLabel("Substracting factor");

	private static JButton 			bnNextOutput 	= new JButton("Next Output");
	private static JButton 			bnPrevOutput 	= new JButton("Previous Output");
	
	private static int				outputInd		= 0;
	private static boolean[]		completeInfo;
	
	private static JComboBox<String>	cmbRangeLow = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private static JComboBox<String>	cmbRangeHigh = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	

	public OutputDimensionStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
		cmbRangeHigh.setSelectedIndex(4);
	}

	@Override
	public void buildPanel() {
		
		HTMLPane info = new HTMLPane(Constants.width, 180);
		info.append("h2", "Input size constraints");
		info.append("p", "<b>Patch size (Q) </b>: If the network has not a predetermined input size, patch decomposition of default size <i>Q</i> is allowed.");
		info.append("p", "<b>Padding (P) </b>: To preserve the input size at the output, convolutions are calculated using zero padding boundary conditions of size <i>P</i>.");
		info.append("p", "<b>Multiple factor (m) </b>: If the network has an auto-encoder architecture, the size of each dimension of the input image, has to be multiple of a minimum size m.");
		
		pnOutputInfo.setBorder(BorderFactory.createEtchedBorder());
		checkIsImage.setSelected(true);
		pnOutputInfo.place(0, 0, lblName);
		pnOutputInfo.place(0, 1, checkIsImage);
		pnOutputInfo.place(1, 1, referenceImage);
		referenceImage.setEditable(false);
		pnOutputInfo.place(2, 1, firstRow);
		pnOutputInfo.place(3, 1, secondRow);
		pnOutputInfo.place(4, 1, thirdRow);
		GridPanel pnRange1 = new GridPanel(true);
		JLabel lblRange1 = new JLabel("Data Range lower bound");
		pnRange1.place(0, 0, lblRange1);
		pnRange1.place(0, 1, cmbRangeLow);
		
		GridPanel pnRange2 = new GridPanel(true);
		JLabel lblRange2 = new JLabel("Data Range lower bound");
		pnRange2.place(0, 0, lblRange2);
		pnRange2.place(0, 1, cmbRangeHigh);

		GridPanel pnRange = new GridPanel(true);
		pnRange.place(0, 0, pnRange1);
		pnRange.place(0, 1, pnRange2);
		pnOutputInfo.place(5, 1, pnRange);
		
		lblRange1.setVisible(true);
		lblRange2.setVisible(true);
		
		cmbRangeLow.setVisible(true);
		cmbRangeHigh.setVisible(true);
		
		cmbRangeLow.setEditable(false);
		cmbRangeHigh.setEditable(false);

		GridPanel buttons = new GridPanel(true);
		buttons.setBorder(BorderFactory.createEtchedBorder());
		buttons.place(0, 0, bnPrevOutput);
		buttons.place(0, 1, bnNextOutput);
		
		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(pnOutputInfo);
		pn.add(buttons, BorderLayout.SOUTH);
		
		panel.removeAll();
		panel.add(pn);
		
		checkIsImage.addActionListener(this);
		
		bnNextOutput.addActionListener(this);
		bnPrevOutput.addActionListener(this);
		
	}
	
	@Override
	public void init() {
		Parameters params = parent.getDeepPlugin().params;
		completeInfo = new boolean[params.outputList.size()];
		bnNextOutput.setEnabled(true);
		bnPrevOutput.setEnabled(true);
		referenceImage.removeAllItems();
		for (DijTensor inp : params.inputList) {
			referenceImage.addItem(inp.name);
		}
		updateInterface(params);
	
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		saveOutputData(params);
		for (boolean completed: completeInfo) {
			if (!completed){
				IJ.error("You need to fill information of every output");
				return false;
			}
		}
		
		return true;
	}
	
	public static void updateInterface(Parameters params) {
		
		firstRowList = new ArrayList<JTextField>();
		secondRowList = new ArrayList<JTextField>();
		thirdRowList = new ArrayList<JTextField>();
		// Check how many outputs there are to enable or not
		// the "next" and "back" buttons
		if (outputInd == 0) {
			bnPrevOutput.setEnabled(false);
		} else {
			bnPrevOutput.setEnabled(true);
		}
		if (outputInd < (params.outputList.size() - 1)) {
			bnNextOutput.setEnabled(true);
		} else {
			bnNextOutput.setEnabled(false);
		}

		// Build the panel
		int[] dimValues = DijTensor.getWorkingDimValues(params.outputList.get(outputInd).form, params.outputList.get(outputInd).tensor_shape); 
		String[] dims = DijTensor.getWorkingDims(params.outputList.get(outputInd).form);

		lblName.setText(params.outputList.get(outputInd).name);
		firstRow.removeAll();
		secondRow.removeAll();
		thirdRow.removeAll();

		for (int i = 0; i < dimValues.length; i ++) {
			JLabel dimLetter1 = new JLabel(dims[i]);
			JLabel dimLetter2 = new JLabel(dims[i]);
			JLabel dimLetter3 = new JLabel(dims[i]);
			JTextField txt1;
			JTextField txt2;
			JTextField txt3;

			txt1 = new JTextField("1", 5);
			txt2 = new JTextField("0", 5);
			txt3 = new JTextField("0", 5);
			txt1.setEditable(true);
			
			int inputFixedSize = findFixedInput((String) referenceImage.getSelectedItem(), dims[i], params.inputList);
			
			if (dimValues[i] != -1 && inputFixedSize != -1) {
				float scale = ((float) dimValues[i]) / ((float) inputFixedSize);
				txt1.setText("" + scale);
				txt1.setEditable(false);
			}

			if (checkIsImage.isSelected()) {
				firstRow.place(0, i + 1, dimLetter1);
				firstRow.place(1, i + 1, txt1);
				secondRow.place(0, i + 1, dimLetter2);
				secondRow.place(1, i + 1, txt2);
				thirdRow.place(0, i + 1, dimLetter3);
				thirdRow.place(1, i + 1, txt3);
			}
			
			firstRowList.add(txt1);
			secondRowList.add(txt2);
			thirdRowList.add(txt3);
		}

		if (checkIsImage.isSelected()) {
			firstRow.place(0, 0, lblFirst);
			secondRow.place(0, 0, lblSecond);
			thirdRow.place(0, 0, lblThird);
		}

		showCorresponding();
	
	}
	//TODO qué haser
	public static void showCorresponding() {
		// Look at the check boxes and find out what options have to be shown

		if (checkIsImage.isSelected()) {
			firstRow.setVisible(true);
			secondRow.setVisible(true);
			thirdRow.setVisible(true);
			} else {
			firstRow.setVisible(false);
			secondRow.setVisible(false);
			thirdRow.setVisible(false);
		}
		
	}
	
	public static boolean saveOutputData(Parameters params) {
		
		// Save all the information for the output given by the variable 'outputInd'
		params.outputList.get(outputInd).isImage = "" + checkIsImage.isSelected();
		params.outputList.get(outputInd).referenceImage = (String) referenceImage.getSelectedItem();
		
		params.outputList.get(outputInd).scale = new float[params.outputList.get(outputInd).tensor_shape.length];
		params.outputList.get(outputInd).halo = new int[params.outputList.get(outputInd).tensor_shape.length];
		params.outputList.get(outputInd).offset = new int[params.outputList.get(outputInd).tensor_shape.length];
		
		int batchInd = getBatchInd(params.outputList.get(outputInd).form);
		
		int textFieldInd = 0;
		for (int i = 0; i < params.outputList.get(outputInd).scale.length; i++) {
			try {
				if (i == batchInd) {
					params.outputList.get(outputInd).scale[i] = 1;
					params.outputList.get(outputInd).halo[i] = 0;
				} else {
					String firstValue = firstRowList.get(textFieldInd).getText();
					params.outputList.get(outputInd).scale[i] = Float.valueOf(firstValue);
					String haloValue = secondRowList.get(textFieldInd++).getText();
					params.outputList.get(outputInd).halo[i] = Integer.valueOf(haloValue);
				}
			} catch( NumberFormatException ex) {
				IJ.error("Make sure that no text field is empty.");
				return false;
			}
		}
		
		textFieldInd = 0;
		if (checkIsImage.isSelected()) {
			for (int i = 0; i < params.outputList.get(outputInd).scale.length; i++) {
				try {
					if (i == batchInd) {
						params.outputList.get(outputInd).offset[i] = 0;
					} else {
						params.outputList.get(outputInd).offset[i] = Integer.parseInt(thirdRowList.get(textFieldInd++).getText());
					}
				} catch( NumberFormatException ex) {
					IJ.error("Make sure that no text field is empty.");
					return false;
				}
			}
		}


		double[] rangeOptions = {Double.NEGATIVE_INFINITY, (double) -1, (double) 0, (double) 1, Double.POSITIVE_INFINITY};
		int lowInd = cmbRangeLow.getSelectedIndex();
		int highInd = cmbRangeHigh.getSelectedIndex();
		if (lowInd >= highInd) {
			IJ.error("The Data Range has to go from a value to a higher one.");
			return false;
		}
		
		params.outputList.get(outputInd).dataRange[0] = rangeOptions[lowInd];
		params.outputList.get(outputInd).dataRange[1] = rangeOptions[highInd];
		
		completeInfo[outputInd] = true;
		return true;
	}
	
	private static int getBatchInd(String form) {
		String[] splitForm = form.split("");
		int batchInd = Index.indexOf(splitForm, "N");
		return batchInd;
	}
	
	private static int findFixedInput(String referenceInput, String dim, List<DijTensor> inputTensors) {
		DijTensor referenceTensor = null;
		int fixed = -1;
		for (DijTensor inp : inputTensors) {
			if (referenceInput.equals(inp.name)) {
				referenceTensor = inp;
				break;
			}
		}
		if (referenceTensor != null) {
			int ind = Index.indexOf(referenceTensor.form.split(""), dim);
			if (ind != -1 && referenceTensor.step[ind] == 0) {
				fixed = referenceTensor.minimum_size[ind];
			}
		}
		return fixed;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Parameters params = parent.getDeepPlugin().params;
		if (e.getSource() == bnNextOutput) {
			saveOutputData(params);
			outputInd ++;
		} else if (e.getSource() == bnPrevOutput) {
			outputInd --;
		}
		updateInterface(params);
	}

}

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

import java.awt.Dimension;
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

public class InputDimensionStamp extends AbstractStamp implements ActionListener {


	private List<JTextField>			allTxtMinSize = new ArrayList<JTextField>();
	private List<JTextField>			allTxtStep = new ArrayList<JTextField>();
		

	private static String 				allowPatches = "Allow tilling";
	private static String 				notAllowPatches = "Do not allow tilling";
	
	private JComboBox<String>			cmbPatches	= new JComboBox<String>(new String[] {allowPatches, notAllowPatches});
	// TODO rmove private JLabel						lblPatches	= new JLabel("Patch size");
	private JLabel						lblMinSize	= new JLabel("Minimum Size");
	private JLabel						lblStep	= new JLabel("Step Size");

	private JButton 					bnNextOutput 	= new JButton("Next Output");
	private JButton 					bnPrevOutput 	= new JButton("Previous Output");
	private GridPanel					pnInput			= new GridPanel();
	
	private static JComboBox<String>	cmbRangeLow  = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private static JComboBox<String>	cmbRangeHigh = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private static double[] 			rangeOptions = {Double.NEGATIVE_INFINITY, (double) -1, (double) 0, (double) 1, Double.POSITIVE_INFINITY};

	private List<DijTensor> 			imageTensors;
	private static int					inputCounter = 0;
	// Parameters to know if something changed and we have to rebuild the GUI
	private String						model		  = "";
	private List<DijTensor>				savedInputs   = null;
	private boolean						tiling;
	
	
	// Whether we need to add or not action listener to the text fields
	private boolean						listenTxtField = false;
	
	public InputDimensionStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
		cmbRangeHigh.setSelectedIndex(4);
	}

	@Override
	public void buildPanel() {
		
		HTMLPane info = new HTMLPane(Constants.width, 265);
		info.append("h", "<b>Input size constraints</b>");
		info.append("p", "<b>Input tile size (Q) </b>: Input size of the model. If <i>Allow tiling</i> or"
				+ " <i>Do not allow tiling (with variable input size)</i> is selected, <i>Q</i> will automatically "
				+ "change for each image during the inference and suggested to the user in DeepImageJ Run.");
		info.append("p", "<b>Minimum size (m) </b>: Size of the smallest input that the model can process.");
		info.append("p", "<b>Step (s) </b>: If the network supports different input sizes, the size of each"
				+ " dimension of the input image (Q), has to be the result of 'Minimum size (m) + N * Step (s)',"
				+ " where N can be any positive integer.");
			
		info.append("h", "<b>Tiling strategies</b>");
		info.append("p", "<b>Allow tiling</b>: <i>Q</i> is editable by the user as long as it "
				+ "fulfills the <i>step (s)</i> and <i>minimum (m)</i> constraints. Large images are processed using a tiling strategy.");
		info.append("p", "<b>Do not allow tiling</b>: The input size will be processed as a whole (no tiling). Depending "
				+ "on the <i>step (s)</i> and <i>minimum (m)</i> constraints, the model might not be applicable to "
				+ "some images (too big or small).");
		
		JPanel buttons = new JPanel(new GridLayout(1, 2));
		buttons.setBorder(BorderFactory.createEtchedBorder());
		buttons.add(bnPrevOutput);
		buttons.add(bnNextOutput);
		
		// Create auxiliary DijTensor to initialise the interface 
		DijTensor auxTensor = new DijTensor("aux");
		auxTensor.tensorType = "image";
		auxTensor.tensor_shape = new int[5];
		auxTensor.form = "BZYXC";
		boolean start = true;
		buildPanelForImage(auxTensor, start);
		
		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridheight = 8;
		c.gridy = 0;
		c.weightx = 1;
		c.ipady = 0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 0, 0);
		pn.add(info.getPane(), c);
		c.gridheight = 8;
		c.weightx = 1;
		c.gridy = 8;
		c.ipady = 20;
		c.insets = new Insets(0, 0, 0, 0);
		pn.add(pnInput, c);
		c.gridheight = 1;
		c.weightx = 1;
		c.gridy = 16;
		c.ipady = 0;
		c.insets = new Insets(0, 0, 0, 0);
		pn.add(buttons, c);
		/*
		pn.add(info.getPane());
		pn.add(pnInput);
		pn.add(buttons, BorderLayout.SOUTH);
		*/
		
		panel.add(pn);	

		bnNextOutput.addActionListener(this);
		bnPrevOutput.addActionListener(this);
	}
	
	@Override
	public void init() {
		//String modelOfInterest = parent.getDeepPlugin().params.path2Model;
		Parameters params = parent.getDeepPlugin().params;
		imageTensors = DijTensor.getImageTensors(params.inputList);
		// Set the screen at the first input if the model changes
		String modelOfInterest = params.path2Model;
		// Repaint interface if model has changed
		if (!modelOfInterest.equals(model)) {
			showCorrespondingInputInterface(params);
			// To avoid referencing
			tiling = true == params.allowPatching;
			savedInputs = DijTensor.copyTensorList(params.inputList);
			model = modelOfInterest;
			inputCounter = 0;
			return;
		}
		// Repaint interface if the number of input tensors has changed
		if (params.inputList.size() != savedInputs.size()) {
			showCorrespondingInputInterface(params);
			// To avoid referencing
			tiling = true == params.allowPatching;
			savedInputs = DijTensor.copyTensorList(params.inputList);
			inputCounter = 0;
			return;
		}
		// If the number of inputs is the same, check if their shape or type have changed
		for (int i = 0; i < params.inputList.size(); i ++) {
			if (!params.inputList.get(i).tensorType.equals(savedInputs.get(i).tensorType) 
				|| !params.inputList.get(i).form.equals(savedInputs.get(i).form)) {
				showCorrespondingInputInterface(params);
				// To avoid referencing
				tiling = true == params.allowPatching;
				savedInputs = DijTensor.copyTensorList(params.inputList);
				inputCounter = 0;
				return;
			}
		}
		// If the model has changed from allow patching to not allow patching
		// or from pyramidal to not pyramidal, repaint again.
		if (tiling != params.allowPatching) {
			showCorrespondingInputInterface(params);
			// To avoid referencing
			tiling = true == params.allowPatching;
			inputCounter = 0;
			return;
		}
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		saveInputData(params);
		for (DijTensor inp : params.inputList) {
			if (inp.tensorType.contains("image") && !inp.finished)
				return false;
		}
		// Save to know when to repaint the interface
		savedInputs = params.inputList;
		tiling = params.allowPatching;
		return true;
	}
	
	public void showCorrespondingInputInterface(Parameters params) {
		
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
		allTxtMinSize = new ArrayList<JTextField>();
		allTxtStep = new ArrayList<JTextField>();
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
		

		// Allow patch decomposition
		for (int i = 0; i < dim.length; i ++) {
			if (dimValues[i] != -1 && !listenTxtField) {
				allTxtMinSize.get(i).setText("" + dimValues[i]);
				allTxtMinSize.get(i).setEditable(false);
				allTxtStep.get(i).setText("" + 0);
				allTxtStep.get(i).setEditable(false);
			} else if (dimValues[i] == -1){
				allTxtMinSize.get(i).setEditable(true);
				String stepGuess = dim[i].equals("C") ? "0" : "1";
				allTxtStep.get(i).setText(stepGuess);
				allTxtStep.get(i).setEditable(true);
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
		ArrayList<Integer> imageTensorInds = new ArrayList<Integer>();
		for (int i = 0; i < params.inputList.size(); i ++) {
			if (params.inputList.get(i).tensorType.contains("image"))
				imageTensorInds.add(i);
		}
		int trueInd = imageTensorInds.get(inputCounter);
		DijTensor tensor = params.inputList.get(trueInd);
		if (imageTensorInds.size() > 0 && tensor.tensorType.contains("image")) {
			wasSaved = saveInputDataForImage(params, tensor);
		}

		int lowInd = cmbRangeLow.getSelectedIndex();
		int highInd = cmbRangeHigh.getSelectedIndex();
		if (lowInd >= highInd) {
			IJ.error("The Data Range has to go from a value to a higher one.");
			return false;
		}
		
		params.inputList.get(trueInd).dataRange[0] = rangeOptions[lowInd];
		params.inputList.get(trueInd).dataRange[1] = rangeOptions[highInd];
		params.inputList.get(trueInd).finished = wasSaved;
		
		return wasSaved;
	}
	
	/*
	 * Method to retrieve from the UI the information necessary to build an 
	 * image from the tensor inputed to the model
	 */
	public boolean saveInputDataForImage(Parameters params, DijTensor tensor) {
		params.fixedInput = true;
		int[] min_size = new int[tensor.form.length()];
		int[] step = new int[tensor.form.length()];
		
		int batchInd = Index.indexOf(tensor.form.split(""), "B");
		if (batchInd != -1) {
			min_size[batchInd] = 1; step[batchInd] = 0;
		}

		// Selected tiling option
		String selection = (String) cmbPatches.getSelectedItem();
		
		boolean auxDetectError = true;
		try {
			int auxCount = 0;
			for (int c = 0; c < tensor.tensor_shape.length; c ++) {
				if (c != batchInd) {
					auxDetectError = false;
					min_size[c] = Integer.parseInt(allTxtMinSize.get(auxCount).getText());
					auxDetectError = true;
					step[c] = Integer.parseInt(allTxtStep.get(auxCount).getText());
					if (min_size[c] <= 0) {
						IJ.error("The step should be larger than 0");
						return false;
					}
					if (step[c] < 0) {
						IJ.error("The step size should be larger or equal to 0");
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
				IJ.error("The step is not a correct integer");
			}
			return false;
		}
		
		
		if (selection.contains(allowPatches)) {
			params.allowPatching = true;
		} else if (selection.contains(notAllowPatches)) {
			// The patch is always the same. No step because
			// no more sizes are allowed
			params.allowPatching = false;
		}
		for (int ss : step) {
			if (ss != 0) {
				params.fixedInput = false;
				break;
			}
		}
		
		tensor.minimum_size = min_size;
		tensor.step = step;
		
		return true;
	}
	
	private void buildPanelForImage(DijTensor tensor) {
		buildPanelForImage(tensor, false);
	}
	
	private void buildPanelForImage(DijTensor tensor, boolean start) {
		// Build panel for when the input tensor is an image

		allTxtMinSize = new ArrayList<JTextField>();
		allTxtStep = new ArrayList<JTextField>();
		String[] dims = DijTensor.getWorkingDims(tensor.form); 

		JPanel pnMinSize = new JPanel(new GridLayout(2, dims.length));
		JPanel pnStep = new JPanel(new GridLayout(2, dims.length));
		
		for (String dim: dims) {
			JLabel dimLetter1 = new JLabel(dim);
			dimLetter1.setPreferredSize( new Dimension( 10, 20 ));
			JLabel dimLetter2 = new JLabel(dim);
			dimLetter2.setPreferredSize( new Dimension( 10, 20 ));
			
			pnMinSize.add(dimLetter1);
			pnStep.add(dimLetter2);
			
		}
		
		for (int i = 0; i < dims.length; i ++) {
			JTextField txtMultiple = new JTextField("1", 5);
			txtMultiple.setPreferredSize( new Dimension( 10, 20 ));
			JTextField txtStep = new JTextField("0", 5);
			txtStep.setPreferredSize( new Dimension( 10, 20 ));
			
			pnMinSize.add(txtMultiple);
			allTxtMinSize.add(txtMultiple);
			
			pnStep.add(txtStep);
			allTxtStep.add(txtStep);
		}
		
		// If we are building the screen at the start of the plugin, 
		// and there are no params defined, just start by default
		if (!start) {
			Parameters params = parent.getDeepPlugin().params;
			if ((!params.allowPatching || params.pyramidalNetwork)) {
				// If we do not allow patching, do no show the corresponding options in the combobox
				cmbPatches	= new JComboBox<String>(new String[] {notAllowPatches});
			} else if (params.allowPatching && !params.pyramidalNetwork) {
				// If we  allow patching but the size is fixed by the model, do not show the two options that
				// allow freedom in the input image
				cmbPatches	= new JComboBox<String>(new String[] {allowPatches, notAllowPatches});
			}
		} else {
			cmbPatches	= new JComboBox<String>(new String[] { allowPatches, notAllowPatches});
		}
		
		pnInput.removeAll();
		pnInput.setBorder(BorderFactory.createEtchedBorder());
		pnInput.place(0, 0, 2, 1, new JLabel("Name: " + tensor.name + "      Input type: " + tensor.tensorType));
		pnInput.place(1, 0, 2, 1, cmbPatches);
		pnInput.place(2, 0, lblMinSize);
		pnInput.place(2, 1, pnMinSize);
		pnInput.place(3, 0, lblStep);
		pnInput.place(3, 1, pnStep);
		GridPanel pnRange1 = new GridPanel(true);
		pnRange1.place(0, 0, new JLabel("Data Range lower bound"));
		pnRange1.place(0, 1, cmbRangeLow);
		pnInput.place(4, 0, pnRange1);
		GridPanel pnRange2 = new GridPanel(true);
		pnRange2.place(0, 0, new JLabel("Data Range higher bound"));
		pnRange2.place(0, 1, cmbRangeHigh);
		pnInput.place(4, 1, pnRange2);
		
		cmbRangeLow.setEditable(false);
		cmbRangeHigh.setEditable(false);
		
		cmbPatches.addActionListener(this);
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

}

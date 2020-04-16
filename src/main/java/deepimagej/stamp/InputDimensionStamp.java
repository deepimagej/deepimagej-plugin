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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
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

	
	private List<JTextField>	allTxtMultiple = new ArrayList<JTextField>();
	//private List<JTextField>	allTxtPadding = new ArrayList<JTextField>();
	private List<JTextField>	allTxtPatches = new ArrayList<JTextField>();
	
	private JCheckBox 			checkAllowPatching = new JCheckBox("Allow processing the image by patches.");
	
	private JComboBox<String>	cmbPatches	= new JComboBox<String>(new String[] { "Allow patch decomposition", "Predetermined input size" });
	//private JComboBox<String>	cmbPadding	= new JComboBox<String>(new String[] { "Fixed padding (recommended)", "User-defined padding (not recommended)" });
	private JLabel				lblPatches	= new JLabel("Patch size");
	//private JLabel				lblPadding	= new JLabel("Padding size");
	private JLabel				lblMultiple	= new JLabel("Multiple factor");
	
	private JComboBox<String>	cmbRangeLow = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	private JComboBox<String>	cmbRangeHigh = new JComboBox<String>(new String [] {"-inf", "-1", "0", "1", "inf"});
	
	public InputDimensionStamp(BuildDialog parent) {
		super(parent);
		//buildPanel();
		cmbRangeHigh.setSelectedIndex(4);
	}

	@Override
	public void buildPanel() {
		checkAllowPatching = new JCheckBox("Allow processing the image by patches.");
		
		cmbPatches = new JComboBox<String>(new String[] { "Allow patch decomposition", "Predetermined input size" });
		//cmbPadding = new JComboBox<String>(new String[] { "Fixed padding (recommended)", "User-defined padding (not recommended)" });
		lblPatches = new JLabel("Patch size");
		//lblPadding = new JLabel("Padding size");
		lblMultiple	= new JLabel("Multiple factor");
		
		allTxtMultiple = new ArrayList<JTextField>();
		allTxtPatches = new ArrayList<JTextField>();
		Parameters params = parent.getDeepPlugin().params;
		
		HTMLPane info = new HTMLPane(Constants.width, 180);
		info.append("h2", "Input size constraints");
		info.append("p", "<b>Patch size (Q) </b>: If the network has not a predetermined input size, patch decomposition of default size <i>Q</i> is allowed.");
		info.append("p", "<b>Padding (P) </b>: To preserve the input size at the output, convolutions are calculated using zero padding boundary conditions of size <i>P</i>.");
		info.append("p", "<b>Multiple factor (m) </b>: If the network has an auto-encoder architecture, the size of each dimension of the input image, has to be multiple of a minimum size m.");

		String[] dims = DijTensor.getWorkingDims(params.inputList.get(0).form); 
		
		//cmbPatches.setPreferredSize(new Dimension(Constants.width/2, 25));
		//cmbPadding.setPreferredSize(new Dimension(Constants.width/2, 25));
		JPanel pnMultiple = new JPanel();
		pnMultiple.setLayout(new GridLayout(2, dims.length));
		//GridPanel pnPadding = new GridPanel(true);
		JPanel pnPatchSize = new JPanel();
		pnPatchSize.setLayout(new GridLayout(2, dims.length));
		
		for (String dim: dims) {
			JLabel dimLetter1 = new JLabel(dim);
			dimLetter1.setPreferredSize( new Dimension( 10, 20 ));
			JLabel dimLetter2 = new JLabel(dim);
			dimLetter2.setPreferredSize( new Dimension( 10, 20 ));
			
			pnMultiple.add(dimLetter1);

			pnPatchSize.add(dimLetter2);
		}
		for (String dim: dims) {
			JTextField txtMultiple = new JTextField("1", 5);
			txtMultiple.setPreferredSize( new Dimension( 10, 20 ));
			JTextField txtPatches = new JTextField("100", 5);
			txtPatches.setPreferredSize( new Dimension( 10, 20 ));
			
			pnMultiple.add(txtMultiple);
			allTxtMultiple.add(txtMultiple);

			pnPatchSize.add(txtPatches);
			allTxtPatches.add(txtPatches);
		}
		
		GridPanel pnPatches = new GridPanel(true);
		pnPatches.setBorder(BorderFactory.createEtchedBorder());
		checkAllowPatching.setSelected(true);
		pnPatches.place(0, 1, checkAllowPatching);
		pnPatches.place(1, 0, lblMultiple);
		pnPatches.place(1, 1, pnMultiple);
		pnPatches.place(3, 0, 2, 1, cmbPatches);
		pnPatches.place(4, 0, lblPatches);
		pnPatches.place(4, 1, pnPatchSize);
		GridPanel pnRange1 = new GridPanel(true);
		JLabel lblRange1 = new JLabel("Data Range lower bound");
		pnRange1.place(0, 0, lblRange1);
		pnRange1.place(0, 1, cmbRangeLow);
		pnPatches.place(5, 0, pnRange1);
		GridPanel pnRange2 = new GridPanel(true);
		JLabel lblRange2 = new JLabel("Data Range higher bound");
		pnRange2.place(0, 0, lblRange2);
		pnRange2.place(0, 1, cmbRangeHigh);
		pnPatches.place(5, 1, pnRange2);

		cmbRangeLow.setEditable(false);
		cmbRangeHigh.setEditable(false);
		
		
		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(pnPatches);
		panel.removeAll();
		panel.add(pn);
		
		//cmbPadding.addActionListener(this);
		cmbPatches.addActionListener(this);
		checkAllowPatching.addActionListener(this);
		
		updateInterface();
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
			cmbPatches.setEnabled(false);
			cmbPatches.setSelectedIndex(1);
		}
	}

	private void updateInterface() {

		Parameters params = parent.getDeepPlugin().params;
		String[] dim = DijTensor.getWorkingDims(params.inputList.get(0).form); 
		int[] dimValues = DijTensor.getWorkingDimValues(params.inputList.get(0).form, params.inputList.get(0).tensor_shape); 
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
	
	@Override
	public void init() {
		buildPanel();
		Parameters params = parent.getDeepPlugin().params;
		int[] dimValues = DijTensor.getWorkingDimValues(params.inputList.get(0).form, params.inputList.get(0).tensor_shape); 
		for (int i = 0; i < dimValues.length; i ++) {
			if (dimValues[i] == -1) {
				addChangeListener(allTxtMultiple.get(i), e -> updateInterface());
			}
		}
	
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;

		int[] multiple = new int[params.inputList.get(0).form.length()];
		int[] patch = new int[params.inputList.get(0).form.length()];
		int[] step = new int[params.inputList.get(0).form.length()];
		
		int batchInd = Index.indexOf(params.inputList.get(0).form.split(""), "N");
		multiple[batchInd] = 1;
		patch[batchInd] = 1;
		step[batchInd] = 1;
		
		int c = 0;
		String dimOfInterest;;
		int ind;
		try {
			c = 0;
			for (JTextField pat: allTxtPatches) {
				dimOfInterest = DijTensor.getWorkingDims(params.inputList.get(0).form)[c];
				ind = Index.indexOf(params.inputList.get(0).form.split(""), dimOfInterest);
				patch[ind] = Integer.parseInt(pat.getText());
				if (patch[ind] <= 0) {
					IJ.error("The patch size should be larger than 0");
					return false;
				}
				c ++;
			}
		}
		catch (Exception ex) {
			IJ.error("The patch size is not a correct integer");
			return false;
		}

		try {
			c = 0;
			for (JTextField mult: allTxtMultiple) {
				dimOfInterest = DijTensor.getWorkingDims(params.inputList.get(0).form)[c];
				ind = Index.indexOf(params.inputList.get(0).form.split(""), dimOfInterest);
				multiple[ind] = Integer.parseInt(mult.getText());
				step[ind] = Integer.parseInt(mult.getText());
				
				if (!mult.isEditable()) {
					step[ind] = 0;
				}
				
				if (multiple[ind] <= 0) {
					IJ.error("The multiple factor size should be larger than 0");
					return false;
				}
				c ++;
			}
		}
		catch (Exception ex) {
			IJ.error("The multiple factor size is not a correct integer");
			return false;
		}

		//params.inputList.get(0).fixedPatch = cmbPatches.getSelectedIndex() == 1;
		//params.inputList.get(0).fixedPadding = cmbPadding.getSelectedIndex() == 0;
		if (cmbPatches.getSelectedIndex() == 1) {
			step = new int[step.length];
		}
		params.inputList.get(0).minimum_size = multiple;
		params.inputList.get(0).recommended_patch = patch;
		params.inputList.get(0).step = step;

		double[] rangeOptions = {Double.NEGATIVE_INFINITY, (double) -1, (double) 0, (double) 1, Double.POSITIVE_INFINITY};
		int lowInd = cmbRangeLow.getSelectedIndex();
		int highInd = cmbRangeHigh.getSelectedIndex();
		if (lowInd >= highInd) {
			IJ.error("The Data Range has to go from a value to a higher one.");
			return false;
		}
		
		params.inputList.get(0).dataRange[0] = rangeOptions[lowInd];
		params.inputList.get(0).dataRange[1] = rangeOptions[highInd];
		
		for (int i = 0; i < multiple.length; i ++) {
			if (multiple[i] != 0 && patch[i]%multiple[i] != 0) {
				IJ.error("At dimension " + params.inputList.get(0).form.split("")[i] + " size " +
						patch[i] + " is not a multiple of "
						+ multiple[i]);
				return false;
			}
		}
		
		return true;
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
		
		updateInterface();
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

/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we strongly encourage you to include adequate citations and acknowledgments 
 * whenever you present or publish results that are based on it.
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
 * DeepImageJ is an open source software (OSS): you can redistribute it and/or modify it under 
 * the terms of the BSD 2-Clause License.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 * You should have received a copy of the BSD 2-Clause License along with DeepImageJ. 
 * If not, see <https://opensource.org/licenses/bsd-license.php>.
 */

package deepimagej.stamp;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import deepimagej.TensorFlowModel;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.Index;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

public class DimensionStamp extends AbstractStamp implements ActionListener {

	private JTextField			txtPatches	= new JTextField("100", 5);
	private JTextField			txtPadding	= new JTextField("100", 5);
	private JTextField			txtMultiple	= new JTextField("1", 5);
	
	private JComboBox<String>	cmbPatches	= new JComboBox<String>(new String[] { "Allow patch decomposition", "Predetermined input size" });
	private JComboBox<String>	cmbPadding	= new JComboBox<String>(new String[] { "Fixed padding (recommended)", "User-defined padding (not recommended)" });
	private JLabel				lblPatches	= new JLabel("Patch size");
	private JLabel				lblPadding	= new JLabel("Padding size");
	private JLabel				lblMultiple	= new JLabel("Multiple factor");

	public DimensionStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {
		HTMLPane info = new HTMLPane(Constants.width, 180);
		info.append("h2", "Input size constraints");
		info.append("p", "<b>Patch size (Q) </b>: If the network has not a predetermined input size, patch decomposition of default size <i>Q</i> is allowed.");
		info.append("p", "<b>Padding (P) </b>: To preserve the input size at the output, convolutions are calculated using zero padding boundary conditions of size <i>P</i>.");
		info.append("p", "<b>Multiple factor (m) </b>: If the network has an auto-encoder architecture, the size of each dimension of the input image, has to be multiple of a minimum size m.");

		//cmbPatches.setPreferredSize(new Dimension(Constants.width/2, 25));
		//cmbPadding.setPreferredSize(new Dimension(Constants.width/2, 25));
		GridPanel pnPatches = new GridPanel(true);
		pnPatches.place(0, 0, lblMultiple);
		pnPatches.place(0, 1, txtMultiple);
		pnPatches.place(1, 0, 2, 1, cmbPadding);
		pnPatches.place(2, 0, lblPadding);
		pnPatches.place(2, 1, txtPadding);
		pnPatches.place(3, 0, 2, 1, cmbPatches);
		pnPatches.place(4, 0, lblPatches);
		pnPatches.place(4, 1, txtPatches);

		JPanel information = new JPanel();
		information.setLayout(new GridLayout(2, 1));

		JPanel pnInOut = new JPanel();
		pnInOut.setBorder(BorderFactory.createEtchedBorder());
		pnInOut.setLayout(new GridLayout(2, 1));

		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
		pn.add(info.getPane());
		pn.add(pnPatches);
		pn.add(pnInOut);
		panel.add(pn);
		
		cmbPadding.addActionListener(this);
		cmbPatches.addActionListener(this);
		addChangeListener(txtMultiple, e -> setOptimal());
		addChangeListener(txtPadding, e -> setOptimal());
		updateInterface();
	}

	private void updateInterface() {
		txtMultiple.setEditable(true);

		boolean pad = cmbPadding.getSelectedIndex() == 0;
		lblPadding.setText(pad ? "Specific padding size" : "Proposed padding size");
		boolean pat = cmbPatches.getSelectedIndex() == 1;
		lblPatches.setText(pat ? "Default patch size" : "Predetermined input size");
		if (pat == true) {
			txtMultiple.setText("1");
			txtMultiple.setEditable(false);
		}
		if (cmbPatches.isEnabled() == true) {
			setOptimal();
		}
	}
	
	@Override
	public void init() {
		txtPatches.setEditable(true);
		txtMultiple.setEditable(true);
		cmbPatches.setEnabled(true);
		Parameters params = parent.getDeepPlugin().params;
		String hSize = TensorFlowModel.hSize(params, params.inputForm[0]);
		String wSize = TensorFlowModel.wSize(params, params.inputForm[0]);
		if (hSize.equals("-1") == false && wSize.equals("-1") == true) {
			cmbPatches.setEnabled(false);
			txtPatches.setText(hSize);
			txtMultiple.setText(hSize);
			txtPatches.setEditable(false);
			txtMultiple.setEditable(false);
			cmbPatches.setSelectedIndex(1);
		} else if (hSize.equals("-1") == true && wSize.equals("-1") == false) {
			cmbPatches.setEnabled(false);
			txtPatches.setText(wSize);
			txtMultiple.setText(wSize);
			txtPatches.setEditable(false);
			txtMultiple.setEditable(false);
			cmbPatches.setSelectedIndex(1);
		} else if (hSize.equals("-1") == false && wSize.equals(hSize) == true) {
			cmbPatches.setEnabled(false);
			txtPatches.setText(wSize);
			txtMultiple.setText(wSize);
			txtPatches.setEditable(false);
			txtMultiple.setEditable(false);
			cmbPatches.setSelectedIndex(1);
		} else if (hSize.equals("-1") == false && wSize.equals(hSize) == false) {
			IJ.error("DeepImageJ only supports square patches for the moment.");
		}
		boolean pat = cmbPatches.getSelectedIndex() == 1;
		if (pat == true) {
			txtMultiple.setText("1");
			txtMultiple.setEditable(false);
		}
		updateInterface();
	}

	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;

		int patch = 0;
		try {
			patch = Integer.parseInt(txtPatches.getText());
		}
		catch (Exception ex) {
			IJ.error("The patch size is not a correct integer");
			return false;
		}
		if (patch <= 0) {
			IJ.error("The patch size should be larger than 0");
			return false;
		}

		int padding = 0;
		try {
			padding = Integer.parseInt(txtPadding.getText());
		}
		catch (Exception ex) {
			IJ.error("The padding size is not a correct integer");
			return false;
		}
		if (padding < 0) {
			IJ.error("The padding size should be larger than 0");
			return false;
		}

		int multiple = 0;
		try {
			multiple = Integer.parseInt(txtMultiple.getText());
		}
		catch (Exception ex) {
			IJ.error("The multiple factor size is not a correct integer");
			return false;
		}
		if (multiple <= 0) {
			IJ.error("The multiple factor size should be larger than 0");
			return false;
		}

		params.fixedPatch = cmbPatches.getSelectedIndex() == 1;
		params.fixedPadding = cmbPadding.getSelectedIndex() == 0;
		params.minimumSize = "" + multiple;
		//int[] overlap_size = ArrayOperations.patchOverlapVerification(patch, params.fixedPatch);
		params.patch = patch;
		params.padding = padding;
		
		if (patch%multiple != 0) {
			IJ.error(params.patch + " is not a multiple of "
					+ params.minimumSize);
			return false;
		}
		
		return true;
	}
	
	public String optimalPatch() {
		// This method looks for the optimal patch size regarding the
		// minimum patch constraint and image size. This is then suggested
		// to the user
		ImagePlus imp = null;
		String patch;
		int minimumSize = Integer.parseInt(txtMultiple.getText());
		int padding = Integer.parseInt(txtPadding.getText());
		if (imp == null) {
			imp = WindowManager.getCurrentImage();
		}
		if (imp == null) {
			patch = "100";
			return patch;	
		}
		int nx = imp.getWidth();
		int ny = imp.getHeight();
		int maxDim = nx;
		if (nx < ny) {
			maxDim = ny;
		}
		int optimalMult = (int)Math.ceil((double)(maxDim + 2 * padding) / (double)minimumSize) * minimumSize;
		if (optimalMult > 3 * maxDim) {
			optimalMult = optimalMult - minimumSize;
		}
		if (optimalMult > 3 * maxDim) {
			optimalMult = (int)Math.ceil((double)maxDim / (double)minimumSize) * minimumSize;
		}
		patch = Integer.toString(optimalMult);
		return patch;
	}
	
	private void setOptimal() {
		// Set the optimal patch size (only 1 patch) to process the image
		if (cmbPatches.isEnabled() == true) {
			txtPatches.setText(optimalPatch());
		}
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

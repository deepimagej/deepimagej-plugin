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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.Index;
import ij.IJ;

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
		pnPatches.place(0, 0, 2, 1, cmbPatches);
		pnPatches.place(1, 0, lblPatches);
		pnPatches.place(1, 1, txtPatches);
		pnPatches.place(2, 0, 2, 1, cmbPadding);
		pnPatches.place(3, 0, lblPadding);
		pnPatches.place(3, 1, txtPadding);
		pnPatches.place(4, 0, lblMultiple);
		pnPatches.place(4, 1, txtMultiple);

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
		updateInterface();
	}

	private void updateInterface() {
		boolean pad = cmbPadding.getSelectedIndex() == 0;
		lblPadding.setText(pad ? "Specific padding size" : "Proposed padding size");
		boolean pat = cmbPatches.getSelectedIndex() == 1;
		lblPatches.setText(pat ? "Default patch size" : "Predetermined input size");
	}
	
	@Override
	public void init() {
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
		if (padding <= 0) {
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
		if (padding <= 0) {
			IJ.error("The multiple factor size should be larger than 0");
			return false;
		}

		params.fixedPatch = cmbPatches.getSelectedIndex() == 0;
		params.fixedPadding = cmbPadding.getSelectedIndex() == 0;
		params.minimumSize = "" + multiple;
		int[] overlap_size = ArrayOperations.patchOverlapVerification(patch, params.fixedPatch);
		params.patch = overlap_size[0];
		params.padding = overlap_size[1];
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		updateInterface();
	}

}

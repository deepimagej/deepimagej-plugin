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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JPanel;


import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.components.HTMLPane;

public class SelectPyramidalStamp extends AbstractStamp  {

	private JCheckBox	checkPyramidal = new JCheckBox("Select if the model uses a Pyramidal Pooling architecture");				
	private String		model = "";
	private HTMLPane	info;

	public SelectPyramidalStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {
		info = new HTMLPane(Constants.width, 70);
		info.append("h2", "Pyramidal Feature Pooling Network selection");
		info.append("p", "Deep learning architectures that combine feature extraction and multilevel "
				+ "detection to infer bounding boxes. These networks are fed with more than one input "
				+ "and return outputs at different levels and dimensions. "
				+ "Examples: RetineNet, R-CNN, Fast-RCNN, Mask-RCNN or PanopticFPN.");
		JPanel main = new JPanel(new BorderLayout());
		main.setLayout(new GridBagLayout());
		GridBagConstraints  c = new GridBagConstraints();
		c.gridheight = 10;
		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 70;
		c.ipady = 70;
		c.weightx = 1;
		c.weighty = 1;
		c.anchor = GridBagConstraints.NORTH;
	    c.fill = GridBagConstraints.HORIZONTAL;
		main.add(info.getPane(), c);
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 10;
		c.ipadx = 0;
		c.ipady = 0;
		c.weighty = 1;
		c.anchor = GridBagConstraints.NORTH;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.insets = new Insets(0, 50, 250, 10); 
		main.add(checkPyramidal, c);
		panel.add(main);
		checkPyramidal.setSelected(false);
	}

	@Override
	public void init() {
		DeepImageJ dp = parent.getDeepPlugin();
		if (model.contains(dp.params.path2Model))
			checkPyramidal.setSelected(dp.params.pyramidalNetwork);
	}
	
	@Override
	public boolean finish() {
		DeepImageJ dp = parent.getDeepPlugin();
		dp.params.pyramidalNetwork = false;
		dp.params.allowPatching = true;
		if (checkPyramidal.isSelected()) {
			dp.params.pyramidalNetwork = true;
			dp.params.allowPatching = false;
		}
		model = dp.params.path2Model;
		return true;
	}
}

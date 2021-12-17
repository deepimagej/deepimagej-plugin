/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

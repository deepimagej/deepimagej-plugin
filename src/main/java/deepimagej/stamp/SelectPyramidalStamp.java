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
import javax.swing.JCheckBox;
import javax.swing.JPanel;


import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.components.HTMLPane;

public class SelectPyramidalStamp extends AbstractStamp  {

	private JCheckBox	checkPyramidal = new JCheckBox("Check if the model uses a Pyramidal Poolibg architecture");				
	private String		model = "";
	private HTMLPane	info;

	public SelectPyramidalStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {
		info = new HTMLPane(Constants.width, 70);
		info.append("h2", "Pyramidal Pooling Network selection");
		info.append("p", "Usually, complex architectures for which more than one input is "
				+ "required or for which the output is multidimensional. These networks are "
				+ "used in the detection, panoptic or instance segmentation, for which the "
				+ "combination of bounding boxes and segmentation is needed. The most famous "
				+ "examples are RetinaNet, R-CNN, Fast-RCNN, Mask-RCNN or PanopticFPN.");
		JPanel main = new JPanel(new BorderLayout());
		main.add(info.getPane(), BorderLayout.CENTER);
		main.add(checkPyramidal, BorderLayout.SOUTH);
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
		if (checkPyramidal.isSelected()) {
			dp.params.pyramidalNetwork = true;
			dp.params.allowPatching = false;
		}
		model = dp.params.path2Model;
		return true;
	}
}

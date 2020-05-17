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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.components.HTMLPane;

public class PostprocessingStamp extends AbstractStamp implements ActionListener {

	private JTextArea				txt	= new JTextArea();

	private HashMap<String, String>	commands;
	private JComboBox<String>		cmb;

	public PostprocessingStamp(BuildDialog parent) {
		super(parent);
		commands = new HashMap<String, String>();
		commands.put("Automatic contrast", "run(\"Enhance Contrast\", \"saturated=0.35\");");
		commands.put("Automatic threshold", "run(\"Make Binary\");");
		commands.put("Convert to 8-bit", "run(\"8-bit\");");
		commands.put("Convert to 16-bit", "run(\"16-bit\");");
		commands.put("Convert to 32-bit", "run(\"32-bit\");");
		buildPanel();
	}

	@Override
	public void buildPanel() {
		cmb = new JComboBox<String>();
		txt.setBackground(Color.BLACK);
		txt.setForeground(Color.GREEN);
		txt.setCaretColor(Color.WHITE);

		cmb.addItem("<Select a usual macro command>");
		for (String key : commands.keySet()) {
			cmb.addItem(key);
		}
		HTMLPane pane = new HTMLPane(Constants.width, 90);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "Postprocessing Macro");
		pane.append("p", "Edit the postprocessing macro in the black window " + "or add one of the usual macro commands listed below."
				+ "The following macro will be saved in a file postprocessing.txt");
		txt.append("// Postprocessing macro\n");
		txt.append("print(\"Postprocessing\");\n");
		txt.setPreferredSize(new Dimension(Constants.width, 200));
		txt.setFont(new Font("Monaco", Font.PLAIN, 11));
		JScrollPane scroll = new JScrollPane(txt, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		JPanel pn = new JPanel(new BorderLayout());
		pn.add(pane.getPane(), BorderLayout.NORTH);
		pn.add(scroll, BorderLayout.CENTER);
		pn.add(cmb, BorderLayout.SOUTH);
		panel.add(pn);
		cmb.addActionListener(this);
	}
	
	@Override
	public void init() {
		txt.setCaretColor(Color.WHITE);
	}

	@Override
	public boolean finish() {
		parent.getDeepPlugin().params.postmacro = txt.getText();
		return true;
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		String key = (String) cmb.getSelectedItem();
		String command = commands.get(key);
		if (command != null)
			txt.append(command + "\n");
	}


}

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
 * E. Gómez-de-Mariscal, C. García-López-de-Haro, L. Donati, M. Unser, A. Muñoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique fédérale de Lausanne (EPFL), Switzerland
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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import deepimagej.BuildDialog;
import deepimagej.Parameters;
import ij.IJ;

public class PostprocessStamp extends AbstractStamp implements ActionListener {

	private JComboBox<String> cmb = new JComboBox<String>();
	private JTextArea txt = new JTextArea(10, 30);
	private JButton add = new JButton("Add");
	private JButton delete = new JButton("Delete");
	private String newline = "\n";
	
	public PostprocessStamp(Parameters params, BuildDialog parent, String title) {
		super(params, parent, title);
		buildPanel();
	}
	
	public void buildPanel() {
		cmb.addItem("Resize");
		cmb.addItem("Median Filter");
		cmb.addItem("Convert to 8-bits");
		cmb.addItem("Convert to 16-bits");
		cmb.addItem("Convert to 32-bits");
		cmb.addItem("RGB Color");
		cmb.addItem("RGB Stack");
		cmb.addItem("Convert to Mask");
		cmb.addItem("Smooth");
		cmb.addItem("Images to Stack");
		
		txt.setEditable(false);
		
		JScrollPane sp = new JScrollPane(txt);

		JPanel p1 = new JPanel(new GridLayout(4, 1));
		p1.add(new JLabel("Predefined operations"));
		p1.add(cmb);
		p1.add(add);
		p1.add(delete);

		JPanel pn2 = new JPanel(new BorderLayout());
		pn2.add(p1, BorderLayout.WEST);
		pn2.add(new JLabel(""), BorderLayout.CENTER);
		
		JPanel pn = new JPanel(new BorderLayout());
		pn.add(pn2, BorderLayout.WEST);
		pn.add(sp, BorderLayout.CENTER);
		add.addActionListener(this);
		delete.addActionListener(this);
		panel.add(pn);
	}
	
	public void validate() {
		String preprocessing_script = txt.getText();
		params.postmacro = preprocessing_script;
		
	}
	
	

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == add) {
			int code = cmb.getSelectedIndex();
			if (code == 0) txt.append("run(\"Size...\", \"width=512 height=384 constrain average interpolation=Bicubic\")\"" + newline);
			if (code == 1) txt.append("run(\"Median...\", \"radius=3\")\"" + newline);
			if (code == 2) txt.append("run(\"8-bit\")\"" + newline);
			if (code == 3) txt.append("run(\"16-bit\")\""  + newline);
			if (code == 4) txt.append("run(\"32-bit\")\"" + newline);
			if (code == 5) txt.append("run(\"RGB Color\")\"" + newline);
			if (code == 6) txt.append("run(\"RGB Stack\")\"" + newline);
			if (code == 7) txt.append("run(\"Convert to Mask\")\""  + newline);
			if (code == 7) txt.append("run(\"Smooth\")\""  + newline);
			if (code == 7) txt.append("run(\"Images to Stack\")\""  + newline);
			
		}
		
		if (e.getSource() == delete) {
			int end;
			try {
				end = txt.getLineEndOffset(txt.getLineCount() - 1);
				txt.replaceRange("", 0, end);
			} catch (BadLocationException e1) {
				IJ.error("There is nothing more to delete.");
			}
		}
	}
	
}

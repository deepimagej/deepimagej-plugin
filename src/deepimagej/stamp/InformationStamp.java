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
import java.io.File;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import ij.IJ;

public class InformationStamp extends AbstractStamp {

	public JTextField	txtName			= new JTextField("", 24);
	public JTextField	txtAuthor		= new JTextField("", 24);
	public JTextField	txtURL			= new JTextField("", 24);
	public JTextField	txtCredit		= new JTextField("", 24);
	public JTextField	txtVersion		= new JTextField("", 24);
	public JTextField	txtDate			= new JTextField("", 24);
	public JTextField	txtReference		= new JTextField("", 24);

	public InformationStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {
		HTMLPane pane = new HTMLPane(Constants.width, 60);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "General Information");
		pane.append("p", "This information will be stored in the config.xml");
		pane.append("p", "Add the reference to properly cite the pretrained model.");
		txtDate.setText("" + new Date());
		txtURL.setText("http://");
		GridPanel pn = new GridPanel(true, 6);
		pn.place(1, 0, new JLabel("Full name"));
		pn.place(1, 1, txtName);
		pn.place(2, 0, new JLabel("Authors"));
		pn.place(2, 1, txtAuthor);
		pn.place(3, 0, new JLabel("Credits"));
		pn.place(3, 1, txtCredit);
		pn.place(4, 0, new JLabel("URL"));
		pn.place(4, 1, txtURL);
		pn.place(5, 0, new JLabel("Version"));
		pn.place(5, 1, txtVersion);
		pn.place(6, 0, new JLabel("Date"));
		pn.place(6, 1, txtDate);
		pn.place(7, 0, new JLabel("Reference"));
		pn.place(7, 1, txtReference);
		JPanel p = new JPanel(new BorderLayout());
		p.add(pane, BorderLayout.CENTER);
		p.add(pn, BorderLayout.SOUTH);
		panel.add(p);
	}
	
	@Override
	public void init() {
		if (!txtName.getText().trim().equals(""))
			return;
		File file = new File(parent.getDeepPlugin().params.path2Model);
		if (file.exists())
			txtName.setText(file.getName());
	}
	
	@Override
	public boolean finish() {
		if (txtName.getText().trim().equals("")) {
			IJ.error("The name is a mandatory field");
			return false;
		}
		Parameters params = parent.getDeepPlugin().params;
		params.name = txtName.getText().trim();
		params.author = txtAuthor.getText().trim();
		params.url = txtURL.getText().trim();
		params.credit = txtCredit.getText().trim();
		params.version = txtVersion.getText().trim();
		params.date = txtDate.getText().trim();
		params.reference = txtReference.getText().trim();
		
		params.name = params.name.equals("") ? "n/a" : params.name;
		params.author = params.author.equals("") ? "n/a" : params.author;
		params.url = params.url.equals("") ? "n/a" : params.url;
		params.credit = params.credit.equals("") ? "n/a" : params.credit;
		params.version = params.version.equals("") ? "n/a" : params.version;
		params.date = params.date.equals("") ? "n/a" : params.date;
		params.reference = params.reference.equals("") ? "n/a" : params.reference;

		return true;
	}
}

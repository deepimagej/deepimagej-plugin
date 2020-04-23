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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.Index;
import ij.IJ;

public class InformationStamp extends AbstractStamp implements ActionListener {

	public JTextField	txtName			= new JTextField("", 24);
	public JTextField	txtAuthor		= new JTextField("", 24);
	public JTextField	txtURL			= new JTextField("", 24);
	public JTextField	txtVersion		= new JTextField("", 24);
	public JTextField	txtDate			= new JTextField("", 24);
	public JTextField	txtReference		= new JTextField("", 24);

	public JTextField	txtDocumentation	= new JTextField("", 24);
	public JComboBox<String> txtInfoTags	= new JComboBox<String>();
	public JTextField	txtLicense			= new JTextField("", 24);
	public JTextField	txtSource			= new JTextField("", 24);
	public JTextArea	txtDescription		= new JTextArea("", 3, 24);

	public JButton		addButtonTag		= new JButton("Add tag");
	public JButton		removeButtonTag		= new JButton("Remove");
	public String[]		tagsArray			= {"deepImageJ"};

	public InformationStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {
		HTMLPane pane = new HTMLPane(Constants.width, 80);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "General Information");
		pane.append("p", "This information will be stored in the config.yaml");
		pane.append("p", "Add the reference to properly cite the pretrained model.");
		txtDate.setText("" + new Date());
		txtURL.setText("http://");
		txtInfoTags.setEditable(true);
		txtDescription.setBorder(BorderFactory.createLineBorder(Color.gray));
		GridPanel tagsPn = new GridPanel();
		GridPanel buttonsPn = new GridPanel();
		GridPanel pn = new GridPanel(true, 6);
		pn.place(1, 0, new JLabel("Full name"));
		pn.place(1, 1, txtName);
		pn.place(2, 0, new JLabel("<html>Author of the<br/>bundled model</html>"));
		pn.place(2, 1, txtAuthor);
		pn.place(3, 0, new JLabel("DOI"));
		pn.place(3, 1, txtURL);
		pn.place(4, 0, new JLabel("Version"));
		pn.place(4, 1, txtVersion);
		pn.place(5, 0, new JLabel("Date"));
		pn.place(5, 1, txtDate);
		pn.place(6, 0, new JLabel("Article reference"));
		pn.place(6, 1, txtReference);
		pn.place(7, 0, new JLabel("<html>Short description of<br/>the model</html>"));
		txtDescription.setSize(new Dimension(3, 24));
		txtDescription.setLineWrap(true);
		txtDescription.setWrapStyleWord(true);

		pn.place(7, 1, txtDescription);

		pn.place(8, 0, new JLabel("Link to documentation"));
		pn.place(8, 1, txtDocumentation);
		pn.place(9, 0, new JLabel("Type of license"));
		pn.place(9, 1, txtLicense);
		pn.place(10, 0, new JLabel("Link to model source"));
		pn.place(10, 1, txtSource);
		pn.place(11, 0, new JLabel("<html>Tags to describe the<br/> model in the model zoo</html>"));
		pn.place(11, 1, tagsPn);
		tagsPn.place(0, 0, txtInfoTags);
		tagsPn.place(0, 1, buttonsPn);
		buttonsPn.place(0, 0, addButtonTag);
		buttonsPn.place(0, 1, removeButtonTag);
		JPanel p = new JPanel(new BorderLayout());
		
		JScrollPane scroll = new JScrollPane();
		pn.setPreferredSize(new Dimension(pn.getWidth() + 400, pn.getWidth() + 500));
        scroll.setPreferredSize(new Dimension(pn.getWidth() + 300, pn.getWidth() + 400));
        scroll.setViewportView(pn);
		
		p.add(pane, BorderLayout.NORTH);
		p.add(scroll, BorderLayout.CENTER);
		panel.add(p);
		txtInfoTags.addItem("deepImageJ");	
		txtInfoTags.setSelectedIndex(-1);
		
		addButtonTag.addActionListener(this);
		removeButtonTag.addActionListener(this);
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
		params.doi = txtURL.getText().trim();
		params.version = txtVersion.getText().trim();
		params.date = txtDate.getText().trim();
		params.reference = txtReference.getText().trim();

		params.documentation = txtDocumentation.getText().trim();
		params.license = txtLicense.getText().trim();
		params.source = txtSource.getText().trim();
		params.description = txtDescription.getText().trim();
		
		params.name = params.name.equals("") ? "n/a" : params.name;
		params.author = params.author.equals("") ? "n/a" : params.author;
		params.doi = params.doi.equals("") ? "n/a" : params.doi;
		params.version = params.version.equals("") ? "n/a" : params.version;
		params.date = params.date.equals("") ? "n/a" : params.date;
		params.reference = params.reference.equals("") ? "n/a" : params.reference;

		params.documentation = params.documentation.equals("") ? "n/a" : params.documentation;
		params.license = params.license.equals("") ? "n/a" : params.license;
		params.source = params.source.equals("") ? "n/a" : params.source;
		params.description = params.description.equals("") ? "n/a" : params.description;
		params.infoTags = Arrays.asList(tagsArray);
		
		return true;
	}
	
	public void addTag() {
		String tagToAdd = (String) txtInfoTags.getSelectedItem();
		List<String> auxList = new ArrayList<String>();
		for (String tag : tagsArray) {
			if (tag.equalsIgnoreCase(tagToAdd) == true) {
				IJ.error("The tag was already added before");
				txtInfoTags.setSelectedIndex(-1);
				return;
			}
			auxList.add(tag);
		}
		auxList.add(tagToAdd);
		tagsArray =  auxList.toArray(new String[auxList.size()]);
		txtInfoTags.addItem(tagToAdd);
		txtInfoTags.setSelectedIndex(-1);
	}
	
	public void removeTag() {
		String tagToRemove = (String) txtInfoTags.getSelectedItem();
		int ind = Index.indexOf(tagsArray, tagToRemove);
		
		if (ind == -1) {
			IJ.error("The tag did not exist, so it cannot be removed.");
		} else if ( ind != -1 && tagToRemove.equalsIgnoreCase("deepImageJ")) {
			IJ.error("Sorry this tag is compulsory.");
		} else {
			txtInfoTags.removeAllItems();
			String[] auxArray = new String[tagsArray.length - 1];
			int c = 0;
			for (int i = 0; i < tagsArray.length; i ++) {
				if (i != ind) {
					auxArray[c++] = tagsArray[i];
					txtInfoTags.addItem(tagsArray[i]);
				}
			}
			tagsArray = new String[auxArray.length];
			tagsArray = auxArray;
		}
		txtInfoTags.setSelectedIndex(-1);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == addButtonTag) {
			addTag();
		} 
		if (e.getSource() == removeButtonTag) {
			removeTag();
		} 
	}
}

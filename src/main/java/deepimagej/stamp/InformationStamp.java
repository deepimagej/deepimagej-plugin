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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.HTMLPane;
import ij.IJ;

public class InformationStamp extends AbstractStamp implements ActionListener {

	public JTextField	txtName			= new JTextField("", 24);
	public JTextField	txtURL			= new JTextField("", 24);
	public JTextField	txtVersion		= new JTextField("", 24);

	public JTextField	txtAuth		= new JTextField("", 24);
	public JTextField	txtCite		= new JTextField("", 24);
	public JTextField	txtTag		= new JTextField("", 24);

	public JTextField	txtDocumentation	= new JTextField("", 24);
	public JTextField	txtLicense			= new JTextField("", 24);
	public JTextField	txtSource			= new JTextField("", 24);
	public JTextArea	txtDescription		= new JTextArea("", 3, 24);
	
	public JList<String>authList			= new JList<String>();
	public JList<String>citeList		= new JList<String>();
	public JList<String>tagList			= new JList<String>();
	
	private DefaultListModel<String> 	authModel;
	private DefaultListModel<String> 	citeModel;
	private DefaultListModel<String> 	tagModel;

	public JButton		authAddBtn		= new JButton("Add");
	public JButton		authRmvBtn		= new JButton("Remove");
	
	public JButton		citeAddBtn		= new JButton("Add");
	public JButton		citeRmvBtn		= new JButton("Remove");

	public JButton		tagAddBtn		= new JButton("Add");
	public JButton		tagRmvBtn		= new JButton("Remove");
	
	public ArrayList<String> introducedAuth = new ArrayList<String>();
	public ArrayList<String> introducedCite = new ArrayList<String>();
	public ArrayList<String> introducedTag = new ArrayList<String>();
	
	// Parameter to keep track of the model being used
	public String		model			= ""; 

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
		txtURL.setText("http://");
		txtDescription.setBorder(BorderFactory.createLineBorder(Color.gray));

		JFrame pnFr = new JFrame();
		Container pn = pnFr.getContentPane();
		pn.setLayout(new GridBagLayout()); 

		GridBagConstraints  labelC = new GridBagConstraints();
		labelC.gridwidth = 4;
		labelC.gridheight = 1;
		labelC.gridx = 0;
		labelC.gridy = 0;
		labelC.ipadx = 5;
		labelC.weightx = 0.2;

		GridBagConstraints  infoC = new GridBagConstraints();
		infoC.gridwidth = 20;
		infoC.gridheight = 1;
		infoC.gridx = 4;
		infoC.gridy = 0;
		infoC.ipadx = 5;
		infoC.weightx = 0.8;
		infoC.anchor = GridBagConstraints.CENTER;
	    infoC.fill = GridBagConstraints.BOTH;
	    infoC.insets = new Insets(10, 0, 10, 10); 

		
		pn.add(new JLabel("Full name"), labelC);
		pn.add(txtName, infoC);
		JFrame authorsFr = createAddRemoveFrame(txtAuth, authAddBtn, "auth", authRmvBtn);
		
		labelC.gridy = 1;
		infoC.gridy = 1;
		pn.add(new JLabel("<html>Author of the<br/>bundled model</html>"), labelC);
		pn.add((JComponent) authorsFr.getContentPane(), infoC);
		labelC.gridy = 2;
		infoC.gridy = 2;
		pn.add(new JLabel("DOI"), labelC);
		pn.add(txtURL, infoC);
		labelC.gridy = 3;
		infoC.gridy = 3;
		pn.add(new JLabel("Version"), labelC);
		pn.add(txtVersion, infoC);
	   	
		//JFrame citeFr = createAddRemoveFrame(txtCite, citeAddBtn, "cite", citeRmvBtn);

		labelC.gridy = 4;
		infoC.gridy = 4;
		pn.add(new JLabel("Article reference"), labelC);
		pn.add(txtCite, infoC);
		
		labelC.gridy = 5;
		labelC.gridheight = 2;
		
		infoC.gridy = 5;
		infoC.gridheight = 2;

		infoC.ipady = 40; 
		infoC.ipadx = 0; 
		
		pn.add(new JLabel("<html>Description of<br/>the model</html>"), labelC);
		txtDescription.setPreferredSize(new Dimension(3000, 1000));
		txtDescription.setLineWrap(true);
		txtDescription.setWrapStyleWord(true);
		JScrollPane txtScroller = new JScrollPane(txtDescription);
		txtScroller.setPreferredSize(new Dimension(txtDescription.getPreferredSize().width, txtDescription.getPreferredSize().height + 50));

		pn.add(txtScroller, infoC);

		labelC.gridy = 7;
		labelC.gridheight = 1;
		infoC.gridy = 7;
		infoC.gridheight = 1;

		infoC.ipady = 0; 
		infoC.ipadx = 0;
		pn.add(new JLabel("Link to documentation"), labelC);
		pn.add(txtDocumentation, infoC);
		labelC.gridy = 8;
		infoC.gridy = 8;
		pn.add(new JLabel("Type of license"), labelC);
		pn.add(txtLicense, infoC);
		labelC.gridy = 9;
		infoC.gridy = 9;
		pn.add(new JLabel("Link to model source"), labelC);
		pn.add(txtSource, infoC);

		
		JFrame tagsFr = createAddRemoveFrame(txtTag, tagAddBtn, "tag", tagRmvBtn);

		labelC.gridy = 10;
		infoC.gridy = 10;
		pn.add(new JLabel("<html>Tags to describe the<br/> model in the model zoo</html>"), labelC);
		pn.add((JComponent) tagsFr.getContentPane(), infoC);
		
		JPanel p = new JPanel(new BorderLayout());
		
		JScrollPane scroll = new JScrollPane();
		pn.setPreferredSize(new Dimension(pn.getWidth() + 400, pn.getHeight() + 600));
        scroll.setPreferredSize(new Dimension(pn.getWidth() + 300, pn.getHeight() + 400));
        scroll.setViewportView(pn);
		
		p.add(pane, BorderLayout.NORTH);
		p.add(scroll, BorderLayout.CENTER);
		panel.add(p);
		

		// Add the tad 'deepImageJ' to the tags field. This tag
		// is not removable
		tagModel = new DefaultListModel<String>();
		tagModel.addElement("deepImageJ");
		tagList.setModel(tagModel);
		introducedTag.add("deepImageJ");
		
		authAddBtn.addActionListener(this);
		authRmvBtn.addActionListener(this);
		
		citeAddBtn.addActionListener(this);
		citeRmvBtn.addActionListener(this);
		
		tagAddBtn.addActionListener(this);
		tagRmvBtn.addActionListener(this);
	}
	
	@Override
	public void init() {
		if (!txtName.getText().trim().equals(""))
			return;
		File file = new File(parent.getDeepPlugin().params.path2Model);
		if (file.exists())
			txtName.setText(file.getName());
		if (model.equals(parent.getDeepPlugin().params.path2Model)) {
			model = parent.getDeepPlugin().params.path2Model;

			introducedAuth = new ArrayList<String>();
			authModel = new DefaultListModel<String>();
			authModel.addElement("");
			authList.setModel(authModel);

			introducedCite = new ArrayList<String>();
			citeModel = new DefaultListModel<String>();
			citeModel.addElement("");
			citeList.setModel(citeModel);
			// Add the tag 'deepImageJ' to the tags field. This tag
			// is not removable
			introducedTag = new ArrayList<String>();
			tagModel = new DefaultListModel<String>();
			citeModel.addElement("deepImageJ");
			citeList.setModel(citeModel);
			introducedTag.add("deepImageJ");
			
			// Reset all the fields
			txtURL.setText("");
			txtVersion.setText("");
			txtAuth.setText("");
			txtCite.setText("");
			txtTag.setText("");
			txtDocumentation.setText("");
			txtLicense.setText("");
			txtSource.setText("");
			txtDescription.setText("");
		}
	}
	
	@Override
	public boolean finish() {
		if (txtName.getText().trim().equals("")) {
			IJ.error("The name is a mandatory field");
			return false;
		}
		Parameters params = parent.getDeepPlugin().params;
		params.name = txtName.getText().trim();
		params.doi = txtURL.getText().trim();
		params.version = txtVersion.getText().trim();
		// TODO decide if allow one or more citations
		params.reference = txtCite.getText().trim();

		params.documentation = txtDocumentation.getText().trim();
		params.license = txtLicense.getText().trim();
		params.source = txtSource.getText().trim();
		params.description = txtDescription.getText().trim();
		
		params.name = params.name.equals("") ? null : params.name;
		params.author = null;
		if (introducedAuth.size() > 0)
			params.author = introducedAuth;
		params.doi = params.doi.equals("") ? null : params.doi;
		params.version = params.version.equals("") ? null : params.version;
		params.reference = params.reference.equals("") ? null : params.reference;
		// TODO see whether to allow only one or several citaions
		/*
		params.reference = null;
		if (introducedCite.size() > 0)
			params.reference = introducedCite;
		*/
		params.documentation = params.documentation.equals("") ? null : params.documentation;
		params.license = params.license.equals("") ? null : params.license;
		params.source = params.source.equals("") ? null : params.source;
		params.description = params.description.equals("") ? null : params.description;
		params.infoTags = introducedTag;
		
		return true;
	}
	
	public void addAuthor() {
		// Get the author introduced
		String authName = txtAuth.getText().trim();
		if (authName.equals("")) {
			IJ.error("Introduce a name");
			return;
		}
		introducedAuth.add(authName);

		authModel = new DefaultListModel<String>();
		
		// Add the elements to the list

		for (String name : introducedAuth){
			authModel.addElement(name);
		}
		authList.setModel(authModel);
		txtAuth.setText("");
	}
	public void removeAuthor() {
		// Get the author selected
		int authInd = authList.getSelectedIndex();
		if (authInd == -1) {
			IJ.error("No author selected");
			return;
		}
		introducedAuth.remove(authInd);

		authModel = new DefaultListModel<String>();
		
		// Add the elements to the list

		for (String name : introducedAuth){
			authModel.addElement(name);
		}
		authList.setModel(authModel);
	}
	
	public void addCite() {
		// Get the author introduced
		String citation = txtCite.getText().trim();
		if (citation.equals("")) {
			IJ.error("Introduce a name");
			return;
		}
		introducedCite.add(citation);

		citeModel = new DefaultListModel<String>();
		
		// Add the elements to the list

		for (String name : introducedCite){
			citeModel.addElement(name);
		}
		citeList.setModel(citeModel);
	}
	public void removeCite() {
		// Get the author selected
		int citation = citeList.getSelectedIndex();
		if (citation == -1) {
			IJ.error("No citation selected");
			return;
		}
		introducedCite.remove(citation);

		citeModel = new DefaultListModel<String>();
		
		// Add the elements to the list

		for (String name : introducedCite){
			citeModel.addElement(name);
		}
		citeList.setModel(citeModel);
	}
	
	public void addTag() {
		// Get the author introduced
		String tag = txtTag.getText().trim();
		if (tag.equals("")) {
			IJ.error("Introduce a name");
			return;
		}
		introducedTag.add(tag);

		tagModel = new DefaultListModel<String>();
		
		// Add the elements to the list

		for (String name : introducedTag){
			tagModel.addElement(name);
		}
		tagList.setModel(tagModel);
		txtTag.setText("");
	}
	public void removeTag() {
		// Get the author selected
		int tag = tagList.getSelectedIndex();
		if (tag == -1) {
			IJ.error("No tag selected");
			return;
		} else if (tag == 0) {
			IJ.error("Cannot remove 'deepImageJ' tag");
			return;
		}
		introducedTag.remove(tag);

		tagModel = new DefaultListModel<String>();
		
		// Add the elements to the list

		for (String name : introducedTag){
			tagModel.addElement(name);
		}
		tagList.setModel(tagModel);
	}
	
	/*
	 * Method that creates the Gui component that allows adding and removing tags
	 */
	public JFrame createAddRemoveFrame(JTextField txt, JButton add, String option, JButton rmv) {
		// Create the panel to add authors
		JFrame authorsFr = new JFrame();
		authorsFr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		Container authorsPn = authorsFr.getContentPane();
		authorsPn.setLayout(new GridBagLayout()); 

	    // creates a constraints object 
	    GridBagConstraints c = new GridBagConstraints(); 
	    c.fill = GridBagConstraints.BOTH;
	    c.ipady = 5; 
	    c.ipadx = 20; 
	    c.weightx = 1;
	    c.gridx = 0;
	    c.gridy = 0;
	    c.gridwidth = 7;
	    authorsPn.add(txt, c);

	    c.ipady = 0; 
	    c.ipadx = 0; 
	    c.weightx = 0.2;
	    c.gridx = 7;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.CENTER;
	    c.fill = GridBagConstraints.NONE;
	    authorsPn.add(add, c);

	    c.ipady = 40; 
	    c.ipadx = 20; 
	    c.weightx = 1;
	    c.weighty = 1;
	    c.gridwidth = 7;
	    c.anchor = GridBagConstraints.CENTER;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 1;
	    if (option.contains("auth")) {
			authModel = new DefaultListModel<String>();
			authModel.addElement("");
			authList = new JList<String>(authModel);
			authList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			authList.setLayoutOrientation(JList.VERTICAL);
			authList.setVisibleRowCount(2);
			JScrollPane listScroller = new JScrollPane(authList);
			listScroller.setPreferredSize(new Dimension(Constants.width, panel.getPreferredSize().height));
		    authorsPn.add(listScroller, c);
	    } else if(option.contains("cite")) {
			citeModel = new DefaultListModel<String>();
			citeModel.addElement("");
			citeList = new JList<String>(citeModel);
			citeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			citeList.setLayoutOrientation(JList.VERTICAL);
			citeList.setVisibleRowCount(2);
			JScrollPane listScroller = new JScrollPane(citeList);
			listScroller.setPreferredSize(new Dimension(Constants.width, panel.getPreferredSize().height));
		    authorsPn.add(listScroller, c);
	    } else if(option.contains("tag")) {
			tagModel = new DefaultListModel<String>();
			tagModel.addElement("");
			tagList = new JList<String>(tagModel);
			tagList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			tagList.setLayoutOrientation(JList.VERTICAL);
			tagList.setVisibleRowCount(2);
			JScrollPane listScroller = new JScrollPane(tagList);
			listScroller.setPreferredSize(new Dimension(Constants.width, panel.getPreferredSize().height));
		    authorsPn.add(listScroller, c);
	    }

	    c.ipady = 0; 
	    c.ipadx = 0; 
	    c.gridx = 7;
	    c.gridy = 1;
	    c.gridheight =1; 
	    c.anchor = GridBagConstraints.CENTER;
	    c.fill = GridBagConstraints.NONE;
	    c.weightx = 0.2;
	    Dimension btnDims = authAddBtn.getPreferredSize();
	    rmv.setPreferredSize(btnDims);
	    authorsPn.add(rmv, c);
	    authorsFr.pack();
	    return authorsFr;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == tagAddBtn) {
			addTag();
		} 
		if (e.getSource() == tagRmvBtn) {
			removeTag();
		} 
		if (e.getSource() == authAddBtn) {
			addAuthor();
		} 
		if (e.getSource() == authRmvBtn) {
			removeAuthor();
		} 
		if (e.getSource() == citeAddBtn) {
			addCite();
		} 
		if (e.getSource() == citeRmvBtn) {
			removeCite();
		} 
	}
}

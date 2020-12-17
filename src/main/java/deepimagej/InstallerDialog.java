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

package deepimagej;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import deepimagej.components.HTMLPane;
import deepimagej.installer.BioimageZooRepository;
import deepimagej.installer.Model;
import ij.gui.GUI;

public class InstallerDialog extends JDialog implements ItemListener, ActionListener {

	private BioimageZooRepository zoo;
	private JButton install = new JButton("Install");
	private JButton cancel = new JButton("Cancel");
	private JButton help = new JButton("Help");
	private HTMLPane repo = new HTMLPane(600, 100);
	private HTMLPane info = new HTMLPane(600, 200);
	private JCheckBox chk = new JCheckBox("I accept to install .....", false);
	private JComboBox<String> cmb = new JComboBox<String>();
	
	public InstallerDialog(BioimageZooRepository zoo) {
		super(new JFrame(), "DeepImageJ Model Installer");

		
		this.zoo = zoo;
		Font font = cmb.getFont();
		cmb.setFont(new Font(font.getFamily(), Font.BOLD, font.getSize()+2));
		cmb.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
		cmb.addItem("<html>&laquo Select a compatible model &raquo</html>");
		for(String name : zoo.models.keySet()) {
			cmb.addItem(zoo.models.get(name).getFacename());
		}

		pack();
		JPanel pn1 = new JPanel(new BorderLayout());
		pn1.add(repo.getPane(), BorderLayout.CENTER);

		info.enableHyperLink();
		repo.enableHyperLink();
		repo.append("h1", zoo.title);
		repo.append("i", zoo.name);
		repo.append("p", "small", zoo.location);
		repo.appendLink(zoo.url, zoo.url);

		JPanel pn2 = new JPanel(new BorderLayout());
		pn2.add(cmb, BorderLayout.NORTH);
		pn2.add(info.getPane(), BorderLayout.CENTER);
		pn2.add(chk, BorderLayout.SOUTH);

		JPanel bn = new JPanel(new GridLayout(1, 3));
		bn.add(help);
		bn.add(cancel);
		bn.add(install);

		JPanel main = new JPanel(new BorderLayout());
		main.add(pn1, BorderLayout.NORTH);
		main.add(pn2, BorderLayout.CENTER);
		main.add(bn, BorderLayout.SOUTH);
		add(main);

		chk.addItemListener(this);
		cmb.addItemListener(this);
		install.setEnabled(false);
		help.addActionListener(this);
		cancel.addActionListener(this);
		install.addActionListener(this);
		pack();
		GUI.center(this);
		setVisible(true);

	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel)
			dispose();
		else if (e.getSource() == install) {
			
			dispose();
		}
		else if (e.getSource() == help) {
			openWebBrowser("https://deepimagej.github.io/deepimagej/");
		}
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == cmb) {
			info.clear();
			chk.setSelected(false);
			String name = (String)cmb.getSelectedItem();
			if (name != null)
				if (!name.equals("")) {
					Model model = zoo.models.get(name);
					if (model != null) {
						String s = "";
						if (model.deepImageJ)
							s = "&nbsp&nbsp <span style=\"color:#10FF10\">deepImageJ compatible</span>";
						else
							s = "&nbsp&nbsp <span style=\"color:#FF1010\">not compatible with deepImageJ</span>";
						info.append("h1", model.name + s);
						info.append("i", model.authors);
						info.appendLink(model.doc, "Read documentation");
						info.append("p", model.getCoverHTML());
						info.append("p", "small", model.desc);
						chk.setEnabled(model.deepImageJ);
						
					}
				}
			
		}
		
		if (e.getSource() == chk) {
			boolean b = chk.isSelected();
			install.setEnabled(b);
		}
		
	}
	
	private void openWebBrowser(String url) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(new URL(url).toURI());
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}

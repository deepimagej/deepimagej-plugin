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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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
import deepimagej.tools.FileTools;
import deepimagej.tools.ModelDownloader;
import ij.IJ;
import ij.gui.GUI;

public class InstallerDialog extends JDialog implements ItemListener, ActionListener, Runnable {

	private BioimageZooRepository zoo;
	private JButton install = new JButton("Install");
	private JButton cancel = new JButton("Cancel");
	private JButton help = new JButton("Help");
	private HTMLPane repo = new HTMLPane(600, 100);
	private HTMLPane info = new HTMLPane(600, 200);
	private JCheckBox chk = new JCheckBox("<html>I accept to install the model knowing that the output of a deep learning model strongly depends on the" 
										+ "<br>data and the conditions of the training process. I understand that a pre-trained model may require a re-training." 
										+ "<br>If you have any doubt, please check the documentation of the model. To get user guidelines press the Help button.", false);
	private JComboBox<String> cmb = new JComboBox<String>();
	private DownloadProgress progressScreen;
	private String fileName = "";
	private String modelsDir = IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
	private long webFileSize = 1;
	private Model model = null;
	boolean stopped = true;
	
	public InstallerDialog(BioimageZooRepository zoo) {
		super(new JFrame(), "DeepImageJ Model Installer");

		
		this.zoo = zoo;
		Font font = cmb.getFont();
		cmb.setFont(new Font(font.getFamily(), Font.BOLD, font.getSize()+2));
		cmb.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
		cmb.addItem("<html>&laquo Select a compatible model &raquo</html>");
		for(String name : zoo.models2.keySet()) {
			cmb.addItem(zoo.models2.get(name).getFacename());
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
			installModel();
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
			if (name != null && !name.equals("")) {
				Model model = zoo.models2.get(name);
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
			install.setEnabled(b && stopped);
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
	
	/*
	 * Get file size of the online model
	 */
	public long getFileSize(URL url) {
	HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			return conn.getContentLengthLong();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
	
	private void installModel() {
		String name = (String)cmb.getSelectedItem();
		if (name != null && !name.equals("")) {
			// First check that "Fiji.App\models" exist or create it if not
			modelsDir = IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
			// TODO modelsDir = "C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models";
			if (!(new File(modelsDir).exists()))
				new File(modelsDir).mkdir();
			model = zoo.models2.get(name);
			if (model != null) {

				int nameStart = model.downloadUrl.lastIndexOf("/") + 1;
				fileName = model.downloadUrl.substring(nameStart);
				// Add timestamp to the model name. 
				// The format consists on: modelName + date as ddmmyyyy + time as hhmmss
		        Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYYY_HHmmss");
				String dateString = sdf.format(cal.getTime());
				fileName = fileName.substring(0, fileName.lastIndexOf(".")) + "_" + dateString + ".zip";
				
				Thread thread = new Thread(this);
				thread.setPriority(Thread.MIN_PRIORITY);
				progressScreen = new DownloadProgress();
				progressScreen.setThread(thread);
				stopped = false;
				thread.start();
			}
		}
	}
	
	@Override
	public void run() {
		install.setEnabled(false);
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			URL website = new URL(model.downloadUrl);
			webFileSize = getFileSize(website);
			rbc = Channels.newChannel(website.openStream());
			// Create the new model file as a zip
			fos = new FileOutputStream(new File(modelsDir, fileName));
			// Send the correct parameters to the progress screen
			progressScreen.setFileName(modelsDir + File.separator +  fileName);
			progressScreen.setmodelName(fileName);
			progressScreen.setFileSize(webFileSize);
			progressScreen.buildScreen();
			progressScreen.setVisible(true);
			ModelDownloader downloader = new ModelDownloader(rbc, fos);
			downloader.call();
			if (!Thread.interrupted()) {
				// Once it is already downloaded unzip the model
				String unzippedFileName = modelsDir + File.separator + fileName.substring(0, fileName.lastIndexOf("."));
				FileTools.unzipFolder(new File(modelsDir, fileName), unzippedFileName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			IJ.error("Error unzipping the model.");
			e.printStackTrace();
		} finally {
			try {
				if (fos != null)
						fos.close();
				if (rbc != null)
					rbc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		progressScreen.stop();
		stopped = true;
		chk.setSelected(false);
	}

}
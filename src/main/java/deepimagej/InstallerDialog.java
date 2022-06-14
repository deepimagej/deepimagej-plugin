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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import deepimagej.components.HTMLPane;
import deepimagej.Constants;
import deepimagej.components.TitleHTMLPane;
import deepimagej.installer.Author;
import deepimagej.installer.BioimageZooRepository;
import deepimagej.installer.Model;
import deepimagej.tools.FileTools;
import deepimagej.tools.ModelDownloader;
import ij.IJ;
import ij.gui.GUI;
import ij.gui.GenericDialog;

public class InstallerDialog extends JDialog implements ItemListener, ActionListener, Runnable {

	private BioimageZooRepository zoo;
	private JButton install = new JButton("Install");
	private JButton close = new JButton("Close");
	private JButton help  = new JButton("Help");
	private HTMLPane repo = new HTMLPane(600, 100);
	private HTMLPane info = new HTMLPane(600, 200);
	private JCheckBox chk = new JCheckBox("<html>I accept to install the model knowing that the output of a deep learning model strongly depends on the" 
										+ "<br>data and the conditions of the training process. I understand that a pre-trained model may require a re-training." 
										+ "<br>If you have any doubt, please check the documentation of the model. To get user guidelines press the Help button.", false);
	private JComboBox<String> cmb = new JComboBox<String>();
	
	// Buttons to support dual behaviour of the plugin
	private JTextField txtURL = new JTextField("http://", 20);
	private JTextField txtZIP = new JTextField("", 20);
	private JRadioButton rbURL = new JRadioButton("From URL web link", true);
	private JRadioButton rbZIP = new JRadioButton("From ZIP file", false);
	private JTabbedPane tab = new JTabbedPane();
	
	private DownloadProgress progressScreen;
	private String fileName = "";
	private String modelsDir = IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
	private long webFileSize = 1;
	private Model model = null;
	boolean stopped = true;
	
	// URL used to download the model
	private List<String> downloadURLs;
	
	
	public InstallerDialog(BioimageZooRepository zoo) {
		super(new JFrame(), "DeepImageJ Install Model [" + Constants.version + "]");

		zoo.listAllModels();
		this.zoo = zoo;
		Font font = cmb.getFont();
		cmb.setFont(new Font(font.getFamily(), Font.BOLD, font.getSize()+2));
		cmb.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
		cmb.addItem("<html>&laquo Select a compatible model &raquo</html>");
		for(String name : zoo.models.keySet()) {
			cmb.addItem(zoo.models.get(name).name);
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
		bn.add(close);
		bn.add(install);

		JPanel repo = new JPanel(new BorderLayout());
		repo.add(pn1, BorderLayout.NORTH);
		repo.add(pn2, BorderLayout.CENTER);

		JPanel botton = new JPanel(new BorderLayout());
		botton.add(chk, BorderLayout.NORTH);
		botton.add(bn, BorderLayout.CENTER);

		ButtonGroup group = new ButtonGroup();
		group.add(rbURL);
		group.add(rbZIP);

		JPanel pn31 = new JPanel(new BorderLayout());
		pn31.setBorder(BorderFactory.createEtchedBorder());
		pn31.add(rbURL, BorderLayout.NORTH);
		pn31.add(txtURL, BorderLayout.CENTER);
		
		JPanel pn31a = new JPanel(new BorderLayout());
		pn31a.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pn31a.add(pn31, BorderLayout.CENTER);
		
		JPanel pn32 = new JPanel(new BorderLayout());
		pn32.setBorder(BorderFactory.createEtchedBorder());
		pn32.add(rbZIP, BorderLayout.NORTH);
		pn32.add(txtZIP, BorderLayout.CENTER);

		JPanel pn32a = new JPanel(new BorderLayout());
		pn32a.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pn32a.add(pn32, BorderLayout.CENTER);

		JPanel pn3 = new JPanel(new BorderLayout());
		pn3.add(pn31a, BorderLayout.NORTH);
		pn3.add(pn32a, BorderLayout.CENTER);

		JPanel pn3f = new JPanel(new BorderLayout());
		pn3f.add(pn3, BorderLayout.NORTH);
		pn3f.add(new JLabel(), BorderLayout.CENTER);
		
		JPanel instModel = new JPanel(new BorderLayout());
		instModel.add(pn3, BorderLayout.NORTH); //Change pn 3necessary
		instModel.add(new JLabel(), BorderLayout.CENTER);

		
		tab.addTab("BioImage Model Zoo", repo);
		tab.addTab("Private Model", pn3f);
		tab.addTab("Installed models", instModel);
		
		JPanel main = new JPanel(new BorderLayout());
		main.add(new TitleHTMLPane().getPane(), BorderLayout.NORTH);
		main.add(tab, BorderLayout.CENTER);
		main.add(botton, BorderLayout.SOUTH);


		add(main);

		chk.addItemListener(this);
		cmb.addItemListener(this);
		install.setEnabled(false);
		help.addActionListener(this);
		close.addActionListener(this);
		install.addActionListener(this);
		pack();
		GUI.center(this);
		setVisible(true);

	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == close) {
			dispose();
		} else if (e.getSource() == install && tab.getSelectedIndex() == 0) {
			// If the tab selected is the first one, install from Bioimage.io
			installModelBioimageio();
		} else if (e.getSource() == install && tab.getSelectedIndex() == 1 && rbURL.isSelected()) {
			// If the tab selected is the seond one, install from URL
			installModelUrl();
		} else if (e.getSource() == install && tab.getSelectedIndex() == 1 && rbZIP.isSelected()) {
			// If the tab selected is the seond one, install from URL
			installModelZip();
		} else if (e.getSource() == help) {
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
				Model model = zoo.models.get(name);
				if (model != null) {
					info.append("h1", model.name);
					for (Author aa : model.authors)
						info.append("i", aa.getName());
					info.appendLink(model.doc, "Read documentation");
					info.append("p", model.getCoverHTML());
					info.append("p", "small", model.description);
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
	public long getFileSize() {
		long totSize = 0;
		HttpURLConnection conn = null;
		for (String str : this.downloadURLs) {
			try {
				URL url = new URL(str);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("HEAD");
				totSize += conn.getContentLengthLong();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return totSize;
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
	
	/**
	 * Copy DeepImageJ model from path and unzip it in the 
	 * models folder of Fiji/ImageJ
	 */
	private void installModelZip() {
		String name = (String) txtZIP.getText();
		File zipFile = new File(name);
		if (name == null || name.equals("") || !zipFile.isFile() || !name.endsWith(".zip")) {
			IJ.error("The path introduced has to correspond to a valid zip file in the system.");
			chk.setSelected(false);
			return;
		}
		// First check that "Fiji.App\models" exist or create it if not
		modelsDir = IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
		// TODO modelsDir = "C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models";
		
		webFileSize = (long) (zipFile.length() / (1024 * 1024.0));
		// Add timestamp to the model name. 
		// The format consists on: modelName + date as ddmmyyyy + time as hhmmss
        Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYYY_HHmmss");
		String dateString = sdf.format(cal.getTime());
		// Get the original name of the zip file
		fileName = zipFile.getName();
		fileName = fileName.substring(0, fileName.lastIndexOf("."));
		fileName = fileName + "_" + dateString + ".zip";
		
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		progressScreen = new DownloadProgress(false);
		progressScreen.setThread(thread);
		stopped = false;
		thread.start();
		chk.setSelected(false);
	}
	
	/**
	 * Download DeepImageJ model from URL and unzip it in the 
	 * models folder of Fiji/ImageJ
	 */
	private void installModelUrl() {
		String name = (String) txtURL.getText();
		if (name == null || name.equals("")) {
			IJ.error("Please introduce a valid URL.");
			chk.setSelected(false);
			return;
		}
		try {
			// Check if the String introduced can be converted into an URL. If
			// itt can, set the downloadURL to that string
			URL url = new URL(name);
			downloadURLs= new ArrayList<String>();
			downloadURLs.add(name);
		} catch (MalformedURLException e) {
			IJ.error("String introduced does not correspond to a valid URL.");
			chk.setSelected(false);
			return;
		}
		
		// First check that "Fiji.App\models" exist or create it if not
		modelsDir = IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
		// TODO modelsDir = "C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models";

		// Add timestamp to the model name. 
		// The format consists on: modelName + date as ddmmyyyy + time as hhmmss
        Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYYY_HHmmss");
		String dateString = sdf.format(cal.getTime());
		fileName = "deepImageJModel" + "_" + dateString + ".zip";
		
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		progressScreen = new DownloadProgress(true);
		progressScreen.setThread(thread);
		stopped = false;
		thread.start();
		chk.setSelected(false);
	}

	
	/**
	 * Download DeepImageJ model from the Bioimage.io and unzip it in the 
	 * models folder of Fiji/ImageJ
	 */
	private void installModelBioimageio() {
		String name = (String)cmb.getSelectedItem();
		if (name != null && !name.equals("")) {
			// First check that "Fiji.App\models" exist or create it if not
			modelsDir = IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
			// TODO modelsDir = "C:\\Users\\Carlos(tfg)\\Desktop\\Fiji.app\\models";
			if (!(new File(modelsDir).exists()))
				new File(modelsDir).mkdir();
			model = zoo.models.get(name);
			if (model != null) {
				downloadURLs = model.downloadLinks;
				if (downloadURLs == null) {
					IJ.error("No download url specified in the rdf.yaml file.\n"
							+ "Cannot download the model");
				}
				
				Thread thread = new Thread(this);
				thread.setPriority(Thread.MIN_PRIORITY);
				progressScreen = new DownloadProgress(true);
				progressScreen.setThread(thread);
				stopped = false;
				thread.start();
				chk.setSelected(false);
			}
		}
	}
	
	@Override
	public void run() {
		install.setEnabled(false);
		if (downloadURLs.size() == 1 && new File(downloadURLs.get(0)).isFile()) {
			copyFromPath();
		} else {
			downloadModelFromInternet();
		}
		
		stopped = true;
		chk.setSelected(false);
	}
	
	/**
	 * Method that copies zip model from introduced
	 * location into the models folder and unzips it
	 */
	public void copyFromPath() {
		boolean copied = false;
		try {
			if (downloadURLs.size() != 1)
				throw new Exception();
			fileName = new File(downloadURLs.get(0)).getName();
			// Add timestamp to the model name. 
			// The format consists on: modelName + date as ddmmyyyy + time as hhmmss
	        Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYYY_HHmmss");
			String dateString = sdf.format(cal.getTime());
			fileName = fileName.substring(0, fileName.lastIndexOf(".")) + "_" + dateString + ".zip";
			File originalModel = new File(downloadURLs.get(0));
			File destFile = new File(modelsDir + File.separator +  fileName);
			progressScreen.setFileName(modelsDir + File.separator +  fileName);
			progressScreen.setmodelName(fileName);
			progressScreen.setFileSize(webFileSize);
			progressScreen.buildScreen();
			progressScreen.setVisible(true);
			FileTools.copyFile(originalModel, destFile);
			copied = true;
			if (!Thread.interrupted()) {
				// Once it is already downloaded unzip the model
				String unzippedFileName = modelsDir + File.separator + fileName.substring(0, fileName.lastIndexOf("."));
				FileTools.unzipFolder(new File(modelsDir, fileName), unzippedFileName);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			if (!copied) {
				IJ.error("Unable to copy file from desired location.\n"
						+ "Check the permissions.");
			} else {
				IJ.error("Unable to unzip he file in the models folder.");
			}
		}
		progressScreen.stop();
	}
	
	public void downloadModelFromInternet() {
		if (downloadURLs.size() == 1) {
			downloadSingleFileModelFromInternet(downloadURLs.get(0));
		} else if (downloadURLs.size() > 1) {
			downloadSeveralFilesModelFromInternet();
		} else {
			throw new IllegalArgumentException("No files to download.");
		}
	}

	/**
	 * Method that downloads the model selected from the internet,
	 * copies it and unzips it into the models folder
	 */
	public void downloadSeveralFilesModelFromInternet() {
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			// Add timestamp to the model name. 
			// The format consists on: modelName + date as ddmmyyyy + time as hhmmss
	        Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYYY_HHmmss");
			String dateString = sdf.format(cal.getTime());
			fileName = model.name + "_" + dateString;
			webFileSize = getFileSize();
			progressScreen.setFileName(modelsDir + File.separator +  fileName);
			progressScreen.setmodelName(fileName);
			progressScreen.setFileSize(webFileSize);
			progressScreen.buildScreen();
			progressScreen.setVisible(true);
			for (String url : this.downloadURLs) {
				String internetName = new File(url).getName();
				URL website = new URL(url);
				rbc = Channels.newChannel(website.openStream());
				// Create the new model file as a zip
				fos = new FileOutputStream(new File(modelsDir + File.separator +  fileName + internetName));
				// Send the correct parameters to the progress screen
				ModelDownloader downloader = new ModelDownloader(rbc, fos);
				downloader.call();
				if (Thread.interrupted()) {
					break;
				}
				fos.close();
				rbc.close();
			}
		} catch (IOException e) {
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
	}

	/**
	 * Method that downloads the model selected from the internet,
	 * copies it and unzips it into the models folder
	 */
	public void downloadSingleFileModelFromInternet(String url) {
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			// Add timestamp to the model name. 
			// The format consists on: modelName + date as ddmmyyyy + time as hhmmss
	        Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMYYYY_HHmmss");
			String dateString = sdf.format(cal.getTime());
			fileName = model.name + "_" + dateString + ".zip";
			URL website = new URL(url);
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
	}
}

/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage.
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique fédérale de Lausanne (EPFL), Switzerland
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


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import deepimagej.Constants;
import deepimagej.tools.FileTools;
import ij.IJ;
import ij.plugin.frame.PlugInFrame;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.utils.ZipUtils;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class Install_From_URL_Or_Path extends PlugInFrame {
    private static final long serialVersionUID = -5457714313295644697L;
	// Components
    private JLabel labelModel;
    private JTextField pathTextField;
    private JButton browseFileBtn;
    private JProgressBar progressBar;
    private JButton cancelButton;
    private JButton installButton;
    
    private boolean correctlyDownloaded = false;
    private Thread installationThread;
    
    private static String MODELS_DIR;
    private static Calendar CALENDAR = Calendar.getInstance();
    private static SimpleDateFormat SDF = new SimpleDateFormat("ddMMYYYY_HHmmss");
    
    
    
    private static final String INPUT_INFO = "<html>"
    		+ "Choose whether to run the model on the current in-focus image or on the image(s) at<br>"
    		+ "the specified path. If a path is provided, it can point to either a single image file<br>"
    		+ "or a folder containing multiple images.</html>";

    public Install_From_URL_Or_Path() {
        super("deepImageJ " + Constants.DIJ_VERSION + " - Install model from zip");
        initComponents();
        pack();
        setLocationRelativeTo(null); // Center the frame
        setVisible(true);
    }

    private void initComponents() {
        // Set the layout manager for the content pane
        this.setLayout(new GridBagLayout());

        // Initialize components
        labelModel = new JLabel("Model folder:");

        pathTextField = new JTextField(40);
        pathTextField.setEnabled(true);
        pathTextField.getDocument().addDocumentListener(new DocumentListener() { // Listener for path text field
            public void changedUpdate(DocumentEvent e) {
                decideInstallEnabled();
            }
            public void removeUpdate(DocumentEvent e) {
            	decideInstallEnabled();
            }
            public void insertUpdate(DocumentEvent e) {
            	decideInstallEnabled();
            }
        });
        browseFileBtn = new JButton("Browse");
        browseFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                FileNameExtensionFilter zipFilter = new FileNameExtensionFilter("ZIP Files", "zip");
                fileChooser.setFileFilter(zipFilter);
                int option = fileChooser.showOpenDialog(Install_From_URL_Or_Path.this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedDirectory = fileChooser.getSelectedFile();
                    pathTextField.setText(selectedDirectory.getAbsolutePath());
                }
            }
        });
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true); // Show percentage text
        progressBar.setValue(0);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
        	this.dispose();
        	if (installationThread != null && installationThread.isAlive())
        		installationThread.interrupt();
        });
        installButton = new JButton("Install");
        installButton.setEnabled(false);
        installButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	install();
            }
        });

        // Adjust label alignment and spacing
        int gridy = 0;

        // Row 1: Question mark, Model label and combo box
        // Panel for path text field and browse button
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.add(pathTextField, BorderLayout.CENTER);
        pathPanel.add(browseFileBtn, BorderLayout.EAST);
        addRowComponents(gridy++, INPUT_INFO, labelModel, pathPanel, true);
        
        // Add progress bar
        GridBagConstraints progressBarGbc = new GridBagConstraints();
        progressBarGbc.gridx = 0;
        progressBarGbc.gridy = gridy++;
        progressBarGbc.gridwidth = 4;
        progressBarGbc.fill = GridBagConstraints.HORIZONTAL;
        progressBarGbc.insets = new Insets(10, 5, 10, 5);
        this.add(progressBar, progressBarGbc);
        

        // Row 6: Buttons aligned to the right
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(installButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        this.add(buttonPanel, gbc);
    }

    private void addRowComponents(int gridy, String tooltipText, JComponent leftComponent, JComponent rightComponent, boolean expandRightComponent) {
        GridBagConstraints gbc = new GridBagConstraints();

        // Question mark icon
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        Icon questionIcon = UIManager.getIcon("OptionPane.questionIcon");
        if (questionIcon == null) {
            questionIcon = UIManager.getIcon("OptionPane.informationIcon");
        }
        JLabel questionMark = new JLabel(questionIcon);
        questionMark.setToolTipText(tooltipText);
        this.add(questionMark, gbc);

        // Left component (label or checkbox)
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        this.add(leftComponent, gbc);

        // Right component (input field or panel)
        gbc.gridx = 2;
        if (expandRightComponent) {
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
        } else {
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
        }
        gbc.anchor = GridBagConstraints.WEST;
        this.add(rightComponent, gbc);
    }
    
    private void decideInstallEnabled() {
    	String text = this.pathTextField.getText().trim();
    	if (text != null && !text.equals(""))
    		this.installButton.setEnabled(true);
    	else
    		this.installButton.setEnabled(false);
    }
    
    private void install() {
    	String text = this.pathTextField.getText().trim();
    	if (new File(text).isFile()) {
    		prepareAndManageLocalModel(text);
    		return;
    	}
    	URL url;
    	try {
    		url = new URL(text);
    	} catch (MalformedURLException ex) {
    		IJ.error("Invalid URL");
    		this.installButton.setEnabled(false);
    		return;
    	}
    	String urlPath;
		try {
			urlPath = url.toURI().getPath();
		} catch (URISyntaxException e) {
    		IJ.error("Invalid URL");
			e.printStackTrace();
			return;
		}
    	if (new File(urlPath).isFile()) {
    		prepareAndManageLocalModel(urlPath);
    		return;
    	}
		prepareAndManageOnlineModel(text);
    }
    
    private void prepareAndManageLocalModel(String fileName) {
    	SwingUtilities.invokeLater(() -> {
    		this.installButton.setEnabled(false);
    		this.pathTextField.setEnabled(false);
            browseFileBtn.setEnabled(false);
    		this.progressBar.setIndeterminate(true);
    		this.progressBar.setString("Preparing installation");
    	});
    	installationThread = new Thread(() -> {
    		try {
		    	SwingUtilities.invokeLater(() -> {
		    		this.progressBar.setIndeterminate(false);
		    	});
				installModelFromLocalFile(fileName);
		    	SwingUtilities.invokeLater(() -> {
		    		this.installButton.setEnabled(true);
		    	});
			} catch (InterruptedException | IOException e) {
				SwingUtilities.invokeLater(() -> IJ.error("Error installing model from local file."));
				e.printStackTrace();
			}
	    	SwingUtilities.invokeLater(() -> {
	            browseFileBtn.setEnabled(true);
	    		this.pathTextField.setEnabled(true);
	    		this.progressBar.setIndeterminate(false);
	    		this.progressBar.setValue(0);
	    		this.progressBar.setString("");
	    	});
    	});
    	installationThread.start();
    }
    
    private void prepareAndManageOnlineModel(String url) {
    	SwingUtilities.invokeLater(() -> {
    		this.installButton.setEnabled(false);
            browseFileBtn.setEnabled(false);
    		this.pathTextField.setEnabled(false);
    		this.progressBar.setIndeterminate(true);
    		this.progressBar.setString("Preparing installation");
    	});
    	installationThread = new Thread(() -> {
    		try {
		    	SwingUtilities.invokeLater(() -> {
		    		this.progressBar.setIndeterminate(false);
		    	});
    			installModelUrl(url);
		    	SwingUtilities.invokeLater(() -> {
		    		this.installButton.setEnabled(true);
		    	});
			} catch (InterruptedException | IOException e) {
				SwingUtilities.invokeLater(() -> IJ.error("Error installing model from url."));
				e.printStackTrace();
			}
	    	SwingUtilities.invokeLater(() -> {
	            browseFileBtn.setEnabled(true);
	    		this.pathTextField.setEnabled(true);
	    		this.progressBar.setIndeterminate(false);
	    		this.progressBar.setValue(0);
	    		this.progressBar.setString("");
	    	});
    	});
    	installationThread.start();
    }
	
	/**
	 * Download DeepImageJ model from URL and unzip it in the 
	 * models folder of Fiji/ImageJ
	 * @param sourceURL
	 * 	source url of a zip file that is going to be downloaded into the models folder
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void installModelUrl(String sourceURL) throws InterruptedException, IOException {
		URL url = new URL(sourceURL);
		
		String fileName = createFileName(sourceURL);
		long fileSize = FileDownloader.getFileSize(url);
		
		
		Thread currentThread = Thread.currentThread();
		correctlyDownloaded = false;
		Thread dwnldThread = new Thread(() -> {
			try (
					ReadableByteChannel rbc = Channels.newChannel(url.openStream());
					FileOutputStream fos = new FileOutputStream(fileName);
					) {
				new FileDownloader(rbc, fos).call(currentThread);
				correctlyDownloaded = true;
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});
		dwnldThread.start();
		
		while (dwnldThread.isAlive()) {
			Thread.sleep(100);
			long nSize = new File(fileName).length();
			double progress = Math.round(((double) nSize) * 100 / fileSize);
			SwingUtilities.invokeLater(() -> {
				progressBar.setValue((int) progress);
				progressBar.setString("Download progress: " + progress + "%");
			});
		}
		if (!correctlyDownloaded) {
			IJ.error("The model was not correctly downloaded, please try again.");
			return;
		}
		unzip(fileName);
	}
	
	/**
	 * Method that copies zip model from introduced
	 * location into the models folder and unzips it
	 * @param sourceFileName
	 * 	source zip file that is going to be copied into the models dir
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void installModelFromLocalFile(String sourceFileName) throws InterruptedException, IOException {
		String fileName = createFileName(sourceFileName);
		File destFile = new File(fileName);
		long fileSize = new File(sourceFileName).length();
		correctlyDownloaded = false;
		Thread parentThread = Thread.currentThread();
		Thread dwnldThread = new Thread(() -> {
			try {
				FileTools.copyFile(sourceFileName, fileName, parentThread);
				correctlyDownloaded = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		dwnldThread.start();
		
		while (dwnldThread.isAlive()) {
			Thread.sleep(100);
			double progress = Math.round(((double) destFile.length()) * 100 / fileSize);
			SwingUtilities.invokeLater(() -> {
				progressBar.setValue((int) progress);
				progressBar.setString("Copying progress: " + progress + "%");
			});
		}
		if (!correctlyDownloaded) {
			IJ.error("The model was not correctly copied to the 'models' directory, please try again.");
			return;
		}
		unzip(fileName);
	}
	
	private static String createFileName(String url) throws IOException {
		if (MODELS_DIR == null)
			getModelsFolder();
		// Add timestamp to the model name. 
		// The format consists on: modelName + date as ddmmyyyy + time as hhmmss
		String dateString = SDF.format(CALENDAR.getTime());
		String fileName;
		if (new File(url).exists())
			fileName = new File(url).getName();
		else
			fileName = FileDownloader.getFileNameFromURLString(url);
		if (fileName.endsWith(".zip")) fileName = fileName.substring(0, fileName.length() - 4);
		fileName = removeInvalidCharacters(fileName + "_" + dateString + ".zip");
		fileName = MODELS_DIR + File.separator + fileName;
		return fileName;
	}
	
	private void unzip(String fileName) throws InterruptedException, IOException {
		if (Thread.interrupted()) {
			return;
		}
		long size = ZipUtils.getUncompressedSize(new File(fileName));
		Thread parentThread = Thread.currentThread();
		String unzippedFileName = fileName.substring(0, fileName.lastIndexOf("."));
		correctlyDownloaded = false;
		Thread unzipThread = new Thread(() -> {
			try {
				FileTools.unzipFolder(new File(fileName), unzippedFileName, parentThread);
				correctlyDownloaded = true;
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});
		unzipThread.start();
		
		while (unzipThread.isAlive()) {
			Thread.sleep(100);
			double progress = Math.round(((double) FileTools.getFolderSize(unzippedFileName)) * 100 / size);
			SwingUtilities.invokeLater(() -> {
				progressBar.setValue((int) progress);
				progressBar.setString("Unzip progress: " + progress + "%");
			});
		}
		if (!correctlyDownloaded) {
			IJ.error("The model was not correctly unzipped in the 'models' directory, please try again.");
			return;
		}
	}
	
	private static String removeInvalidCharacters(String filename) {
		String[] listForbidden = new String[] {"\\", "|", "/", "<", ">", 
												":", "\"", "?", "*"};
		for (String invalid : listForbidden)
			filename = filename.replace(invalid, "_");
		return filename;
	}
	
	private static void getModelsFolder() throws IOException {
		String fijiFolder = getFijiFolder();
		File modelsFolder = new File(fijiFolder, "models");
		if (!modelsFolder.isDirectory() && !modelsFolder.mkdirs()) {
			throw new IOException("Unable to access the models folder in Fiji:" 
								+ System.lineSeparator() + " - " + modelsFolder.getAbsolutePath());
		}
		MODELS_DIR = modelsFolder.getAbsolutePath();
	}
	
	private static String getFijiFolder() {
		File jvmFolder = new File(System.getProperty("java.home"));
		String imageJExecutable;
		if (PlatformDetection.isWindows())
			imageJExecutable = "ImageJ-win64.exe";
		else if (PlatformDetection.isLinux())
			imageJExecutable = "ImageJ-linux64";
		else if (PlatformDetection.isMacOS())
			imageJExecutable = "Contents/MacOS/ImageJ-macosx";
		else
			throw new IllegalArgumentException("Unsupported Operating System");
		while (true && jvmFolder != null) {
			jvmFolder = jvmFolder.getParentFile();
			if (new File(jvmFolder + File.separator + imageJExecutable).isFile())
				return jvmFolder.getAbsolutePath();
		}
		return new File("").getAbsolutePath();
	}
    
    public static void main(String[] args) {
    	SwingUtilities.invokeLater(() -> new Install_From_URL_Or_Path());
    }
}

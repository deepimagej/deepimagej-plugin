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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import deepimagej.Constants;
import ij.IJ;
import ij.plugin.frame.PlugInFrame;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class Install_Local_Model extends PlugInFrame {
    private static final long serialVersionUID = 5648984831136983153L;
    // Components
    private JLabel labelModel;
    private JTextField pathTextField;
    private JButton browseImageButton;
    private JButton cancelButton;
    private JButton saveAsButton;
    
    private static final String INPUT_INFO = "<html>"
    		+ "Choose whether to run the model on the current in-focus image or on the image(s) at<br>"
    		+ "the specified path. If a path is provided, it can point to either a single image file<br>"
    		+ "or a folder containing multiple images.</html>";

    public Install_Local_Model() {
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

        pathTextField = new JTextField(20);
        pathTextField.setEnabled(false);
        browseImageButton = new JButton("Browse");
        browseImageButton.setEnabled(false); // Initially disabled
        browseImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int option = fileChooser.showOpenDialog(Install_Local_Model.this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedDirectory = fileChooser.getSelectedFile();
                    pathTextField.setText(selectedDirectory.getAbsolutePath());
                }
            }
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> this.dispose());
        saveAsButton = new JButton("Save As");
        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	// TODO
            }
        });

        // Adjust label alignment and spacing
        int gridy = 0;

        // Row 1: Question mark, Model label and combo box
        // Panel for path text field and browse button
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.add(pathTextField, BorderLayout.CENTER);
        pathPanel.add(browseImageButton, BorderLayout.EAST);
        addRowComponents(gridy++, INPUT_INFO, labelModel, pathPanel, true);
        

        // Row 6: Buttons aligned to the right
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveAsButton);

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
		fileName = removeInvalidCharacters("deepImageJModel" + "_" + dateString + ".zip");
		
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		progressScreen = new DownloadProgress(true);
		progressScreen.setThread(thread);
		stopped = false;
		thread.start();
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
			fileName = removeInvalidCharacters(new File(downloadURLs.get(0)).getName());
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
			progressScreen.setFileSize(originalModel.length());
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
    
    public static void main(String[] args) {
    	SwingUtilities.invokeLater(() -> new Install_Local_Model());
    }
}

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
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import deepimagej.Constants;
import ij.plugin.frame.PlugInFrame;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class Create_Macro extends PlugInFrame {
    private static final long serialVersionUID = 5648984831136983153L;
    // Components
    private JLabel labelModel;
    private JComboBox<String> modelComboBox;
    private JRadioButton currentImageRadioButton;
    private JRadioButton pathToImageFolderRadioButton;
    private JTextField pathTextField;
    private JButton browseImageButton;
    private JCheckBox displayOutputCheckBox;
    private JTextField periodTextField;
    private JCheckBox outputFolderCheckBox;
    private JTextField outputFolderTextField;
    private JButton browseOutputButton;
    private JTextArea codeTextArea;
    private JButton cancelButton;
    private JButton saveAsButton;

    private static final String NO_MODELS_STR = "No models installed";
    
    private static final String MACRO_FORMAT = "run(\"DeepImageJ Run\", \"modelPath=%s inputPath=%s outputFolder=% displayOutput=%\")"; 

    public Create_Macro() {
        super("deepImageJ " + Constants.DIJ_VERSION + " - Create Macro");
        initComponents();
        pack();
        setLocationRelativeTo(null); // Center the frame
        setVisible(true);
    }
    
    private void createModelComboBox() {
    	File modelsFolder = new File("models");
    	if (!modelsFolder.isDirectory()) {
    		modelComboBox = new JComboBox<>(new String[]{NO_MODELS_STR});
    		return;
    	}
		List<File> modelFiles = Arrays.stream(modelsFolder.listFiles())
				.filter(ff -> Arrays.asList(ff.list()).contains(io.bioimage.modelrunner.utils.Constants.RDF_FNAME))
				.collect(Collectors.toList());
		if (modelFiles.size() == 0) {
    		modelComboBox = new JComboBox<>(new String[]{NO_MODELS_STR});
    		return;
		}
		String[] arr = new String[modelFiles.size()];
		modelFiles.stream().map(f -> f.getName()).collect(Collectors.toList()).toArray(arr);
		modelComboBox = new JComboBox<>(arr);
		modelComboBox.setSelectedIndex(1);
    }

    private void initComponents() {
        // Set the layout manager for the content pane
        this.setLayout(new GridBagLayout());

        // Initialize components
        labelModel = new JLabel("Model:");
        createModelComboBox();
        modelComboBox.addActionListener(e -> updateCodeTextArea()); // Listener for model selection

        currentImageRadioButton = new JRadioButton("Current Image");
        pathToImageFolderRadioButton = new JRadioButton("Path to Image Folder");
        ButtonGroup imageOptionGroup = new ButtonGroup();
        imageOptionGroup.add(currentImageRadioButton);
        imageOptionGroup.add(pathToImageFolderRadioButton);

        pathTextField = new JTextField(20);
        pathTextField.setEnabled(false); // Initially disabled
        pathTextField.getDocument().addDocumentListener(new DocumentListener() { // Listener for path text field
            public void changedUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
            public void removeUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
            public void insertUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
        });
        browseImageButton = new JButton("Browse");
        browseImageButton.setEnabled(false); // Initially disabled

        displayOutputCheckBox = new JCheckBox("Display Output");
        displayOutputCheckBox.addActionListener(e -> {
            periodTextField.setEnabled(displayOutputCheckBox.isSelected());
            updateCodeTextArea();
        });
        periodTextField = new JTextField(5);
        periodTextField.setEnabled(false); // Initially disabled
        periodTextField.getDocument().addDocumentListener(new DocumentListener() { // Listener for period text field
            public void changedUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
            public void removeUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
            public void insertUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
        });

        outputFolderCheckBox = new JCheckBox("Output Folder");
        outputFolderCheckBox.addActionListener(e -> {
            boolean selected = outputFolderCheckBox.isSelected();
            outputFolderTextField.setEnabled(selected);
            browseOutputButton.setEnabled(selected);
            updateCodeTextArea();
        });
        outputFolderTextField = new JTextField(20);
        outputFolderTextField.setEnabled(false); // Initially disabled
        outputFolderTextField.getDocument().addDocumentListener(new DocumentListener() { // Listener for output folder text field
            public void changedUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
            public void removeUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
            public void insertUpdate(DocumentEvent e) {
                updateCodeTextArea();
            }
        });
        browseOutputButton = new JButton("Browse");
        browseOutputButton.setEnabled(false); // Initially disabled

        codeTextArea = new JTextArea(5, 30); // Make it taller
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(true);
        codeTextArea.setEditable(false);
        JScrollPane codeScrollPane = new JScrollPane(codeTextArea);

        cancelButton = new JButton("Cancel");
        saveAsButton = new JButton("Save As");

        // Action listeners to enable/disable text fields and buttons
        pathToImageFolderRadioButton.addActionListener(e -> {
            boolean selected = pathToImageFolderRadioButton.isSelected();
            pathTextField.setEnabled(selected);
            browseImageButton.setEnabled(selected);
            updateCodeTextArea();
        });
        currentImageRadioButton.addActionListener(e -> {
            boolean selected = currentImageRadioButton.isSelected();
            pathTextField.setEnabled(!selected);
            browseImageButton.setEnabled(!selected);
            updateCodeTextArea();
        });
        displayOutputCheckBox.addActionListener(e -> {
            periodTextField.setEnabled(displayOutputCheckBox.isSelected());
            updateCodeTextArea();
        });
        outputFolderCheckBox.addActionListener(e -> {
            boolean selected = outputFolderCheckBox.isSelected();
            outputFolderTextField.setEnabled(selected);
            browseOutputButton.setEnabled(selected);
            updateCodeTextArea();
        });

        // Listeners for radio buttons
        currentImageRadioButton.addActionListener(e -> updateCodeTextArea());
        pathToImageFolderRadioButton.addActionListener(e -> updateCodeTextArea());

        // Set default selections
        currentImageRadioButton.setSelected(true);

        // Adjust label alignment and spacing
        int gridy = 0;

        // Row 1: Question mark, Model label and combo box
        addRowComponents(gridy++, "Select the model you want to use.", labelModel, modelComboBox, true);

        // Row 2: Question mark, Radio buttons vertically
        JPanel radioPanel = new JPanel(new GridLayout(2, 1));
        radioPanel.add(currentImageRadioButton);
        radioPanel.add(pathToImageFolderRadioButton);

        // Panel for path text field and browse button
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.add(pathTextField, BorderLayout.CENTER);
        pathPanel.add(browseImageButton, BorderLayout.EAST);

        addRowComponents(gridy++, "Select the image source.", radioPanel, pathPanel, true);

        // Row 3: Question mark, Display output checkbox and period text field
        addRowComponents(gridy++, "Toggle the display of output images.", displayOutputCheckBox, periodTextField, false);

        // Row 4: Question mark, Output folder checkbox and text field with browse button
        JPanel outputFolderPanel = new JPanel(new BorderLayout(5, 0));
        outputFolderPanel.add(outputFolderTextField, BorderLayout.CENTER);
        outputFolderPanel.add(browseOutputButton, BorderLayout.EAST);

        addRowComponents(gridy++, "Specify an output folder for the results.", outputFolderCheckBox, outputFolderPanel, true);

        // Row 5: Code text area with scroll pane spanning the whole width
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        gbc.gridwidth = 4; // Span all columns
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        this.add(codeScrollPane, gbc);

        // Row 6: Buttons aligned to the right
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveAsButton);

        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        this.add(buttonPanel, gbc);

        // Initial update of the code text area
        updateCodeTextArea();
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

    private void updateCodeTextArea() {
        StringBuilder codeBuilder = new StringBuilder();

        // Get the selected model
        String selectedModel = (String) modelComboBox.getSelectedItem();
        if (NO_MODELS_STR.equals(selectedModel)) {
            codeBuilder.append("// No model selected");
            return;
        }
        
        String modelFolder = (String) this.modelComboBox.getSelectedItem();
        boolean applyOnCurrent = currentImageRadioButton.isSelected();
        boolean displayOutput = this.displayOutputCheckBox.isSelected();
        boolean saveOutput = this.outputFolderCheckBox.isSelected();

        String inputStr;
        String outputStr;
        String nDisplayedStr;

        if (applyOnCurrent)
        	inputStr = "null";
        else
        	inputStr = this.pathTextField.getText();
        if (!displayOutput)
        	nDisplayedStr = "" + 0;
        else
        	nDisplayedStr = this.periodTextField.getText();
        if (!saveOutput)
        	outputStr = "null";
    	else
    		outputStr = this.outputFolderTextField.getText();
        	

        codeTextArea.setText(String.format(MACRO_FORMAT, modelFolder, inputStr, nDisplayedStr, outputStr));
    }
    
    public static void main(String[] args) {
    	SwingUtilities.invokeLater(() -> new Create_Macro());
    }
}

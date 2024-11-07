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

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import deepimagej.Constants;

public class Create_Macro extends JFrame {
    private static final long serialVersionUID = 5648984831136983153L;
    // Components
    private JLabel labelModel;
    private JComboBox<String> modelComboBox;
    private JRadioButton currentImageRadioButton;
    private JRadioButton pathToImageFolderRadioButton;
    private JTextField pathTextField;
    private JCheckBox displayOutputCheckBox;
    private JTextField periodTextField;
    private JCheckBox outputFolderCheckBox;
    private JTextField outputFolderTextField;
    private JTextArea codeTextArea;
    private JButton cancelButton;
    private JButton saveAsButton;

    public Create_Macro() {
        setTitle("deepImageJ " + Constants.DIJ_VERSION + " - Create Macro");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Frame is closeable
        initComponents();
        pack();
        setLocationRelativeTo(null); // Center the frame
        setVisible(true);
    }

    private void initComponents() {
        // Set the layout manager for the content pane
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); // Padding

        // Initialize components
        labelModel = new JLabel("Model:");
        modelComboBox = new JComboBox<>(new String[]{"Model1", "Model2", "Model3"});
        addQuestionMark(labelModel, "Select the model you want to use.");

        currentImageRadioButton = new JRadioButton("Current Image");
        pathToImageFolderRadioButton = new JRadioButton("Path to Image Folder");
        ButtonGroup imageOptionGroup = new ButtonGroup();
        imageOptionGroup.add(currentImageRadioButton);
        imageOptionGroup.add(pathToImageFolderRadioButton);
        addQuestionMark(currentImageRadioButton, "Process the currently open image.");
        addQuestionMark(pathToImageFolderRadioButton, "Provide a path to a folder with images.");

        pathTextField = new JTextField(20);
        pathTextField.setEnabled(false); // Initially disabled

        displayOutputCheckBox = new JCheckBox("Display Output");
        periodTextField = new JTextField(5);
        periodTextField.setEnabled(false); // Initially disabled
        addQuestionMark(displayOutputCheckBox, "Toggle the display of output images.");

        outputFolderCheckBox = new JCheckBox("Output Folder");
        outputFolderTextField = new JTextField(20);
        outputFolderTextField.setEnabled(false); // Initially disabled
        addQuestionMark(outputFolderCheckBox, "Specify an output folder for the results.");

        codeTextArea = new JTextArea(5, 30); // Make it taller
        codeTextArea.setLineWrap(true);
        codeTextArea.setWrapStyleWord(true);
        JScrollPane codeScrollPane = new JScrollPane(codeTextArea);

        cancelButton = new JButton("Cancel");
        saveAsButton = new JButton("Save As");

        // Action listeners to enable/disable text fields
        pathToImageFolderRadioButton.addActionListener(e -> pathTextField.setEnabled(pathToImageFolderRadioButton.isSelected()));
        currentImageRadioButton.addActionListener(e -> pathTextField.setEnabled(pathToImageFolderRadioButton.isSelected()));
        displayOutputCheckBox.addActionListener(e -> periodTextField.setEnabled(displayOutputCheckBox.isSelected()));
        outputFolderCheckBox.addActionListener(e -> outputFolderTextField.setEnabled(outputFolderCheckBox.isSelected()));

        // Set default selections
        currentImageRadioButton.setSelected(true);

        // Row 1: Model label and combo box
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        getContentPane().add(labelModel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(modelComboBox, gbc);

        // Row 2: Radio buttons and path text field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        getContentPane().add(currentImageRadioButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        getContentPane().add(pathToImageFolderRadioButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(pathTextField, gbc);

        // Row 3: Display output checkbox and period text field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        getContentPane().add(displayOutputCheckBox, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(periodTextField, gbc);

        // Row 4: Output folder checkbox and text field
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        getContentPane().add(outputFolderCheckBox, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(outputFolderTextField, gbc);

        // Row 5: Code text area with scroll pane spanning the whole width
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        getContentPane().add(codeScrollPane, gbc);

        // Row 6: Buttons aligned to the right
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveAsButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        getContentPane().add(buttonPanel, gbc);
    }

    // Method to add a question mark label with tooltip next to a component
    private void addQuestionMark(JComponent component, String tooltipText) {
        JLabel questionMark = new JLabel("?");
        questionMark.setToolTipText(tooltipText);
        questionMark.setForeground(java.awt.Color.BLUE);

        // Add the question mark label next to the component in the layout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,0,5,5);
        gbc.anchor = GridBagConstraints.WEST;

        // Get the layout manager of the content pane
        GridBagLayout layout = (GridBagLayout) getContentPane().getLayout();

        // Get the constraints of the original component
        GridBagConstraints origGbc = layout.getConstraints(component);

        // Set the position next to the original component
        gbc.gridx = origGbc.gridx + 1;
        gbc.gridy = origGbc.gridy;

        // Add the question mark label to the content pane
        getContentPane().add(questionMark, gbc);
    }

    // Main method to run the GUI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Create_Macro::new);
    }
}

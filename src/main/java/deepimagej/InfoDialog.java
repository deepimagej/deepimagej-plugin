package deepimagej;

import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import deepimagej.components.BorderLabel;
import deepimagej.components.HTMLPane;
import deepimagej.tools.NumFormat;
import ij.gui.GUI;

public class InfoDialog extends JDialog implements ActionListener {

    public JButton          closeButton = new JButton("Close");

    public InfoDialog(){
        super(new JFrame(), "Information");
    }

    public void BuildScreen(String msg) {

        HTMLPane information= new HTMLPane(Constants.width * 2 ,110);
        if (msg == "Models"){
            information.append("i","This option selects the models already installed in your computer. If you want to run another model please install it beforehand using DeepImageJ Install Model");
        }
        if (msg == "Postprocessing"){
            information.append("i","This option help you select between the different postprocessing options that are offered in the yaml");
        }
        if (msg == "Format"){
            information.append("i","Select between tensorflow and pytorch format, only the available formats are displayed");
        }
        if (msg == "Preprocessing"){
            information.append("i","This option help you select between the different preprocessing options that are offered in the yaml");
        }


        JPanel pnButtons = new JPanel(new GridLayout(1,1));
        pnButtons.add(closeButton);

        JPanel all = new JPanel(new BorderLayout());
        all.add(information, BorderLayout.NORTH);
        all.add(pnButtons, BorderLayout.SOUTH);
        add(all);

        closeButton.addActionListener(this);

        pack();
        setModal(true);
        setMinimumSize(new Dimension(Constants.width * 2, 110));
        GUI.center(this);
        setVisible(true);
    }

    @Override
    public void actionPerformed (ActionEvent e) {
        if (e.getSource() == closeButton) {
            dispose();
        }
    }

}

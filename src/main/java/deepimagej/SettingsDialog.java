package deepimagej;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import deepimagej.components.BorderLabel;
import deepimagej.components.HTMLPane;
import deepimagej.components.GridPanel;
import deepimagej.tools.NumFormat;
import ij.gui.GUI;

public class SettingsDialog extends JDialog implements ActionListener, ItemListener{
    private JButton             closeButton = new JButton("Close");
    private JButton             applyButton = new JButton("Apply and Close");
	private JComboBox<String> x= new JComboBox<String>();
	private JComboBox<String> y = new JComboBox<String>();
	private JComboBox<String> z = new JComboBox<String>();
	private JComboBox<String> c = new JComboBox<String>();
	private JComboBox<String> b = new JComboBox<String>();


    public SettingsDialog(){
        super(new JFrame(), "Axes Information");
    }

    public void buildScreen(){
        JPanel pnButtons = new JPanel(new GridLayout(1, 2));
		pnButtons.add(closeButton);
		pnButtons.add(applyButton);

		
		x.addItem("X");
		x.addItem("Y");
		x.addItem("Z");
		x.addItem("C");
		x.addItem("B");
		x.setSelectedItem("X");
		
		y.addItem("X");
		y.addItem("Y");
		y.addItem("Z");
		y.addItem("C");
		y.addItem("B");
		y.setSelectedItem("Y");
		
		z.addItem("X");
		z.addItem("Y");
		z.addItem("Z");
		z.addItem("C");
		z.addItem("B");
		z.setSelectedItem("Z");
		
		c.addItem("X");
		c.addItem("Y");
		c.addItem("Z");
		c.addItem("C");
		c.addItem("B");
		c.setSelectedItem("C");
		
		b.addItem("X");
		b.addItem("Y");
		b.addItem("Z");
		b.addItem("C");
		b.addItem("B");
		b.setSelectedItem("B");
		
		x.addItemListener(this);
        y.addItemListener(this);
        z.addItemListener(this);
        c.addItemListener(this);
        b.addItemListener(this);

        GridPanel params = new GridPanel(false, 1);
        params.place(1,1,1,1, new JLabel("X = "));
        params.place(1,2,1,1, x);
        params.place(2,1,1,1, new JLabel("Y = "));
        params.place(2,2,1,1, y);
        params.place(3,1,1,1, new JLabel("Z = "));
        params.place(3,2,1,1, z);
        params.place(4,1,1,1, new JLabel("C = "));
        params.place(4,2,1,1, c);
        params.place(5,1,1,1, new JLabel("B = "));
        params.place(5,2,1,1, b);

        GridPanel paramsBorder = new GridPanel(false, 8);
        paramsBorder.place(0,0, params);

        HTMLPane warning = new HTMLPane(Constants.width, 60);
        warning.append("i", 
        "<small>Note: coordinates are chosen automatically by the Plugin.\n"
        + "Don't perform any change if the model is running correctly\n"
        + "or if you don't know the technical specifications of the original image.</small>");
        
        JPanel top = new JPanel(new BorderLayout());
        top.add(paramsBorder);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(warning, BorderLayout.CENTER);
        bottom.add(pnButtons, BorderLayout.SOUTH);

        JPanel all = new JPanel(new BorderLayout());
        all.add(top, BorderLayout.NORTH);
        all.add(bottom, BorderLayout.SOUTH);
        add(all);

        closeButton.addActionListener(this);
        applyButton.addActionListener(this);

        setResizable(true);

        pack();
        setModal(true);
        setMinimumSize(new Dimension(Constants.width, 300));
        GUI.center(this);
        setVisible(true);
        
        
    }

    @Override
    public void itemStateChanged(ItemEvent e){
    	if((x.getSelectedItem()==y.getSelectedItem())||(x.getSelectedItem()==z.getSelectedItem())||(x.getSelectedItem()==c.getSelectedItem())||(x.getSelectedItem()==b.getSelectedItem())||(y.getSelectedItem()==z.getSelectedItem())||(y.getSelectedItem()==c.getSelectedItem())||(y.getSelectedItem()==b.getSelectedItem())||(z.getSelectedItem()==c.getSelectedItem())||(z.getSelectedItem()==b.getSelectedItem())||(c.getSelectedItem()==b.getSelectedItem())) {
    		applyButton.setEnabled(false);
    	}else {
    		applyButton.setEnabled(true);
    	}
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == closeButton) {
            dispose();
        }
        if (e.getSource() == applyButton) {
        	dispose(); //Change by the specific behaviour
        }
    }
}

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
 * E. Gómez-de-Mariscal, C. García-López-de-Haro, L. Donati, M. Unser, A. Muñoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique fédérale de Lausanne (EPFL), Switzerland
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
package additionaluserinterface;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Vector;

/**
 * This class allows to store and load key-associated values in a text file.
 * The class has methods to load and store single value linked to a string
 * key describing the value. Futhermore, this class has methods to record
 * a GUI component to a specified key. By this way this class allows to
 * load and store all recorded items.
 * 
 * @author Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland.
 *
 */  
public class Settings {

	private String filename;
	private String project;
	private Vector items;
	private Properties props;
	
	/**
	* Constructors a Settings abject for a given project name and a given filename.
	*
	* @param project	a string describing the project
	* @param filename	a string give the full name of the file, including the path
	*/
	public Settings(String project, String filename) {
		this.filename = filename;
		this.project = project;
		items = new Vector();
		props = new Properties();
	}
	
	/**
	* Records a JTextField component to store/load automatically.
	*
	* @param key			a string describing the value
	* @param component		the component to record
	* @param defaultValue	the default value
	*/
	public void record(String key, JTextField component, String defaultValue) {
		Item item = new Item(key, component, defaultValue);
		items.add(item);
	}

	/**
	 * Records a JComboBox component to store/load automatically.
	 *
	 * @param key			a string describing the value
	 * @param component		the component to record
	 * @param defaultValue	the default value
	 */
	public void record(String key, JComboBox component, String defaultValue) {
		Item item = new Item(key, component, defaultValue);
		items.add(item);
	}

	/**
 	* Records a JSpinner component to store/load automatically.
	 *
	 * @param key			a string describing the value
	 * @param component		the component to record
	 * @param defaultValue	the default value
	 */
	public void record(String key, JSpinner component, String defaultValue) {
		Item item = new Item(key, component, defaultValue);
		items.add(item);
	}
	
	/**
 	 * Records a JToggleButton component to store/load automatically.
	 *
	 * @param key			a string describing the value
	 * @param component		the component to record
	 * @param defaultValue	the default value
	 */
	public void record(String key, JToggleButton component, boolean defaultValue) {
		Item item = new Item(key, component, (defaultValue ? "on" : "off"));
		items.add(item);
	}

	/**
	 * Records a JCheckBox component to store/load automatically.
	 *
	 * @param key			a string describing the value
	 * @param component		the component to record
	 * @param defaultValue	the default value
	 */
	public void record(String key, JCheckBox component, boolean defaultValue) {
		Item item = new Item(key, component, (defaultValue ? "on" : "off"));
		items.add(item);
	}

	/**
	 * Records a JSlider component to store/load automatically.
	 *
	 * @param key			a int value
	 * @param component		the component to record
	 * @param defaultValue	the default value
	 */
	public void record(String key, JSlider component, String defaultValue) {
		Item item = new Item(key, component, defaultValue);
		items.add(item);
	}
	
	/**
 	 * Load an individual double value given a specified key
	 *
	 * @param key			a string describing the value
	 * @param defaultValue	the default value
	 * @return the value get from the file
	 */
	public String loadValue(String key, String defaultValue) {
		String s = "";
		try {
			FileInputStream in = new FileInputStream(filename);
			props.load(in);
			s = props.getProperty(key, "" + defaultValue);
		}
		catch(Exception e) {
			s = defaultValue;
		}
		return s;
	}

	/**
	 * Load an individual double value given a specified key
	 *
	 * @param key			a string describing the value
	 * @param defaultValue	the default value
	 * @return the value get from the file
	 */
	public double loadValue(String key, double defaultValue) {
		double d = 0;
		try {
			FileInputStream in = new FileInputStream(filename);
			props.load(in);
			String value = props.getProperty(key, "" + defaultValue);
			d = (new Double(value)).doubleValue();
		}
		catch(Exception e) {
			d = defaultValue;
		}
		return d;
	}

	/**
	 * Load an individual integer value given a specified key
	 *
	 * @param key			a string describing the value
	 * @param defaultValue	the default value
	 * @return the value get from the file
	 */
	public int loadValue(String key, int defaultValue) {
		int i = 0;
		try {
			FileInputStream in = new FileInputStream(filename);
			props.load(in);
			String value = props.getProperty(key, "" + defaultValue);
			i = (new Integer(value)).intValue();
		}
		catch(Exception e) {
			i = defaultValue;
		}
		return i;
	}

	/**
	 * Load an individual boolean value given a specified key
	 *
	 * @param key			a string describing the value
	 * @param defaultValue	the default value
	 * @return the value get from the file
	 */
	public boolean loadValue(String key, boolean defaultValue) {
		boolean b = false;
		try {
			FileInputStream in = new FileInputStream(filename);
			props.load(in);
			String value = props.getProperty(key, "" + defaultValue);
			b = (new Boolean(value)).booleanValue();
		}
		catch(Exception e) {
			b = defaultValue;
		}
		return b;
	}
	
	/**
 	 * Store an individual double value given a specified key
	 *
	 * @param key		a string describing the value
	 * @param value		the value to store
	 */
	public void storeValue(String key, String value) {
		props.setProperty(key, value);
		try {
			FileOutputStream out = new FileOutputStream(filename);
			props.store(out, project);
		}
		catch(Exception e) {
			new Msg(project, "Impossible to store settings in (" + filename + ")");
		}
	}

	/**
	 * Store an individual double value given a specified key
	 *
	 * @param key		a string describing the value
	 * @param value		the value to store
	 */
	public void storeValue(String key, double value) {
		props.setProperty(key, ""+value);
		try {
			FileOutputStream out = new FileOutputStream(filename);
			props.store(out, project);
		}
		catch(Exception e) {
			new Msg(project, "Impossible to store settings in (" + filename + ")");
		}
	}

	/**
	 * Store an individual integer value given a specified key
	 *
	 * @param key			a string describing the value
	 * @param value		the value to store
	 */
	public void storeValue(String key, int value) {
		props.setProperty(key, ""+value);
		try {
			FileOutputStream out = new FileOutputStream(filename);
			props.store(out, project);
		}
		catch(Exception e) {
			new Msg(project, "Impossible to store settings in (" + filename + ")");
		}
	}

	/**
	 * Store an individual boolean value given a specified key
	 *
	 * @param key			a string describing the value
	 * @param value		the value to store
	 */
	public void storeValue(String key, boolean value) {
		props.setProperty(key, ""+value);
		try {
			FileOutputStream out = new FileOutputStream(filename);
			props.store(out, project);
		}
		catch(Exception e) {
			new Msg(project, "Impossible to store settings in (" + filename + ")");
		}
	}
	
	/**
	 * Load all recorded values.
	 */
	public void loadRecordedItems() {
		loadRecordedItems(filename);
	}
	
	/**
	 * Load all recorded values from a specified filename.
	 */
	public void loadRecordedItems(String fname) {
		try {
			FileInputStream in = new FileInputStream(fname);
			props.load(in);
		}
		catch(Exception e) {
			new Msg(project, "Loading default value. No settings file (" + fname + ")");
		}
		
		for(int i=0; i<items.size(); i++) {
			Item item = (Item)items.get(i);
			String value = props.getProperty(item.key, item.defaultValue);
			if (item.component instanceof JTextField) {
				((JTextField)item.component).setText(value);
			}
			else if (item.component instanceof JComboBox) {
				((JComboBox)item.component).setSelectedItem(value);
			}
			else if (item.component instanceof JCheckBox) {
				((JCheckBox)item.component).setSelected(value.equals("on") ? true : false);
			}
			else if (item.component instanceof JToggleButton) {
				((JToggleButton)item.component).setSelected(value.equals("on") ? true : false);
			}
			else if (item.component instanceof JSpinner) {
				((JSpinner)item.component).setValue((new Double(value)).doubleValue());
			}
			else if (item.component instanceof JSlider) {
				((JSlider)item.component).setValue((new Integer(value)).intValue());
			}
		}
	}

	/**
  	 * Store all recorded values.
	 */
	public void storeRecordedItems() {
		storeRecordedItems(filename);
	}

	/**
  	 * Store all recorded values into a specified filename
	 */
	public void storeRecordedItems(String fname) {
	
		for(int i=0; i<items.size(); i++) {
			Item item = (Item)items.get(i);
			if (item.component instanceof JTextField) {
				String value = ((JTextField)item.component).getText();
				props.setProperty(item.key, value);
			}
			else if (item.component instanceof JComboBox) {
				String value = (String)((JComboBox)item.component).getSelectedItem();
				props.setProperty(item.key, value);
			}
			else if (item.component instanceof JCheckBox) {
				String value = (((JCheckBox)item.component).isSelected() ? "on" : "off");
				props.setProperty(item.key, value);
			}
			else if (item.component instanceof JToggleButton) {
				String value = (((JToggleButton)item.component).isSelected() ? "on" : "off");
				props.setProperty(item.key, value);
			}
			else if (item.component instanceof JSpinner) {
				String value = ""+((JSpinner)item.component).getValue();
				props.setProperty(item.key, value);
			}
			else if (item.component instanceof JSlider) {
				String value = ""+((JSlider)item.component).getValue();
				props.setProperty(item.key, value);
			}
		}
	
		try {
			FileOutputStream out = new FileOutputStream(fname);
			props.store(out, project);
		}
		catch(Exception e) {
			new Msg(project, "Impossible to store settings in (" + fname + ")");

		}
	}
	
	/**
	 * Private class to store one component and its key.
	 */
	private class Item {
		public Object component;
		public String defaultValue;
		public String key;
		
		public Item(String key, Object component, String defaultValue) {
			this.component = component;
			this.defaultValue = defaultValue;
			this.key = key;
		}
	}
	
	/**
	 * Private class to display an alert message when the file is not found.
	 */
	private class Msg extends JFrame {
		
		public Msg(String project, String msg) {
			super(project);
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints constraints = new GridBagConstraints();
			Container contentPane = getContentPane();
			contentPane.setLayout(layout);
			constraints.weightx = 0.0;
			constraints.weighty = 1.0;
			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.gridwidth = 1;
			constraints.gridheight = 1;
			constraints.insets = new Insets(10, 10, 10, 10);
			constraints.anchor = GridBagConstraints.CENTER;
			JLabel newLabel = new JLabel(msg);
			layout.setConstraints(newLabel,constraints);
			contentPane.add(newLabel);            	
			setResizable(false);
			pack();
			setVisible(true);
			Dimension dim = getToolkit().getScreenSize();
			Rectangle abounds = getBounds();
			setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
			Timer timer = new Timer(1000, new DelayListener(this));
			timer.start();
		}
	}
	
	/**
	 * Private class to dispose the message after 1 second.
	 */
	private class DelayListener implements ActionListener {
		private Msg msg;
		public DelayListener(Msg msg) {
			this.msg = msg;
		}
		public void actionPerformed(ActionEvent evt) {
			msg.dispose();	
		}    
	}
	
	
}

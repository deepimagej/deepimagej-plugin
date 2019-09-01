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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.util.*;

/**
 * This class extends the JToolbar of Java to create a status bar
 * including some of the following component ProgressBar, Help Button
 * About Button and Close Button
 * 
 * @author Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland.
 *
 */  
public class WalkBar extends JToolBar implements ActionListener {

	private JProgressBar progress = new JProgressBar();
	private JButton bnHelp  = new JButton("Help");
	private JButton bnAbout = new JButton("About");
	private JButton bnClose = new JButton("Close");
	private String about[] = {"About", "Version", "Description", "Author", "Biomedical Image Group", "2008", "http://bigwww.epfl.ch"};
	private String help;
	private double chrono;
	private int xSizeAbout = 400;
	private int ySizeAbout = 400;
	private int xSizeHelp = 400;
	private int ySizeHelp = 400;
	
	/**
	* Constructor.
	*/
	public WalkBar(String initialMessage, boolean isAbout, boolean isHelp, boolean isClose) {
		super("Walk Bar");
		build(initialMessage, isAbout, isHelp, isClose, 100);
	}
	
	public WalkBar(String initialMessage, boolean isAbout, boolean isHelp, boolean isClose, int size) {
		super("Walk Bar");
		build(initialMessage, isAbout, isHelp, isClose, size);
			
	}
	
	private void build(String initialMessage, boolean isAbout, boolean isHelp, boolean isClose, int size) {
		if (isAbout)
			add(bnAbout);
		if (isHelp)
			add(bnHelp);
		addSeparator();
		add(progress);
		addSeparator();
		if (isClose)
			add(bnClose);
		
		progress.setStringPainted(true);
		progress.setString(initialMessage);
		progress.setFont(new Font("Arial", Font.PLAIN, 10));
		progress.setMinimum(0);
		progress.setMaximum(100);
		progress.setPreferredSize(new Dimension(size, 20));
		bnAbout.addActionListener(this);
		bnHelp.addActionListener(this);
		
		setFloatable(false);
		setRollover(true);
		setBorderPainted(false);
		chrono = System.currentTimeMillis();
	}
	
	
	/**
	 * Implements the actionPerformed for the ActionListener.
	 */
	public synchronized  void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
    	if (e.getSource() == bnHelp) {
			showHelp();
    	}
		else if (e.getSource() == bnAbout) {
			showAbout();
    	}
		else if (e.getSource() == bnClose) {
		
		}
	}

	/**
	* Return a reference to the Close button.
	*/
	public JButton getButtonClose() {
		return bnClose;
	}
	
	/**
	* Set a value in the progress bar.
	*/
	public void setValue(int value) {
		progress.setValue(value);
	}

	/**
	* Set a message in the progress bar.
	*/
	public void setMessage(String msg) {
		progress.setString(msg);
	}

	/**
	* Set a value and a message in the progress bar.
	*/
	public void progress(String msg, int value) {
		progress.setValue(value);
		double elapsedTime = System.currentTimeMillis() - chrono;
		String t = " [" + (elapsedTime > 3000 ?  Math.round(elapsedTime/10)/100.0 + "s." : elapsedTime + "ms") + "]";
		progress.setString(msg + t);
	}
	
	/**
	* Set a value and a message in the progress bar.
	*/
	public void progress(String msg, double value) {
		progress(msg, (int)Math.round(value));
	}
	
	/**
	* Set to 0 the progress bar.
	*/
	public void reset() {
		chrono = System.currentTimeMillis();
		progress.setValue(0);
		progress.setString("Starting ...");
	}

	/**
	* Set to 100 the progress bar.
	*/
	public void finish() {
		progress("Terminated", 100);
	}

	/**
	* Set to 100 the progress bar with an additional message.
	*/
	public void finish(String msg) {
		progress(msg, 100);
	}
	
	/**
	* Specify the content of the About window.
	*/
	public void fillAbout(String name, String version, String description, String author, String organisation, String date, String info) {
		this.about[0] = name;
		this.about[1] = version;
		this.about[2] = description;
		this.about[3] = author;
		this.about[4] = organisation;
		this.about[5] = date;
		this.about[6] = info;
	}

	/**
	* Specify the content of the Help window.
	*/
	public void fillHelp(String help) {
		this.help = help;
	}
	
	/**
	 * Show the content of the About window.
	 */
	public void showAbout() {
		
		final JFrame frame = new JFrame("About "+ about[0]);
		JEditorPane pane = new JEditorPane();
		pane.setEditable(false);
		pane.setContentType("text/html; charset=ISO-8859-1");
		pane.setText("<html><head><title>" + about[0] + "</title>" + getStyle() + "</head><body>" +
					 (about[0] == "" ? "" : "<p class=\"name\">" + about[0] + "</p>") +		// Name
					 (about[1] == "" ? "" : "<p class=\"vers\">" + about[1] + "</p>") +		// Version
					 (about[2] == "" ? "" : "<p class=\"desc\">" + about[2] + "</p><hr>") +		// Description
					 (about[3] == "" ? "" : "<p class=\"auth\">" + about[3] + "</p>") +		//author
					 (about[4] == "" ? "" : "<p class=\"orga\">" + about[4] + "</p>") + 
					 (about[5] == "" ? "" : "<p class=\"date\">" + about[5] + "</p>") +
					 (about[6] == "" ? "" : "<p class=\"more\">" + about[6] + "</p>") +
					 "</html>"	
					 );	
		
		final JButton bnClose = new JButton("Close");
		bnClose.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.dispose();
			}
		});
		pane.setCaret(new DefaultCaret());
		JScrollPane scrollPane = new JScrollPane(pane);
		//helpScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setPreferredSize(new Dimension(xSizeAbout, ySizeAbout));
		frame.getContentPane().add(scrollPane, BorderLayout.NORTH);
		frame.getContentPane().add(bnClose, BorderLayout.CENTER);
		
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);
		center(frame);
	}
	
	/**
	* Show the content of the Help window of a given size.
	*/
	public void showHelp() {
		final JFrame frame = new JFrame("Help "+ about[0]);
		JEditorPane pane = new JEditorPane();
		pane.setEditable(false);
		pane.setContentType("text/html; charset=ISO-8859-1");
		pane.setText("<html><head><title>" + about[0] + "</title>" + getStyle() + "</head><body>" +
					 (about[0] == "" ? "" : "<p class=\"name\">" + about[0] + "</p>") +		// Name
					 (about[1] == "" ? "" : "<p class=\"vers\">" + about[1] + "</p>") +		// Version
					 (about[2] == "" ? "" : "<p class=\"desc\">" + about[2] + "</p>") +		// Description
					 "<hr><p class=\"help\">" + help + "</p>" +  
					 "</html>"	
		 );	
		final JButton bnClose = new JButton("Close");
		bnClose.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			frame.dispose();
		}
		});
		pane.setCaret(new DefaultCaret());
		JScrollPane scrollPane = new JScrollPane(pane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setPreferredSize(new Dimension(xSizeHelp, ySizeHelp));
		frame.setPreferredSize(new Dimension(xSizeHelp, ySizeHelp));
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		frame.getContentPane().add(bnClose, BorderLayout.SOUTH);
		frame.setVisible(true);
		frame.pack();
		center(frame);
	}
	
	/*
	* Place the window in the center of the screen.
	*/
	private void center(Window w) {
		Dimension screenSize = new Dimension(0, 0);
		boolean isWin = System.getProperty("os.name").startsWith("Windows");
		if (isWin) { // GraphicsEnvironment.getConfigurations is *very* slow on Windows
			screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		}
		if (GraphicsEnvironment.isHeadless())
			screenSize = new Dimension(0, 0);
		else {
			// Can't use Toolkit.getScreenSize() on Linux because it returns 
			// size of all displays rather than just the primary display.
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gd = ge.getScreenDevices();
			GraphicsConfiguration[] gc = gd[0].getConfigurations();
			Rectangle bounds = gc[0].getBounds();
			if (bounds.x==0&&bounds.y==0)
				screenSize = new Dimension(bounds.width, bounds.height);
			else
				screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		}
		Dimension window = w.getSize();
		if (window.width==0)
			return;
		int left = screenSize.width/2-window.width/2;
		int top = (screenSize.height-window.height)/4;
		if (top<0) top = 0;
		w.setLocation(left, top);
	}
	
	/*
	* Defines the CSS style for the help and about window.
	*/
	private String getStyle() {
		return
		"<style type=text/css>" +
		"body {backgroud-color:#222277}" + 
		"hr {width:80% color:#333366; padding-top:7px }" +
		"p, li {margin-left:10px;margin-right:10px; color:#000000; font-size:1em; font-family:Verdana,Helvetica,Arial,Geneva,Swiss,SunSans-Regular,sans-serif}" +
		"p.name {color:#ffffff; font-size:1.2em; font-weight: bold; background-color: #333366; text-align:center;}" +
		"p.vers {color:#333333; text-align:center;}" +
		"p.desc {color:#333333; font-weight: bold; text-align:center;}" +
		"p.auth {color:#333333; font-style: italic; text-align:center;}" +
		"p.orga {color:#333333; text-align:center;}" +
		"p.date {color:#333333; text-align:center;}" +
		"p.more {color:#333333; text-align:center;}" +
		"p.help {color:#000000; text-align:left;}" +
		"</style>";
	}
}



    

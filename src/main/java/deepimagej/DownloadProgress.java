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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import deepimagej.components.BorderLabel;
import deepimagej.tools.NumFormat;
import ij.gui.GUI;

public class DownloadProgress extends JDialog implements ActionListener {

	private Timer				timer		= new Timer(true);
	private JButton				bnStop		= new JButton("Stop");
	private BorderLabel			downloadedSize	= new BorderLabel("Portion downloaded");
	private BorderLabel			time		= new BorderLabel("Elapsed time");
	private BorderLabel			name		= new BorderLabel("Elapsed time");
	private BorderLabel			modelSize	= new BorderLabel("Model size");
	private double				chrono;
	private Clock				clock;
	private GridBagLayout		layout		= new GridBagLayout();
	private GridBagConstraints	constraint	= new GridBagConstraints();
	private boolean stop = false;
	private String fileName = "";
	private String modelName = "";
	private long totalFileSize = 1;
	private Thread thread = null;
	private double modelSizeMb = 1;
	
	public DownloadProgress() {
		super(new JFrame(), "Downloading model");
	}
	
	public void buildScreen() {
	
		JPanel prog = new JPanel(layout);
		place(prog, 0, 1, 0, name);
		place(prog, 1, 1, 0, time);
		place(prog, 2, 1, 0, downloadedSize);
		place(prog, 3, 1, 0, modelSize);
		place(prog, 4, 1, 0, bnStop);
		info();
		JPanel panel = new JPanel(layout);
		place(panel, 0, 0, 10, prog);
		
		add(panel);
		setResizable(false);
		pack();
		GUI.center(this);

		bnStop.addActionListener(this);
		clock = new Clock();
		chrono = System.nanoTime();
		timer.scheduleAtFixedRate(clock, 0, 300);
		stop = false;
	}
	
	public void place(JPanel panel, int row, int col, int space, JComponent comp) {
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = 1;
		constraint.gridheight = 1;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		panel.add(comp);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		stop();
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void setmodelName(String modelName) {
		this.modelName = modelName;
	}
	
	public void setFileSize(long totalFileSize) {
		this.totalFileSize = totalFileSize;
		this.modelSizeMb = totalFileSize / (1024.0 * 1024);
		if (this.modelSizeMb > 1000) {
			this.modelSizeMb /= 1024.0; 
			String txt = "" + this.modelSizeMb;
			if (txt.lastIndexOf(".") != -1 && txt.length() - txt.lastIndexOf(".") >= 2)
				txt = txt.substring(0, txt.lastIndexOf(".") + 2);
			modelSize.setText("Model size: " + txt + " GB");
		} else {
			String txt = "" + this.modelSizeMb;
			if (txt.lastIndexOf(".") != -1 && txt.length() - txt.lastIndexOf(".") >= 2)
				txt = txt.substring(0, txt.lastIndexOf(".") + 2);
			modelSize.setText("Model size: " + txt + " GB");
		}
			
	}
	
	public void setThread(Thread thread) {
		this.thread = thread;
	}

	public boolean isStopped() {
		return stop;
	}
	
	public void stop() {
		if (timer == null)
			return;
		if (clock == null)
			return;
		clock.cancel();
		timer.cancel();
		timer.purge();
		timer = null;
		stop = true;
		dispose();
		thread.interrupt();
	}
 
	public void info() {
		name.setText(modelName);
		time.setText("Runtime: " + NumFormat.seconds((System.nanoTime() - chrono)));
		String progress = "" + 0;
		long currentFileSize = new File(fileName).length();
		progress = Math.round(currentFileSize * 100 / totalFileSize) + "";

		if (currentFileSize < totalFileSize)
			downloadedSize.setText("Download progress: " + progress + "%");
		else
			downloadedSize.setText("Unzipping model");
	}
	
	public class Clock extends TimerTask {
		public void run() {
			info();
		}
	}

}

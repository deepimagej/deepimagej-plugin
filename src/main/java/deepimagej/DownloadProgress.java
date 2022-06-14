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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
	private String progressString = "Download progress: ";
	
	public DownloadProgress(boolean downloading) {
		super(new JFrame(), "");
		String header = "Downloading model";
		if (!downloading)
			header = "Copying model: ";
		this.setTitle(header);
		
		if (!downloading)
			progressString = "Copying progress: ";
		
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
			modelSize.setText("Model size: " + txt + " MB");
		}
			
	}
	
	/**
	 * Get the size of the wanted file, being either a file or directory 
	 * @return the size in bytes
	 */
	public long getFileOrDirSize() {
		File ff = new File(fileName);
		if (ff.isDirectory()) {
		      try (Stream<Path> walk = Files.walk(ff.toPath())) {
		          return walk.filter(Files::isRegularFile)
		                  .mapToLong(DownloadProgress::getSizeFromPath).sum();
		      } catch (IOException e) {
		          System.out.printf("IO errors %s", e);
		          return 0;
		      }
		} else if (ff.isFile()) {
			return ff.length();
		} else {
			return 0;
		}
	}
	
	/**
	 * Get the size of a file specifying the path
	 * @param p
	 * 	path of a file
	 * @return the size, if there is any error, returns 0
	 */
	private static long getSizeFromPath(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            System.out.printf("Failed to get size of %s%n%s", p, e);
            return 0L;
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
		long currentFileSize = getFileOrDirSize();
		progress = Math.round(currentFileSize * 100 / totalFileSize) + "";

		if (currentFileSize < totalFileSize)
			downloadedSize.setText(progressString + progress + "%");
		else
			downloadedSize.setText("Unzipping model");
	}
	
	public class Clock extends TimerTask {
		public void run() {
			info();
		}
	}

}

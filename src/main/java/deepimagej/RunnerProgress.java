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
import deepimagej.tools.SystemUsage;
import ij.gui.GUI;

public class RunnerProgress extends JDialog implements ActionListener {

	private BorderLabel			title		= new BorderLabel("Name ........");
	private BorderLabel			patches		= new BorderLabel("Patch not set");
	private BorderLabel			memory		= new BorderLabel("Memory........");
	private BorderLabel			peak		= new BorderLabel("Memory........");
	private BorderLabel			processor	= new BorderLabel("Model Inference (GPU: NO)");
	private Timer				timer		= new Timer(true);
	private JButton				bnStop		= new JButton("Stop");
	private BorderLabel			time		= new BorderLabel("Elapsed time");
	private double				chrono;
	private double				peakmem 	= 0;
	private Clock				clock;
	private GridBagLayout		layout		= new GridBagLayout();
	private GridBagConstraints	constraint	= new GridBagConstraints();
	private Object runner;
	private boolean stop = false;
	private String name;
	private String infoTag = "applyModel";
	private String GPU = "CPU";
	private boolean unzipping = false;
	private ExecutorService service;
	private boolean allowStopping = true;
	private long startTime = System.currentTimeMillis();
	
	public RunnerProgress(DeepImageJ dp, String info) {
		super(new JFrame(), "Run DeepImageJ");
		name = dp.getName();
		JPanel prog = new JPanel(layout);
		place(prog, 0, 1, 0, title);
		place(prog, 1, 1, 0, time);
		infoTag = info;
		place(prog, 2, 1, 0, processor);
		place(prog, 3, 1, 0, peak);
		place(prog, 4, 1, 0, memory);
		place(prog, 5, 1, 0, patches);
		place(prog, 7, 1, 0, bnStop);
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
	
	public RunnerProgress(DeepImageJ dp, String info, ExecutorService serv) {
		super(new JFrame(), "Run DeepImageJ");
		name = dp.getName();
		JPanel prog = new JPanel(layout);
		place(prog, 0, 1, 0, title);
		place(prog, 1, 1, 0, time);
		infoTag = info;
		service = serv;
		// TODO show tag GPU all the time or only when there is a GPU
		//sif (!GPU.equals("CPU")) 
		place(prog, 2, 1, 0, processor);
		place(prog, 3, 1, 0, peak);
		place(prog, 4, 1, 0, memory);
		place(prog, 5, 1, 0, patches);
		place(prog, 7, 1, 0, bnStop);
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

	public void setRunner(Object runner) {
		this.runner = runner;
	}

	public void setService(ExecutorService service) {
		this.service = service;
	}

	public void setInfoTag(String info) {
		this.infoTag = info;
	}

	public void setGPU(String gpu) {
		this.GPU = gpu;
	}

	public void setUnzipping(boolean unzip) {
		this.unzipping = unzip;
	}

	public boolean getUnzipping() {
		return this.unzipping;
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
	
	public void allowStopping(boolean allow) {
		this.allowStopping = allow;
		if (isStopped())
			stop();
	}
	
	public boolean canRPStop() {
		return this.allowStopping;
	}

	public boolean isStopped() {
		return stop;
	}
	
	public void stop() {
		stop = true;
		if (!canRPStop()) {
			bnStop.setText("Stopping...");
			bnStop.setEnabled(false);
			return;
		}
			
		if (timer == null)
			return;
		if (clock == null)
			return;
		clock.cancel();
		timer.cancel();
		timer.purge();
		timer = null;
		dispose();
		if (service != null)
			service.shutdownNow();
		stop = true;
	}
 
	public void info() {
		title.setText(name);
		double mem = SystemUsage.getHeapUsed();
		peakmem = Math.max(peakmem, mem);
		time.setText("Runtime: " + NumFormat.seconds((System.nanoTime() - chrono)));
		memory.setText("Used memory: " + NumFormat.bytes(mem) + " / " + SystemUsage.getMaxMemory());
		peak.setText("Peak memory: " + NumFormat.bytes(peakmem));
		String gpuTag = "NO";
		
		if (infoTag.equals("load") && unzipping) {
			processor.setText("Unzipping model");
			patches.setText("No patches");
			return;
		} else if (infoTag.equals("load") && !unzipping) {
			processor.setText("Loading model");
			patches.setText("No patches");
			return;
		} else if (infoTag.equals("preprocessing")) {
			processor.setText("Preprocessing image");
			patches.setText("No patches");
			return;
		} else if (infoTag.equals("postprocessing")) {
			processor.setText("Postprocessing image");
			patches.setText("No patches");
			return;
		} else if (infoTag.equals("applyModel") && GPU.toLowerCase().equals("gpu")) {
			gpuTag = "YES";
		} else if (infoTag.equals("applyModel") && GPU.equals("???")) {
			gpuTag = "Unknown";
		}
		
		processor.setText("Model Inference (GPU: " + gpuTag + ")");
		if (runner != null && (runner instanceof RunnerTf))
			patches.setText("Patches: " + ((RunnerTf) runner).getCurrentPatch() + "/" + ((RunnerTf) runner).getTotalPatch());
		if (runner != null && (runner instanceof RunnerPt))
			patches.setText("Patches: " + ((RunnerPt) runner).getCurrentPatch() + "/" + ((RunnerPt) runner).getTotalPatch());
		
		
	}
	
	/*
	 * Get maximum memory used to run the model
	 */
	public double getPeakmem() {
		return this.peakmem;
	}
	
	/*
	 * Get time that it has taken to run the model (pre-processing,
	 * inference and postprocessing)
	 */
	public String getRuntime() {
		double time = (System.currentTimeMillis() - startTime) / 1000.0;
		String timeStr = "" + time;
		// Only show one decimal
		timeStr = timeStr.substring(0, timeStr.lastIndexOf(".") + 2);
		return timeStr;
	}
	
	public class Clock extends TimerTask {
		public void run() {
			info();
		}
	}
	
	/*
	 * Stop the RunnerProgress window and close the thread
	 */
	public static void stopRunnerProgress(ExecutorService thread, RunnerProgress rp) {
		thread.shutdown();
		if (!rp.isStopped()) {
			rp.allowStopping(true);
			rp.stop();
		}
		rp.dispose();
	}

}

package deepimagej;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

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
	private BorderLabel			peak			= new BorderLabel("Memory........");
	private BorderLabel			processor	= new BorderLabel("Processor.....");
	private Timer				timer		= new Timer(true);
	private JButton				bnStop		= new JButton("Stop");
	private BorderLabel			time			= new BorderLabel("Elapsed time");
	private double				chrono;
	private double				peakmem 		= 0;
	private Clock				clock;
	private GridBagLayout		layout		= new GridBagLayout();
	private GridBagConstraints	constraint	= new GridBagConstraints();
	private Runner runner;
	private boolean stop = false;
	private String name;
	
	public RunnerProgress(DeepPlugin dp) {
		super(new JFrame(), "Run DeepImageJ");
		name = dp.getName();
		JPanel prog = new JPanel(layout);
		place(prog, 0, 1, 0, title);
		place(prog, 1, 1, 0, time);
		place(prog, 2, 1, 0, processor);
		place(prog, 3, 1, 0, patches);
		place(prog, 4, 1, 0, memory);
		place(prog, 5, 1, 0, peak);
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

	public void setRunner(Runner runner) {
		this.runner = runner;
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
		dispose();
		stop = true;
	}
 
	public void info() {
		title.setText(name);
		double mem = SystemUsage.getHeapUsed();
		peakmem = Math.max(peakmem, mem);
		time.setText("Runtime: " + NumFormat.seconds((System.nanoTime() - chrono)));
		memory.setText("Used memory: " + NumFormat.bytes(mem) + " / " + SystemUsage.getMaxMemory());
		peak.setText("Peak memory: " + NumFormat.bytes(peakmem));
		processor.setText("Load CPU: " + String.format("%1.3f", SystemUsage.getLoad()) + "%");
		if (runner != null)
			patches.setText("Patches: " + runner.getCurrentPatch() + "/" + runner.getTotalPatch());
	}
	
	public class Clock extends TimerTask {
		public void run() {
			info();
		}
	}

}

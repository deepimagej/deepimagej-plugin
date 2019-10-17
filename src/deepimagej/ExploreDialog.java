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


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;

import deepimagej.components.BoldLabel;
import deepimagej.components.CustomizedColumn;
import deepimagej.components.CustomizedTable;
import deepimagej.components.HTMLPane;
import deepimagej.exceptions.MacrosError;
import deepimagej.tools.FileUtils;
import deepimagej.tools.Log;
import deepimagej.tools.WebBrowser;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

public class ExploreDialog extends JDialog implements Runnable, ActionListener, MouseListener, KeyListener {

	private CustomizedTable				table;
	private CustomizedTable				modelTable;
	private JButton						bnRefresh	= new JButton("Refresh");
	private JButton						bnClose		= new JButton("Close");
	private JButton						bnAbout		= new JButton("About");
	private JButton						bnArchi		= new JButton("Architecture");
	private JButton						bnApply		= new JButton("Apply");
	private JButton						bnHelp		= new JButton("Help");
	private String						path;
	private String 						image; 
	private HashMap<String, DeepPlugin>	dps;
	private BoldLabel					lblName		= new BoldLabel("");
	private HTMLPane						info		= new HTMLPane("Information");
	private Thread						thread		= null;
	private Log log = new Log();
	private	ImagePlus	imp;
	private DeepPlugin	dp;
	
	public ExploreDialog(String path) {
		super(new JFrame(), "DeepImageJ Explore [" + Constants.version + "]");
		this.path = path;
		doDialog();
		load();

		if (table.getRowCount() >= 1) {
			table.setRowSelectionInterval(0, 0);
			String name = table.getCell(0, 0);
			updateModel(name);
		}
	}
	
	private void doDialog() {
		ArrayList<CustomizedColumn> columns = new ArrayList<CustomizedColumn>();
		columns.add(new CustomizedColumn("Name", String.class, 100, false));
		columns.add(new CustomizedColumn("Model", String.class, 100, false));
		columns.add(new CustomizedColumn("Loading time", String.class, 40, false));
		// TODO allow re-sorting of rows 
		table = new CustomizedTable(columns, false);
		modelTable = new CustomizedTable(new String[] { "Feature", "Value" }, true);

		JPanel buttons1 = new JPanel(new GridLayout(1, 3));
		buttons1.add(bnAbout);
		buttons1.add(bnRefresh);
		buttons1.add(bnClose);

		JPanel buttons2 = new JPanel(new GridLayout(1, 6));
		buttons2.add(bnHelp);
		buttons2.add(bnArchi);
		buttons2.add(bnApply);

		JPanel pnList = new JPanel(new BorderLayout());
		pnList.add(new BoldLabel(path), BorderLayout.NORTH);
		pnList.add(table.getPane(270, 300), BorderLayout.CENTER);
		pnList.add(buttons1, BorderLayout.SOUTH);

		JScrollPane scroll = new JScrollPane(info);
		scroll.setPreferredSize(new Dimension(270, 300));

		JSplitPane pn = new JSplitPane(SwingConstants.VERTICAL, scroll, modelTable.getPane(270, 300));

		JPanel pnModel = new JPanel(new BorderLayout());
		pnModel.add(lblName, BorderLayout.NORTH);
		pnModel.add(pn, BorderLayout.CENTER);
		pnModel.add(buttons2, BorderLayout.SOUTH);

		table.addMouseListener(this);
		table.addKeyListener(this);
		bnHelp.addActionListener(this);
		bnRefresh.addActionListener(this);
		bnClose.addActionListener(this);
		bnApply.addActionListener(this);
		bnArchi.addActionListener(this);
		bnAbout.addActionListener(this);

		JSplitPane main = new JSplitPane(SwingConstants.VERTICAL, pnList, pnModel);
		add(main);

		setModal(false);
		pack();
		setVisible(true);

	}

	private void load() {
		// TODO added by Carlos to be consecuent with the new
		// DeepPlugin constructor
		boolean isDeveloper = false;
		table.removeRows();
		dps = DeepPlugin.list(path, log, isDeveloper);
		ArrayList<LoadThreaded> loaders = new ArrayList<LoadThreaded>();
		for (String name : dps.keySet())
			loaders.add(new LoadThreaded(name, dps.get(name), table));
		ExecutorService executor = Executors.newFixedThreadPool(2);
		for (LoadThreaded loader : loaders)
			executor.execute(loader);

		executor.shutdown();
		while (!executor.isTerminated())
			;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int row = table.getSelectedRow();
		dp = null;
		if (row >= 0)
			dp = dps.get(table.getCell(row, 0));
		if (e.getSource() == bnAbout)
			WebBrowser.open(Constants.url);
		if (e.getSource() == bnHelp) {
			if (dp == null)
				return;
			WebBrowser.open(dp.params.url);
		}
		if (e.getSource() == bnRefresh) {
			load();
		}
		if (e.getSource() == bnClose) {
			dispose();
		}
		if (e.getSource() == bnArchi) {
			if (dp != null) 
				TensorFlowModel.showArchitecture(dp.getName(), dp.msgArchis);
		}
		if (e.getSource() == bnApply) {
			if (dp == null)
				return;
			if (row < 0)
				return;
			if (thread != null) 
				return;
			image = path + table.getCell(row, 0) + File.separator + "exampleImage.tiff";
			Log log = new Log();
			log.print(image);
			if (new File(image).exists()) {
				imp = IJ.openImage(image);
				if (imp != null) {
					imp.show();
					if (thread == null) {
						thread = new Thread(this);
						thread.setPriority(Thread.MIN_PRIORITY);
						thread.start();
					}
					
				}
			}
		}
	}
	
	@Override
	public void run() {
		bnApply.setEnabled(false);
		int runStage = 0;
		try {
			String dir = dp.getPath();
			String m = dir + dp.params.preprocessingFile;
			if (new File(m).exists()) {
				log.print("start preprocessing");
				runMacro(m);
				log.print("end preprocessing");
				imp = WindowManager.getCurrentImage();
			}
			if (WindowManager.getCurrentWindow() == null) {
				IJ.error("Something failed in the preprocessing.");
				thread = null;
				return;
			}
			runStage ++;
			
			log.print("start progress");
			RunnerProgress rp = new RunnerProgress(dp);
			log.print("start runner");
			Runner runner = new Runner(dp, rp, log);
			rp.setRunner(runner);
			ImagePlus out = runner.call();
			
			if (out == null) {
				log.print("Error, output is null");
				IJ.error("Output is null");
				thread = null;
				return;
			}
			runStage ++;
			
			m = dir + dp.params.postprocessingFile;
			if (new File(m).exists()) {
				log.print("start postprocessing");
				runMacro(m);
				log.print("end postprocessing");
				out = WindowManager.getCurrentImage();
			}
			log.print("display " + out.getTitle());
			out.show();
			out.setSlice(1);
			out.getProcessor().resetMinAndMax();
		} catch(MacrosError ex) {
			if (runStage == 0) {
				IJ.error("Error applying the preprocessing macro to the image.");
			} else if (runStage == 2) {
				IJ.error("Error applying the postprocessing macro to the image.");
			}
			thread = null;
			return;
		
		} catch (FileNotFoundException e) {
			if (runStage == 1) {
				IJ.error("There was not preprocessing file found.");
			} else if (runStage == 3) {
				IJ.error("There was not preprocessing file found.");
			}
			thread = null;
			return;
		
		} catch (Exception e) {
			IJ.error("Runner Exception" + e.getMessage());
			if (runStage == 0){
				IJ.error("Error during preprocessing.");
			} else if (runStage == 1) {
				IJ.error("Error during the aplication of the model.");
			} else if (runStage == 2) {
				IJ.error("Error during postprocessing.");
			}
		}
		bnApply.setEnabled(true);
		thread = null;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == table) {
			int row = table.getSelectedRow();
			if (row < 0)
				return;
			String name = table.getCell(row, 0);
			updateModel(name);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getSource() == table) {
			int row = table.getSelectedRow();
			if (row < 0)
				return;
			String name = table.getCell(row, 0);
			updateModel(name);
		}
	}
	
	private void updateModel(String name) {
		modelTable.removeRows();
		String dir = path + File.separator + name + File.separator;
		lblName.setText(dir);
		if (dps == null) {
			modelTable.append(new String[] { "DeepPlugins", "Error" });
			return;
		}
		DeepPlugin dp = dps.get(name);
		if (dp == null) {
			modelTable.append(new String[] { "DeepPlugins", "Error" });
			return;
		}
		Parameters params = dp.params;
		String patch = params.fixedPatch ? "Fix" : "" + params.minimumSize + " mini.";
		String dimension = "";
		for (int dim : params.inDimensions)
			dimension += " " + dim;
		String mgd = "" + dp.getModel().metaGraphDef().length;
		String gd = "" + dp.getModel().graph().toGraphDef().length;
		info.clear();
		if (!params.author.equals(""))
			info.append("h1", dp.getName());
		else
			info.append("h1", dp.dirname);
		bnHelp.setEnabled(!params.url.equals(""));

		if (!params.author.equals(""))
			info.append("p", params.author);
		if (!params.credit.equals(""))
			info.append("p", params.credit);
		if (!params.url.equals(""))
			info.append("p", "<b>URL:</b> " + params.url);
		if (!params.version.equals(""))
			info.append("p", "<b>Version:</b> " + params.version);
		if (!params.date.equals(""))
			info.append("p", "<b>Date:</b> " + params.date);
		if (!params.reference.equals("")) {
			info.append("p", "<hr><b>Reference:</b>");
			info.append("p", params.reference);
		}
		info.append("<hr>");
		info.append("p", "<b>Test<b>");
		info.append("p", "Input size: " + params.inputSize);
		info.append("p", "Output size: " + params.outputSize);
		info.append("p", "Memory peak: " + params.memoryPeak);
		info.append("p", "Runtime: " + params.runtime);

		modelTable.append(new String[] { "Tag", dp.params.tag });
		modelTable.append(new String[] { "Signature", dp.params.graph });
		modelTable.append(new String[] { "Model size", FileUtils.getFolderSizeKb(path + name + File.separator + "variables") });
		modelTable.append(new String[] { "Graph size", "" + gd });
		modelTable.append(new String[] { "Metagraph size", "" + mgd });
		modelTable.append(new String[] { "Patch policy", patch });
		modelTable.append(new String[] { "Patch size", "" + params.patch });
		modelTable.append(new String[] { "Padding", "" + params.padding });
		modelTable.append(new String[] { "Dimension", dimension });
		modelTable.append(new String[] { "Slices/Channels", "" + params.slices + "/" + params.channels });
		for (String p : dp.preprocessing)
			if (dp.getInfoMacro(dir + p) != null)
				modelTable.append(new String[] { "Preprocessing", dp.getInfoMacro(dir + p) });
		for (String p : dp.postprocessing)
			if (dp.getInfoMacro(dir + p) != null)
				modelTable.append(new String[] { "Postprocessing", dp.getInfoMacro(dir + p) });
		modelTable.append(new String[] { "Test input image", dp.getInfoImage(dir + "exampleImage.tiff") });
		modelTable.append(new String[] { "Test output image", dp.getInfoImage(dir + "resultImage.tiff") });

		for (int i = 0; i < params.nInputs; i++)
			modelTable.append(new String[] { "Input name(form)", params.inputs[i] + "(" + params.inputForm[i] + ")" });
		for (int i = 0; i < params.nOutputs; i++)
			modelTable.append(new String[] { "Output name(form)", params.outputs[i] + "(" + params.outputForm[i] + ")" });
	}
	
	public class LoadThreaded implements Runnable {

		private String			name;
		private DeepPlugin		dp;
		private CustomizedTable	table;

		public LoadThreaded(String name, DeepPlugin dp, CustomizedTable table) {
			this.name = name;
			this.dp = dp;
			this.table = table;
		}

		@Override
		public void run() {
			double chrono = System.nanoTime();
			dp.loadModel();
			String size = FileUtils.getFolderSizeKb(path + name + File.separator + "variables");
			String time = String.format("%3.1f ms", (System.nanoTime() - chrono) / (1024 * 1024));
			String row[] = { dp.dirname, size, time };
			table.append(row);
		}
	}
	
	public static void runMacro(String macroFile) throws FileNotFoundException, MacrosError {
		String macro = "";
		try {
			macro = new Scanner(new File(macroFile)).useDelimiter("\\Z").next();
		} catch (NoSuchElementException ex) {
			macro ="";
		}
		String executionResult = "";
		if (macro.contentEquals("") == false) {
			executionResult = IJ.runMacro(macro);
			if (executionResult != null ) {
				if (executionResult.contentEquals("[aborted]") == true) {
					throw new MacrosError();
				}
			}
		}
		
	}


}
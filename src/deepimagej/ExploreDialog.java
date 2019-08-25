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
import java.util.ArrayList;
import java.util.HashMap;
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
import deepimagej.tools.FileUtils;
import deepimagej.tools.WebBrowser;
import ij.IJ;
import ij.ImagePlus;

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
		table = new CustomizedTable(columns, true);
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
		table.removeRows();
		dps = DeepPlugin.list(path, log);
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
			if (dp != null) {
				JFrame frame = new JFrame("Architedcture of " + dp.getName());
				ArrayList<CustomizedColumn> columns = new ArrayList<CustomizedColumn>();
				columns.add(new CustomizedColumn("Operation", String.class, 100, false));
				columns.add(new CustomizedColumn("Name", String.class, 100, false));
				columns.add(new CustomizedColumn("Type", String.class, 40, false));
				columns.add(new CustomizedColumn("NumOutputs", String.class, 20, false));

				CustomizedTable arch = new CustomizedTable(columns, true);
				ArrayList<String[]> archis = dp.msgArchis;
				for (String[] archi : archis)
					arch.append(archi);
				frame.add(arch.getPane(500, 500));
				frame.pack();
				frame.setVisible(true);
			}
		}
		if (e.getSource() == bnApply) {
			if (dp == null)
				return;
			if (row < 0)
				return;
			if (thread != null) 
				return;
			String image = path + table.getCell(row, 0) + File.separator + "exampleImage.tiff";
			Log log = new Log();
			log.print(image);
			if (new File(image).exists()) {
				imp = IJ.openImage(image);
				if (imp != null) {
					imp.show();
					thread = new Thread(this);
					thread.setPriority(Thread.MIN_PRIORITY);
					thread.start();
				}
			}
		}
	}
	
	@Override
	public void run() {
		bnApply.setEnabled(false);
		try {
			RunnerProgress rp = new RunnerProgress(dp);
			rp.setVisible(true);
			Runner runner = new Runner(dp, rp, imp, log);
			rp.setRunner(runner);
			ExecutorService executor = Executors.newFixedThreadPool(1);
			executor.submit(runner);
			executor.shutdownNow();
		}
		catch (Exception e) {
			IJ.error("Runner Exception" + e.getMessage());
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
		String patch = params.fixedPatch ? "Fix" : "" + params.minimum_patch_size + " mini.";
		String dimension = "";
		for (int dim : params.in_dimensions)
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
		modelTable.append(new String[] { "Overlap", "" + params.overlap });
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

		for (int i = 0; i < params.n_inputs; i++)
			modelTable.append(new String[] { "Input name(form)", params.inputs[i] + "(" + params.input_form[i] + ")" });
		for (int i = 0; i < params.n_outputs; i++)
			modelTable.append(new String[] { "Output name(form)", params.outputs[i] + "(" + params.output_form[i] + ")" });
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


}

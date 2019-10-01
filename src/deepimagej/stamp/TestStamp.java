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

package deepimagej.stamp;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.protobuf.InvalidProtocolBufferException;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepPlugin;
import deepimagej.Parameters;
import deepimagej.Runner;
import deepimagej.RunnerProgress;
import deepimagej.TensorFlowModel;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.exceptions.IncorrectChannelsNumber;
import deepimagej.exceptions.MacrosError;
import deepimagej.tools.Log;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.Duplicator;

public class TestStamp extends AbstractStamp implements Runnable, ActionListener {

	private HTMLPane				pnTest;
	private JButton				bnTest	= new JButton("Run a test");
	private JComboBox<String>	cmb		= new JComboBox<String>();

	public TestStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	public void buildPanel() {
		SwingUtilities.invokeLater(() -> {

		pnTest = new HTMLPane(Constants.width, 100);
		HTMLPane pane = new HTMLPane(Constants.width, 100);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "Run a test on an image");
		pane.append("p", "Select on input image and click on 'Run a test ");
		GridPanel pn1 = new GridPanel(true);
		pn1.place(1, 0, cmb);
		pn1.place(2, 0, bnTest);

		JPanel pnt = new JPanel();
		pnt.setLayout(new BoxLayout(pnt, BoxLayout.PAGE_AXIS));
		pnt.add(pane.getPane());
		pnt.add(pn1);

		JPanel pn = new JPanel(new BorderLayout());
		pn.add(pnt, BorderLayout.NORTH);
		pn.add(pnTest, BorderLayout.CENTER);

		panel.add(pn);
		bnTest.addActionListener(this);
		cmb.addActionListener(this);
		});
	}

	@Override
	public void init() {
		cmb.removeAll();
		int list[] = WindowManager.getIDList();
		for (int im : list) {
			ImagePlus imp = WindowManager.getImage(im);
			if (imp != null) {
				cmb.addItem(imp.getTitle());
			}
		}
		bnTest.setEnabled(parent.getDeepPlugin() != null);
	}

	@Override
	public boolean finish() {
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnTest) {
				test();
		}
	}

	public void test() {
		Parameters params = parent.getDeepPlugin().params;

		File file = new File(params.path2Model);
		if (!file.exists()) {
			IJ.error("No selected model.");
			return;
		}
		String dirname = file.getName();
		bnTest.setEnabled(false);
		pnTest.append("h2", "Test " + dirname);
		
		String image = (String)cmb.getSelectedItem();
		params.testImage = WindowManager.getImage(image);
		if (params.testImage == null) {
			pnTest.append("p", "No selected test image");
			IJ.error("No selected test image.");
			return;
		}
		pnTest.append("Selected input image " + params.testImage.getTitle());
	
		try {
			// Set Parameter params.inputSize for config.xml
			params.inputSize = Integer.toString(params.testImage.getWidth()) + "x" + Integer.toString(params.testImage.getHeight());
			params.testImageBackup = new Duplicator().run(params.testImage);
			params.testImage = runPreprocessingMacro(params.testImage);
			params.channels = TensorFlowModel.nChannels(params, params.inputForm[0]);
			int imageChannels = params.testImage.getNChannels();
			if (params.channels.equals(Integer.toString(imageChannels)) != true) {
				throw new IncorrectChannelsNumber(Integer.parseInt(params.channels), imageChannels);
			}

			Thread thread = new Thread(this);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		catch (MacrosError e1) {
			pnTest.append("p", "Error in the Macro's code");
			IJ.error("Failed preprocessing");
		}
		catch (IllegalArgumentException e1) {
			IJ.error("The model failed to execute because " + "at some point \nthe size of the activations map was incorrect");
			e1.printStackTrace();

		}
		catch (InvalidProtocolBufferException e) {
			pnTest.append("p", "Impossible to load model");
			e.printStackTrace();
		}
		catch (IOException e) {
			pnTest.append("p", "There is no image open in ImageJ");
		}
		catch (UnsupportedOperationException e) {
			pnTest.append("p", "The model could not be executed properly. Try with another parameters.\n");
		}
		catch (IncorrectChannelsNumber e) {
			pnTest.append("p", "The number of channels of the iput image is incorrect.");
		}
	}

	public void run() {
		DeepPlugin dp = parent.getDeepPlugin();

		Log log = new Log();
		RunnerProgress rp = new RunnerProgress(dp);
		Runner runner = new Runner(dp, rp, dp.params.testImage, log);
		rp.setRunner(runner);
		dp.params.testResultImage = runner.call();
		// Flag to apply post processing if needed
		if (dp.params.testResultImage != null) {
			dp.params.testResultImage = runPostprocessingMacro(dp.params.testResultImage);
			parent.endsTest();
			bnTest.setEnabled(true);
			dp.params.testResultImage.getProcessor().resetMinAndMax();
			dp.params.testResultImage.show();
			pnTest.append("p", "Peak memory:" + dp.params.memoryPeak);
			pnTest.append("p", "Runtime:" + dp.params.runtime);
		}
		else {
			IJ.error("The execution of the model failed.");
		}
	}

	private ImagePlus runPreprocessingMacro(ImagePlus img) throws MacrosError, IOException {
		WindowManager.setTempCurrentImage(img);
		Parameters params = parent.getDeepPlugin().params;

		if (params.postmacro.equals("") == false) {
			String result = IJ.runMacro(params.premacro);
			if (result == "[aborted]") {
				throw new MacrosError();
			}
		}
		ImagePlus result = WindowManager.getCurrentImage();
		return result;
	}

	private ImagePlus runPostprocessingMacro(ImagePlus img) {
		DeepPlugin dp = parent.getDeepPlugin();
		WindowManager.setTempCurrentImage(img);
		if (dp.params.postmacro.equals("") == false) {
			String result = IJ.runMacro(dp.params.postmacro);
			if (result == "[aborted]") {
				IJ.error("The postprocessing macros did not work.\n" + "The image displayed is the raw output.");
			}
		}
		ImagePlus result = WindowManager.getCurrentImage();
		return result;
	}

}

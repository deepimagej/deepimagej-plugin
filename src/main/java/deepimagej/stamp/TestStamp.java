/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we strongly encourage you to include adequate citations and acknowledgments 
 * whenever you present or publish results that are based on it.
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
 * DeepImageJ is an open source software (OSS): you can redistribute it and/or modify it under 
 * the terms of the BSD 2-Clause License.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 * You should have received a copy of the BSD 2-Clause License along with DeepImageJ. 
 * If not, see <https://opensource.org/licenses/bsd-license.php>.
 */

package deepimagej.stamp;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.protobuf.InvalidProtocolBufferException;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.Runner;
import deepimagej.RunnerProgress;
import deepimagej.TensorFlowModel;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.exceptions.IncorrectChannelsSlicesNumber;
import deepimagej.exceptions.MacrosError;
import deepimagej.processing.ExternalClassManager;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Log;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.Duplicator;

public class TestStamp extends AbstractStamp implements Runnable, ActionListener {

	private HTMLPane			pnTest;
	private JButton				bnTest	= new JButton("Run a test");
	private List<JComboBox<String>>	cmbList	= new ArrayList<JComboBox<String>>();
	private JPanel				inputsPn = new JPanel();
	
	private List<DijTensor>		imageTensors;

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
		JComboBox<String> cmb = new JComboBox<String>();
		cmbList.add(cmb);
		inputsPn.add(new JLabel("Image"));
		inputsPn.add(cmb);
		pn1.place(1, 0, inputsPn);
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
		Parameters params = parent.getDeepPlugin().params;
		inputsPn.removeAll();
		imageTensors = DijTensor.getImageTensors(params.inputList);
		inputsPn.setLayout(new GridLayout(2, imageTensors.size()));
		cmbList = new ArrayList<JComboBox<String>>();

		JComboBox<String> cmb = new JComboBox<String>();
		String[] titlesList = WindowManager.getImageTitles();
		if (titlesList.length != 0) {
			for (DijTensor tensor : imageTensors) {
				cmb = new JComboBox<String>();
				for (String title : titlesList)
					cmb.addItem(title);
				inputsPn.add(new JLabel(tensor.name));
				cmbList.add(cmb);
				inputsPn.add(cmb);
			}
			bnTest.setEnabled(parent.getDeepPlugin() != null);
		} else {
			bnTest.setEnabled(false);
			params.testImageBackup = null;
			cmb.addItem("No image");
			cmbList.add(cmb);
			inputsPn.add(new JLabel("Select image"));
			inputsPn.add(cmb);
		}
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
		
		String[] images = new String[imageTensors.size()];
		for (int i = 0; i < images.length; i++)
			images[i] = (String)cmbList.get(i).getSelectedItem();
		params.testImage = new ImagePlus[imageTensors.size()];
		int c = 0;
		params.inputPixelSize = new String[imageTensors.size()];
		String imagesNames = "";
		for (String imageName : images) {
			params.testImage[c] = WindowManager.getImage(imageName);
			params.inputPixelSize[c] = ArrayOperations.findPixelSize(params.testImage[c]);
			imagesNames = imagesNames + params.testImage[c].getTitle() + "\n";
			c ++;
		}

		for (ImagePlus im : params.testImage) {
			if (im == null) {
				pnTest.append("p", "No selected test image");
				IJ.error("No selected test image.");
				return;
			}	
		}
		
		pnTest.append("Selected input image " + imagesNames);
	
		try {
			// Set Parameter params.inputSize for config.xml
			runPreprocessing(params);
			// Check if the images have the adequate channels and slices
			for (int i = 0; i < imageTensors.size(); i ++) {
				int channels = TensorFlowModel.nChannelsOrSlices(imageTensors.get(i), "channels");
				int slices = TensorFlowModel.nChannelsOrSlices(imageTensors.get(i), "slices");
				int imageChannels = params.testImage[i].getNChannels();
				int imageSlices = params.testImage[i].getNSlices();
				if (channels != imageChannels) {
					throw new IncorrectChannelsSlicesNumber(channels, imageChannels, "channels");
				}
				if (slices != imageSlices) {
					throw new IncorrectChannelsSlicesNumber(slices, imageSlices, "slices");
				}
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
		catch (IncorrectChannelsSlicesNumber e) {
			String type = e.getExceptionType();
			pnTest.append("p", "The number of " + type + " of the input image is incorrect.");
		}
	}

	public void run() {
		DeepImageJ dp = parent.getDeepPlugin();

		Log log = new Log();
		RunnerProgress rp = new RunnerProgress(dp);
		Runner runner = new Runner(dp, rp, dp.params.testImage[0], log);
		rp.setRunner(runner);
		dp.params.testResultImage = runner.call();
		// Flag to apply post processing if needed
		if (dp.params.testResultImage != null) {
			dp.params.testResultImage[0] = runPostprocessingMacro(dp.params.testResultImage[0]);
			parent.endsTest();
			bnTest.setEnabled(true);
			dp.params.testResultImage[0].getProcessor().resetMinAndMax();
			dp.params.testResultImage[0].show();
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
	
	// TODO decide whether to allow or not more than 1 image input to the model
	private void runPreprocessing(Parameters params) throws MacrosError, IOException {
		params.testImageBackup = new Duplicator().run(params.testImage[0]);
		if (params.isJavaPreprocessing == true && params.preprocessingBeforeMacro == true) {
			params.testImage[0] = runPreprocessingJava(params.testImage[0], params);
			params.testImage[0] = runPreprocessingMacro(params.testImage[0]);
		} else if (params.isJavaPreprocessing == true && params.preprocessingBeforeMacro == false) {
			params.testImage[0] = runPreprocessingMacro(params.testImage[0]);
			params.testImage[0] = runPreprocessingJava(params.testImage[0], params);
		} else if (params.isJavaPreprocessing == false && params.preprocessingBeforeMacro == true) {
			params.testImage[0] = runPreprocessingMacro(params.testImage[0]);
		}
	}

	private ImagePlus runPostprocessingMacro(ImagePlus img) {
		DeepImageJ dp = parent.getDeepPlugin();
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

	private ImagePlus runPreprocessingJava(ImagePlus img, Parameters params) {
		ExternalClassManager processingRunner = new ExternalClassManager (params.javaPreprocessing, false);
		ImagePlus result = processingRunner.javaProcessImage(img);
		return result;
	}
}

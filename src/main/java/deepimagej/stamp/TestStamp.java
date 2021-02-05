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
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.RunnerTf;
import deepimagej.RunnerProgress;
import deepimagej.RunnerPt;
import deepimagej.components.HTMLPane;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.DijRunnerPostprocessing;
import deepimagej.tools.DijRunnerPreprocessing;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Log;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

public class TestStamp extends AbstractStamp implements ActionListener, MouseListener, Runnable {

	private HTMLPane				pnTest;
	private JButton					bnTest	= new JButton("Run a test");
	private JTextField				axesTxt	= new JTextField("C,Y,X");
	private JTextField				sizeTxt	= new JTextField("3,256,256");
	private List<JComboBox<String>>	cmbList	= new ArrayList<JComboBox<String>>();
	private List<JButton>			btnList	= new ArrayList<JButton>();
	private JPanel					inputsPn = new JPanel(new GridLayout(3, 1));
	private String					selectedImage = "";
	
	private List<DijTensor>		imageTensors;

	public TestStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	public void buildPanel() {
		SwingUtilities.invokeLater(() -> {

		pnTest = new HTMLPane(Constants.width, 100);
		JScrollPane pnTestScroller = new JScrollPane(pnTest);
		//pnTestScroller.setPreferredSize(new Dimension(Constants.width, pnTest.getPreferredSize().height));
		HTMLPane pane = new HTMLPane(Constants.width, 100);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "Run a test on an image");
		pane.append("p", "Select an input image.");
		pane.append("p", "Introduce an image size that can be accepted by the model.");
		pane.append("p", "The tile size will be used together with the parameters\n"
					   + "previously introduced to process the whole image.");
		pane.append("p", "Take into account that if you are using CPU, the images\n"
					   + "processed at once cannot be too big due to memory limitations.");
		pane.append("p", "The smallest size that allows processing the whole image with\n"
				       + "only 1 tile and that fulfils the parameters in suggested\n"
				       + "automatically");
		pane.append("p", "After setting the tile size for the test run, click on <b>Run a test</b>");
		
		JPanel pn1 = new JPanel();
		pn1.setLayout(new BoxLayout(pn1, BoxLayout.Y_AXIS));
		JComboBox<String> cmb = new JComboBox<String>();
		cmbList.add(cmb);
		JButton btn = retrieveJComboBoxArrow(cmb);
		btnList.add(btn);
		JPanel firstPn = new JPanel();
		firstPn.setLayout(new BoxLayout(firstPn, BoxLayout.LINE_AXIS));
		firstPn.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		//JPanel firstPn = new JPanel(new GridLayout(1, 2));
		firstPn.add(new JLabel("input"));
		firstPn.add(cmb);
		//JPanel secondPn = new JPanel(new GridLayout(1, 2));
		JPanel secondPn = new JPanel();
		secondPn.setLayout(new BoxLayout(secondPn, BoxLayout.LINE_AXIS));
		secondPn.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		secondPn.add(new JLabel("Axes"));
		secondPn.add(axesTxt);
		//JPanel thirdPn = new JPanel(new GridLayout(1, 2));
		JPanel thirdPn = new JPanel();
		thirdPn.setLayout(new BoxLayout(thirdPn, BoxLayout.LINE_AXIS));
		thirdPn.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		thirdPn.add(new JLabel("Input tile size"));
		thirdPn.add(sizeTxt);
		inputsPn.add(firstPn);
		inputsPn.add(secondPn);
		inputsPn.add(thirdPn);
		pn1.add(inputsPn, BorderLayout.CENTER);
		pn1.add(bnTest, BorderLayout.SOUTH);
		pn1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JPanel pnt = new JPanel();
		pnt.setLayout(new BoxLayout(pnt, BoxLayout.PAGE_AXIS));
		pnt.add(pane.getPane());
		pnt.add(pn1);

		JPanel pn = new JPanel(new BorderLayout());
		pn.add(pnt, BorderLayout.NORTH);
		pn.add(pnTestScroller, BorderLayout.CENTER);
		
		pnTest.setEnabled(true);

		panel.add(pn);
		bnTest.addActionListener(this);
		});
	}

	@Override
	public void init() {
		Parameters params = parent.getDeepPlugin().params;
		inputsPn.removeAll();
		imageTensors = DijTensor.getImageTensors(params.inputList);
		//inputsPn.setLayout(new GridLayout(3, 1));
		inputsPn.setLayout(new BoxLayout(inputsPn, BoxLayout.PAGE_AXIS));
		cmbList = new ArrayList<JComboBox<String>>();
		btnList = new ArrayList<JButton>();
		JComboBox<String> cmb = new JComboBox<String>();
		cmb = new JComboBox<String>();
		String[] titlesList = WindowManager.getImageTitles();
		int c = 0;
		for (DijTensor tensor : imageTensors) {
			cmb = new JComboBox<String>();
			if (titlesList.length != 0) {
				for (String title : titlesList)
					cmb.addItem(title);
				cmbList.add(cmb);
				bnTest.setEnabled(parent.getDeepPlugin() != null);
			} else {
				bnTest.setEnabled(false);
				params.testImageBackup = null;
				cmb.addItem("No image");
				cmbList.add(cmb);
			}
			
			JPanel firstPn = new JPanel();
			firstPn.setLayout(new BoxLayout(firstPn, BoxLayout.LINE_AXIS));
			firstPn.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
			JLabel lab1 = new JLabel(tensor.name);
			firstPn.add(lab1);
			firstPn.add(cmb);
			lab1.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			JPanel secondPn = new JPanel();
			secondPn.setLayout(new BoxLayout(secondPn, BoxLayout.LINE_AXIS));
			secondPn.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
			JLabel lab2 = new JLabel("Axes");
			secondPn.add(lab2);
			secondPn.add(axesTxt);
			lab2.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			setAxes(c);
			JPanel thirdPn = new JPanel();
			thirdPn.setLayout(new BoxLayout(thirdPn, BoxLayout.LINE_AXIS));
			thirdPn.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
			JLabel lab3 = new JLabel("Input tile size");
			thirdPn.add(lab3);
			thirdPn.add(sizeTxt);
			lab3.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			setOptimalPatch((String) cmb.getSelectedItem(), c);
			inputsPn.add(firstPn);
			inputsPn.add(secondPn);
			inputsPn.add(thirdPn);
			
			btnList.add(retrieveJComboBoxArrow(cmb));
			btnList.get(c).addMouseListener(this);
			cmbList.get(c).addMouseListener(this);
			cmbList.get(c ++).addActionListener(this);
		}
	}

	@Override
	public boolean finish() {
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnTest) {
			// Check if all the input images are associated to an image
			// opened in ImageJ
			String[] titlesList = WindowManager.getImageTitles();
			for (int j = 0; j < cmbList.size(); j ++) {
				String selectedOption = (String) cmbList.get(j).getSelectedItem();
				if (Arrays.asList(titlesList).contains(selectedOption))
					continue;
				 else {
					 IJ.error("Select images open in ImageJ");
					return;
				}	
			}
			// If all the images selected are opened in ImageJ, perform a test run
			if (!testPreparation())
				return;
			Thread thread = new Thread(this);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		
		for (int i = 0; i < cmbList.size(); i ++) {
			if (e.getSource() == cmbList.get(i)) {
				// If all the selected items in every cmbBox correspond
				// to an existing image, set the button enabled, if not, 
				// not enabled
				cmbList.get(0).removeActionListener(this);
				String[] titlesList = WindowManager.getImageTitles();
				for (int j = 0; j < cmbList.size(); j ++) {
					String selectedOption = (String) cmbList.get(j).getSelectedItem();
					if (Arrays.asList(titlesList).contains(selectedOption)) {
						setOptimalPatch(selectedOption, j);
						continue;
					} else {
						 bnTest.setEnabled(false);
						return;
					}	
				}
				bnTest.setEnabled(true);
				cmbList.get(0).addActionListener(this);
				
				break;
			}
		}
		
		// If the selected image has changed, update the patch size
		// TODO generalise for several inputa images
		String newSelectedImage = (String) cmbList.get(0).getSelectedItem();
		if (!selectedImage.contentEquals(newSelectedImage)) {
			selectedImage = newSelectedImage;
			if (Arrays.asList(WindowManager.getImageTitles()).contains(selectedImage))
				setOptimalPatch(selectedImage, 0);
		}
	}
	
	/*
	 * Prepare the inputs and check that the image complies with the
	 * model requirements
	 * Return false if something is wrong
	 */
	public boolean testPreparation() {
		Parameters params = parent.getDeepPlugin().params;

		File file = new File(params.path2Model);
		if (!file.exists()) {
			IJ.error("The model was removed from its original location.");
			return false;
		}
		String dirname = file.getName();
		bnTest.setEnabled(false);
		pnTest.append("h2", "Test " + dirname);
		
		String[] images = new String[imageTensors.size()];
		for (int i = 0; i < images.length; i++) {
			images[i] = (String)cmbList.get(i).getSelectedItem();
			// TODO generalise for several input images
			String[] dims = DijTensor.getWorkingDims(imageTensors.get(i).form);
			int[] tileSize = ArrayOperations.getPatchSize(dims, imageTensors.get(i).form, sizeTxt.getText(), sizeTxt.isEditable());
			boolean isTileCorrect = checkInputTileSize(tileSize, imageTensors.get(i).name, params);
			if (tileSize == null || !isTileCorrect)
				return false;
			imageTensors.get(i).recommended_patch = tileSize;
		}
		if (images.length == 1)
			params.testImage = WindowManager.getImage(images[0]);
		String imagesNames = Arrays.toString(images);

		for (String im : images) {
			if (WindowManager.getImage(im) == null) {
				pnTest.append("p", im + " does not correspond to an open image");
				IJ.error("No selected test image.");
				return false;
			}
			params.testImageBackup = WindowManager.getImage(im).duplicate();
			params.testImageBackup.setTitle("DUP_" + im);
		}
		
		pnTest.append("Selected input images " + imagesNames);
		return true;
	}

	@Override
	public void run() {
		test();
		
	}

	/*// TODO create methods to group code in charge of stopping the execution
	 * Perform a test run on the selected image
	 */
	public void test() {
		String runnerError = "";
		ExecutorService service = Executors.newFixedThreadPool(1);
		DeepImageJ dp = parent.getDeepPlugin();
		RunnerProgress rp = new RunnerProgress(dp, "preprocessing");
		String step = "pre";
		
		try {
			DijRunnerPreprocessing preprocess = new DijRunnerPreprocessing(dp, rp, null, false);
			Future<HashMap<String, Object>> f0 = service.submit(preprocess);
			HashMap<String, Object> inputsMap = f0.get();
			if (rp.isStopped()) {
			    RunnerProgress.stopRunnerProgress(service, rp);
				pnTest.append("p", "Test run was stoped during preprocessing.");
				IJ.error("Test run was stoped during preprocessing.");
				// Remove possible hidden images from IJ workspace
				ArrayOperations.removeProcessedInputsFromMemory(inputsMap, true);
				return;
			} else if (inputsMap == null && preprocess.error.contentEquals("")) {
			    RunnerProgress.stopRunnerProgress(service, rp);
				pnTest.append("p", "Error during preprocessing.");
				pnTest.append("p", "The preprocessing did not return anything.");
				// Remove possible hidden images from IJ workspace
				ArrayOperations.removeProcessedInputsFromMemory(inputsMap, true);
				return;
			} else if (!preprocess.error.contentEquals("")) {
			    RunnerProgress.stopRunnerProgress(service, rp);
				pnTest.append("p", preprocess.error);
				// Remove possible hidden images from IJ workspace
				ArrayOperations.removeProcessedInputsFromMemory(inputsMap, true);
				return;
			}
			
			step = "model";
			HashMap<String, Object> output = null;
			if (dp.params.framework.equals("Tensorflow")) {
				rp.setGPU(parent.getGPUTf());
				RunnerTf runner = new RunnerTf(dp, rp, inputsMap, new Log());
				rp.setRunner(runner);
				// TODO decide what to store at the end of the execution
				Future<HashMap<String, Object>> f1 = service.submit(runner);
				output = f1.get();
				runnerError = runner.error;
			} else {
				rp.setGPU(parent.getGPUPt());
				RunnerPt runner = new RunnerPt(dp, rp, inputsMap, new Log());
				rp.setRunner(runner);
				// TODO decide what to store at the end of the execution
				Future<HashMap<String, Object>> f1 = service.submit(runner);
				output = f1.get();
				runnerError = runner.error;
			}
			
			if (output == null && !rp.isStopped()) {
			    RunnerProgress.stopRunnerProgress(service, rp);
				pnTest.append("p", "Test run failed");
				pnTest.append("p", runnerError);
				IJ.error("The execution of the model failed.");
				// Remove possible hidden images from IJ workspace
				ArrayOperations.removeProcessedInputsFromMemory(inputsMap, true);
				return;
			} else if (rp.isStopped()) {
			    RunnerProgress.stopRunnerProgress(service, rp);
				pnTest.append("p", "Model execution of the test run stopped");
				IJ.error("Model execution of the test run stopped.");
				// Remove possible hidden images from IJ workspace
				ArrayOperations.removeProcessedInputsFromMemory(inputsMap, true);
				return;
			}
			
			step = "post";
			DijRunnerPostprocessing postprocess = new DijRunnerPostprocessing(dp, rp, output);
			Future<HashMap<String, Object>> f2 = service.submit(postprocess);
			output = f2.get();

			if (rp.isStopped()) {
				pnTest.append("p", "Test run was stoped during postprocessing.");
				IJ.error("Test run was stoped during postprocessing.");
			}
			
		    RunnerProgress.stopRunnerProgress(service, rp);
			// Print the outputs of the postprocessing
			// Retrieve the opened windows and compare them to what the model has outputed
			// Display only what has not already been displayed

			String[] finalFrames = WindowManager.getNonImageTitles();
			String[] finalImages = WindowManager.getImageTitles();
			ArrayOperations.displayMissingOutputs(finalImages, finalFrames, output);
			// Remove possible hidden images from IJ workspace
			ArrayOperations.removeProcessedInputsFromMemory(inputsMap, true);
			
			parent.endsTest();
			bnTest.setEnabled(true);
			pnTest.append("p", "Peak memory:" + dp.params.memoryPeak);
			dp.params.runtime = rp.getRuntime();
			pnTest.append("p", "Runtime: " + dp.params.runtime + "s");
			
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			if (step.contains("pre")) {
				pnTest.append("p", "Thread stopped working during the preprocessing.\n"
									+ "The reason might be a faulty preprocessing");
				IJ.error("p", "Thread stopped working during the preprocessing.\n"
						+ "The reason might be a faulty preprocessing");
			} else if (step.contains("model")) {
				pnTest.append("p", "Thread stopped working during the execution of the model.");
				IJ.error("Thread stopped working during the execution of the model.");
			} else if (step.contains("post")) {
				pnTest.append("p", "Thread stopped working during the execution of the model.\n"
								+ "The reason might be a faulty postprocessing");
				IJ.error("p", "Thread stopped working during the execution of the model.\n"
						+ "The reason might be a faulty postprocessing");
			}
		}
	    RunnerProgress.stopRunnerProgress(service, rp);
	}
	
	public JButton retrieveJComboBoxArrow(Container container) {
		if (container instanceof JButton) {
	         return (JButton) container;
		} else {
			Component[] components = container.getComponents();
			for (Component component : components) {
				if (component instanceof Container) {
					return retrieveJComboBoxArrow((Container)component);
	            }
			}
	    }
		return null;
	}

	/*
	 * This method sets the axes specified by the user separated by commas
	 */
	private void setAxes(int imageTensorInd) {
		DijTensor tensor = imageTensors.get(imageTensorInd);
		// Get basic information about the input from the yaml
		String tensorForm = tensor.form;
		String[] dim = DijTensor.getWorkingDims(tensorForm);

		String axesAux = "";
		for (String dd : dim) {axesAux += dd + ",";}
		axesTxt.setText(axesAux.substring(0, axesAux.length() - 1));
		axesTxt.setEditable(false);
		
	}

	/*
	 * This method calculates an acceptable input tile size to the model
	 * considering the image selected size and the parameters set previously
	 * by the user
	 */
	private void setOptimalPatch(String selectedOption, int imageTensorInd) {
		ImagePlus imp = WindowManager.getImage(selectedOption);
		DijTensor tensor = imageTensors.get(imageTensorInd);
		// Get basic information about the input from the yaml
		String tensorForm = tensor.form;
		// Minimum size if it is not fixed, 0s if it is
		int[] tensorMin = tensor.minimum_size;
		// Step if the size is not fixed, 0s if it is
		int[] tensorStep = tensor.step;
		int[] haloSize = ArrayOperations.findTotalPadding(tensor, parent.getDeepPlugin().params.outputList, parent.getDeepPlugin().params.pyramidalNetwork);
		int[] min = DijTensor.getWorkingDimValues(tensorForm, tensorMin); 
		int[] step = DijTensor.getWorkingDimValues(tensorForm, tensorStep); 
		int[] haloVals = DijTensor.getWorkingDimValues(tensorForm, haloSize); 
		// Auxiliary variable that is only needed to run the method. Its value will 
		// not be used
		int[] dimValue = new int[min.length]; 
		String[] dim = DijTensor.getWorkingDims(tensorForm);

		String optimalPatch = ArrayOperations.optimalPatch(imp, dimValue, haloVals, dim, step, min, parent.getDeepPlugin().params.allowPatching);
		
		sizeTxt.setText(optimalPatch);
		int auxFixed = 0;
		for (int ss : step)
			auxFixed += ss;

		sizeTxt.setEditable(true);
		if (!parent.getDeepPlugin().params.allowPatching || parent.getDeepPlugin().params.pyramidalNetwork || auxFixed == 0) {
			sizeTxt.setEditable(false);
		}
	}
	
	/*
	 * Check patch size introduced by the user complies with the parameters previously
	 * entered to define the model
	 */
	public static boolean checkInputTileSize(int[] tileSize, String tensorName, Parameters params) {
		DijTensor inpTensor = DijTensor.retrieveByName(tensorName, params.inputList);
		String[] form = inpTensor.form.split("");
		// TODO generalise error messages for several input images
		// Check that the input  fulfils the conditions
		for (int i = 0; i < tileSize.length; i ++) {
			int step = inpTensor.step[i];
			int min = inpTensor.minimum_size[i];
			int pp = tileSize[i];
			if (step == 0 && min != pp) {
				IJ.error("           INCORRECT INPUT TILE SIZE           \n"
						   + "The size for dimension " + form[i] + " should be\n"
				   		   + "equal to " + min + " and it is instead set to " + pp);
						return false;
			} else if (params.allowPatching && step != 0 && (pp - min) % step != 0) {
				double n = Math.floor(((double)(pp - min)) / ((double) step));
				double sugest = n * step + min;
				IJ.error("           INCORRECT INPUT TILE SIZE           \n"
					   + "Every dimension of the tile size introduced must\n"
					   + "be the result of:\n"
					   + " minimum_size + step_size x N, where N is any\n"
					   + "posititive integer."
					   + "This condition is not fulfiled at dimension " + form[i] + "(" + pp + ").\n"
				   	   + "The immediately smaller value that fulfils the\n"
				   	   + "necessary condition is " + sugest);
					return false;
			} else if (params.allowPatching && pp <= 0) {
				IJ.error("        INCORRECT INPUT TILE SIZE        \n"
					   + "Every dimension of the tile size introduced"
					   + "must be strictly bigger than 0.\n"
					   + "Tile size at '" + form[i] + "' is " + pp);
				return false;
			}
		}
		
		// Now check that the output makes sense
		if (params.pyramidalNetwork)
			return true;
		for (DijTensor outTensor : params.outputList) {
			if (!outTensor.tensorType.contains("image"))
				continue;
			String[] outForm = outTensor.form.split("");
			int[] outShape = outTensor.tensor_shape;
			int[] halo = outTensor.halo;
			int[] offset = outTensor.offset;
			float[] scale = outTensor.scale;
			for ( int i = 0; i < outForm.length; i ++) {
				// Find which dimension corresponds to the current output dimension
				int ind = inpTensor.form.indexOf(outForm[i]);
				if (ind == -1)
					continue;
				int outSize = 0;
				//  Check that the input to the model is not automatically calculated by
				// the plugin. If it is, we cannot make sure anything.
				if (params.allowPatching || inpTensor.step[ind] == 0)
					outSize = ((int) (Math.round(((double)tileSize[ind]) * scale[i])) - 2 * offset[i]);
				if ((params.allowPatching || inpTensor.step[ind] == 0) && outShape[i] != -1 && outSize != outShape[i]) {
					// Check that with the given parameters, the input size gives the 
					// output size specified by the model
					IJ.error("         INCORRECT INPUT TILE SIZE         \n"
						   + "The output size for this model at dimension '" + outForm[i]+ "'\n"
				   		   + "is specified to be " + outShape[i] + ". Applying the\n"
		   		   		   + "scaling and offset specified for dimension '" + outForm[i]+ "'\n"
   		   		   		   + "considering an input size of " + tileSize[ind] + " yields an\n"
	   		   		   	   + "incorrect output size of " + outSize + ". Please, correct these parameters.");
					return false;
				} else if ((params.allowPatching || inpTensor.step[ind] == 0) && outSize <= 0){
					// Check that taking into account halo and offset
					// the output produced is bigger than 0
					IJ.error("        INCORRECT INPUT TILE SIZE        \n"
						   + "Applying the scaling and offset for\n"
						   + "output '" + outTensor.name + "' at dimension '" + outForm[i] + "' the\n"
					   	   + "resulting output size was " + outSize + " which is\n"
					   	   + "smaller than 0. The output size cannot\n"
					   	   + "be negative. Please, correct these parameters.");
					return false;
				} else if (2 * halo[i] > outSize) {
					// The size of the halo is too big for the chosen tile size
					IJ.error("        INCORRECT INPUT TILE SIZE        \n"
							   + "Applying the scaling, offset and halo for\n"
							   + "output '" + outTensor.name + "' at dimension '" + outForm[i] + "' the\n"
						   	   + "resulting output size was " + (outSize - 2 * halo[i]) + " which is\n"
						   	   + "smaller than 0. The output size cannot\n"
						   	   + "be negative. Please, correct these parameters.");
						return false;
				}
			}
		}
		return true;
	}

	@Override
	/*
	 * Update the JComboBox list when it is clicked
	 */
	public void mouseClicked(MouseEvent e) {
		// Check for clicks on the arrow
		for (int i = 0; i < btnList.size(); i ++) {
			if (e.getSource() == btnList.get(i)) {
				String[] titlesList = WindowManager.getImageTitles();

				cmbList.get(0).removeActionListener(this);
				cmbList.get(0).removeAllItems();
				// Update the list of options provided by each of 
				// the images
				for (int j = 0; j < cmbList.size(); j ++) {
					//cmbList.get(j).removeAllItems();
					if (titlesList.length != 0) {
						for (String title : titlesList)
							cmbList.get(j).addItem(title);
						bnTest.setEnabled(true);
					} else {
						cmbList.get(j).addItem("No image");
						bnTest.setEnabled(false);
					}	
				}
				cmbList.get(0).addActionListener(this);
				
				break;
			}
		}
		
		// Check for clicks on the text field
		for (int i = 0; i < cmbList.size(); i ++) {
			if (e.getSource() == cmbList.get(i)) {
				String[] titlesList = WindowManager.getImageTitles();

				cmbList.get(0).removeActionListener(this);
				cmbList.get(0).removeAllItems();
				// Update the list of options provided by each of 
				// the images
				for (int j = 0; j < cmbList.size(); j ++) {
					//cmbList.get(j).removeAllItems();
					if (titlesList.length != 0) {
						for (String title : titlesList)
							cmbList.get(j).addItem(title);
						bnTest.setEnabled(true);
					} else {
						cmbList.get(j).addItem("No image");
						bnTest.setEnabled(false);
					}	
				}
				cmbList.get(0).addActionListener(this);
				
				break;
			}
		}
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// Not necessary for our use case
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// Not necessary for our use case
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// Not necessary for our use case
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// Not necessary for our use case
		
	}
}

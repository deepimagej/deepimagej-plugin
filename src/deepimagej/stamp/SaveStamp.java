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
package deepimagej.stamp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JLabel;
import javax.swing.JTextField;

import com.google.protobuf.InvalidProtocolBufferException;

import additionaluserinterface.GridPanel;
import deepimagej.Runner;
import deepimagej.RunnerProgress;
import deepimagej.components.HTMLPane;
import deepimagej.exceptions.IncorrectChannelsNumber;
import deepimagej.exceptions.MacrosError;
import deepimagej.Log;
import deepimagej.BuildDialog;
import deepimagej.Parameters;
import deepimagej.DeepPlugin;
import deepimagej.tools.XmlUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;

public class SaveStamp extends AbstractStamp implements Runnable {

	private JTextField txtPathSave = new JTextField();
	private JTextField txtFileSave = new JTextField("ready2upload");
	private HTMLPane list = new HTMLPane();
	private Thread thread = null;
	private Log	log	= new Log();
	private DeepPlugin dp;
	
	public SaveStamp(Parameters params, BuildDialog parent, String title, Log log) {
		super(params, parent, title);
		buildPanel();
		this.log = log;
	}
	
	public void buildPanel() {
		list.append("give here the list of files to put into the ZIP");
		txtPathSave.setEditable(true);
		txtFileSave.setEditable(true);
		GridPanel pn = new GridPanel(false);
		pn.place(0, 0, 2, 1, list);
		pn.place(1, 0, new JLabel("Path"));
		pn.place(1, 1, txtPathSave);
		pn.place(2, 0, new JLabel("File"));
		pn.place(2, 1, txtFileSave);
		panel.add(pn);
	}

	public void validate(DeepPlugin dp) {
		if (params.testResultImage == null) {
			IJ.error("No result was obtained in the test");
		} else {
			params.saveDir = txtPathSave.getText();
			File dir = new File(params.saveDir);
			if (params.saveDir.isEmpty() == true || dir.isDirectory() == false) {
				IJ.error("Introduce a valid directory");
			} else {
				//params.directory = params.saveDir;
				params.saveFilename = txtFileSave.getText();
				try {
					// The following method creates the user defined folder and copies the model files to it
					createFolder();
					// The following method copies the preprocessing macros file to the folder
					saveFile();
					// The following method creates the config.xml with the needed parameters.
					//XmlUtils.writeXml(params);
					XmlUtils.writeXml(dp);
					// Finally copy the template images
					IJ.saveAsTiff(params.testImageBackup, params.saveDir + File.separator + params.saveFilename + File.separator + "exampleImage.tiff");
					IJ.saveAsTiff(params.testResultImage, params.saveDir + File.separator + params.saveFilename + File.separator + "resultImage.tiff");
				} catch(Exception ex) {
					IJ.error("Impossible to save the ZIP file");
				} 
			}
		}

	}
	
	public ImagePlus duplicateImage(ImagePlus img) {
		
		ImageProcessor ip = img.getProcessor();
		ImagePlus result_image = IJ.createHyperStack(img.getTitle(), img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), img.getBitDepth());
		result_image.setProcessor(ip);
		return result_image;
	}
	
	public ImagePlus runPreprocessingMacro(ImagePlus img) throws MacrosError, IOException {
		WindowManager.setTempCurrentImage(img);
		if (params.postmacro.equals("") == false) {
			String result = IJ.runMacro(params.premacro);
			if (result == "[aborted]") {
				throw new MacrosError();
			}
		}
		ImagePlus result = WindowManager.getCurrentImage();
		return result;
	}
	public ImagePlus runPostprocessingMacro(ImagePlus img){
		WindowManager.setTempCurrentImage(img);
		if (params.postmacro.equals("") == false) {
			String result = IJ.runMacro(params.postmacro);
			if (result == "[aborted]") {
				IJ.error("The postprocessing macros did not work.\n"
						+ "The image displayed is the raw output.");
			}
		}
		ImagePlus result = WindowManager.getCurrentImage();
		return result;
	}
	
	
	public  ImagePlus test(DeepPlugin dp1) {
		dp = dp1;
		// Initialise testImage, so it does not use data from previous tests
		params.testImage =  null;
		try {
			params.testImage = WindowManager.getCurrentImage();
			if (params.testImage == null) {
				throw new IOException();
			}
				// Duplicate the image so in case the model fails after
				// applying the macro, the origina image is the one shown
				params.testImageBackup = duplicateImage(params.testImage);
				params.testImage = runPreprocessingMacro(params.testImage);
				// We select params.input_form[0] because the code is tried to be prepared 
				// for when there are several inputs and outputs admitted.
				params.channels = DeepPlugin.nChannels(params, params.inputForm[0]);
				int imageChannels = params.testImage.getNChannels();
				if (params.channels.equals(Integer.toString(imageChannels)) != true) {
					throw new IncorrectChannelsNumber(Integer.parseInt(params.channels),
													  imageChannels);
				}
				dp.params = params;
				
				if (thread == null) {
					thread = new Thread(this);
					thread.setPriority(Thread.MIN_PRIORITY);
					thread.start();
				}
				thread = null;
		} catch (MacrosError e1) {
			System.out.print("Error in the Macro's code");
			// Go to the stamp where the macros are programmed
			params.card = 5;
			IJ.error("Failed preprocessing");
		
		} catch (IllegalArgumentException e1) {
			IJ.error("The model failed to execute because "
					+ "at some point \nthe size of the activations map was incorrect");
			e1.printStackTrace();
			
		} catch (InvalidProtocolBufferException e) {
			System.out.println("Impossible to load model");
			e.printStackTrace();
		} catch (IOException e){
			IJ.error("There is no image open in ImageJ");
		} catch (UnsupportedOperationException e){
			IJ.error("The model could not be executed properly. Try with another parameters.\n"
					+ "The given parameters are:\n"
					+ modelParameters());
		} catch (IncorrectChannelsNumber e) {
			IJ.error("The number of channels of the iput image is incorrect.");
		}
		
		
		return params.testResultImage;
		
	}
	
	public void run() {
		log.print("start runner");
		RunnerProgress rp = new RunnerProgress(dp);
		Runner runner = new Runner(dp, rp, log);
		rp.setRunner(runner);
		params.testResultImage = runner.call();
		// Flag to apply post processing if needed
		if (params.testResultImage != null) {
			params.testResultImage = runPostprocessingMacro(params.testResultImage);
			params.testResultImage.setSlice(1);
			params.testResultImage.getProcessor().resetMinAndMax(); 
		} else {
			IJ.error("The execution of the model failed.");
		}
	}
	
	public void saveFile() {
		try {
			PrintWriter preprocessing = new PrintWriter(params.saveDir + File.separator + params.saveFilename + File.separator + "preprocessing.txt", "UTF-8");
			preprocessing.println(params.premacro);
			preprocessing.close();
			PrintWriter postprocessing = new PrintWriter(params.saveDir + File.separator + params.saveFilename + File.separator + "postprocessing.txt", "UTF-8");
			postprocessing.println(params.postmacro);
			postprocessing.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public void createFolder() {
		// This method creates the final output folder and copies inside the 
		// particular TensorFlow model
		
		File dir = new File(params.saveDir + File.separator + params.saveFilename);
		dir.mkdir();
		File source_archi = new File(params.path2Model + File.separator + "saved_model.pb");
		File dest_archi = new File(params.saveDir + File.separator + params.saveFilename + File.separator + "saved_model.pb");
		File source_weights = new File(params.path2Model + File.separator + "variables");
		File dest_weights = new File(params.saveDir + File.separator + params.saveFilename + File.separator + "variables");
		
		try {
			copyFile(source_archi, dest_archi);
			copyWeights(source_weights, dest_weights);

		} catch (IOException e) {
		    e.printStackTrace();
		    IJ.error("Unable to copy the model files\n to the"
		    		+ "desired destination folder");
		}
	}
	
	private static void copyWeights(File source, File dest) throws IOException {
		// Copies all the files inside the source directory to the 
		// dest directory
		String source_path;
		String dest_path;
		String filename;
		File[] n_files = source.listFiles();
		for (int i = 0; i < n_files.length; i++) {
			if (n_files[i].isFile() == true) {
				filename = n_files[i].getName();
				source_path = source.getAbsolutePath() + File.separator + filename;
				dest_path = dest.getAbsolutePath() + File.separator + filename;
				copyFile(new File(source_path), new File(dest_path));
			}
		}
	}
	
	
	private static void copyFile(File sourceFile, File destFile)
	        throws IOException {
		if (!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdir();
		}
	    if (!sourceFile.exists()) {
	        return;
	    }
	    if (!destFile.exists()) {
	        destFile.createNewFile();
	    }
	    FileChannel source = null;
	    FileChannel destination = null;
	    source = new FileInputStream(sourceFile).getChannel();
	    destination = new FileOutputStream(destFile).getChannel();
	    if (destination != null && source != null) {
	        destination.transferFrom(source, 0, source.size());
	    }
	    if (source != null) {
	        source.close();
	    }
	    if (destination != null) {
	        destination.close();
	    }

	}
	
	public String modelParameters() {
		// Creates a string summing up the parameters used for the model.
		// This string is printed as an error message when the model cannot run.
		String[] input_form = params.inputForm[0].split("");
		int[] image_dims = params.testImage.getDimensions();
		String message = "Input tensor dimensions:";
		for (int i = 0; i < input_form.length; i ++) {
			message = message + " " + input_form[i] + ":" + params.inDimensions[i];
		}
		message = message + "\n Input image dimensions: " + "W: " + image_dims[0] + ", "
				+ "H: " + image_dims[1] + ", " + "C: " + image_dims[2];
		message = message + "\nPatch size: " + params.patch + " Overlap: " + params.overlap;
		return message;
	}
}

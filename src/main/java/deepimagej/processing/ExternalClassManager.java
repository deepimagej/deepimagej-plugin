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

package deepimagej.processing;

import java.io.File;
import java.util.*;

import deepimagej.Parameters;
import deepimagej.exceptions.JavaProcessingError;
import ij.IJ;

public class ExternalClassManager {
	
	/*
	 * The directory where we keep the plugin classes
	 */
	String processingDir;
	
	/* 
	 * Choose which method implemented in the interface has to e used
	 * to obtain the tensor. The main difference is that one admits an
	 * image, and the other does not admit anything
	 */
	boolean useImage;

	/* 
	 * Class that implements the interface of interest.
	 * In this case the interface of interest is the one that implements
	 * methods to preprocess or postprocess ImageJ ImagePlus
	 */
	PreProcessingInterface preProcessingClass;
	PostProcessingInterface postProcessingClass;

	/* 
	 * Booelan to identify if the interface the code needs to find is either 
	 * the preprocessing interface or the postprocessing interface
	 */
	boolean preprocessing;

	public ExternalClassManager (String jarDir, boolean preProc, Parameters params) {
		String interfaceOfInterest = "ProcessingInterface";
		processingDir = jarDir;
		preprocessing = preProc;
		//System.setSecurityManager(new PluginSecurityManager(processingDir));
		if (jarDir.contains(".jar") && (preProc == true)) {
			preProcessingClass = LoadJar.loadPreProcessingInterface(jarDir, params);
		} else if (jarDir.contains(".jar") && (preProc == false)) {
			postProcessingClass = LoadJar.loadPostProcessingInterface(jarDir, params);
		} else {
			getClasses(interfaceOfInterest, params);
		}
	}

	protected void getClasses(String interfaceOfInterest, Parameters params) {
		File dir = new File(processingDir);
		List<String> fileList = new ArrayList<String>();
		if (processingDir.contains(".class")) {
			fileList.add(new File(processingDir).getName());
		} else {
			fileList = listFilesForFolder(dir, fileList, processingDir.length());
		}
		ClassLoader cl = new PluginClassLoader(dir);
		for (String file : fileList) {
			try {
				// only consider files ending in ".class"
				if (! file.endsWith(".class"))
					continue;
				
				String className = file.substring(0, file.indexOf("."));
				className = className.replace(File.separator, ".");
				Class c = cl.loadClass(className);
				Class[] intf = c.getInterfaces();
				for (int j=0; j<intf.length; j++) {
					if (interfaceOfInterest.contains("ProcessingInterface") && intf[j].getName().contains(interfaceOfInterest)){
						if (preprocessing == true) {
							// the following line assumes that PluginFunction has a no-argument constructor
							PreProcessingInterface pf = (PreProcessingInterface) c.newInstance();
							preProcessingClass = pf;
							params.javaPreprocessingClass.add(className);
							continue;
						} else {
							// the following line assumes that PluginFunction has a no-argument constructor
							PostProcessingInterface pf = (PostProcessingInterface) c.newInstance();
							postProcessingClass = pf;
							params.javaPostprocessingClass.add(className);
							continue;
						}
					}
				}
			} catch (Exception ex) {
				System.err.println("File " + file + " does not contain a valid PluginFunction class.");
			}
		}
	}
	
	public List<String> listFilesForFolder(final File folder, List<String> fileList, int relPathLength) {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            fileList = listFilesForFolder(fileEntry, fileList, relPathLength);
	        } else {
	        	fileList.add(fileEntry.getPath().substring(relPathLength + 1));
	        }
	    }
	    return fileList;
	}

	public HashMap<String, Object> javaPreprocess(HashMap<String, Object> map, String path2config) throws JavaProcessingError {
		try {
			preProcessingClass.setConfigFile(path2config);
			map = preProcessingClass.deepimagejPreprocessing(map);
			if (!preProcessingClass.error().contentEquals("")) {
				IJ.log(preProcessingClass.error());
				throw new JavaProcessingError(preProcessingClass.error());
			}
		} catch (SecurityException secEx) {
			secEx.printStackTrace();
			System.err.println("Procesing plugin tried to do something illegal");
			throw new JavaProcessingError(preProcessingClass.error());
		} catch (AbstractMethodError ex) {
			ex.printStackTrace();
			IJ.log("Error in the Java preprocessing class");
			IJ.log("Exception " + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				IJ.log(ste.getClassName());
				IJ.log(ste.getMethodName());
				IJ.log("line:" + ste.getLineNumber());
			}
			IJ.log("Exception " + ex.getMessage());
			throw new JavaProcessingError(preProcessingClass.error());
		} catch (Exception ex) {
			ex.printStackTrace();
			IJ.log("Error in the Java preprocessing class");
			IJ.log("Exception " + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				IJ.log(ste.getClassName());
				IJ.log(ste.getMethodName());
				IJ.log("line:" + ste.getLineNumber());
			}
			IJ.log("Exception " + ex.getMessage());
			throw new JavaProcessingError(preProcessingClass.error());
		}
		return map;
	}

	public HashMap<String, Object> javaPostprocess(HashMap<String, Object> map, String path2config) throws JavaProcessingError {
		try {
			postProcessingClass.setConfigFile(path2config);
			postProcessingClass.deepimagejPostprocessing(map);
			if (!postProcessingClass.error().contentEquals("")) {
				IJ.log(postProcessingClass.error());
				throw new JavaProcessingError(postProcessingClass.error());
			}
		} catch (SecurityException secEx) {
			secEx.printStackTrace();
			System.err.println("Procesing plugin tried to do something illegal");
			throw new JavaProcessingError(postProcessingClass.error());
		} catch (AbstractMethodError ex) {
			ex.printStackTrace();
			IJ.log("Error in the Java preprocessing class");
			IJ.log("Exception " + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				IJ.log(ste.getClassName());
				IJ.log(ste.getMethodName());
				IJ.log("line:" + ste.getLineNumber());
			}
			IJ.log("Exception " + ex.getMessage());
			throw new JavaProcessingError(postProcessingClass.error());
		}
		return map;
	}
}


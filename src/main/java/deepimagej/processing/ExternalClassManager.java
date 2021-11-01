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

package deepimagej.processing;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import deepimagej.Parameters;
import deepimagej.exceptions.JavaProcessingError;
import deepimagej.tools.SystemUsage;
import ij.IJ;

public class ExternalClassManager {
	
	/*
	 * URLClassLoader that contains all the classes needed to run the pre- or post-processing 
	 */
	URLClassLoader processingClassLoader;
	
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

	public ExternalClassManager (String jarDir, boolean preProc, Parameters params) throws JavaProcessingError {
		processingDir = jarDir;
		preprocessing = preProc;
		// Create a class loader with the wanted dependencies
		/*
		 *  TODO remove if dependencies should be installed by the user 
     	 *  createProcessingClassLoader(params);
		 */
		// In order to run pre- and post-processing, the class has to be loaded on a ClassLoader
		// that includes the basic dependencies (DeepImageJ.jar, tensorflow.jar, api.jar, etc)
		// There are two options: creating a new ClassLoader and loading all the required dependencies,
		// which would be useful if the model was packed with the jar dependencies for running the 
		// processing. The other option is using the class loader that has all the classes laoded already. In
		// this option we rely that the user has all the needed dependencies installed correctly
		// in the jars folder.
		if (SystemUsage.checkFiji())
			processingClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		else 
			processingClassLoader = (URLClassLoader) IJ.getClassLoader();
		createProcessingClassLoader(params);
		if (jarDir.contains(".jar") && (preProc == true)) {
			processingClassLoader = LoadJar.loadSingleJarToExistingURLClassLoader(jarDir, processingClassLoader);
			preProcessingClass = LoadJar.loadPreProcessingInterface(jarDir, params, processingClassLoader);
		} else if (jarDir.contains(".jar") && (preProc == false)) {
			processingClassLoader = LoadJar.loadSingleJarToExistingURLClassLoader(jarDir, processingClassLoader);
			postProcessingClass = LoadJar.loadPostProcessingInterface(jarDir, params, processingClassLoader);
		} else {
			processingClassLoader = LoadJar.loadClassFileIntoURLClassLoader(jarDir, new File(jarDir).getParent(), processingClassLoader);
			getInterface(jarDir, params);
		}
	}
	
	/**
	 * Find if the class provided contains the needed interface
	 * @param classFile: file containing the external class
	 * @param params: model parameters
	 * @throws JavaProcessingError 
	 */
	protected void getInterface(String classFile, Parameters params) throws JavaProcessingError {
		Class c;
		JavaProcessingError ex = new JavaProcessingError();
		String className = "";
		try {
			String file = new File(classFile).getName();
			className = file.substring(0, file.indexOf("."));
			className = className.replace(File.separator, ".");
			c = processingClassLoader.loadClass(className);
			Class[] intf = c.getInterfaces();
			for (int j=0; j<intf.length; j++) {
				if (intf[j].getName().contains("PreProcessingInterface") || intf[j].getName().contains("PostProcessingInterface")){
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
		} catch (ClassNotFoundException e) {
			ex.setJavaError("Cannot load Java class " + className + " from file " + new File(classFile).getName() + ".\n"
					+ "Check that the file exists and that ImageJ/Fiji has the permissions to open the it.");
			e.printStackTrace();
		} catch (InstantiationException e) {
			ex.setJavaError("Cannot load Java class dinamically from file " + new File(classFile).getName() + ".\n"
					+ "Check that the file exists and that ImageJ/Fiji has the permissions to open the it.");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			ex.setJavaError("Cannot load Java class dinamically from file " + new File(classFile).getName() + ".\n"
					+ "Check that the file exists and that ImageJ/Fiji has the permissions to open the it.");
			e.printStackTrace();
		}
		throw ex;
	}
	
	/**
	 * Method that creates classloader with the needed Java dependencies
	 * @param params: model parameters, contains dependencies to build the path
	 */
	private void createProcessingClassLoader(Parameters params) {
		URL[] urls = new URL[params.attachments.size()];
		int c = 0;
		for (String url : params.attachments) {
			try {
				urls[c ++] = new File(url).toURI().toURL();
			} catch (MalformedURLException e) {
				IJ.log("Cannot find file: " + url);
				e.printStackTrace();
			}
		}
		processingClassLoader = new URLClassLoader(urls, processingClassLoader);
	}

	public HashMap<String, Object> javaPreprocess(HashMap<String, Object> map, ArrayList<String> config) throws JavaProcessingError {
		try {
			preProcessingClass.setConfigFiles(config);
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

	public HashMap<String, Object> javaPostprocess(HashMap<String, Object> map, ArrayList<String> config) throws JavaProcessingError {
		try {
			postProcessingClass.setConfigFiles(config);
			map = postProcessingClass.deepimagejPostprocessing(map);
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


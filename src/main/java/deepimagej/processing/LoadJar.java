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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


import deepimagej.Parameters;

public class LoadJar {
	
	public static URLClassLoader loadClassFileIntoURLClassLoader(String classFilePath, String dir,
																URLClassLoader urlClassLoader) throws NoSuchMethodException,
																SecurityException, IllegalAccessException, IllegalArgumentException,
																InvocationTargetException, IOException {
		// Obtain the class name from the .class file
		String className = classFilePath.substring(dir.length());
		if (className.indexOf(File.separator) == 0) {
			className = className.substring(1);
		}
		className = className.substring(0, className.lastIndexOf("."));
		className = className.replace(File.separator, ".");
		// Check if the class is already loaded
		// As these methods are protected, the methods have to be called using reflect
        Method method = URLClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
		method.setAccessible(true);
		Class c = (Class) method.invoke(urlClassLoader, className);
		// If it was not load, try to load it as a system class.
		// Again use reflect
		if (c == null) {
			try {
				method = URLClassLoader.class.getDeclaredMethod("findSystemClass", String.class);
				method.setAccessible(true);
				c = (Class) method.invoke(urlClassLoader, className);
			} catch(Exception ex) {
			}
		}
		// If the class is not loaded yet. Load directly from the class file
		if (c == null) {
			File f = new File(classFilePath);
			// Get the length of the class file, allocate an array of bytes for
	        // it, and read it in all at once.
	        int length = (int) f.length();
	        byte[] classbytes = new byte[length];
	        DataInputStream in = new DataInputStream(new FileInputStream(f));
	        in.readFully(classbytes);
	        in.close();
	        // Now call an inherited method to convert those bytes into a Class
	        // Again call it from reflect
			method = URLClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, Integer.class, Integer.class);
			method.setAccessible(true);
			c = (Class) method.invoke(urlClassLoader, className, classbytes, 0, length);
		}
		return urlClassLoader;
	}
	
	/**
	 * Adds Jar file classes to an already existing URLClassLoder
	 * @param jar: path to the file to be added
	 * @param urlClassLoader: already existing class loader
	 * @return URLClassLoader with the new jar added
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws MalformedURLException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public static URLClassLoader loadSingleJarToExistingURLClassLoader(String jar, URLClassLoader urlClassLoader) throws NoSuchMethodException, SecurityException, MalformedURLException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		URL url = new File(jar).toURI().toURL();

		Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
		method.setAccessible(true);
		method.invoke(urlClassLoader, url);
		return urlClassLoader;
	}
	
	public static PostProcessingInterface loadPostProcessingInterface(String jar, Parameters params, URLClassLoader urlClassLoader) {
		PostProcessingInterface pf = null;
		 // Load all the classes from the pre-processing file and look for the wanted interface
        ZipFile jarFile;
		try {
			jarFile = new ZipFile(jar);
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements() && pf == null) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String file = entry.getName();
                if (file.endsWith(".class")) {
                	String className = file.substring(0, file.indexOf("."));
    				className = className.replace("/", ".");
                	Class<?> c = urlClassLoader.loadClass(className);
                	Class[] intf = c.getInterfaces();
                	for (int j=0; j<intf.length; j++) {
    					if (intf[j].getName().contains("PostProcessingInterface")){
							// the following line assumes that PluginFunction has a no-argument constructor
							pf = (PostProcessingInterface) c.newInstance();
							params.javaPostprocessingClass.add(className);
							continue;
    					}
    				}
                }
            }
            jarFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return pf;
	}
	
	public static PreProcessingInterface loadPreProcessingInterface(String jar, Parameters params, URLClassLoader urlClassLoader) {
		PreProcessingInterface pf = null;
		try {
	        ZipFile jarFile = new ZipFile(jar);
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements() && pf == null) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String file = entry.getName();
                if (file.endsWith(".class")) {
                	String className = file.substring(0, file.indexOf("."));
    				className = className.replace("/", ".");
                	Class<?> c = urlClassLoader.loadClass(className);
                	Class[] intf = c.getInterfaces();
                	for (int j=0; j<intf.length; j++) {
    					if (intf[j].getName().contains("PreProcessingInterface")){
							// the following line assumes that PluginFunction has a no-argument constructor
							pf = (PreProcessingInterface) c.newInstance();
							params.javaPreprocessingClass.add(className);
							continue;
						}
    				}
                }
            }
            jarFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return pf;
	}
		
}
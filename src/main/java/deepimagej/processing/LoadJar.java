package deepimagej.processing;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.scijava.Context;

import deepimagej.Parameters;

public class LoadJar {
	
	public static PostProcessingInterface loadPostProcessingInterface(String jar, Parameters params) {
		File f = new File(jar);
		PostProcessingInterface pf = null;
        // Add plugin directory to search path
		try {
	        URL url;
			url = f.toURI().toURL();
	        // Getting the jar URL which contains target class
	        URL[] classLoaderUrls = new URL[]{url};
	         
	        // Create a new URLClassLoader 
	        URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);
	         
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
    					if (intf[j].getName().contains("PostProcessingInterface")){
							// the following line assumes that PluginFunction has a no-argument constructor
							pf = (PostProcessingInterface) c.newInstance();
							params.javaPostprocessingClass.add(className);
							continue;
    					}
    				}
                }
            }
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pf;
	}
	
	public static PreProcessingInterface loadPreProcessingInterface(String jar, Parameters params) {
		File f = new File(jar);
        // Add plugin directory to search path
		PreProcessingInterface pf = null;
		try {
	        URL url;
			url = f.toURI().toURL();
	        // Getting the jar URL which contains target class
	        URL[] classLoaderUrls = new URL[]{url};
	         
	        // Create a new URLClassLoader 
	        URLClassLoader urlClassLoader = new URLClassLoader(classLoaderUrls);
	         
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
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pf;
	}
	
	public static void loadClasses() {
		URLClassLoader aa = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		URL[] originalClasses = aa.getURLs();
		List<URL> classesArray = Arrays.asList(originalClasses);
		String allClasses = originalClasses.toString();
		System.out.print(originalClasses.toString());
		for (File f : new File("C:\\Users\\Carlos(tfg)\\3D Objects\\source\\plugins").listFiles()) {
			String name = f.getName();
			if (!name.contains(".jar") || (!name.contains("scijava") && !name.contains("imagej")))
				continue;
	        // Add plugin directory to search path
	        URL url;
			try {
				url = f.toURI().toURL();
				originalClasses = new URL[classesArray.size() + 1];
				int i = 0;
				for (URL cc : classesArray)
					originalClasses[i ++] = cc;
				originalClasses[i] = url;
				classesArray = Arrays.asList(originalClasses);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}/*
		URL[] classLoaderUrls = new URL[classesArray.size()];
		for (int i = 0; i < classesArray.size(); i ++)
			classLoaderUrls[i] = classesArray.get(i);
			*/
		//URLClassLoader urlClassLoader = new URLClassLoader(originalClasses);
		URLClassLoader urlClassLoader = URLClassLoader.newInstance(originalClasses, aa.getParent());
		ClassLoader bb = urlClassLoader.getParent();
		Thread.currentThread().setContextClassLoader(bb);
	}
		
}
package deepimagej.processing;

import java.io.File;
import java.util.*;

import org.tensorflow.Tensor;

import deepimagej.tools.DijTensor;
import ij.ImagePlus;

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
	 * methods to produce a Tensorflow tensor
	 */
	CreateDIJTensorInterface createTensorClass;

	/* 
	 * Class that implements the interface of interest.
	 * In this case the interface of interest is the one that implements
	 * methods to produce a Tensorflow tensor
	 */
	ProcessingInterface processingClass;

	public ExternalClassManager (DijTensor tensor) {
		String interfaceOfInterest = "ProcessingInterface";
		processingDir = tensor.parameterPath;
		useImage = tensor.useImage;
		System.setSecurityManager(new PluginSecurityManager(processingDir));
		getClasses(interfaceOfInterest);
	}

	public ExternalClassManager (String jarDir, boolean aargumentIncludesImage) {
		String interfaceOfInterest = "CreateDIJTensorInterface";
		processingDir = jarDir;
		useImage = aargumentIncludesImage;
		System.setSecurityManager(new PluginSecurityManager(processingDir));
		getClasses(interfaceOfInterest);
	}

	protected void getClasses(String interfaceOfInterest) {
		File dir = new File(processingDir);
		List<String> fileList = listFilesForFolder(dir, new ArrayList<String>(), processingDir.length());
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
					if (interfaceOfInterest.contains("CreateDIJTensorInterface") && intf[j].getName().contains(interfaceOfInterest)) {
						// the following line assumes that PluginFunction has a no-argument constructor
						CreateDIJTensorInterface pf = (CreateDIJTensorInterface) c.newInstance();
						createTensorClass = pf;
						continue;
					} else if (interfaceOfInterest.contains("ProcessingInterface") && intf[j].getName().contains(interfaceOfInterest)){
						// the following line assumes that PluginFunction has a no-argument constructor
						ProcessingInterface pf = (ProcessingInterface) c.newInstance();
						processingClass = pf;
						continue;
					}
				}
			} catch (Exception ex) {
				System.err.println("File " + file + " does not contain a valid PluginFunction class.");
			}
		}
	}

	public ImagePlus javaProcessImage(ImagePlus im) {
		ImagePlus  result = null;
		ProcessingInterface pf = (ProcessingInterface) processingClass;
		try {
			if (useImage == true) {
				result = pf.processingRoutineUsingImage(im);
			} else {
				result = pf.processingRoutineWithoutImage();
			}
		} catch (SecurityException secEx) {
			System.err.println("plugin '"+pf.getClass().getName()+"' tried to do something illegal");
		}
		return result;
	}

	public Tensor<?> getTensorFromCode(ImagePlus im) {
		Tensor<?>  result = null;
		CreateDIJTensorInterface pf = (CreateDIJTensorInterface) createTensorClass;
		try {
			if (useImage == true) {
				result = pf.getParameterFromImage(im);
			} else {
				result = pf.getParameterWithoutImage();
			}
		} catch (SecurityException secEx) {
			System.err.println("plugin '"+pf.getClass().getName()+"' tried to do something illegal");
		}
		return result;
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
}


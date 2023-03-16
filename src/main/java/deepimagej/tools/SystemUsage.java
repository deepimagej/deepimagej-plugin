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

package deepimagej.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ij.IJ;

public class SystemUsage {
	
	private static String checkFiji = null;
	private static boolean fiji = false;

	/**
	 * HashMap containing the versions of CUDA compatible with each Pytorch versions
	 */
	public static final Map<String, List<String>> MAP_PYTORCH_CUDA = createCudaCompatiblePytorchMap();
	/**
	 * HashMap containing the versions of CUDA compatible with each Tensorflow versions
	 */
	public static final Map<String, List<String>> MAP_TF_CUDA = createCudaCompatibleTensorflowMap();
	/**
	 * HashMap containing the versions of CUDA compatible with each Onnx versions
	 */
	public static final Map<String, List<String>> MAP_ONNX_CUDA = createCudaCompatibleOnnxMap();

	public static String getMemoryMB() {
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		double c = 1.0 / (1024.0 * 1024.0);
		double heap = mem.getHeapMemoryUsage().getUsed() * c;
		return String.format("%6.1fMb ", heap);
	}

	public String getMemoryUsage() {
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		double c = 1.0 / (1024.0 * 1024.0);
		double heap = mem.getHeapMemoryUsage().getUsed() * c;
		double nonheap = mem.getNonHeapMemoryUsage().getUsed() * c;
		return String.format("Heap:%6.1fMb NonHeap:%6.1fMb ", heap, nonheap);
	}

	public static String getMemory() {
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		String heap = NumFormat.bytes(mem.getHeapMemoryUsage().getUsed());
		String max = NumFormat.bytes(mem.getHeapMemoryUsage().getMax());
		return heap + "/" + max;
	}
	
	public static String getMaxMemory() {
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		return NumFormat.bytes(mem.getHeapMemoryUsage().getMax());
	}


	public static String getCores() {
		String load = "" + Runtime.getRuntime().availableProcessors() + " cores";
		return load;
	}

	public static double getHeapUsed() {
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		return mem.getHeapMemoryUsage().getUsed();
	}

	public static double getNonHeapUsed() {
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		return mem.getNonHeapMemoryUsage().getUsed();
	}
	
	public static double[] getHeap() {
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		double u = mem.getHeapMemoryUsage().getUsed();
		double m = mem.getHeapMemoryUsage().getMax();
		double i = mem.getHeapMemoryUsage().getInit();
		return new double[] { i, u, m };
	}

	public static String getNonHeap() {
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		double c = 1.0 / (1024.0 * 1024.0);
		double u = mem.getNonHeapMemoryUsage().getUsed() * c;
		double m = mem.getNonHeapMemoryUsage().getMax() * c;
		double i = mem.getNonHeapMemoryUsage().getMax() * c;
		return String.format("u=%3.2f m=%3.2f i=%3.2f Mb", u, m, i);
	}

	public static double getLoad() {
		try {
			OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
			return os.getSystemLoadAverage();
		}
		catch (Exception ex) {
		}
		return 0;
	}
	
	public static double getAvailableSpace() {
		File[] roots = File.listRoots();
		for(File root : roots)
			return root.getUsableSpace();
		return 0;
	}

	public static double getTotalSpace() {
		File[] roots = File.listRoots();
		for(File root : roots)
			return root.getTotalSpace();
		return 0;
	}
	
	/*
	 * Compare nvidia-smi outputs before and after loading the model
	 * to check if the model has been loaded into the GPU
	 */
	public static String isUsingGPU(ArrayList<String> firstSmi, ArrayList<String> secondSmi) {
		String result = "noImageJProcess";
		if (firstSmi == null || secondSmi == null)
			return result;
		Object[] firstSmiArr = firstSmi.toArray();
		// If they are not the same, look for information that is not on the first smi
		// and where the process name contains "java"
		for (String info : secondSmi) {
			// TODO check if what happens when Imagej is called from Pyton, do we need to look for a python tag?
			if (Arrays.toString(firstSmiArr).contains(info) && info.toUpperCase().contains("IMAGEJ")) {
				// Use '¡RepeatedImageJGPU!' as marker for this option
				result += info + "¡RepeatedImageJGPU!";
			} else {
				return info;
			}
		}
		return result;
	}
	
	public static int numberOfImageJInstances() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win"))
			return numberOfImageJInstancesWin();
		else if (os.contains("lin") || os.contains("unix"))
			return numberOfImageJInstancesLinux();
		else if (os.contains("ios"))
			return 1;
		else 
			return 1;
	}
	
	/*
	 * Get the number of ImageJ instances open. The value will be used to deduce
	 * if the instance of interest is using a GPU or not. 
	 */
	public static int numberOfImageJInstancesLinux() {
		Process proc;
		int nIJInstances = 0;
		try {
			String line;
			proc = Runtime.getRuntime().exec("ps");

		    BufferedReader input =
		            new BufferedReader(new InputStreamReader(proc.getInputStream()));
		    while ((line = input.readLine()) != null) {
		        if (line.toUpperCase().contains("IMAGEJ"))
		        	nIJInstances += 1;
		    }
		    input.close();
		} catch (Exception err) {
		    err.printStackTrace();
		}
		return nIJInstances;
	}
	
	/*
	 * Get the number of ImageJ instances open. The value will be used to deduce
	 * if the instance of interest is using a GPU or not. 
	 */
	public static int numberOfImageJInstancesWin() {
		Process proc;
		int nIJInstances = 0;
		try {
			String line;
			proc = Runtime.getRuntime().exec(System.getenv("windir") +"\\system32\\"+"tasklist.exe");

		    BufferedReader input =
		            new BufferedReader(new InputStreamReader(proc.getInputStream()));
		    while ((line = input.readLine()) != null) {
		        if (line.toUpperCase().contains("IMAGEJ"))
		        	nIJInstances += 1;
		    }
		    input.close();
		} catch (Exception err) {
		    err.printStackTrace();
		}
		return nIJInstances;
	}
	
	/*
	 * Run commands in the terminal and retrieve the output in the terminal
	 * GPUs cannot run on ios operating systems
	 */
	public static ArrayList<String> runNvidiaSmi() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win"))
			return runNvidiaSmiWin();
		else if (os.contains("lin"))
			return runNvidiaSmiLinux();
		else if (os.contains("ios"))
			return null;
		else 
			return null;
	}
	
	/*
	 * Run commands in the terminal and retrieve the output in the terminal
	 */
	public static ArrayList<String> runNvidiaSmiWin() {
		return runNvidiaSmiWin("nvidia-smi", true);
	}
	
	/*
	 * Run commands in the terminal and retrieve the output in the terminal
	 */
	public static ArrayList<String> runNvidiaSmiLinux() {

        Process proc;
		try {
			proc = Runtime.getRuntime().exec("nvidia-smi");

	        // Read the output
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        ArrayList<String> result = new ArrayList<String>();
	        // Relevant information comes after the following header
	        String infoHeader = "|  GPU       PID   Type   Process name                             Usage      |";
	        boolean startCapturing = false;
        	String aux = reader.readLine();
	        while(aux != null) {
	        	if (startCapturing && aux != null)
		            result.add(aux);
	        	if (aux != null && aux.equals(infoHeader))
	        		startCapturing = true;
	        	aux = reader.readLine();
	        }

	        proc.waitFor(); 
	        return result;
		} catch (IOException | InterruptedException e) {
			return null;
		}  
	}
	
	/*
	 * Run commands in the terminal and retrieve the output in the terminal
	 */
	public static ArrayList<String> runNvidiaSmiWin(String command, boolean firstCall) {

        Process proc;
		try {
			proc = Runtime.getRuntime().exec(command);

	        // Read the output
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        ArrayList<String> result = new ArrayList<String>();
	        // Relevant information comes after the following header
	        String infoHeader = "|  GPU       PID   Type   Process name                             Usage      |";
	        boolean startCapturing = false;
        	String aux = reader.readLine();
	        while(aux != null) {
	        	if (startCapturing && aux != null)
		            result.add(aux);
	        	if (aux != null && aux.equals(infoHeader))
	        		startCapturing = true;
	        	aux = reader.readLine();
	        }

	        proc.waitFor(); 
	        return result;
		} catch (IOException | InterruptedException e) {
			// Not able to run terminal command. Look for the nvidia-smi.exe
			// in another location. If it is not found, we cannot know 
			// if we are using GPU or not
			if (firstCall) {
				String nvidiaSmi = findNvidiaSmiWin();
				if (nvidiaSmi != null)
					return runNvidiaSmiWin(nvidiaSmi, false);
			} else {
				return null;
			}
		}  
		return null;
	}
	
	/*
	 * Look for nvidia-smi in its default location:
	 *  - C:\Windows\System32\DriverStore\FileRepository\nv*\nvidia-smi.exe
	 * Older installs might have it at:
	 *  - C:\Program Files\NVIDIA Corporation\NVSMI
	 */
	private static String findNvidiaSmiWin() {
		// Look in the default directory
		File grandfatherDir = new File("C:\\Windows\\System32\\DriverStore\\FileRepository");
		if (!grandfatherDir.exists())
			return null;
		for (File f : grandfatherDir.listFiles()) {
			if (f.getName().indexOf("nv") == 0 && findNvidiaSmiExeWin(f))
				return f.getAbsolutePath() + File.separator + "nvidia-smi.exe";
		}
		// Look inside the default directory in old versions
		grandfatherDir = new File("C:\\Program Files\\NVIDIA Corporation\\NVSMI");
		if (!grandfatherDir.exists())
			return null;
		for (File f : grandfatherDir.listFiles()) {
			if (f.getName().equals("nvidia-smi.exe"))
				return f.getAbsolutePath();
		}
		return null;
	}
	
	/*
	 * Look for the nvidia-smi executable in a given folder
	 */
	private static boolean findNvidiaSmiExeWin(File f) {
		for (File ff : f.listFiles()) {
			if (ff.getName().equals("nvidia-smi.exe"))
				return true;
		}
		return false;
	}

	/*
	 * Find enviromental variables corresponding to CUDA files.
	 * If they are present and correspond to the needed CUDA vesion
	 * for the installed TF or Pytorch version, it is possible that
	 * we are using a GPU
	 */
	public static String getCUDAEnvVariables() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win"))
			return getCUDAEnvVariablesWin();
		else if (os.contains("linux") || os.contains("unix"))
			return getCUDAEnvVariablesLinux();
		else if (os.contains("mac"))
			return "nocuda";
		else 
			return "nocuda";
	}

	/*
	 * Find enviromental variables corresponding to CUDA files.
	 * If they are present and correspond to the needed CUDA vesion
	 * for the installed TF or Pytorch version, it is possible that
	 * we are using a GPU
	 */
	public static String getCUDAEnvVariablesLinux() {
		ArrayList<String> nvccFiles = findNVCCFile();
		String nvccFilesString = nvccFiles.toString();
		ArrayList<String> cudaVersionFiles = findCudaVersionFile();
		String foundCudaVersions = "";
		for (String str : nvccFiles) {
			String version = findNVCCVersion(str);
			if (version != null && str.toLowerCase().contains("cuda"))
				foundCudaVersions += version + "---";
		}
		for (String str : cudaVersionFiles) {
			// First find if the parent directory does not correspond
			// to a directory already evaluated
			String parentDir = new File(str).getParent();
			if (!nvccFilesString.contains(parentDir + ",")) {
				String version = findVersionFromFile(str);
				// Add version if it was not found already
				if (version != null && str.toLowerCase().contains("cuda") && !foundCudaVersions.toString().contains(version))
					foundCudaVersions += version + "---";
			}
		}
		if (foundCudaVersions.equals(""))
			return "nocuda";
		else
			return foundCudaVersions;
	}
	
	/*
	 * In Linux, runs nvcc command to find CUDA version
	 */
	public static String findVersionFromFile(String command) {
		Process proc;
        String result = null;
		try {
			proc = Runtime.getRuntime().exec("cat " + command);
			/* 
			 * Output should look like this for CUDA 9.0
			  	CUDA Version 9.0.176

			 */

	        // Read the output
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        // Version information comes after the following header
	        String aux = reader.readLine();
	        while(aux != null) {
	        	if (aux != null && aux.toLowerCase().contains("cuda")) {
	        		aux = aux.split(" ")[2];
	        		result = aux.substring(0, aux.lastIndexOf("."));
	        	}
	        	aux = reader.readLine();
	        }

	        proc.waitFor(); 
		} catch( Exception ex) {
			
		}
		return result;
	}
	
	/*
	 * In Linux, runs nvcc command to find CUDA version
	 */
	public static String findNVCCVersion(String command) {
		Process proc;
        String result = null;
		try {
			proc = Runtime.getRuntime().exec(command + " --version");
			/* 
			 * Output should look like this for CUDA 9.0
			 * 	nvcc: NVIDIA (R) Cuda compiler driver
				Copyright (c) 2005-2017 NVIDIA Corporation
				Built on Fri_Sep__1_21:08:03_CDT_2017
				Cuda compilation tools, release 9.0, V9.0.176

			 */

	        // Read the output
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        // Version information comes after the following header
	        String infoHeader = "Cuda compilation tools, release ";
	        String aux = reader.readLine();
	        while(aux != null) {
	        	if (aux != null && aux.contains(infoHeader)) {
	        		result = aux.substring(aux.indexOf("V") + 1, aux.lastIndexOf("."));
	        	}
	        	aux = reader.readLine();
	        }

	        proc.waitFor(); 
		} catch( Exception ex) {
			
		}
		return result;
	}
	
	/*
	 * In Linux, find the file location of nvcc to check which CUDA versions
	 * are installed in the system
	 */
	public static ArrayList<String> findNVCCFile() {
		Process proc;
        ArrayList<String> installedVersions = new ArrayList<String>();
		try {
			proc = Runtime.getRuntime().exec("locate nvcc");

	        // Read the output
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        // Version information comes after the following header
	        String aux = reader.readLine();
	        while(aux != null) {
	        	if (aux != null && aux.endsWith(File.separator + "nvcc"))
	        		installedVersions.add(aux);
	        	aux = reader.readLine();
	        }

	        proc.waitFor(); 
		} catch( Exception ex) {
			
		}
		return installedVersions;
	}
	
	/*
	 * In Linux, find the file location of version.txt containing the CUDA version to check which CUDA versions
	 * are installed in the system
	 */
	public static ArrayList<String> findCudaVersionFile() {
		Process proc;
        ArrayList<String> installedVersions = new ArrayList<String>();
		try {
			proc = Runtime.getRuntime().exec("locate version.txt");

	        // Read the output
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        // Version information comes after the following header
	        String aux = reader.readLine();
	        while(aux != null) {
	        	if (aux != null && aux.toLowerCase().contains("cuda"))
	        		installedVersions.add(aux);
	        	aux = reader.readLine();
	        }

	        proc.waitFor(); 
		} catch( Exception ex) {
			
		}
		// Now look in the default directory where CUDA is installed
		// The default directory is '/usr/local'
		try {
			String[] defaultDirs = new String[]{"/usr/local"};
			for (String command : defaultDirs) {
				proc = Runtime.getRuntime().exec("ls " + command);
				
				String previouslyFound = installedVersions.toString();
		        // Read the output
		        BufferedReader reader =  
		              new BufferedReader(new InputStreamReader(proc.getInputStream()));
		        // Version information comes after the following header
		        String aux = reader.readLine();
		        while(aux != null) {
		        	String wholeStr = command + File.separator + aux + File.separator + "version.txt";
		        	if (aux != null && aux.toLowerCase().contains("cuda") && !previouslyFound.contains(wholeStr) && new File(wholeStr).exists()) {
		        		installedVersions.add(wholeStr);
		        	}
		        	aux = reader.readLine();
		        }
	
		        proc.waitFor(); 
			}
		} catch( Exception ex) {
			
		}
		
		return installedVersions;
	}

	/*
	 * Find enviromental variables corresponding to CUDA files.
	 * If they are present and correspond to the needed CUDA vesion
	 * for the installed TF or Pytorch version, it is possible that
	 * we are using a GPU
	 */
	public static String getCUDAEnvVariablesWin() {
		// Look for environment variable containing the path to CUDA
		String cudaPath = System.getenv("CUDA_PATH");
		if (cudaPath == null || !(new File(cudaPath).exists()))
			return "nocuda";
		String cudaVersion = new File(cudaPath).getName();
		String vars = System.getenv("path");
		String[] arrVars = vars.split(";");
		// Look for the other needed environment variables in the path
		// - CUDA_PATH + /bin
		// - CUDA_PATH + /libnvvp
		boolean bin = false;
		boolean libnvvp = false;
		for (String i : arrVars) {
			if (i.equals(cudaPath + File.separator + "bin"))
				bin = true;
			else if (i.equals(cudaPath + File.separator + "libnvvp"))
				libnvvp = true;
		}
		
		if (bin && libnvvp) {
			// If all the needed variables are set, return the CUDA version
			// In all possible cases return first the CUDA version found followed by ";"
			return cudaVersion;
		} else if (!bin && libnvvp) {
			// If bin is missing return 'bin'
			return cudaVersion + ";" + cudaPath + File.separator + "bin";
		} else if (bin && !libnvvp) {
			// If libnvvp is missing return 'libnvvp'
			return cudaVersion + ";" + cudaPath + File.separator + "libnvvp";
		} else {
			// If bin and libnvvp is missing return 'libnvvp' and 'bin' 
			// separated by ';'
			return cudaVersion + ";" + cudaPath + File.separator + "libnvvp" + ";" + cudaPath + File.separator + "bin";
		}
	}
	
	/*
	 * Check whether the plugin is running on an IJ1 or Fiji/IJ2 distribution
	 */
	public static boolean checkFiji() {
		if (checkFiji != null) {
			return fiji;
		}
		try {
			// Try loading the service 'net.imagej.ImageJService'. This service should
			// always load in IJ2/Fiji but not in IJ1
			ClassLoader cl = IJ.getClassLoader();
			Class<?> dijClass = cl.loadClass("net.imagej.ImageJService");
			fiji = true;
			checkFiji = "done";
			return fiji;
		} catch (Exception ex) {
			fiji = false;
			checkFiji = "done";
			return fiji;
		}
	}
	
	private static HashMap<String, List<String>> createCudaCompatiblePytorchMap(){
		HashMap<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1.7.1", Arrays.asList(new String[]{"CUDA 10.1", "CUDA 10.2", "CUDA 11.0"}));
		map.put("1.8.1", Arrays.asList(new String[]{"CUDA 10.2", "CUDA 11.1"}));
		map.put("1.9.0", Arrays.asList(new String[]{"CUDA 10.2", "CUDA 11.1"}));
		map.put("1.9.1", Arrays.asList(new String[]{"CUDA 10.2", "CUDA 11.1"}));
		map.put("1.10.0", Arrays.asList(new String[]{"CUDA 10.2", "CUDA 11.3"}));
		map.put("1.11.0", Arrays.asList(new String[]{"CUDA 10.2", "CUDA 11.3"}));
		map.put("1.12.1", Arrays.asList(new String[]{"CUDA 10.2", "CUDA 11.6"}));
		map.put("1.13.0", Arrays.asList(new String[]{"CUDA 11.7"}));
		return map;
	}
	
	private static HashMap<String, List<String>> createCudaCompatibleTensorflowMap(){
		HashMap<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("1.12.0", Arrays.asList(new String[]{"CUDA 9.0"}));
		map.put("1.13.0", Arrays.asList(new String[]{"CUDA 10.0"}));
		map.put("1.13.1", Arrays.asList(new String[]{"CUDA 10.0"}));
		map.put("1.14.0", Arrays.asList(new String[]{"CUDA 10.0"}));
		map.put("1.15.0", Arrays.asList(new String[]{"CUDA 10.0"}));
		map.put("2.0.0", Arrays.asList(new String[]{"CUDA 10.0"}));
		map.put("2.1.0", Arrays.asList(new String[]{"CUDA 10.1"}));
		map.put("2.2.0", Arrays.asList(new String[]{"CUDA 10.1"}));
		map.put("2.3.0", Arrays.asList(new String[]{"CUDA 10.1"}));
		map.put("2.3.1", Arrays.asList(new String[]{"CUDA 10.1"}));
		map.put("2.4.0", Arrays.asList(new String[]{"CUDA 11.0"}));
		map.put("2.4.1", Arrays.asList(new String[]{"CUDA 11.0"}));
		map.put("2.5.0", Arrays.asList(new String[]{"CUDA 11.2"}));
		map.put("2.6.0", Arrays.asList(new String[]{"CUDA 11.2"}));
		map.put("2.7.0", Arrays.asList(new String[]{"CUDA 11.2"}));
		map.put("2.7.1", Arrays.asList(new String[]{"CUDA 11.2"}));
		map.put("2.7.4", Arrays.asList(new String[]{"CUDA 11.2"}));
		return map;
	}
	
	private static HashMap<String, List<String>> createCudaCompatibleOnnxMap(){
		HashMap<String, List<String>> map = new HashMap<String, List<String>>();
		map.put("8", Arrays.asList(new String[]{"CUDA 10.1"}));
		map.put("9", Arrays.asList(new String[]{"CUDA 10.1"}));
		map.put("10", Arrays.asList(new String[]{"CUDA 10.2"}));
		map.put("11", Arrays.asList(new String[]{"CUDA 10.2"}));
		map.put("12", Arrays.asList(new String[]{"CUDA 11.0.3"}));
		map.put("13", Arrays.asList(new String[]{"CUDA 11.0.3"}));
		map.put("14", Arrays.asList(new String[]{"CUDA 11.4"}));
		map.put("15", Arrays.asList(new String[]{"CUDA 11.4"}));
		map.put("16", Arrays.asList(new String[]{"CUDA 11.4"}));
		map.put("17", Arrays.asList(new String[]{"CUDA 11.4"}));
		map.put("18", Arrays.asList(new String[]{"CUDA 11.6"}));
		return map;
	}
}

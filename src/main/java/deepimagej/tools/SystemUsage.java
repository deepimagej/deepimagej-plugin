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

public class SystemUsage {

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
		Object[] firstSmiArr = firstSmi.toArray();
		String result = "noImageJProcess";
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
		return runNvidiaSmiWin(true);
	}
	
	/*
	 * Run commands in the terminal and retrieve the output in the terminal
	 */
	public static ArrayList<String> runNvidiaSmiLinux() {
		return runNvidiaSmiLinux(true);
	}
	
	/*
	 * Run commands in the terminal and retrieve the output in the terminal
	 */
	public static ArrayList<String> runNvidiaSmiLinux(boolean firstCall) {

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
	        while(reader.readLine() != null) {
	        	if (startCapturing && reader.readLine() != null)
		            result.add(reader.readLine());
	        	String aux = reader.readLine();
	        	if (aux != null && aux.equals(infoHeader))
	        		startCapturing = true;
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
	public static ArrayList<String> runNvidiaSmiWin(boolean firstCall) {

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
	        while(reader.readLine() != null) {
	        	if (startCapturing && reader.readLine() != null)
		            result.add(reader.readLine());
	        	String aux = reader.readLine();
	        	if (aux != null && aux.equals(infoHeader))
	        		startCapturing = true;
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
					return runNvidiaSmiWin(false);
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
			return getCUDAEnvVariablesLinux(true);
		else if (os.contains("mac"))
			return "noCuda";
		else 
			return "noCuda";
	}

	/*
	 * Find enviromental variables corresponding to CUDA files.
	 * If they are present and correspond to the needed CUDA vesion
	 * for the installed TF or Pytorch version, it is possible that
	 * we are using a GPU
	 */
	public static String getCUDAEnvVariablesLinux(boolean firstRun) {
		Process proc;
		try {
			if (firstRun) {
				proc = Runtime.getRuntime().exec("/usr/local/cuda/bin/nvcc --version");
				/* 
				 * Output should look like this
				 * 	nvcc: NVIDIA (R) Cuda compiler driver
					Copyright (c) 2005-2017 NVIDIA Corporation
					Built on Fri_Sep__1_21:08:03_CDT_2017
					Cuda compilation tools, release 9.0, V9.0.176

				 */
			} else {
				proc = Runtime.getRuntime().exec("cat /usr/local/cuda/version.txt");
				/* 
				 * Output should look like this
				  	CUDA Version 9.0.176

				 */
			}

	        // Read the output
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        String result = "noCuda";
	        // Version information comes after the following header
	        String infoHeader = "CUDA compilation tools, release ";
	        while(reader.readLine() != null) {
	        	String aux = reader.readLine();
	        	if (aux != null && aux.contains(infoHeader) && firstRun) {
	        		result = aux.substring(aux.indexOf("V") + 1, aux.lastIndexOf("."));
	        	} else if (aux != null && aux.contains(infoHeader) && !firstRun) {
	        		aux = aux.split(" ")[2];
	        		result = aux.substring(0, aux.lastIndexOf("."));
	        	}
	        }

	        proc.waitFor(); 
	        if (result.equals("noCuda") && firstRun)
	        	result = getCUDAEnvVariablesLinux(false);
	        return result;
		} catch (Exception ex) {
			if (firstRun)
				return getCUDAEnvVariablesLinux(false);
			else
				return "noCuda";
		}
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
			return "noCuda";
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
}
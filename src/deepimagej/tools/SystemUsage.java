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
package deepimagej.tools;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;

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

}

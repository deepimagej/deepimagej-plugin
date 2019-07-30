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

package deepimagej.tools;

public class NumFormat {
	
	public static String chrono(double chrono) {
		return time(System.nanoTime() - chrono);
	}

	public static String seconds(double ns) {
		return String.format("%5.1f s", ns * 1e-9);
		
	}

	public static String time(double ns) {
		if (ns < 3000.0)
			return String.format("%3.1f ns", ns);
		ns *= 0.001;
		if (ns < 3000.0)
			return String.format("%3.1f us", ns);
		ns *= 0.001;
		if (ns < 3000.0)
			return String.format("%3.1f ms", ns);
		ns *= 0.001;
		if (ns < 3000.0)
			return String.format("%3.1f s", ns);
		ns *= 0.001;
		if (ns < 3000.0)
			return String.format("%3.1f s", ns);
		ns /= 3600.0;
		return String.format("%3.1f h", ns);
	}

	public static String bytes(double bytes) {
		if (bytes < 3000)
			return String.format("%3.0f b", bytes);
		bytes /= 1024.0;
		if (bytes < 3000)
			return String.format("%3.1f Kb", bytes);
		bytes /= 1024.0;
		if (bytes < 3000)
			return String.format("%3.1f Mb", bytes);
		bytes /= 1024.0;
		if (bytes < 3000)
			return String.format("%3.1f Gb", bytes);
		bytes /= 1024.0;
		return String.format("%3.1f Tb", bytes);
	}

	public static String toPercent(String value) {
		try {
			return toPercent(Double.parseDouble(value));
		}
		catch(Exception ex) {}
		return value;
	}

	public static String toPercent(double value) {
		return String.format("%5.3f", value * 100) + "%";
	}

}

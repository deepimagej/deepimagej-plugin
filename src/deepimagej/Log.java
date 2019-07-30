package deepimagej;

import deepimagej.tools.NumFormat;
import ij.IJ;

public class Log {

	private int level = 1; // normal
	private double chrono = System.nanoTime();
		
	public void print(String message) {
		if (level >= 2)
			IJ.log("DeepImageJ (" + NumFormat.time(System.nanoTime()-chrono) + ") : " + message);
	}
	
	public void reset() {
		chrono = System.nanoTime();
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public int getLevel() {
		return level;
	}
}

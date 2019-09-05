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

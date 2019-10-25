
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
package additionaluserinterface;

import java.text.DecimalFormat;

/**
 * This class provides static methods to measures the elapsed time.
 * It is a equivalent to the function tic and toc of Matlab. 
 * 
 * @author Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland.
 *
 */  
public class Chrono {

	static private double chrono = 0;
	
	/**
	* Register the current time.
	*/
	public static void tic() {
		chrono = System.currentTimeMillis();
	}
	
	/**
	* Returns a string that indicates the elapsed time since the last tic() call.
	*/
	public static String toc() {
		return toc("");
	}

	/**
	* Returns a string that indicates the elapsed time since the last tic() call.
	*
	* @param msg	message to print
	*/
	public static String toc(String msg) {
		double te = System.currentTimeMillis()-chrono;
		String s = msg + " ";
		DecimalFormat df = new DecimalFormat("####.##"); 
		if (te < 3000.0) 
			return s + df.format(te) + " ms";
		te /= 1000;
		if (te < 600.1)
			return s + df.format(te) + " s";
		te /= 60;
		if (te < 240.1)
			return s + df.format(te) + " min.";
		te /= 24;
		return s + df.format(te) + " h.";
	}

 }

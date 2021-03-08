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

package deepimagej.validation;

import ij.process.ImageProcessor;

public class MinMax {

	public static double getminimum(ImageProcessor img1) {
		double min_im = Double.MAX_VALUE;
		double val;
		for (int i = 0 ; i < img1.getHeight(); i++)
			for( int j = 0 ; j < img1.getWidth(); j++) {
				val = img1.getPixelValue(j, i);
				if(val<min_im)
					min_im = val;
			}
		return min_im;
	}
	
	public static double getmaximum(ImageProcessor img1) {
		double max_im = Double.MIN_VALUE;
		double val;
		for (int i = 0 ; i < img1.getHeight(); i++)
			for( int j = 0 ; j < img1.getWidth(); j++) {
				val = img1.getPixelValue(j, i);
				if(val>max_im)
					max_im = val;
			}
		return max_im;
	}
}

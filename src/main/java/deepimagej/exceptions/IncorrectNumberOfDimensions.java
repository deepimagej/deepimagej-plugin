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

package deepimagej.exceptions;

import java.util.Arrays;

public class IncorrectNumberOfDimensions extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	long[] shape;
	String dims;
	String name;
	
	public IncorrectNumberOfDimensions(long[] shape, String dims, String name) {
		this.shape = shape;
		this.dims = dims;
		this.name = name;
	}
	
	public String toString() {
		return "Tensor dimensions mismatch, " + dims.length() + " dimensions specified (" + dims + 
				") for tensor '" + name + "' whereas it has " + shape.length + " dimensions with shape "
				+ Arrays.toString(shape);
	}
	
	public String getDims() {
		return this.dims;
	}
	
	public long[] getShape() {
		return this.shape;
	}
	
	public String getName() {
		return this.name;
	}

}
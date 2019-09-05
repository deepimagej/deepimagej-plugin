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

public class Index {

	// Class that goups all the functions to obtain the index of a component in an array
	
	public static int indexOf(String[] array, String element) {
		//Find the index of the of the first entry of the array
		// that coincides with the variabe 'element'
		int array_size = array.length;
		boolean found = false;
		int counter = 0;
		int index = -1;
		String array_pos;
		while (counter < array_size && found == false) {
			array_pos = array[counter];
			if (array_pos.equals(element) == true) {
				found = true;
				index = counter;
			}
			counter ++;
		}
		return index;
	}
	
	public static int indexOf(String[] array, String element, int start) {
		//Find the index of the of the first entry of the array
		// that coincides with the variabe 'element' starting
		// from the index 'start'
		int array_size = array.length;
		boolean found = false;
		int counter = start;
		int index = -1;
		char array_pos;
		while (counter < array_size && found == false) {
			array_pos = array[counter].charAt(0);
			if (array_pos == element.charAt(0)) {
				found = true;
				index = counter;
			}
			counter ++;
		}
		return index;
	}
	
	
	public static int indexOf(int[] array, int element) {
		// Find the index of the of the first entry of the array
		// that coincides with the variabe 'element'
		int array_size = array.length;
		boolean found = false;
		int counter = 0;
		int index = -1;
		int array_pos = 0;
		while (counter < array_size && found == false) {
			array_pos = array[counter];
			if (array_pos == element) {
				found = true;
				index = counter;
			}
			counter ++;
		}
		return index;
	}
	
	
	public static int indexOf(int[] array, int element, int start) {
		//Find the index of the of the first entry of the array
		// that coincides with the variabe 'element' starting at 'start
		int array_size = array.length;
		boolean found = false;
		int counter = start;
		int index = -1;
		int array_pos = 0;
		while (counter < array_size && found == false) {
			array_pos = array[counter];
			if (array_pos == element) {
				found = true;
				index = counter;
			}
			counter ++;
		}
		return index;
	}
	
	
	public static int lastIndexOf(int[] array, int element) {
		// Find the index of the int in the array of int starting
		// from the end
		int array_size = array.length;
		boolean found = false;
		int counter = array_size - 1;
		int index = -1;
		int array_pos = 0;
		while (counter > -1 && found == false) {
			array_pos = array[counter];
			if (array_pos == element) {
				found = true;
				index = counter;
			}
			counter --;
		}
		return index;
	}
	
	public static int lastIndexOf(String[] array, String element) {
		// Find the index of the String in the array of String starting
		// from the end
		int array_size = array.length;
		boolean found = false;
		int counter = array_size - 1;
		int index = -1;
		String array_pos;
		while (counter > -1 && found == false) {
			array_pos = array[counter];
			if (array_pos.equals(element) == true) {
				found = true;
				index = counter;
			}
			counter --;
		}
		return index;
	}
}

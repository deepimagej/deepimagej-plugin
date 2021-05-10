/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package deepimagej.tools;

public class Index {

	// Class that goups all the functions to obtain the index of a component in an array
	
	public static int indexOf(String[] array, String element) {
		//Find the index of the of the first entry of the array
		// that coincides with the variable 'element'
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

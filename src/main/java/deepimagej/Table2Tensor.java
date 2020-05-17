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

package deepimagej;

import org.tensorflow.Tensor;

import deepimagej.tools.Index;
import ij.IJ;
import ij.measure.ResultsTable;


public class Table2Tensor {
	// TODO allow other types of tensors
	// TODO allow batch size != 1
	// TODO admit more than 3 dimensions for a table
	
	// Methods to transform a TensorFlow tensors into ImageJ Table
	
	/*
	 * Transform a tenor into an ImageJ results table following the scheme
	 * given by form.
	 * In the string form, N represents batch size; R, rows and C, columns
	 * For the moment it only admits up to 3 dimensions (one for N)
	 */
	public static ResultsTable tensor2Table(Tensor<?> tensor, String form) {
		//Method to transform an ImagePlus into a TensorFLow tensor of the
		// dimensions specified by form
		ResultsTable table = null;
		long[] tensorShape = tensor.shape();
		if (tensorShape.length == 2) {
			table = copyData2table1D(tensor, form);
		}else if (tensorShape.length == 3) {
			table = copyData2table2D(tensor, form);
		}else if (tensorShape.length == 4) {
			// TODO admit more than 3 dims
		}
		return table;
	}

	
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	///////////////  Convert tensors to arrays  //////////////////////////////////////////////////
	/*
	 * Copy the tensor to an ImageJ ResultsTable with only one row.
	 */
	public static ResultsTable copyData2table1D(Tensor<?> tensor, String form){
		// TODO support batch size different to 1
		ResultsTable table = null;
		int batchInd = form.indexOf("N");
		long[] tensorShape = tensor.shape();
		//  Create array from tensor
		float[][] tableMat2D = new float[(int) tensorShape[0]][(int) tensorShape[1]];
		if (batchInd != -1 && tensorShape[batchInd] != 1) {
			IJ.error("For the moment DeepImageJ only supports batch size of 1.");
		} else if (batchInd == -1) {
			float[][][] auxArray = new float[1][(int) tensorShape[0]][(int) tensorShape[1]];
			auxArray[0] = tableMat2D;
			table = getTablefrom3DArray(auxArray, (int)tensorShape[form.indexOf("R")], form, "NRC".split(""));
		} else {
			tensor.copyTo(tableMat2D);
			table = new ResultsTable();
			int rowCount = 0;
			for (int i = 0; i < tableMat2D.length; i ++) {
				for (int k = 0; k < tableMat2D[i].length; k ++) {
					table.setValue(0, rowCount ++, tableMat2D[i][k]);
				}
			}
		}
		return table;		
	}
	/*
	 * Copy the tensor to an ImageJ ResultsTable with th needed rows
	 * and columns.
	 * This method is ver similar to the ones in the class ImagePlus2Tensor
	 */
	public static ResultsTable copyData2table2D(Tensor<?> tensor, String form){
		// TODO support batch size different to 1
		ResultsTable table = null;
		String[] tableForm = "NRC".split("");
		int batchInd = form.indexOf("N");
		long[] tensorShape = tensor.shape();
		if (batchInd == -1 || tensorShape[batchInd] != 1) {
			IJ.error("For the moment DeepImageJ only supports batch size of 1.");
		} else {
			int[] relateDims = new int[3];
			int c = 0;
			for (String letter : tableForm) {
				relateDims[c ++] = Index.indexOf(form.split(""), letter);
			}
			float[][][] tableMat3D = new float[(int) tensorShape[0]][(int) tensorShape[1]][(int) tensorShape[2]];
			tensor.copyTo(tableMat3D);
			table = getTablefrom3DArray(tableMat3D, (int) tensorShape[form.indexOf("R")], form, tableForm);
			// TODO uncomment
			/*
			// Create a results table with the adequate number of rows
			table = new ResultsTable((int) tensorShape[form.indexOf("R")]);
			int[] auxCounter = {-1, -1, -1};
			for (int A = 0; A < tableMat3D.length; A ++) {
				auxCounter[relateDims[0]] += 1; 
				auxCounter[relateDims[1]] = -1; 
				for (int B = 0; B < tableMat3D[A].length; B ++) {
					auxCounter[relateDims[1]] += 1; 
					auxCounter[relateDims[2]] = -1; 
					for (int C = 0; C < tableMat3D[A][B].length; C ++) {
						auxCounter[relateDims[2]] += 1; 
						table.setValue(auxCounter[2], auxCounter[1], tableMat3D[A][B][C]);
					}
				}
			}
			 * 
			 */
			// Help to count rows and columns. Dimensions are N, R, C
		}
		return table;		
	}
	
	public static ResultsTable getTablefrom3DArray(float[][][] tableMat3D, int RIndex,
													String form, String[] tableForm) {
		int[] relateDims = new int[3];
		int c = 0;
		for (String letter : tableForm) {
			relateDims[c ++] = Index.indexOf(form.split(""), letter);
		}
		// Create a results table with the adequate number of rows
		ResultsTable table = new ResultsTable(RIndex);
		// Help to count rows and columns. Dimensions are N, R, C
		int[] auxCounter = {-1, -1, -1};
		for (int A = 0; A < tableMat3D.length; A ++) {
			auxCounter[relateDims[0]] += 1; 
			auxCounter[relateDims[1]] = -1; 
			for (int B = 0; B < tableMat3D[A].length; B ++) {
				auxCounter[relateDims[1]] += 1; 
				auxCounter[relateDims[2]] = -1; 
				for (int C = 0; C < tableMat3D[A][B].length; C ++) {
					auxCounter[relateDims[2]] += 1; 
					table.setValue(auxCounter[2], auxCounter[1], tableMat3D[A][B][C]);
				}
			}
		}
		return table;
	}		

}
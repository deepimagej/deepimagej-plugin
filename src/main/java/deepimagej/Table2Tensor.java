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

package deepimagej;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;


import deepimagej.exceptions.BatchSizeBiggerThanOne;
import deepimagej.exceptions.IncorrectNumberOfDimensions;
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
	
	/*
	 * Convert a float array into table
	 */
	public static ResultsTable flatArrayToTable(float[] flatArray, long[] shape, String form) {
		int[] completeShape = longShape5(shape);
		String completeForm = longForm(form);
		// Get the table dims in the default IJ table format -> RCBXY
		int[] tableDims = getTableDims(completeForm, completeShape);
		int numRows = tableDims[0];
		ResultsTable table = new ResultsTable(numRows);
		
		// Get the position of the Rows and Columns dimensions
		int fRows = completeForm.indexOf("R");
		int fCols = completeForm.indexOf("C");
		
		int pos = 0;
		int[] auxInd = {0, 0, 0, 0, 0};
		for (int i0 = 0; i0 < completeShape[0]; i0 ++) {
			auxInd[0] = i0;
			for (int i1 = 0; i1 < completeShape[1]; i1 ++) {
				auxInd[1] = i1;
				for (int i2 = 0; i2 < completeShape[2]; i2 ++) {
					auxInd[2] = i2;
					for (int i3 = 0; i3 < completeShape[3]; i3 ++) {
						auxInd[3] = i3;
						for (int i4 = 0; i4 < completeShape[4]; i4 ++) {
							auxInd[4] = i4;
							// In reality we only care about the values for positions
							// corresponding to R and C. HOwever as mentioned in other comments
							// the whole structure is left for maintenance purposes
							table.setValue(auxInd[fCols], auxInd[fRows], (double) flatArray[pos ++]);
						}
					}
				}
			}
		}
		return table;
	}

	
	/*
	 * Convert NDArrays into results table
	 */
	public static ResultsTable tensorToTable(Tensor<?> tensor, String form, String name) throws IncorrectNumberOfDimensions, BatchSizeBiggerThanOne {

		long[] shape = tensor.shape();
		if (form == null)
			form = findTableForm(shape, name);
		// If DeepImageJ has not been able to induce a form, the tensor is not valid
		if (form == null)
			return null;
		// Check that the output dimensions correspond to the form length
		if (shape.length != form.length())
			throw new IncorrectNumberOfDimensions(shape, form, name);
		// TODO add possibility of batch>1
		int batchIndex = form.indexOf("B");
		if (batchIndex != -1 && shape[batchIndex] > 1)
			throw new BatchSizeBiggerThanOne(shape, form, name);
				
		// For the moment DeepImageJ only supports 2D tables, thus
		// if the tensor has more than to dimensions greater than one,
		// the plugin throws an exception
		// TODO support more than 2d tables
		ArrayList<Integer> non1Occurences = findNon1occurences(shape);
		if (non1Occurences.size() > 2) {
			IJ.error("For the moment DeepImageJ only supports 2D tables as outputs with batch_size = 1.\n"
					+ "Represent the tensor as an image, instead of as a list.");
			return null;
		}

		// Array of one dimension containing all the data from the tensor
		int arraySize = 1;
		for (long el : shape)
			arraySize = arraySize * ((int) el);
		float[] flatArray = new float[arraySize];

		FloatBuffer outBuff = FloatBuffer.wrap(flatArray);
	 	tensor.writeTo(outBuff);
		return flatArrayToTable(flatArray, shape, form);
	}

	
	/*
	 * Convert NDArrays into results table
	 */
	public static ResultsTable tensorToTable(NDArray tensor, String form, String name, String ptVersion) throws IncorrectNumberOfDimensions, BatchSizeBiggerThanOne {
		// Array of one dimension containing all the data from the tensor
		float[] flatArray = tensor.toFloatArray();
		long[] shape = tensor.getShape().getShape();
		if (form == null)
			form = findTableForm(shape, name);
		// If DeepImageJ has not been able to induce a form, the tensor is not valid
		if (form == null)
			return null;
		int batchIndex = form.indexOf("B");
		// TODO should batch be eliminated always or only when the dimensions are incorrect
		boolean old = ImagePlus2Tensor.olderThanPytorch170(ptVersion);
		if (old && batchIndex != -1) {
			String oldForm = "" + form;
			form = oldForm.substring(0, batchIndex) +  oldForm.substring(batchIndex + 1);
			IJ.log("WARNING: DJL Pytorch versions <=1.6.0 do not allow definition of the batch size.");
			IJ.log("WARNING: Output tensor " + name + " dimension organization has changed: " + oldForm + " --> " + form);
		}
		// REtrieve again the batch index
		batchIndex = form.indexOf("B");
		// TODO add possibility of batch>1
		if (batchIndex != -1 && shape[batchIndex] > 1)
			throw new BatchSizeBiggerThanOne(shape, form, name);
		// Check that the output dimensions correspond to the form length
		if (shape.length != form.length())
			throw new IncorrectNumberOfDimensions(shape, form, name);
				
		// For the moment DeepImageJ only supports 2D tables, thus
		// if the tensor has more than to dimensions greater than one,
		// the plugin throws an exception
		// TODO support more than 2d tables
		ArrayList<Integer> non1Occurences = findNon1occurences(shape);
		if (non1Occurences.size() > 2) {
			IJ.error("For the moment DeepImageJ only supports 2D tables as outputs with batch_size = 1.\n"
					+ "Represent the tensor as an image, instead of as a list.");
			return null;
		}
		return flatArrayToTable(flatArray, shape, form);
	}
	
	/*
	 * find occurences of the number 1 in an array
	 */
	private static ArrayList<Integer> findNon1occurences(long[] shape) {
		ArrayList<Integer> occur = new ArrayList<Integer>();
		for (int i = 0; i < shape.length; i ++) {
			if (shape[i] != 1)
				occur.add(i);
		}
		return occur;
	}
	
	/*
	 * Get the table shape, with 5 dimensions, following 
	 * the IJ form -> RCBXY, remember that the last 3 dimensions
	 * should be singleton, at least for the moment
	 */
	private static int[] getTableDims(String form, int[] shape) {

		// IJ table default dimensions
		int[] tableDims = {1, 1, 1, 1, 1};
		// IJ table default dimension indices, given that
		// the form should be: 'RCBXY'
		int rInd = 0; int cInd = 1; int bInd = 2;
		int xInd = 3; int yInd = 4;
		int fBatch;
		if (form.indexOf("B") != -1) {
			fBatch = form.indexOf("B");
			tableDims[bInd] = (int) shape[fBatch];
		} else {
			fBatch = form.length();
			form += "B";
		}
		int fY;
		if (form.indexOf("Y") != -1) {
			fY = form.indexOf("Y");
			tableDims[yInd] = (int) shape[fY];
		} else {
			fY = form.length();
			form += "Y";
		}
		int fX;
		if (form.indexOf("X") != -1) {
			fX = form.indexOf("X");
			tableDims[xInd] = (int) shape[fX];
		} else {
			fX = form.length();
			form += "X";
		}
		int fCol;
		if (form.indexOf("C") != -1) {
			fCol = form.indexOf("C");
			tableDims[cInd] = (int) shape[fCol];
		} else {
			fCol = form.length();
			form += "C";
		}
		int fRow;
		if (form.indexOf("R") != -1) {
			fRow = form.indexOf("R");
			tableDims[rInd] = (int) shape[fRow];
		} else {
			fRow = form.length();
			form += "Z";
		}
		return tableDims;
	}
	
	/*
	 * Add extra dimensions to the form that will be considered as singleton dimensions in the shape. 
	 * Theoretically only 3 dimensions are needed, as DeepImageJ only supports
	 * 2D tables (that is 3D considering one singleton dimensionat least).
	 * I will let up to 5 dimensions just in case this changes in the future (Carlos) 
	 */
	private static String longForm(String form) {
		// In principle DeepImageJ form for tables only can have letters:
		// B->batch; R->rows; C->columns. I will add X and Y as auxiliary

		// IJ table default form
		String[] tableForm = "RCBXY".split("");
		for (String ff : tableForm) {
			if (form.indexOf(ff) == -1)
				form += ff;
		}
		return form;
	}
	
	/*
	 * Add singleton dimensions to the shape. 
	 * Theoretically only 3 dimensions are needed, as DeepImageJ only supports
	 * 2D tables (that is 3D considering one singleton dimensionat least).
	 * I will let up to 5 dimensions just in case this changes in the future (Carlos) 
	 */
	private static int[] longShape5(long[] shape) {
		// First convert add the needed entries with value 1 to the array
		// until its length is 5
		int[] f_shape = { 1, 1, 1, 1, 1 };
		for (int i = 0; i < shape.length; i++) {
			f_shape[i] = (int) shape[i];
		}
		return f_shape;
	}
	
	/*
	 * Method to infer automatically which dimension corresponds to rows and 
	 * which to columns. 
	 */
	public static String findTableForm(long[] ogShape, String name) {
		String form = null;
		int[] shape = new int[ogShape.length];
		for (int i = 0; i < shape.length; i ++)
			shape[i] = (int) ogShape[i];
		if (shape.length == 1) {
			form = "R";
		} else if (shape.length == 2 && shape[0] == (long) 1) {
			form = "BR";
		} else if (shape.length == 2) {
			form = "RC";
		} else if (shape.length == 3 && Index.indexOf(shape, 1) == -1) {
			// TODO
			// For the moment DeepImageJ only supports 2d tables with batchSize = 1
			IJ.log("Output list tensor '" + name + "' with dimensions " + Arrays.toString(shape) + " cannot be represented as an IJ table.");
			IJ.log("Current version of DeepImageJ only supports ResultsTable representation for 2D tensors or 3D with singleton dimension.");
		} else if (shape.length > 3) {
			// TODO
			// For the moment DeepImageJ only supports 2d tables with batchSize = 1
			IJ.log("Output list tensor '" + name + "' with dimensions " + Arrays.toString(shape) + " cannot be represented as an IJ table.");
			IJ.log("Current version of DeepImageJ only supports ResultsTable representation for 2D tensors or 3D with singleton dimension.");
		} else if (shape.length == 3 && Index.indexOf(shape, 1) != -1) {
			form = "BRC";
			if (Index.indexOf(shape, 1) == 1)
				form = "RBC";
			else if (Index.indexOf(shape, 1) == 2)
				form = "RCB";
		}
		return form;
	}
	
	
	public static void main(String[] ags) {
		ResultsTable rt = new ResultsTable(8);
		int ss = rt.getLastColumn();
		System.out.println("2");
	}
	
	/*
	 * Method that gets an long[] array from a given ResultsTable to be able to
	 * generate a tensor/ndarray with the given form and shape
	 */
	public static float[] tableToFlatArray(ResultsTable rt, String form, long[] arrayShape) {
		// Create the a flat array with as many component as the table has
		int nComponents = 1;
		for (long el : arrayShape)
			nComponents = nComponents * ((int) el);
		float[] flatRt = new float[nComponents];
		// Create a 5 components version of the shape just in case the plugin
		// is later adapted for more dimensions
		int[] longShape = longShape5(arrayShape);
		// Get the form with all the possible dimensions.
		// Doing this is equivalent to extending the tensor with singleton dimensions.
		// The dimensions added are the missing ones among B,R and C and X and Y, added
		// just in case in the future more dimensions are accepted
		form = longForm(form);

		// Get the index that corresponds to rows and columsn in the form
		int rInd = form.indexOf("R");
		int cInd = form.indexOf("C");
		// Again with 3 counter should be enough .3 (B,R,C) is the maximum number of 
		// dimension at the moment (JAnuary 2021). However, do it with 5 just in
		// case it is changed in the future
		int[] auxCounter = new int[5];
		int pos = 0;
		for (int t0 = 0; t0 < longShape[0]; t0 ++) {
			auxCounter[0] = t0;
			for (int t1 = 0; t1 < longShape[1]; t1 ++) {
				auxCounter[1] = t1;
				for (int t2 = 0; t2 < longShape[2]; t2 ++) {	
					auxCounter[2] = t2;
					for (int t3 = 0; t3 < longShape[3]; t3 ++) {
						auxCounter[3] = t3;
						for (int t4 = 0; t4 < longShape[4]; t4 ++) {	
							auxCounter[4] = t4;
							
							flatRt[pos ++] = (float) rt.getValueAsDouble(auxCounter[cInd], auxCounter[rInd]);
						}
					}	
				}
			}
		}
		return flatRt;
	}
	
	/*
	 * Method that gets a Tensor from a ResultsTable
	 */
	// TODO can this method be used somewhere in the plugin?
	public static NDArray tableToTensor(ResultsTable rt, String form, String ptVersion, NDManager manager){
		// Get rows and columns (by default sum 1 to the number of columns)
		int rSize = rt.size();
		// Get last column indicates position, that is why we sum 1
		int cSize = rt.getLastColumn() + 1;
		if (cSize == 0)
			cSize = 1;
		if (cSize != 1 && rSize != 1 && form.indexOf("B") != -1) {
			IJ.error("Batch size should be 1.");
			return null;
		} else if (cSize != 1 && rSize != 1 && form.length() == 1) {
			IJ.error("Table has 2 dimensions but only one (" + form + ") was specified.");
			return null;
		}

		boolean old = ImagePlus2Tensor.olderThanPytorch170(ptVersion);
		int batchIndex = form.indexOf("B");
		// TODO should batch be eliminated always or only when the dimensions are incorrect
		if (old && batchIndex != -1) {
			String oldForm = "" + form;
			form = oldForm.substring(0, batchIndex) +  oldForm.substring(batchIndex + 1);
			IJ.log("WARNING: DJL Pytorch versions <=1.6.0 do not allow definition of the batch size.");
			IJ.log("WARNING: List input tensor dimension organization has changed: " + oldForm + " --> " + form);
		}
		
		// Create a variable that acts as imp.getDimensions() for ImagePlus types
		int[] defaultTableDimensions = new int[] {rSize, cSize};
		long[] arrayShape = getTableTensorDims(defaultTableDimensions, form);
		// Get the array
		float[] flatRt = tableToFlatArray(rt, form, arrayShape);
		// Create the tensor
		FloatBuffer outBuff = FloatBuffer.wrap(flatRt);
		NDArray tensor = manager.create(flatRt, new Shape(arrayShape));
		return tensor;
	}
	
	/*
	 * Method that gets a Tensor from a ResultsTable
	 */
	// TODO can this method be used somewhere in the plugin?
	public static Tensor<Float> tableToTensor(ResultsTable rt, String form){
		// Get rows and columns (by default sum 1 to the number of columns)
		int rSize = rt.size();
		// Get last column indicates position, that is why we sum 1
		int cSize = rt.getLastColumn() + 1;
		if (cSize == 0)
			cSize = 1;
		if (cSize != 1 && rSize != 1 && form.indexOf("B") != -1) {
			IJ.error("Batch size should be 1.");
			return null;
		} else if (cSize != 1 && rSize != 1 && form.length() == 1) {
			IJ.error("Table has 2 dimensions but only one (" + form + ") was specified.");
			return null;
		}
		// Create a variable that acts as imp.getDimensions() for ImagePlus types
		int[] defaultTableDimensions = new int[] {rSize, cSize};
		long[] arrayShape = getTableTensorDims(defaultTableDimensions, form);
		// Get the array
		float[] flatRt = tableToFlatArray(rt, form, arrayShape);
		// Create the tensor
		FloatBuffer outBuff = FloatBuffer.wrap(flatRt);
		Tensor<Float> tensor = Tensor.create(arrayShape, outBuff);
		return tensor;
	}
	
	/*
	 * Get the tensor dimension for the table. For the moment only
	 * suuport Rows, Columns and Batch
	 */
	public static long[] getTableTensorDims(int[] dims, String form) {
		long[] tableDims = new long[form.length()];
		// Create aux variable to indicate
		// if it is channels one of the dimensions of
		// the tensor or it is the batch size
		if (form.indexOf("B") != -1) {
			// TODO batch is always 1
			tableDims[form.indexOf("B")] = (long) 1;
		}
		if (form.indexOf("R") != -1) {
			// Using the default table dimensions (determined by us
			// in the previous method) rows will always be determined at 0
			tableDims[form.indexOf("R")] = (long) dims[0];
		}
		if (form.indexOf("C") != -1) {
			// Using the default table dimensions (determined by us
			// in the previous method) columns will always be determined at 1
			tableDims[form.indexOf("C")] = (long) dims[1];
		}
		return tableDims;
	}
	
	public static int[] getTableShape(String form, ResultsTable rt) {
		int[] shape = new int[form.length()];
		// Get rows and columns (by default sum 1 to the number of columns)
		int rSize = rt.size();
		// Get last column indicates position, that is why we sum 1
		int cSize = rt.getLastColumn() + 1;
		if (cSize == 0)
			cSize = 1;
		for (int i = 0; i < form.length(); i ++) {
			if (form.split("")[i].contentEquals("B")) {
				shape[i] = 1;
			} else if (form.split("")[i].contentEquals("R")) {
				shape[i] = rSize;
			} else if (form.split("")[i].contentEquals("C")) {
				shape[i] = cSize;
			}
		}
		return shape;
	}

}
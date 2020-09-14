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

import java.nio.FloatBuffer;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.Index;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;


public class ImagePlus2TensorPt {
	// TODO allow other types of tensors
	// TODO allow batch size != 1
	// TODO review and try to put Pytorch and TF together. Use buffers as in here
	// Methods to transform a Pytorch tensors into ImageJ ImagePlus

	public static NDArray imPlus2tensor(NDManager manager, ImagePlus img, String form){
		// Convert ImagePlus into tensor calling the corresponding
		// method depending on the dimensions of the required tensor 
		// Find the number of dimensions of the tensor
		int nDim = form.length();
		NDArray tensor = null;
		if (nDim >= 2 && nDim <= 5) {
			tensor = imPlus2matrix(img, form, manager);
		}
		return tensor;
	}
	
	public static NDArray imPlus2matrix(ImagePlus img, String form, NDManager manager){
		// Create a float array of four dimensions out of an 
		// ImagePlus object
		float[] matImage;
		// Initialise ImageProcessor variable used later
		ImageProcessor ip;
		int[] dims = img.getDimensions();
		int xSize = dims[0];
		int ySize = dims[1];
		int cSize = dims[2];
		int zSize = dims[3];
		// TODO allow different batch sizes
		int batch = 1;
		int[] tensorDims = new int[] {1, 1, 1, 1, 1};
		// Create aux variable to indicate
		// if it is channels one of the dimensions of
		// the tensor or it is the batch size
		int fBatch = -1;
		int fChannel = -1;
		int fDepth = -1;
		int fWidth = -1;
		int fHeight = -1;
		// Create auxiliary variable to represent the order
		// of the dimensions in the ImagePlus
		String[] imPlusForm = "XYCZB".split("");
		long[] arrayShape;
		if (form.indexOf("B") != -1) {
			fBatch = form.indexOf("B");
			tensorDims[fBatch] = batch;
			arrayShape = new long[form.length() - 1];
		} else {
			arrayShape = new long[form.length()];
			fBatch = form.length();
			form += "B";
		}
		if (form.indexOf("Y") != -1) {
			fHeight = form.indexOf("Y");
			tensorDims[fHeight] = ySize;
			if (fBatch != -1 && fHeight > fBatch)
				arrayShape[fHeight - 1] = (long) ySize;
			else
				arrayShape[fHeight] = (long) ySize;
		} else {
			fHeight = form.length();
			form += "Y";
		}
		if (form.indexOf("X") != -1) {
			fWidth = form.indexOf("X");
			tensorDims[fWidth] = xSize;
			if (fBatch != -1 && fWidth > fBatch)
				arrayShape[fWidth - 1] = (long) xSize;
			else
				arrayShape[fWidth] = (long) xSize;
		} else {
			fWidth = form.length();
			form += "X";
		}
		if (form.indexOf("C") != -1) {
			fChannel = form.indexOf("C");
			tensorDims[fChannel] = cSize;
			if (fBatch != -1 && fChannel > fBatch)
				arrayShape[fChannel - 1] = (long) cSize;
			else
				arrayShape[fChannel] = (long) cSize;
		} else {
			fChannel = form.length();
			form += "C";
		}
		if (form.indexOf("Z") != -1) {
			fDepth = form.indexOf("Z");
			tensorDims[fDepth] = zSize;
			if (fBatch != -1 && fDepth > fBatch)
				arrayShape[fDepth - 1] = (long) zSize;
			else
				arrayShape[fDepth] = (long) zSize;
		} else {
			fDepth = form.length();
			form += "Z";
		}
		matImage = new float[tensorDims[0] * tensorDims[1] * tensorDims[2] * tensorDims[3] * tensorDims[4]];
		
		// Obtain the shapes association
		int[] dimsAssociation = createDimOrder(imPlusForm, form);
		
		// Make sure the array is written from last dimension to first dimension.
		// For example, for CYX we first iterate over all the X, then over the Y and then 
		// over the C
		int[] auxCounter = new int[5];
		int pos = 0;
		for (int t0 = 0; t0 < tensorDims[0]; t0 ++) {
			auxCounter[0] = t0;
			for (int t1 = 0; t1 < tensorDims[1]; t1 ++) {
				auxCounter[1] = t1;
				for (int t2 = 0; t2 < tensorDims[2]; t2 ++) {	
					auxCounter[2] = t2;
					for (int t3 = 0; t3 < tensorDims[3]; t3 ++) {
						auxCounter[3] = t3;
						for (int t4 = 0; t4 < tensorDims[4]; t4 ++) {	
							auxCounter[4] = t4;
							
							img.setPositionWithoutUpdate(auxCounter[fChannel] + 1, auxCounter[fDepth] + 1, 1);
							ip = img.getProcessor();
							matImage[pos ++] = ip.getPixelValue(auxCounter[fWidth], auxCounter[fHeight]);
						}
					}	
				}
			}
		}
		FloatBuffer outBuff = FloatBuffer.wrap(matImage);
		NDArray tensor = manager.create(matImage, new Shape(arrayShape));
	return tensor;
	}
	
	
	/////////// Methods to transform an TensorFlow tensor into an ImageJ ImagePlus
	
	
	public static ImagePlus tensor2ImagePlus(NDArray tensor, String form) {
		//Method to transform an ImagePlus into a TensorFLow tensor of the
		// dimensions specified by form
		ImagePlus image;
		Shape tensorShape = tensor.getShape();
		if (tensorShape.dimension() == 2) {
			image = copyData2Image2D(tensor, form);
		}else if (tensorShape.dimension() == 3) {
			image = copyData2Image3D(tensor, form);
		}else if (tensorShape.dimension() == 4) {
			image = copyData2Image4D(tensor, form);
		}else {
			image = copyData2Image5D(tensor, form);
		}
		return image;
	}
	
	
	
	public static ImagePlus copyData2Image5D(NDArray tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		
		ImagePlus imPlus = null;
		Shape longShape = tensor.getShape();
		int batchIndex = form.indexOf("B");
		if (batchIndex == -1 || longShape.get(batchIndex) == 1) {
			int[] tensorShape = new int[longShape.dimension()];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape.get(i);
			}
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] imShape = getShape(tensorShape, form);
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			double[][][][][] correctImage = copyData2Array5D(tensor, form);
			imPlus = ArrayOperations.convertArrayToImagePlus(correctImage, imShape);
		} else {
			IJ.error("Sorry only batch size equal to 1 is allowed.");
		}
		return imPlus;
	}
	
	
	public static ImagePlus copyData2Image4D(NDArray tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "XYCZB";
		
		ImagePlus imPlus = null;
		Shape longShape = tensor.getShape();
		int batchIndex = form.indexOf("B");
		if (batchIndex == -1 || longShape.get(batchIndex) == 1) {
			int[] tensorShape = new int[longShape.dimension()];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape.get(i);
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensorShape6 = longShape6(tensorShape);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] imShape = getShape(tensorShape, form);
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			double[][][][][] correctImage =new double[imShape[0]][imShape[1]][imShape[2]][imShape[3]][imShape[4]];
			// Find the association between the tensor and the image dimensions
			String[] n_form = findMissingDimensions(form, imPlusForm);
			int[] dimensionAssotiation = createDimOrder(n_form, imPlusForm);
			
			int[] aux_array = {-1,-1,-1,-1,-1};
			int x = -1; int y = -1; int z = -1; int c = -1; int t = -1;
			for (int A = 0; A < tensorShape6[0]; A++) {
				aux_array[dimensionAssotiation[0]] = A;
				for (int B = 0; B < tensorShape6[1]; B++) {
					aux_array[dimensionAssotiation[1]] = B;
					for (int C = 0; C < tensorShape6[2]; C++) {
						aux_array[dimensionAssotiation[2]] = C;
						for (int D = 0; D < tensorShape6[3]; D++) {
							aux_array[dimensionAssotiation[3]] = D;
							for (int E = 0; E < tensorShape6[4]; E++) {
								aux_array[dimensionAssotiation[4]] = E;
								x = aux_array[0];
								y = aux_array[1];
								c = aux_array[2];
								z = aux_array[3];
								t = aux_array[4];
								correctImage[x][y][c][z][t] = (double) tensor.getDouble(new long[]{A, B, C, D});
							}
						}
					}
				}
			}
			imPlus = ArrayOperations.convertArrayToImagePlus(correctImage, imShape);
		} else {
			IJ.error("Sorry only batch size equal to 1 is allowed.");
		}
		return imPlus;
	}
	
	public static ImagePlus copyData2Image3D(NDArray tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "XYCZB";
		
		ImagePlus imPlus = null;
		Shape longShape = tensor.getShape();
		int batchIndex = form.indexOf("B");
		if (batchIndex == -1 || longShape.get(batchIndex) == 1) {
			int[] tensorShape = new int[longShape.dimension()];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape.get(i);
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensorShape6 = longShape6(tensorShape);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] imShape = getShape(tensorShape, form);
			
			// Create the matrix containing the image. note that the dimensions are arranged differently because in
			// imageJ channels go before slices
			double[][][][][] correcImage = new double[imShape[0]][imShape[1]][imShape[2]][imShape[3]][imShape[4]];
			// Find the association between the tensor and the image dimensions
			String[] n_form = findMissingDimensions(form, imPlusForm);
			int[] dimensionAssotiation = createDimOrder(n_form, imPlusForm);
			
			int[] aux_array = {-1,-1,-1,-1,-1};
			int x = -1; int y = -1; int z = -1; int c = -1; int t = -1;
			for (int A = 0; A < tensorShape6[0]; A++) {
				aux_array[dimensionAssotiation[0]] = A;
				for (int B = 0; B < tensorShape6[1]; B++) {
					aux_array[dimensionAssotiation[1]] = B;
					for (int C = 0; C < tensorShape6[2]; C++) {
						aux_array[dimensionAssotiation[2]] = C;
						for (int D = 0; D < tensorShape6[3]; D++) {
							aux_array[dimensionAssotiation[3]] = D;
							for (int E = 0; E < tensorShape6[4]; E++) {
								aux_array[dimensionAssotiation[4]] = E;
								x = aux_array[0];
								y = aux_array[1];
								c = aux_array[2];
								z = aux_array[3];
								t = aux_array[4];
								correcImage[x][y][c][z][t] = (double) tensor.getFloat(new long[] {A, B, C});
							}
						}
					}
				}
			}
			imPlus = ArrayOperations.convertArrayToImagePlus(correcImage, imShape);
		} else {
			IJ.error("Sorry only batch size equal to 1 is allowed.");
		}
		return imPlus;
	}
	
	public static ImagePlus copyData2Image2D(NDArray tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "XYCZB";
		
		ImagePlus imPlus = null;
		Shape longShape = tensor.getShape();
		int batchIndex = form.indexOf("B");
		if (batchIndex == -1 || longShape.get(batchIndex) == 1) {
			int[] tensorShape = new int[longShape.dimension()];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape.get(i);
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensorShape6 = longShape6(tensorShape);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] imShape = getShape(tensorShape, form);
			
			// Create the matrix containing the image. note that the dimensions are arranged differently because in
			// imageJ channels go before slices
			double[][][][][] correctImage = new double[imShape[0]][imShape[1]][imShape[2]][imShape[3]][imShape[4]];
			// Find the association between the tensor and the image dimensions
			String[] n_form = findMissingDimensions(form, imPlusForm);
			int[] dimensionAsociattion = createDimOrder(n_form, imPlusForm);
			
			int[] auxArray = {-1,-1,-1,-1,-1};
			int x = -1; int y = -1; int z = -1; int c = -1; int t = -1;
			for (int A = 0; A < tensorShape6[0]; A++) {
				auxArray[dimensionAsociattion[0]] = A;
				for (int B = 0; B < tensorShape6[1]; B++) {
					auxArray[dimensionAsociattion[1]] = B;
					for (int C = 0; C < tensorShape6[2]; C++) {
						auxArray[dimensionAsociattion[2]] = C;
						for (int D = 0; D < tensorShape6[3]; D++) {
							auxArray[dimensionAsociattion[3]] = D;
							for (int E = 0; E < tensorShape6[4]; E++) {
								auxArray[dimensionAsociattion[4]] = E;
								x = auxArray[0];
								y = auxArray[1];
								c = auxArray[2];
								z = auxArray[3];
								t = auxArray[4];
								correctImage[x][y][c][z][t] = (double) tensor.getFloat(new long[] {A, B});
							}
						}
					}
				}
			}
			imPlus = ArrayOperations.convertArrayToImagePlus(correctImage, imShape);
		} else {
			IJ.error("Sorry only batch size equal to 1 is allowed.");
		}
		return imPlus;
	}
	
	
	private static int[] longShape6(int[] shape) {
		// First convert add the needed entries with value 1 to the array
		// until its length is 5
		int[] f_shape = { 1, 1, 1, 1, 1, 1 };
		for (int i = 0; i < shape.length; i++) {
			f_shape[i] = shape[i];
		}
		return f_shape;
	}
	
	private static int[] getShape(int[] tensorShape, String form) {
		// Find out which entry corresponds to each dimension. The biggest
		// dimensions correspond to nx, then ny and successively
		// img_shape = [nx,ny,nc,nz,nt, batch_size]
		int[] shape = { 1, 1, 1, 1, 1, 1 };
		// Define the mapping and position in the ImagePlus and the letter
		String[] dimList = { "X", "Y", "C", "Z", "B" };
		int[] positionMapping = { 0, 1, 2, 3, 5 };
		String dimLetter;
		int position;
		int imPlusIndex;
		for (int index = 0; index < tensorShape.length; index++) {
			dimLetter = Character.toString(form.charAt(index));
			position = Index.indexOf(dimList, dimLetter);
			if (position != -1) {
				imPlusIndex = positionMapping[position];
				shape[imPlusIndex] = tensorShape[index];
			}
		}
		return shape;
	}
	
	
	
	//// Method for both cases
	public static int[] createDimOrder(String[] imageJOrder, String requiredOrder) {
		// Example: imageJ_order = [c,d,e,b,a]; required_order = [d,e,b,c,a]
		// output--> dim_order = [3,0,1,2,4], because c goes in position 3, d in 0
		// position
		// and so on in the required_order array
		int size = imageJOrder.length;
		int pos = 0;
		int[] dimOrder = new int[size];
		for (int i = 0; i < size; i++) {
			pos = requiredOrder.indexOf(imageJOrder[i]);
			dimOrder[i] = pos;
		}
		return dimOrder;
	}
	
	// TODO fix the thing about batch size
	public static String[] findMissingDimensions(String tensorForm, String imagePlusForm) {
		// Method that extends the form of the tensor to 5 letters in order to match the 
		// ImagePlus form
		String[] tForm = new String[imagePlusForm.length()];
		String[] separatedImForm = imagePlusForm.split("");
		
		int extra = tensorForm.length();
		
		for (int i = 0; i < tForm.length; i ++) {
			int index = tensorForm.indexOf(separatedImForm[i]);
			if (index != -1) {
				// If the image dimension is found in the tensor form,
				// locate it in the same position
				tForm[index] = separatedImForm[i];
			} else {
				// If the dimension is not found, locate it in the end. It 
				// does not matter if it makes sense or not because all of them
				// are going to be 1.
				tForm[extra] = separatedImForm[i];
				extra ++;
			}
		}
		return tForm;
	}
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	///////////////  Convert tensors to arrays  //////////////////////////////////////////////////
	

	public static double[][][][][] copyData2Array5D(NDArray tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "XYCZB";
		
		double[][][][][] correctImage = null;
		Shape longShape = tensor.getShape();
		int batchIndex = form.indexOf("B");
		if (batchIndex == -1 || longShape.get(batchIndex) == 1) {
			int[] tensorShape = new int[longShape.dimension()];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape.get(i);
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensorShape6 = longShape6(tensorShape);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] imShape = getShape(tensorShape, form);
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			correctImage = new double[imShape[0]][imShape[1]][imShape[2]][imShape[3]][imShape[4]];
			// Find the association between the tensor and the image dimensions
			String[] n_form = findMissingDimensions(form, imPlusForm);
			int[] dimensionAssotiation = createDimOrder(n_form, imPlusForm);
			
			int[] auxArray = {-1,-1,-1,-1,-1};
			int x = -1; int y = -1; int z = -1; int c = -1; int t = -1;
			for (int A = 0; A < tensorShape6[0]; A++) {
				auxArray[dimensionAssotiation[0]] = A;
				for (int B = 0; B < tensorShape6[1]; B++) {
					auxArray[dimensionAssotiation[1]] = B;
					for (int C = 0; C < tensorShape6[2]; C++) {
						auxArray[dimensionAssotiation[2]] = C;
						for (int D = 0; D < tensorShape6[3]; D++) {
							auxArray[dimensionAssotiation[3]] = D;
							for (int E = 0; E < tensorShape6[4]; E++) {
								auxArray[dimensionAssotiation[4]] = E;
								x = auxArray[0];
								y = auxArray[1];
								c = auxArray[2];
								z = auxArray[3];
								t = auxArray[4];
								correctImage[x][y][c][z][t] = (double) tensor.getFloat(new long[] {A, B, C, D, E});
							}
						}
					}
				}
			}
		} else {
			IJ.error("Sorry only batch size equal to 1 is allowed.");
		}
		return correctImage;
	}
}

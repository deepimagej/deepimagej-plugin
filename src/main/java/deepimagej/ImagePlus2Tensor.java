/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we strongly encourage you to include adequate citations and acknowledgments 
 * whenever you present or publish results that are based on it.
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
 * DeepImageJ is an open source software (OSS): you can redistribute it and/or modify it under 
 * the terms of the BSD 2-Clause License.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 * You should have received a copy of the BSD 2-Clause License along with DeepImageJ. 
 * If not, see <https://opensource.org/licenses/bsd-license.php>.
 */

package deepimagej;

import org.tensorflow.Tensor;

import deepimagej.tools.ArrayOperations;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;


public class ImagePlus2Tensor {
	
	// Methods to transform a TensorFlow tensors into ImageJ ImagePlus

	public static Tensor<?> imPlus2tensor(ImagePlus img, String form, int channels){
		// Convert ImagePlus into tensor calling the corresponding
		// method depending on the dimensions of the required tensor 
		// Find the number of dimensions of the tensor
		int nDim = form.length();
		Tensor<?> tensor = null;
		if (nDim == 2) {
			float[][] matImage = imPlus2matrix(img, form);
			tensor = tensor(matImage);
		} else if (nDim == 3) {
			float[][][] matImage = imPlus2matrix3(img, form, channels);
			tensor = tensor(matImage);
		} else if (nDim ==4) {
			float[][][][] matImage = imPlus2matrix4(img, form, channels);
			tensor = tensor(matImage);
		}
		return tensor;
	}
	
	public static Tensor<Float> tensor(final float[][] image){
		// Create tensor object of 2 dims from a float[][]
		Tensor<Float> tensor = Tensor.create(image, Float.class);
		return tensor;
	}
	
	public static Tensor<Float> tensor(final float[][][] image){
		// Create tensor object of 3 dims from a float[][][]
		Tensor<Float> tensor = Tensor.create(image, Float.class);
		return tensor;
		}
	
	public static Tensor<Float> tensor(final float[][][][] image){
		// Create tensor object of 4 dims from a float[][][][]
		Tensor<Float> tensor = Tensor.create(image, Float.class);
		return tensor;
		}
	
	public static float[][] imPlus2matrix(ImagePlus img, String form){
		// Create a float array of two dimensions out of an 
		// ImagePlus object
		float[][] matImage;
		int[] dims = img.getDimensions();
		int xSize = dims[0];
		int ySize = dims[1];
		// Get the processor (matrix representing one slice, frame or channel)
		// of the ImagePlus
		ImageProcessor ip = img.getProcessor();
		
		if (form.equals("HW") == true) {
			matImage = new float[ySize][xSize];
			matImage = iProcessor2matrixHW(ip);
		} else {
			matImage = new float[xSize][ySize];
			matImage = iProcessor2matrixWH(ip);
		}
		return matImage;
	}
	
	
	public static float[][][] imPlus2matrix3(ImagePlus img, String form, 
											int nChannels){
		// Create a float array of three dimensions out of an 
		// ImagePlus object
		float[][][] matImage;
		// Initialize ImageProcessor variable used later
		ImageProcessor ip;
		int[] dims = img.getDimensions();
		int xSize = dims[0];
		int ySize = dims[1];
		// TODO allow different batch sizes
		int batch = 1;
		int[] tensorDims = new int[3];
		// Create aux variable to indicate
		// if it is channels one of the dimensions of
		// the tensor or it is the batch size
		int fChannelsOrBatch = -1;
		// Create auxiliary variable to represent the order
		// of the dimensions in the ImagePlus
		String[] imPlusForm = new String[3];
		
		if (form.indexOf("N") != -1) {
			fChannelsOrBatch = form.indexOf("N");
			tensorDims[fChannelsOrBatch] = batch;
			// The third dimension of the tensor is batch size
			imPlusForm[0] = "N"; imPlusForm[1] =  "H";
			imPlusForm[2] = "W";
			}
		if (form.indexOf("H") != -1) {
			int fHeight = form.indexOf("H");
			tensorDims[fHeight] = ySize;
		}
		if (form.indexOf("W") != -1) {
			int fWidth = form.indexOf("W");
			tensorDims[fWidth] = xSize;
		}
		if (form.indexOf("C") != -1) {
			fChannelsOrBatch = form.indexOf("C");
			tensorDims[fChannelsOrBatch] = nChannels;
			// The third dimension of the tensor is channels
			imPlusForm[0] = "C"; imPlusForm[1] =  "H";
			imPlusForm[2] = "W";
		}
		matImage = new float[tensorDims[0]][tensorDims[1]][tensorDims[2]];
	
		// Obtain the shapes association
		int[] dimsAssociation = createDimOrder(imPlusForm, form);
		
		int[] aux_coord = {-1, -1, -1};
		for (int n = 0; n < tensorDims[fChannelsOrBatch]; n ++) {
			aux_coord[dimsAssociation[0]] = n;
			for (int x = 0; x < xSize; x ++) {
				aux_coord[dimsAssociation[1]] = x;
				for (int y = 0; y < ySize; y ++) {
					aux_coord[dimsAssociation[2]] = y;
					img.setPositionWithoutUpdate(n + 1, 1, 1);
					ip = img.getProcessor();
					matImage[aux_coord[0]][aux_coord[1]][aux_coord[2]] = ip.getPixelValue(x, y);
				}
			}
		}
		return matImage;
	}
	
	
	public static float[][][][] imPlus2matrix4(ImagePlus img, String form,
											  int nChannels){
		// Create a float array of four dimensions out of an 
		// ImagePlus object
		float[][][][] matImage;
		// Initialize ImageProcessor variable used later
		ImageProcessor ip;
		int[] dims = img.getDimensions();
		int xSize = dims[0];
		int ySize = dims[1];
		// TODO allow different batch sizes
		int batch = 1;
		int[] tensorDims = new int[4];
		// Create aux variable to indicate
		// if it is channels one of the dimensions of
		// the tensor or it is the batch size
		int fBatch = -1;
		int fChannel = -1;
		// Create auxiliary variable to represent the order
		// of the dimensions in the ImagePlus
		String[] imPlusForm = {"N", "C", "H", "W"};
		
		if (form.indexOf("N") != -1) {
			fBatch = form.indexOf("N");
			tensorDims[fBatch] = batch;
		}
		if (form.indexOf("H") != -1) {
			int fHeight = form.indexOf("H");
			tensorDims[fHeight] = ySize;
		}
		if (form.indexOf("W") != -1) {
			int fWidth = form.indexOf("W");
			tensorDims[fWidth] = xSize;
		}
		if (form.indexOf("C") != -1) {
			fChannel = form.indexOf("C");
			tensorDims[fChannel] = nChannels;
		}
		matImage = new float[tensorDims[0]][tensorDims[1]][tensorDims[2]][tensorDims[3]];
		
		// Obtain the shapes association
		int[] dimsAssociation = createDimOrder(imPlusForm, form);
		
		int[] auxCoord = {-1, -1, -1, -1};
		for (int n = 0; n < tensorDims[fBatch]; n ++) {
			auxCoord[dimsAssociation[0]] = n;
			for (int c = 0; c < nChannels; c ++) {
				auxCoord[dimsAssociation[1]] = c;
				for (int x = 0; x < xSize; x ++) {	
					auxCoord[dimsAssociation[2]] = x;
					for (int y = 0; y < ySize; y ++) {
						auxCoord[dimsAssociation[3]] = y;
						img.setPositionWithoutUpdate(c + 1, 1, 1);
						ip = img.getProcessor();
						matImage[auxCoord[0]][auxCoord[1]][auxCoord[2]][auxCoord[3]] = ip.getPixelValue(x, y);
					}	
				}
			}
		}
		return matImage;
		}	
	
	
	public static float[][] iProcessor2matrixWH(ImageProcessor image){
		// this method transforms an image processor into a matrix
		float pixelVal = 0;
		int ySize = image.getHeight();
		int xSize = image.getWidth();
		float[][] matImage = new float[xSize][ySize];
		for (int y = 0; y < ySize; y ++) {
			for (int x = 0; x < xSize; x ++) {
				pixelVal = (float) image.getPixelValue(x, y);
				matImage[x][y] = pixelVal;
			}
		}
		return matImage;
	}

	
	public static float[][] iProcessor2matrixHW(ImageProcessor image){
		// this method transforms an image processor into a matrix
		float pixelVal = 0;
		int ySize = image.getHeight();
		int xSize = image.getWidth();
		float[][] matImage = new float[ySize][xSize];
		for (int y = 0; y < ySize; y ++) {
			for (int x = 0; x < xSize; x ++) {
				pixelVal = (float) image.getPixelValue(x, y);
				matImage[y][x] = pixelVal;
			}
		}
		return matImage;
	}
	
	
	/////////// Methods to transform an TensorFlow tensor into an ImageJ ImagePlus
	
	
	public static ImagePlus tensor2ImagePlus(Tensor<?> tensor, String form) {
		//Method to transform an ImagePlus into a TensorFLow tensor of the
		// dimensions specified by form
		ImagePlus image;
		long[] tensorShape = tensor.shape();
		if (tensorShape.length == 2) {
			image = copyData2Image2D(tensor, form);
		}else if (tensorShape.length == 3) {
			image = copyData2Image3D(tensor, form);
		}else if (tensorShape.length == 4) {
			image = copyData2Image4D(tensor, form);
		}else {
			image = copyData2Image5D(tensor, form);
		}
		return image;
	}
	
	public static ImagePlus copyData2Image5D(Tensor<?> tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment bothe are going to be 1
		String imPlusForm = "HWCZN";
		
		ImagePlus imPlus = null;
		long[] longShape = tensor.shape();
		int batchIndex = form.indexOf("N");
		if (batchIndex == -1 || longShape[batchIndex] == 1) {
			int[] tensorShape = new int[longShape.length];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape[i];
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensorShape6 = longShape6(tensorShape);
			float[][][][][] imgMatrix5D = new float[tensorShape6[0]][tensorShape6[1]][tensorShape6[2]][tensorShape6[3]][tensorShape6[4]];
			tensor.copyTo(imgMatrix5D);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] imShape = getShape(tensorShape, form);
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			double[][][][][] correctImage = new double[imShape[0]][imShape[1]][imShape[2]][imShape[3]][imShape[4]];
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
								correctImage[x][y][c][z][t] = (double) imgMatrix5D[A][B][C][D][E];
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
	
	
	public static ImagePlus copyData2Image4D(Tensor<?> tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "HWCZN";
		
		ImagePlus imPlus = null;
		long[] longShape = tensor.shape();
		int batchIndex = form.indexOf("N");
		if (batchIndex == -1 || longShape[batchIndex] == 1) {
			int[] tensorShape = new int[longShape.length];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape[i];
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensorShape6 = longShape6(tensorShape);
			float[][][][] imgMatrix4D = new float[tensorShape6[0]][tensorShape6[1]][tensorShape6[2]][tensorShape6[3]];
			tensor.copyTo(imgMatrix4D);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] imShape = getShape(tensorShape, form);
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			double[][][][][] correctImage = new double[imShape[0]][imShape[1]][imShape[2]][imShape[3]][imShape[4]];
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
								correctImage[x][y][c][z][t] = (double) imgMatrix4D[A][B][C][D];
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
	
	public static ImagePlus copyData2Image3D(Tensor<?> tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "HWCZN";
		
		ImagePlus imPlus = null;
		long[] longShape = tensor.shape();
		int batchIndex = form.indexOf("N");
		if (batchIndex == -1 || longShape[batchIndex] == 1) {
			int[] tensorShape = new int[longShape.length];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape[i];
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensorShape6 = longShape6(tensorShape);
			float[][][] imageMatrix3D = new float[tensorShape6[0]][tensorShape6[1]][tensorShape6[2]];
			tensor.copyTo(imageMatrix3D);
			
			
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
								correcImage[x][y][c][z][t] = (double) imageMatrix3D[A][B][C];
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
	
	public static ImagePlus copyData2Image2D(Tensor<?> tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "HWCZN";
		
		ImagePlus imPlus = null;
		long[] longShape = tensor.shape();
		int batchIndex = form.indexOf("N");
		if (batchIndex == -1 || longShape[batchIndex] == 1) {
			int[] tensorShape = new int[longShape.length];
			for (int i = 0; i < tensorShape.length; i ++) {
				tensorShape[i] = (int) longShape[i];
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensorShape6 = longShape6(tensorShape);
			float[][] imgMatrix2D = new float[tensorShape6[0]][tensorShape6[1]];
			tensor.copyTo(imgMatrix2D);
			
			
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
								correctImage[x][y][c][z][t] = (double) imgMatrix2D[A][B];
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
		String[] dimList = { "W", "H", "C", "D", "N" };
		int[] positionMapping = { 0, 1, 2, 3, 5 };
		String dimLetter;
		int position;
		int imPlusIndex;
		for (int index = 0; index < tensorShape.length; index++) {
			dimLetter = Character.toString(form.charAt(index));
			position = ArrayOperations.indexOf(dimList, dimLetter);
			if (position != -1) {
				imPlusIndex = positionMapping[position];
				shape[imPlusIndex] = tensorShape[index];
			}
		}
		return shape;
	}
	
	
	
	//// Method for both cases
	public static int[] createDimOrder(String[] originalOrder, String requiredOrder) {
		// Example: original_order = [c,d,e,b,a]; required_order = [d,e,b,c,a]
		// output--> dim_order = [3,0,1,2,4], because c goes in position 3, d in 0
		// position
		// and so on in the required_order array
		int size = originalOrder.length;
		int pos = 0;
		int[] dimOrder = new int[size];
		for (int i = 0; i < size; i++) {
			pos = requiredOrder.indexOf(originalOrder[i]);
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

}

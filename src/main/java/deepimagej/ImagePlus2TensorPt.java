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

import org.tensorflow.Tensor;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import deepimagej.exceptions.IncorrectNumberOfDimensions;
import deepimagej.tools.ArrayOperations;
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
			tensor = implus2NDArray(img, form, manager);
		}
		return tensor;
	}
	
	public static NDArray implus2NDArray(ImagePlus img, String form, NDManager manager){
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
	
	public static Tensor<Float> implus2TensorFloat(ImagePlus img, String form){
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

		long[] arrayShape = new long[form.length()];;
		if (form.indexOf("B") != -1) {
			fBatch = form.indexOf("B");
			tensorDims[fBatch] = batch;
		} else {
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
	 	
		Tensor<Float> tensor = Tensor.create(arrayShape, outBuff);
	return tensor;
	}
	
	
	/////////// Methods to transform an NDArray tensor into an ImageJ ImagePlus
	
	
	public static ImagePlus NDArray2ImagePlus(NDArray tensor, String form, String name) throws IncorrectNumberOfDimensions{
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "XYCZB";
		
		ImagePlus imPlus = null;
		long[] tensorShape = tensor.getShape().getShape();
		if (tensorShape.length != form.length())
			throw new IncorrectNumberOfDimensions(tensorShape, form, name);
		int[] completeTensorShape = longShape6(tensorShape);
		int[] imageDims = {1, 1, 1, 1, 1};
		
		int batchIndex = form.indexOf("B");
		if (batchIndex == -1 || tensorShape[batchIndex] == (long) 1) {
			int fBatch;
			if (form.indexOf("B") != -1) {
				fBatch = form.indexOf("B");
				imageDims[4] = (int) tensorShape[fBatch];
			} else {
				fBatch = form.length();
				form += "B";
			}
			int fHeight;
			if (form.indexOf("Y") != -1) {
				fHeight = form.indexOf("Y");
				imageDims[1] = (int) tensorShape[fHeight];
			} else {
				fHeight = form.length();
				form += "Y";
			}
			int fWidth;
			if (form.indexOf("X") != -1) {
				fWidth = form.indexOf("X");
				imageDims[0] = (int) tensorShape[fWidth];
			} else {
				fWidth = form.length();
				form += "X";
			}
			int fChannel;
			if (form.indexOf("C") != -1) {
				fChannel = form.indexOf("C");
				imageDims[2] = (int) tensorShape[fChannel];
			} else {
				fChannel = form.length();
				form += "C";
			}
			int fDepth;
			if (form.indexOf("Z") != -1) {
				fDepth = form.indexOf("Z");
				imageDims[3] = (int) tensorShape[fDepth];
			} else {
				fDepth = form.length();
				form += "Z";
			}
			
			float[] flatImageArray = tensor.toFloatArray();
			double[][][][][] matImage = new double[imageDims[0]][imageDims[1]][imageDims[2]][imageDims[3]][imageDims[4]];
			
			int pos = 0;
			int[] auxInd = {0, 0, 0, 0, 0};
			for (int i0 = 0; i0 < completeTensorShape[0]; i0 ++) {
				auxInd[0] = i0;
				for (int i1 = 0; i1 < completeTensorShape[1]; i1 ++) {
					auxInd[1] = i1;
					for (int i2 = 0; i2 < completeTensorShape[2]; i2 ++) {
						auxInd[2] = i2;
						for (int i3 = 0; i3 < completeTensorShape[3]; i3 ++) {
							auxInd[3] = i3;
							for (int i4 = 0; i4 < completeTensorShape[4]; i4 ++) {
								auxInd[4] = i4;
								matImage[auxInd[fWidth]][auxInd[fHeight]][auxInd[fChannel]][auxInd[fDepth]][auxInd[fBatch]] = (double) flatImageArray[pos ++];
							}
						}
					}
				}
			}
			imPlus = ArrayOperations.convertArrayToImagePlus(matImage, imageDims);
		} else {
			IJ.error("Sorry only batch size equal to 1 is allowed.");
		}
		return imPlus;
	}	
	
	// TODO make specific for different types
	public static ImagePlus tensor2ImagePlus(Tensor<?> tensor, String form, String name) throws IncorrectNumberOfDimensions{
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider N as T,
		// as for the moment both are going to be 1
		String imPlusForm = "XYCZB";
		
		ImagePlus imPlus = null;
		long[] tensorShape = tensor.shape();
		if (tensorShape.length != form.length())
			throw new IncorrectNumberOfDimensions(tensorShape, form, name);
		int[] completeTensorShape = longShape6(tensorShape);
		int[] imageDims = {1, 1, 1, 1, 1};
		
		int batchIndex = form.indexOf("B");
		if (batchIndex == -1 || tensorShape[batchIndex] == (long) 1) {
			int fBatch;
			if (form.indexOf("B") != -1) {
				fBatch = form.indexOf("B");
				imageDims[4] = (int) tensorShape[fBatch];
			} else {
				fBatch = form.length();
				form += "B";
			}
			int fHeight;
			if (form.indexOf("Y") != -1) {
				fHeight = form.indexOf("Y");
				imageDims[1] = (int) tensorShape[fHeight];
			} else {
				fHeight = form.length();
				form += "Y";
			}
			int fWidth;
			if (form.indexOf("X") != -1) {
				fWidth = form.indexOf("X");
				imageDims[0] = (int) tensorShape[fWidth];
			} else {
				fWidth = form.length();
				form += "X";
			}
			int fChannel;
			if (form.indexOf("C") != -1) {
				fChannel = form.indexOf("C");
				imageDims[2] = (int) tensorShape[fChannel];
			} else {
				fChannel = form.length();
				form += "C";
			}
			int fDepth;
			if (form.indexOf("Z") != -1) {
				fDepth = form.indexOf("Z");
				imageDims[3] = (int) tensorShape[fDepth];
			} else {
				fDepth = form.length();
				form += "Z";
			}
			
			float[] flatImageArray = new float[imageDims[0] * imageDims[1] * imageDims[2] * imageDims[3] * imageDims[4]];

			FloatBuffer outBuff = FloatBuffer.wrap(flatImageArray);
		 	tensor.writeTo(outBuff);
			double[][][][][] matImage = new double[imageDims[0]][imageDims[1]][imageDims[2]][imageDims[3]][imageDims[4]];
			
			int pos = 0;
			int[] auxInd = {0, 0, 0, 0, 0};
			for (int i0 = 0; i0 < completeTensorShape[0]; i0 ++) {
				auxInd[0] = i0;
				for (int i1 = 0; i1 < completeTensorShape[1]; i1 ++) {
					auxInd[1] = i1;
					for (int i2 = 0; i2 < completeTensorShape[2]; i2 ++) {
						auxInd[2] = i2;
						for (int i3 = 0; i3 < completeTensorShape[3]; i3 ++) {
							auxInd[3] = i3;
							for (int i4 = 0; i4 < completeTensorShape[4]; i4 ++) {
								auxInd[4] = i4;
								matImage[auxInd[fWidth]][auxInd[fHeight]][auxInd[fChannel]][auxInd[fDepth]][auxInd[fBatch]] = (double) flatImageArray[pos ++];
							}
						}
					}
				}
			}
			imPlus = ArrayOperations.convertArrayToImagePlus(matImage, imageDims);
		} else {
			IJ.error("Sorry only batch size equal to 1 is allowed.");
		}
		return imPlus;
	}	
	
	private static int[] longShape6(long[] shape) {
		// First convert add the needed entries with value 1 to the array
		// until its length is 5
		int[] f_shape = { 1, 1, 1, 1, 1, 1 };
		for (int i = 0; i < shape.length; i++) {
			f_shape[i] = (int) shape[i];
		}
		return f_shape;
	}
}

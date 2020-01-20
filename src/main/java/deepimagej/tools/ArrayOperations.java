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

package deepimagej.tools;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class ArrayOperations {

	public static ImagePlus convertArrayToImagePlus(double[][][][][] array, int[] shape) {
		int nx = shape[0];
		int ny = shape[1];
		int nz = shape[3];
		int nc = shape[2];
		int nt = shape[4];
		ImagePlus imp = IJ.createImage("out", "32-bit", nx, ny, nc, nz, nt);
		for (int t = 0; t < nt; t++) {
			for (int c = 0; c < nc; c++) {
				for (int z = 0; z < nz; z++) {
					imp.setPositionWithoutUpdate(c + 1, z + 1, t + 1);
					ImageProcessor ip = imp.getProcessor();
					for (int x = 0; x < nx; x++)
						for (int y = 0; y < ny; y++)
							ip.putPixelValue(x, y, array[x][y][c][z][t]);
				}
			}
		}
		return imp;
	}

	public static ImagePlus extractPatch(ImagePlus image, int sPatch, int xStart, int yStart,
										int overlapX, int overlapY, int channels) {
		// This method obtains a patch with the wanted size, starting at 'x_start' and
		// 'y_start' and returns it as RandomAccessibleInterval with the dimensions
		// already adjusted
		ImagePlus patchImage = IJ.createImage("aux", "32-bit", sPatch, sPatch, channels, 1, 1);
		for (int c = 0; c < channels; c++) {
			image.setPositionWithoutUpdate(c + 1, 1, 1);
			patchImage.setPositionWithoutUpdate(c + 1, 1, 1);
			ImageProcessor ip = image.getProcessor();
			ImageProcessor op = patchImage.getProcessor();
			// The actual patch with false and true information goes from patch_size/2
			// number of pixels before the actual start of the patch until patch_size/2 number of pixels after
			int xi = -1;
			int yi = -1;
			for (int x = xStart - overlapX; x < xStart - overlapX + sPatch; x++) {
				xi++;
				yi = -1;
				for (int y = yStart - overlapY; y < yStart - overlapY + sPatch; y++) {
					yi++;
					op.putPixelValue(xi, yi, (double) ip.getPixelValue(x, y));
				}
			}
			patchImage.setProcessor(op);
		}
		return patchImage;
	}

	public static void imagePlusReconstructor(ImagePlus fImage, ImagePlus patch,
											   int xImageStartPatch, int xImageEndPatch,
											   int yImageStartPatch, int yImageEndPatch,
											   int leftoverX, int leftoverY) {
		// This method inserts the pixel values of the true part of the patch into its corresponding location
		// in the image
		int[] patchDimensions = patch.getDimensions();
		int channels = patchDimensions[2];
		int slices = patchDimensions[3];
		ImageProcessor patchIp;
		ImageProcessor imIp;
		// Horizontal size of the roi
		int roiX = xImageEndPatch - xImageStartPatch;
		// Vertical size of the roi
		int roiY = yImageEndPatch - yImageStartPatch;
		
		
		for (int z = 0; z < slices; z ++) {
			for (int c = 0; c < channels; c ++) {
				int xImage = xImageStartPatch - 1;
				int yImage = yImageStartPatch - 1;
				patch.setPositionWithoutUpdate(c + 1, z + 1, 1);
				fImage.setPositionWithoutUpdate(c + 1, z + 1, 1);
				patchIp = patch.getProcessor();
				imIp = fImage.getProcessor();
				// The information non affected by 'the edge effect' is the one important to us. 
				// This is why we only take the center of the patch. The size of this center is 
				// the size of the patch minus the distorted number of pixels at each side (overlap)
				for (int xMirror = leftoverX; xMirror < leftoverX + roiX; xMirror ++) {
					xImage ++;
					yImage = yImageStartPatch - 1;
					for (int yMirror = leftoverY; yMirror < leftoverY + roiY; yMirror ++) {
						yImage ++;
						imIp.putPixelValue(xImage, yImage, (double) patchIp.getPixelValue(xMirror, yMirror));
					}
				}
				fImage.setProcessor(imIp);
			}
		}
	}
	
	
	public static int[] findAddedPixels(int xSize, int ySize, int overlap, int roiSize) {
		// This method calculates the number of pixels that have to be
		// added at each side of the image to create the mirrored image with the exact needed size
		// The resulting vector is a 4 dims vector of this shape --> [x_left, x_right, y_top, y_bottom]
		int[] extraPixels = new int[4];
		int neededX;
		if (roiSize > xSize) {
			neededX = roiSize - xSize + 2 * overlap;
		} else {
			neededX = 2 * overlap;
		}
		
		int neededY;
		if (roiSize > ySize) {
			neededY = roiSize - ySize + 2 * overlap;
		} else {
			neededY =  2 * overlap;
		}
		
		int xLeft = (int) Math.ceil((double) neededX / 2);
		int xRight = neededX - xLeft;

		int yTop = (int) Math.ceil((double) neededY / 2);
		int yBottom = neededY - yTop;
		extraPixels[0] = xLeft; extraPixels[1] = xRight;
		extraPixels[2] = yTop; extraPixels[3] = yBottom;
		return extraPixels;
	}


	/**
	 * Find the index of the of the first entry of the array that coincides with the variable 'element'
	 * @param array
	 * @param element
	 * @return index
	 */
	public static int indexOf(int[] array, int element) {
		boolean found = false;
		int counter = 0;
		int index = -1;
		int array_pos = 0;
		while (counter < array.length && found == false) {
			array_pos = array[counter];
			if (array_pos == element) {
				found = true;
				index = counter;
			}
			counter++;
		}
		return index;
	}

	/**
	 * Find the index of the of the first entry of the array that coincides with the
	 * variable 'element'
	 * 
	 * @param array
	 * @param element
	 * @return index
	 */
	public static int indexOf(String[] array, String element) {
		boolean found = false;
		int counter = 0;
		int index = -1;
		String arrayPos;
		while (counter < array.length && found == false) {
			arrayPos = array[counter];
			if (arrayPos.equals(element) == true) {
				found = true;
				index = counter;
			}
			counter++;
		}
		return index;
	}

	/**
	 * Finds the index of the of the first entry of the array that coincides with
	 * the variable 'element' starting at start
	 * 
	 * @param array
	 * @param element
	 * @param start
	 * @return index
	 */
	public static int indexOf(int[] array, int element, int start) {
		boolean found = false;
		int counter = start;
		int index = -1;
		int array_pos = 0;
		while (counter <  array.length && found == false) {
			array_pos = array[counter];
			if (array_pos == element) {
				found = true;
				index = counter;
			}
			counter++;
		}
		return index;
	}

}

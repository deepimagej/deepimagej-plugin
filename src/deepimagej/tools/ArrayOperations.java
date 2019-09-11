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

	public static double[][] iProcessor2matrix(ImageProcessor image) {
		// this method transforms an image processor into a matrix
		double pixelVal = 0;
		int ySize = image.getHeight();
		int xSize = image.getWidth();
		double[][] matImage = new double[xSize][ySize];
		for (int y = 0; y < ySize; y++) {
			for (int x = 0; x < xSize; x++) {
				pixelVal = (double) image.getPixelValue(x, y);
				matImage[x][y] = pixelVal;
			}
		}
		return matImage;
	}

	public static ImageProcessor matrix2iProcessor(double[][] matImage, int xSize, int ySize, ImageProcessor ip) {
		// This method transforms a matrix of 2d into an image processor
		for (int x = 0; x < xSize; x++) {
			for (int y = 0; y < ySize; y++) {
				ip.putPixelValue(x, y, matImage[x][y]);
			}
		}
		return ip;
	}

	public static ImagePlus extractPatch(ImagePlus image, int xStart, int yStart,
			int roi, int overlap, int channels) {
		// This method obtains a patch with the wanted size, starting at 'x_start' and
		// 'y_start' and returns it as RandomAccessibleInterval with the dimensions
		// already adjusted
		ImagePlus patchImage = IJ.createImage("aux", "32-bit", roi + overlap * 2, roi + overlap * 2,
				channels, 1, 1);
		for (int c = 0; c < channels; c++) {
			image.setPositionWithoutUpdate(c + 1, 1, 1);
			patchImage.setPositionWithoutUpdate(c + 1, 1, 1);
			ImageProcessor ip = image.getProcessor();
			ImageProcessor op = patchImage.getProcessor();
			// The actual patch with false and true information goes from patch_size/2
			// number of pixels before the actual start of the patch until patch_size/2 number of pixels after
			int xi = -1;
			int yi = -1;
			for (int x = xStart - overlap; x < xStart + roi + overlap - 1; x++) {
				xi++;
				yi = -1;
				for (int y = yStart - overlap; y < yStart + roi + overlap - 1; y++) {
					yi++;
					op.putPixelValue(xi, yi, (double) ip.getPixelValue(x, y));
				}
			}
			patchImage.setProcessor(op);
		}
		return patchImage;
	}

	// TODO implement constraints in patch size because of the image
	public static int findPatchSize(int minPatchMultiple, boolean fixedPatchSize) {
		// Find the size of the patches to process the image. It will
		// be around the defined constant 'approx_size'
		int patchSize;
		int estimatedSize = 200;
		if (minPatchMultiple > estimatedSize || fixedPatchSize == true) {
			patchSize = minPatchMultiple;
		}
		else {
			int n_patches = estimatedSize / minPatchMultiple;
			patchSize = (n_patches + 1) * minPatchMultiple;
		}
		return patchSize;
	}

	public static void imagePlusReconstructor(ImagePlus fImage, ImagePlus patch,
											   int xStartPatch, int xEndPatch,
											   int yStartPatch, int yEndPatch,
											   int xStartIMage, int yStartImage,
											   int overlap, int roiOverlapXLeft,
											   int roiOverlapXRight, int roiOverlapYTop,
											   int roiOverlapYBottom) {
		// This method inserts the pixel values of the true part of the patch into its corresponding location
		// in the image
		int[] patchDimensions = patch.getDimensions();
		int channels = patchDimensions[2];
		int slices = patchDimensions[3];
		int[] fImageDimensions = fImage.getDimensions();
		int fxLength = fImageDimensions[0];
		int fyLength = fImageDimensions[1];
		ImageProcessor patchIp;
		ImageProcessor imIp;
		for (int z = 0; z < slices; z ++) {
			for (int c = 0; c < channels; c ++) {
				patch.setPositionWithoutUpdate(c + 1, z + 1, 1);
				fImage.setPositionWithoutUpdate(c + 1, z + 1, 1);
				patchIp = patch.getProcessor();
				imIp = fImage.getProcessor();
				// The number of false pixels at each side of the part of interest
				// is the overlap
				int xStart = overlap;
				int yStart = overlap;
				
				int xEnd = xStart + (xEndPatch - xStartPatch);
				int yEnd = yStart + (yEndPatch - yStartPatch);
				
				int endImageX = xStartIMage + fxLength;
				int endImageY = yStartImage + fyLength;
				
				if (xEndPatch > endImageX) {
					xEnd = xEnd - (xEndPatch - endImageX);
				}
				
				if (yEndPatch > endImageY) {
					yEnd = yEnd - (yEndPatch - endImageY);
				}
				// The information non affected by 'the edge effect' is the one important to us. 
				// This is why we only take the center of the patch. The size of this center is 
				// the size of the patch minus the distorted number of pixels at each side (overlap)
				int xPatch = xStartPatch - xStartIMage - 1 + (int) Math.ceil((double)(roiOverlapXLeft/2));
				int yPatch = yStartPatch - yStartImage - 1 + (int) Math.ceil((double)(roiOverlapYTop/2));
				for (int x = xStart  + (int) Math.ceil((double)(roiOverlapXLeft/2)); x < xEnd - (int) Math.floor((double)(roiOverlapXRight/2)); x ++) {
					xPatch ++;
					yPatch = yStartPatch - yStartImage - 1 + (int) Math.ceil((double)(roiOverlapYTop/2));
					for (int y = yStart + (int) Math.ceil((double)(roiOverlapYTop/2)); y < yEnd - (int) Math.floor((double)(roiOverlapYBottom/2)); y ++) {
						yPatch ++;
						if (xPatch >= 0 && yPatch >= 0) {
							imIp.putPixelValue(xPatch, yPatch, (double) patchIp.getPixelValue(x, y));
						}
					}
				}
				fImage.setProcessor(imIp);
			}
		}
	}
	
	
	public static int[] patchOverlapVerification(int minMult, boolean fixed) {
		// Now find the true and total patch size, the padding size and create the
		// mirrored image.
		// If the developer marked the patch size as fixed, the true patch size will be
		// the one indicated
		// by him, if not, the size of the patch will be the first multiple above 200 of
		// the number
		// given, or if it is already over 200, that same number.
		// Also the total patch size will never be bigger than the size of the image
		int totalPatch = ArrayOperations.findPatchSize(minMult, fixed);
		// By default the overlap is a fourth of the patch size, and should not be
		// bigger than that
		int overlap = totalPatch / 4;
		int[] patchInfo = new int[2];
		patchInfo[0] = totalPatch;
		patchInfo[1] = overlap;

		return patchInfo;
	}
	/*
	public static double[][] mirrorXY(double[][] image, int[] paddingSize){
		int x_size1 = image.length;
		int y_size1 = image[0].length;
		int x_size = image.length + paddingSize[0] + paddingSize[1];
		int y_size = image[0].length + paddingSize[2] + paddingSize[3];
		double[][] im_padded  = new double[x_size][y_size];
		
		for (int x = 0; x < x_size; x ++) {
			for (int y = 0; y < y_size; y ++) {
				// left top corner
				if (x < paddingSize[0] && y < paddingSize[2]) {
					im_padded[x][y] = image[paddingSize[0] - 1 - x][paddingSize[2] - 1 - y];
					
				// right bottom corner
				} else if (x >= x_size - paddingSize[1] && y >= y_size - paddingSize[3]) {
					im_padded[x][y] = image[2*x_size1 + paddingSize[0] - 1 - x][2*y_size1 + paddingSize[2] - 1 -  y];
					
				// left bottom corner
				} else if (x < paddingSize[0] && y >= y_size - paddingSize[3]) {
					//im_padded[x][y] = image[padding_size[0] - x - 1][2*y_size - 3*padding_size[3] - 1 -  y];
					im_padded[x][y] = image[paddingSize[0] - x - 1][2*y_size1 + paddingSize[2] - 1 -  y];
					
				// right top corner
				} else if (x >= x_size - paddingSize[1] && y < paddingSize[2]) {
					im_padded[x][y] = image[2*x_size1 + paddingSize[0] - 1 - x][paddingSize[2] - 1 - y];
					
				// left middle
				} else if (x < paddingSize[0]) {
					im_padded[x][y] = image[paddingSize[0] - x - 1][y - paddingSize[2]];
					
				// top middle
				} else if (y < paddingSize[2]) {
					im_padded[x][y] = image[x - paddingSize[0]][paddingSize[2] - 1 - y];
					
				// bottom middle
				} else if (y >= y_size - paddingSize[3]) {
					im_padded[x][y] = image[x - paddingSize[0]][2*y_size1 + paddingSize[2] - 1 -  y];
					
				// right middle
				} else if (x >= x_size - paddingSize[1]) {
					im_padded[x][y] = image[2*x_size1 + paddingSize[0] - 1 - x][y - paddingSize[2]];
					
				} else {
					im_padded[x][y] = image[x - paddingSize[0]][y - paddingSize[2]];
				}
			}
		}
	return im_padded;
	}*/
	
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


	public static int[] findStartOfRoi(int nx, int totalRoiX, int ny, int totalRoiY) {
		int[] start = new int[2];
		int extraX = totalRoiX - nx;
		int extraXRight = (int) Math.ceil((double) extraX/2);
		int extraY = totalRoiY - ny;
		int extraYTop = (int) Math.ceil((double) extraY/2);
		start[0] = extraXRight; start[1] = extraYTop;
		return start;
	}


	public static int[] findOverlapRoi(int size, int regionOfInterest, int nPatch) {
		// This method finds the number of pixels that need to overlap between ROIs.
		// The vector calculated represents the pixels in each of the overlaps.
		
		// The array of overlaps is bigger because we consider overlap of both sides
		// of the ROIs
		int[] overlap = new int[nPatch + 1];
		if (size <= regionOfInterest) {
			overlap[0] = 0;
		} else {
			int extraPixels = regionOfInterest * nPatch - size;
			int overlapPixels = (int) Math.ceil((double)extraPixels / (double)(nPatch - 1));
			for (int i = 1; i < nPatch; i ++) {
				if (i != nPatch - 1) {
					overlap[i] = overlapPixels;
					extraPixels = extraPixels - overlapPixels;
				} else {
					// The last overlap is whatever the pixels are remaining
					overlap[i] = extraPixels;
				}
			}
		}
		return overlap;
	}
}

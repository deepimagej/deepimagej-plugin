package deepimagej;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class ArrayOperations {

	public static ImagePlus createMirroredImage(ImagePlus imp, int mirror_size, int x_size, int y_size, int channels) {
		// Obtains the mirrored image, whose size is going to be the size of the
		// original image plus 2 times
		// the size of the mirror ( 2 times because a mirror is create at both ends of
		// the image)
		double[][] mirror_mat = new double[x_size + 2 * mirror_size][y_size + 2 * mirror_size];
		int x_size2 = x_size + 2 * mirror_size;
		int y_size2 = y_size + 2 * mirror_size;
		ImagePlus out = IJ.createImage("aux", "32-bit", x_size2, y_size2, channels, 1, 1);
		for (int i = 0; i < channels; i++) {
			out.setPositionWithoutUpdate(i + 1, 1, 1);
			imp.setPositionWithoutUpdate(i + 1, 1, 1);
			ImageProcessor ip = imp.getProcessor();
			ImageProcessor ip_mirror = out.getProcessor();
			double[][] mat_image = iProcessor2matrix(ip);
			mirror_mat = mirrorXY(mat_image, mirror_size);
			ip_mirror = matrix2iProcessor(mirror_mat, x_size + 2 * mirror_size, y_size + 2 * mirror_size, ip_mirror);
			out.setProcessor(ip_mirror);
		}
		return out;
	}

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
		double pixel_val = 0;
		int y_size = image.getHeight();
		int x_size = image.getWidth();
		double[][] mat_image = new double[x_size][y_size];
		for (int y = 0; y < y_size; y++) {
			for (int x = 0; x < x_size; x++) {
				pixel_val = (double) image.getPixelValue(x, y);
				mat_image[x][y] = pixel_val;
			}
		}
		return mat_image;
	}

	public static ImageProcessor matrix2iProcessor(double[][] mat_image, int x_size, int y_size, ImageProcessor ip) {
		// This method transforms a matrix of 2d into an image processor
		for (int x = 0; x < x_size; x++) {
			for (int y = 0; y < y_size; y++) {
				ip.putPixelValue(x, y, mat_image[x][y]);
			}
		}
		return ip;
	}

	public static ImagePlus extractPatch(ImagePlus image, int x_start, int y_start,
			int roi, int overlap, int[] tensor_dim, int channels) {
		// This method obtains a patch with the wanted size, starting at 'x_start' and
		// 'y_start' and returns it as RandomAccessibleInterval with the dimensions
		// already adjusted
		ImagePlus patch_image = IJ.createImage("aux", "32-bit", roi + overlap * 2, roi + overlap * 2,
				channels, 1, 1);
		for (int c = 0; c < channels; c++) {
			image.setPositionWithoutUpdate(c + 1, 1, 1);
			patch_image.setPositionWithoutUpdate(c + 1, 1, 1);
			ImageProcessor ip = image.getProcessor();
			ImageProcessor op = patch_image.getProcessor();
			// The actual patch with false and true information goes from patch_size/2
			// number of pixels before the actual start of the patch until patch_size/2 number of pixels after
			int xi = -1;
			int yi = -1;
			for (int x = x_start - overlap; x < x_start + roi + overlap - 1; x++) {
				xi++;
				yi = -1;
				for (int y = y_start - overlap; y < y_start + roi + overlap - 1; y++) {
					yi++;
					op.putPixelValue(xi, yi, (double) ip.getPixelValue(x, y));
				}
			}
			patch_image.setProcessor(op);
		}
		return patch_image;
	}

	// TODO implement constraints in patch size because of the image
	public static int findPatchSize(int min_patch_multiple, boolean fixed_patch_size) {
		// Find the size of the patches to process the image. It will
		// be around the defined constant 'approx_size'
		int patch_size;
		int estimated_size = 200;
		if (min_patch_multiple > estimated_size || fixed_patch_size == true) {
			patch_size = min_patch_multiple;
		}
		else {
			int n_patches = estimated_size / min_patch_multiple;
			patch_size = (n_patches + 1) * min_patch_multiple;
		}
		return patch_size;
	}

	public static int paddingSize(int x_size, int y_size, int patch_size, int overlap) {
		// This method obtains the size of the padding that needs to be added
		// to the sides of the image.
		// This method calculates automatically the needed mirroring considering
		// that from the total patch we are only going to keep the center of the result,
		// removing a fourth of the total size at each side.
		// The maximum pad allowed will be that one that increases the image by its size
		// at
		// each side. For example a 128x128 image will be converted in a 354x354 image
		// at most
		// The return 'pad' is the number of pixels we have to add at each extreme
		// of each row. For example if an image is 512x512, and pad is 148, the final
		// image will be 808x808
		int pad;
		// First calculate the number of patches that would be needed if we only took
		// the
		// center of the patch for each calculation
		int relevant_patch = patch_size - overlap * 2;

		int x_patches = x_size / relevant_patch;
		int x_remaining_pixels = x_size - x_patches * relevant_patch;
		int y_patches = y_size / relevant_patch;
		int y_remaining_pixels = y_size - y_patches * relevant_patch;
		pad = x_remaining_pixels;
		if (pad > y_remaining_pixels) {
			pad = y_remaining_pixels;
		}
		// 'patch_size' is the size of the total patch. The
		// actual size of the area of interest is half of that.
		// This is why we are adding at the end half of
		// the region of interest
		// --> one region of interest + 2*patch sides = 1 total patch
		pad = relevant_patch - pad + overlap;
		if (pad > x_size || pad > y_size) {
			pad = x_size;
		}
		return pad;
	}

	public static void imagePlusReconstructor(ImagePlus f_image, ImagePlus patch, int x_start, int y_start,
			int true_patch_size, int overlap) {
		// This method inserts the pixel values of the true part of the patch into its
		// corresponding location
		// in the image
		int[] patch_dimensions = patch.getDimensions();
		int channels = patch_dimensions[2];
		int slices = patch_dimensions[3];
		int[] f_image_dimensions = f_image.getDimensions();
		int fx_length = f_image_dimensions[0];
		int fy_length = f_image_dimensions[1];
		ImageProcessor patch_ip;
		ImageProcessor im_ip;
		for (int z = 0; z < slices; z++) {
			for (int c = 0; c < channels; c++) {
				patch.setPositionWithoutUpdate(c + 1, z + 1, 1);
				f_image.setPositionWithoutUpdate(c + 1, z + 1, 1);
				patch_ip = patch.getProcessor();
				im_ip = f_image.getProcessor();
				// The number of false pixels at each side of the part of interest
				// is the overlap
				int false_pixels = overlap;
				int x_patch = false_pixels - 1;
				int y_patch = false_pixels - 1;

				int x_end = x_start + true_patch_size;
				if (x_end > fx_length) {
					x_end = fx_length;
				}
				int y_end = y_start + true_patch_size;
				if (y_end > fy_length) {
					y_end = fy_length;
				}
				// The information non affected by 'the edge effect' is the one important to us.
				// This is why we only take the center of the patch. The size of this center is
				// the size of the patch minus the distorted number of pixels at each side
				// (overlap)
				for (int x = x_start; x < x_end; x++) {
					x_patch++;
					y_patch = false_pixels - 1;
					for (int y = y_start; y < y_end; y++) {
						y_patch++;
						im_ip.putPixelValue(x, y, (double) patch_ip.getPixelValue(x_patch, y_patch));
					}
				}
				f_image.setProcessor(im_ip);
			}
		}
	}

	public static int[] patchOverlapVerification(int min_mult, boolean fixed) {
		// Now find the true and total patch size, the padding size and create the
		// mirrored image.
		// If the developer marked the patch size as fixed, the true patch size will be
		// the one indicated
		// by him, if not, the size of the patch will be the first multiple above 200 of
		// the number
		// given, or if it is already over 200, that same number.
		// Also the total patch size will never be bigger than the size of the image
		int total_patch = ArrayOperations.findPatchSize(min_mult, fixed);
		// By default the overlap is a fourth of the patch size, and should not be
		// bigger than that
		int overlap = total_patch / 4;
		int[] patch_info = new int[2];
		patch_info[0] = total_patch;
		patch_info[1] = overlap;

		return patch_info;
	}

	public static double[][] mirrorXY(double[][] image, int padding_size) {
		int x_size = image.length + 2 * padding_size;
		int y_size = image[0].length + 2 * padding_size;
		double[][] im_padded = new double[x_size][y_size];

		for (int x = 0; x < x_size; x++) {
			for (int y = 0; y < y_size; y++) {
				if (x < padding_size && y < padding_size) {
					im_padded[x][y] = image[padding_size - 1 - x][padding_size - 1 - y];

				}
				else
					if (x >= x_size - padding_size && y >= y_size - padding_size) {
						im_padded[x][y] = image[2 * x_size - 3 * padding_size - 1 - x][2 * y_size - 3 * padding_size - 1
								- y];

					}
					else
						if (x < padding_size && y >= y_size - padding_size) {
							im_padded[x][y] = image[padding_size - x - 1][2 * y_size - 3 * padding_size - 1 - y];

						}
						else
							if (x >= x_size - padding_size && y < padding_size) {
								im_padded[x][y] = image[2 * x_size - 3 * padding_size - 1 - x][padding_size - 1 - y];

							}
							else
								if (x < padding_size) {
									im_padded[x][y] = image[padding_size - x - 1][y - padding_size];

								}
								else
									if (y < padding_size) {
										im_padded[x][y] = image[x - padding_size][padding_size - 1 - y];

									}
									else
										if (y >= y_size - padding_size) {
											im_padded[x][y] = image[x - padding_size][2 * y_size - 3 * padding_size - 1
													- y];

										}
										else
											if (x >= x_size - padding_size) {
												im_padded[x][y] = image[2 * x_size - 3 * padding_size - 1 - x][y
														- padding_size];

											}
											else {
												im_padded[x][y] = image[x - padding_size][y - padding_size];
											}
			}
		}
		return im_padded;
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
		String array_pos;
		while (counter < array.length && found == false) {
			array_pos = array[counter];
			if (array_pos.equals(element) == true) {
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

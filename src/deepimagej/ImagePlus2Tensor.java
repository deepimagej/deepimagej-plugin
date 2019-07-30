package deepimagej;

import org.tensorflow.Tensor;

import ij.ImagePlus;
import ij.process.ImageProcessor;


public class ImagePlus2Tensor {
	
	// Methods to transform a TensorFlow tensors into ImageJ ImagePlus

	public static Tensor<?> imPlus2tensor(ImagePlus img, String form, int channels){
		// Convert ImagePlus into tensor calling the corresponding
		// method depending on the dimensions of the required tensor 
		// Find the number of dimensions of the tensor
		int n_dim = form.length();
		Tensor<?> tensor = null;
		if (n_dim == 2) {
			float[][] mat_image = imPlus2matrix(img, form);
			tensor = tensor(mat_image);
		} else if (n_dim == 3) {
			float[][][] mat_image = imPlus2matrix3(img, form, channels);
			tensor = tensor(mat_image);
		} else if (n_dim ==4) {
			float[][][][] mat_image = imPlus2matrix4(img, form, channels);
			tensor = tensor(mat_image);
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
		float[][] mat_image;
		int[] dims = img.getDimensions();
		int x_size = dims[0];
		int y_size = dims[1];
		// Get the processor (matrix representing one slice, frame or channel)
		// of the ImagePlus
		ImageProcessor ip = img.getProcessor();
		
		if (form.equals("HW") == true) {
			mat_image = new float[y_size][x_size];
			mat_image = iProcessor2matrixHW(ip);
		} else {
			mat_image = new float[x_size][y_size];
			mat_image = iProcessor2matrixWH(ip);
		}
		return mat_image;
	}
	
	
	public static float[][][] imPlus2matrix3(ImagePlus img, String form, 
											int n_channels){
		// Create a float array of three dimensions out of an 
		// ImagePlus object
		float[][][] mat_image;
		// Initialize ImageProcessor variable used later
		ImageProcessor ip;
		int[] dims = img.getDimensions();
		int x_size = dims[0];
		int y_size = dims[1];
		// TODO allow different batch sizes
		int batch = 1;
		int[] tensor_dims = new int[3];
		// Create aux variable to indicate
		// if it is channels one of the dimensions of
		// the tensor or it is the batch size
		int f_channels_or_batch = -1;
		// Create auxiliary variable to represent the order
		// of the dimensions in the ImagePlus
		String[] implus_form = new String[3];
		
		if (form.indexOf("N") != -1) {
			f_channels_or_batch = form.indexOf("N");
			tensor_dims[f_channels_or_batch] = batch;
			// The third dimension of the tensor is batch size
			implus_form[0] = "N"; implus_form[1] =  "H";
			implus_form[2] = "W";
			}
		if (form.indexOf("H") != -1) {
			int f_height = form.indexOf("H");
			tensor_dims[f_height] = y_size;
		}
		if (form.indexOf("W") != -1) {
			int f_width = form.indexOf("W");
			tensor_dims[f_width] = x_size;
		}
		if (form.indexOf("C") != -1) {
			f_channels_or_batch = form.indexOf("C");
			tensor_dims[f_channels_or_batch] = n_channels;
			// The third dimension of the tensor is channels
			implus_form[0] = "C"; implus_form[1] =  "H";
			implus_form[2] = "W";
		}
		mat_image = new float[tensor_dims[0]][tensor_dims[1]][tensor_dims[2]];
	
		// Obtain the shapes association
		int[] dims_association = createDimOrder(implus_form, form);
		
		int[] aux_coord = {-1, -1, -1};
		for (int n = 0; n < tensor_dims[f_channels_or_batch]; n ++) {
			aux_coord[dims_association[0]] = n;
			for (int x = 0; x < x_size; x ++) {
				aux_coord[dims_association[1]] = x;
				for (int y = 0; y < y_size; y ++) {
					aux_coord[dims_association[2]] = y;
					img.setPositionWithoutUpdate(n + 1, 1, 1);
					ip = img.getProcessor();
					mat_image[aux_coord[0]][aux_coord[1]][aux_coord[2]] = ip.getPixelValue(x, y);
				}
			}
		}
		return mat_image;
	}
	
	
	public static float[][][][] imPlus2matrix4(ImagePlus img, String form,
											  int n_channels){
		// Create a float array of four dimensions out of an 
		// ImagePlus object
		float[][][][] mat_image;
		// Initialize ImageProcessor variable used later
		ImageProcessor ip;
		int[] dims = img.getDimensions();
		int x_size = dims[0];
		int y_size = dims[1];
		// TODO allow different batch sizes
		int batch = 1;
		int[] tensor_dims = new int[4];
		// Create aux variable to indicate
		// if it is channels one of the dimensions of
		// the tensor or it is the batch size
		int f_batch = -1;
		int f_channel = -1;
		// Create auxiliary variable to represent the order
		// of the dimensions in the ImagePlus
		String[] implus_form = {"N", "C", "H", "W"};
		
		if (form.indexOf("N") != -1) {
			f_batch = form.indexOf("N");
			tensor_dims[f_batch] = batch;
		}
		if (form.indexOf("H") != -1) {
			int f_height = form.indexOf("H");
			tensor_dims[f_height] = y_size;
		}
		if (form.indexOf("W") != -1) {
			int f_width = form.indexOf("W");
			tensor_dims[f_width] = x_size;
		}
		if (form.indexOf("C") != -1) {
			f_channel = form.indexOf("C");
			tensor_dims[f_channel] = n_channels;
		}
		mat_image = new float[tensor_dims[0]][tensor_dims[1]][tensor_dims[2]][tensor_dims[3]];
		
		// Obtain the shapes association
		int[] dims_association = createDimOrder(implus_form, form);
		
		int[] aux_coord = {-1, -1, -1, -1};
		for (int n = 0; n < tensor_dims[f_batch]; n ++) {
			aux_coord[dims_association[0]] = n;
			for (int c = 0; c < n_channels; c ++) {
				aux_coord[dims_association[1]] = c;
				for (int x = 0; x < x_size; x ++) {	
					aux_coord[dims_association[2]] = x;
					for (int y = 0; y < y_size; y ++) {
						aux_coord[dims_association[3]] = y;
						img.setPositionWithoutUpdate(c + 1, 1, 1);
						ip = img.getProcessor();
						mat_image[aux_coord[0]][aux_coord[1]][aux_coord[2]][aux_coord[3]] = ip.getPixelValue(x, y);
					}	
				}
			}
		}
		return mat_image;
		}	
	
	
	public static float[][] iProcessor2matrixWH(ImageProcessor image){
		// this method transforms an image processor into a matrix
		float pixel_val = 0;
		int y_size = image.getHeight();
		int x_size = image.getWidth();
		float[][] mat_image = new float[x_size][y_size];
		for (int y = 0; y < y_size; y ++) {
			for (int x = 0; x < x_size; x ++) {
				pixel_val = (float) image.getPixelValue(x, y);
				mat_image[x][y] = pixel_val;
			}
		}
		return mat_image;
	}

	
	public static float[][] iProcessor2matrixHW(ImageProcessor image){
		// this method transforms an image processor into a matrix
		float pixel_val = 0;
		int y_size = image.getHeight();
		int x_size = image.getWidth();
		float[][] mat_image = new float[y_size][x_size];
		for (int y = 0; y < y_size; y ++) {
			for (int x = 0; x < x_size; x ++) {
				pixel_val = (float) image.getPixelValue(x, y);
				mat_image[y][x] = pixel_val;
			}
		}
		return mat_image;
	}
	
	
	/////////// Methods to transform an TensorFlow tensor into an ImageJ ImagePlus
	
	
	public static ImagePlus tensor2ImagePlus(Tensor<?> tensor, String form) {
		//Method to transform an ImagePlus into a TensorFLow tensor of the
		// dimensions specified by form
		ImagePlus image;
		long[] tensor_shape = tensor.shape();
		if (tensor_shape.length == 2) {
			image = copyData2Image2D(tensor, form);
		}else if (tensor_shape.length == 3) {
			image = copyData2Image3D(tensor, form);
		}else if (tensor_shape.length == 4) {
			image = copyData2Image4D(tensor, form);
		}else {
			image = copyData2Image5D(tensor, form);
		}
		return image;
	}
	
	public static ImagePlus copyData2Image5D(Tensor<?> tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		ImagePlus im_plus = null;
		long[] long_shape = tensor.shape();
		int batch_index = form.indexOf("N");
		if (batch_index == -1 || long_shape[batch_index] == 1) {
			int[] tensor_shape = new int[long_shape.length];
			for (int i = 0; i < tensor_shape.length; i ++) {
				tensor_shape[i] = (int) long_shape[i];
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensor_shape_6 = longShape6(tensor_shape);
			float[][][][][] img_matrix_5d = new float[tensor_shape_6[0]][tensor_shape_6[1]][tensor_shape_6[2]][tensor_shape_6[3]][tensor_shape_6[4]];
			tensor.copyTo(img_matrix_5d);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] im_shape = getShape(tensor_shape, form);
			int[] im_shape_copy = {im_shape[0], im_shape[1], im_shape[2], im_shape[3], im_shape[4]};
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			double[][][][][] correct_image = new double[im_shape[0]][im_shape[1]][im_shape[2]][im_shape[3]][im_shape[4]];
			// Find the association between the tensor and the image dimensions
			int[] dimension_assotiation = imgTensorAssociation(tensor_shape_6, im_shape);
			
			int[] aux_array = {-1,-1,-1,-1,-1};
			int x = -1; int y = -1; int z = -1; int c = -1; int t = -1;
			for (int A = 0; A < tensor_shape_6[0]; A++) {
				aux_array[dimension_assotiation[0]] = A;
				for (int B = 0; B < tensor_shape_6[1]; B++) {
					aux_array[dimension_assotiation[1]] = B;
					for (int C = 0; C < tensor_shape_6[2]; C++) {
						aux_array[dimension_assotiation[2]] = C;
						for (int D = 0; D < tensor_shape_6[3]; D++) {
							aux_array[dimension_assotiation[3]] = D;
							for (int E = 0; E < tensor_shape_6[4]; E++) {
								aux_array[dimension_assotiation[4]] = E;
								x = aux_array[0];
								y = aux_array[1];
								c = aux_array[2];
								z = aux_array[3];
								t = aux_array[4];
								correct_image[x][y][c][z][t] = (double) img_matrix_5d[A][B][C][D][E];
							}
						}
					}
				}
			}
			im_plus = ArrayOperations.convertArrayToImagePlus(correct_image, im_shape_copy);
		}
		return im_plus;
	}
	
	
	public static ImagePlus copyData2Image4D(Tensor<?> tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		ImagePlus im_plus = null;
		long[] long_shape = tensor.shape();
		int batch_index = form.indexOf("N");
		if (batch_index == -1 || long_shape[batch_index] == 1) {
			int[] tensor_shape = new int[long_shape.length];
			for (int i = 0; i < tensor_shape.length; i ++) {
				tensor_shape[i] = (int) long_shape[i];
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensor_shape_6 = longShape6(tensor_shape);
			float[][][][] img_matrix_4d = new float[tensor_shape_6[0]][tensor_shape_6[1]][tensor_shape_6[2]][tensor_shape_6[3]];
			tensor.copyTo(img_matrix_4d);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] im_shape = getShape(tensor_shape, form);
			int[] im_shape_copy = {im_shape[0], im_shape[1], im_shape[2], im_shape[3], im_shape[4]};
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			double[][][][][] correct_image = new double[im_shape[0]][im_shape[1]][im_shape[2]][im_shape[3]][im_shape[4]];
			// Find the association between the tensor and the image dimensions
			int[] dimension_assotiation = imgTensorAssociation(tensor_shape_6, im_shape);
			
			int[] aux_array = {-1,-1,-1,-1,-1};
			int x = -1; int y = -1; int z = -1; int c = -1; int t = -1;
			for (int A = 0; A < tensor_shape_6[0]; A++) {
				aux_array[dimension_assotiation[0]] = A;
				for (int B = 0; B < tensor_shape_6[1]; B++) {
					aux_array[dimension_assotiation[1]] = B;
					for (int C = 0; C < tensor_shape_6[2]; C++) {
						aux_array[dimension_assotiation[2]] = C;
						for (int D = 0; D < tensor_shape_6[3]; D++) {
							aux_array[dimension_assotiation[3]] = D;
							for (int E = 0; E < tensor_shape_6[4]; E++) {
								aux_array[dimension_assotiation[4]] = E;
								x = aux_array[0];
								y = aux_array[1];
								c = aux_array[2];
								z = aux_array[3];
								t = aux_array[4];
								correct_image[x][y][c][z][t] = (double) img_matrix_4d[A][B][C][D];
							}
						}
					}
				}
			}
			im_plus = ArrayOperations.convertArrayToImagePlus(correct_image, im_shape_copy);
		}
		return im_plus;
	}
	
	public static ImagePlus copyData2Image3D(Tensor<?> tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		ImagePlus im_plus = null;
		long[] long_shape = tensor.shape();
		int batch_index = form.indexOf("N");
		if (batch_index == -1 || long_shape[batch_index] == 1) {
			int[] tensor_shape = new int[long_shape.length];
			for (int i = 0; i < tensor_shape.length; i ++) {
				tensor_shape[i] = (int) long_shape[i];
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensor_shape_6 = longShape6(tensor_shape);
			float[][][] img_matrix_3d = new float[tensor_shape_6[0]][tensor_shape_6[1]][tensor_shape_6[2]];
			tensor.copyTo(img_matrix_3d);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] im_shape = getShape(tensor_shape, form);
			int[] im_shape_copy = {im_shape[0], im_shape[1], im_shape[2], im_shape[3], im_shape[4]};
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			double[][][][][] correct_image = new double[im_shape[0]][im_shape[1]][im_shape[2]][im_shape[3]][im_shape[4]];
			// Find the association between the tensor and the image dimensions
			int[] dimension_assotiation = imgTensorAssociation(tensor_shape_6, im_shape);
			
			int[] aux_array = {-1,-1,-1,-1,-1};
			int x = -1; int y = -1; int z = -1; int c = -1; int t = -1;
			for (int A = 0; A < tensor_shape_6[0]; A++) {
				aux_array[dimension_assotiation[0]] = A;
				for (int B = 0; B < tensor_shape_6[1]; B++) {
					aux_array[dimension_assotiation[1]] = B;
					for (int C = 0; C < tensor_shape_6[2]; C++) {
						aux_array[dimension_assotiation[2]] = C;
						for (int D = 0; D < tensor_shape_6[3]; D++) {
							aux_array[dimension_assotiation[3]] = D;
							for (int E = 0; E < tensor_shape_6[4]; E++) {
								aux_array[dimension_assotiation[4]] = E;
								x = aux_array[0];
								y = aux_array[1];
								c = aux_array[2];
								z = aux_array[3];
								t = aux_array[4];
								correct_image[x][y][c][z][t] = (double) img_matrix_3d[A][B][C];
							}
						}
					}
				}
			}
			im_plus = ArrayOperations.convertArrayToImagePlus(correct_image, im_shape_copy);
		}
		return im_plus;
	}
	
	public static ImagePlus copyData2Image2D(Tensor<?> tensor, String form){
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		ImagePlus im_plus = null;
		long[] long_shape = tensor.shape();
		int batch_index = form.indexOf("N");
		if (batch_index == -1 || long_shape[batch_index] == 1) {
			int[] tensor_shape = new int[long_shape.length];
			for (int i = 0; i < tensor_shape.length; i ++) {
				tensor_shape[i] = (int) long_shape[i];
			}
			// Create an array with length 5 in case the length
			// of the shape array is smaller
			int[] tensor_shape_6 = longShape6(tensor_shape);
			float[][] img_matrix_2d = new float[tensor_shape_6[0]][tensor_shape_6[1]];
			tensor.copyTo(img_matrix_2d);
			
			
			// Prepare the dimensions of the imagePlus and create a copy
			// because method 'imgTensorAssociation' changes it
			int[] im_shape = getShape(tensor_shape, form);
			int[] im_shape_copy = {im_shape[0], im_shape[1], im_shape[2], im_shape[3], im_shape[4]};
			
			// Create the matrix containing the image. note that the dimensions are arranged differntly because in
			// imageJ channels go before slices
			double[][][][][] correct_image = new double[im_shape[0]][im_shape[1]][im_shape[2]][im_shape[3]][im_shape[4]];
			// Find the association between the tensor and the image dimensions
			int[] dimension_assotiation = imgTensorAssociation(tensor_shape_6, im_shape);
			
			int[] aux_array = {-1,-1,-1,-1,-1};
			int x = -1; int y = -1; int z = -1; int c = -1; int t = -1;
			for (int A = 0; A < tensor_shape_6[0]; A++) {
				aux_array[dimension_assotiation[0]] = A;
				for (int B = 0; B < tensor_shape_6[1]; B++) {
					aux_array[dimension_assotiation[1]] = B;
					for (int C = 0; C < tensor_shape_6[2]; C++) {
						aux_array[dimension_assotiation[2]] = C;
						for (int D = 0; D < tensor_shape_6[3]; D++) {
							aux_array[dimension_assotiation[3]] = D;
							for (int E = 0; E < tensor_shape_6[4]; E++) {
								aux_array[dimension_assotiation[4]] = E;
								x = aux_array[0];
								y = aux_array[1];
								c = aux_array[2];
								z = aux_array[3];
								t = aux_array[4];
								correct_image[x][y][c][z][t] = (double) img_matrix_2d[A][B];
							}
						}
					}
				}
			}
			im_plus = ArrayOperations.convertArrayToImagePlus(correct_image, im_shape_copy);
		}
		return im_plus;
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
		String[] dim_list = { "W", "H", "C", "D", "N" };
		int[] position_mapping = { 0, 1, 2, 3, 5 };
		String dim_letter;
		int position;
		int im_plus_index;
		for (int index = 0; index < tensorShape.length; index++) {
			dim_letter = Character.toString(form.charAt(index));
			position = ArrayOperations.indexOf(dim_list, dim_letter);
			if (position != -1) {
				im_plus_index = position_mapping[position];
				shape[im_plus_index] = tensorShape[index];
			}
		}
		return shape;
	}
	
	
	// TODO fix the thing about batch size
	private static int[] imgTensorAssociation(int[] t_shape, int[] im_shape) {
		// Mapping between the dimensions of the output tensor and the
		// default dimensions of ImagePlus.
		/// We only produce a 5 int array because we know that the first
		/// element (batch size) is always going to be 1. Also at 'im_shape',
		/// it corresponds with the 6th element and this is why we cannot ignore it
		int[] shapes_association = new int[5];
		// As previously stated, position 5 corresponds to batch size which is always
		// 1, so we can ignore it.
		int significant_size = t_shape.length - 1;
		for (int i = 0; i < significant_size; i++) {
			int ind = ArrayOperations.indexOf(im_shape, t_shape[i]);
			shapes_association[i] = ind;
			// set that position in 'im_shape' array to 0 so they wont be found again
			im_shape[ind] = 0;
		}
		return shapes_association;
	}
	
	//// Method for both cases
	public static int[] createDimOrder(String[] original_order, String required_order) {
		// Example: original_order = [c,d,e,b,a]; required_order = [d,e,b,c,a]
		// output--> dim_order = [3,0,1,2,4], because c goes in position 3, d in 0
		// position
		// and so on in the required_order array
		int size = original_order.length;
		int pos = 0;
		int[] dim_order = new int[size];
		for (int i = 0; i < size; i++) {
			pos = required_order.indexOf(original_order[i]);
			dim_order[i] = pos;
		}
		return dim_order;
	}

}

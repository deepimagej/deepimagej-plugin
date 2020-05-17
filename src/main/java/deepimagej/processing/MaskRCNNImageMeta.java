package plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class MaskRCNNImageMeta {
	
	private double IMAGE_MIN_DIM = 800;
	private double IMAGE_MIN_SCALE = 0;
	private double IMAGE_MAX_DIM = 1024;
	private String IMAGE_RESIZE_MODE = "square";
	private double NUM_CLASSES = 81;

	private static double INPUT_SCALE;
	private static double[] INPUT_WINDOW = new double[4];
	private static double[][] INPUT_PADDING = new double[4][2];
	private static double[] MEAN_PIXEL = new double[] {123.7, 116.8, 103.9};
	
	// RPN ANCHOR PARAMETERS
	private static float[] RPN_ANCHOR_SCALES = new float[] {32, 64, 128, 256, 512};
	private static float[] RPN_ANCHOR_RATIOS = new float[] {(float) 0.5, 1, 2};
	private static float[] BACKBONE_STRIDES = new float[] {4, 8, 16, 32, 64};
	private static float RPN_ANCHOR_STRIDE = 1;
	
	
	
	private static ImagePlus resizeImage(ImagePlus im, double minDim, double maxDim, double minScale,
							   String mode) {
		double nx = (double) im.getWidth();
		double ny = (double) im.getHeight();
		double nz = (double) im.getNSlices();
		double scale = Math.max((double) 1, minDim / Math.min(nx, ny));
		
		INPUT_WINDOW[2] = ny;
		INPUT_WINDOW[3] = nx;
		
		if (scale < minScale)
			scale = minScale;
		if (mode.equals("square")) {
			double imageMax = Math.max(nx,  ny);
			if (Math.round(imageMax * scale) > maxDim) {
				scale = maxDim / imageMax;
			}
		}
		if (scale != 1) {
			// TODO Interpolation?
			IJ.error("The image should be of size XX");
		}
		if (mode.equals("square")) {
			// TODO import dij module to create mirroring
			IJ.error("The image should be a square");
		}
		INPUT_SCALE = scale;
		im = moldImage(im);
		return im;
	}
	
	private static ImagePlus moldImage(ImagePlus im) {
		// Substract the mean of each of the RGB channel calculated
		// during training
		// Check if the image is an RGB, if it is make it composite,
		// so ImageJ can see the 3 channels of the RGB image
		if (im.getType() == 4){
			IJ.run(im, "Make Composite", "");
			//imp = WindowManager.getCurrentImage();
		}
		int channels = im.getNChannels();
		for (int c = 0; c < channels; c ++) {
			im.setPositionWithoutUpdate(c + 1, 1, 1);
			ImageProcessor ip = im.getProcessor();
			ip.subtract(MEAN_PIXEL[c]);
			im.setProcessor(ip);
		}
		return im;
	}
	
	private static float[] composeImageMeta(float id, float[] originalImShape, float[] finalShape,
										 float[] window, float scale, int nClasses) {
		/*
		meta = np.array(
		        [image_id] +                  # size=1
		        list(original_image_shape) +  # size=3
		        list(image_shape) +           # size=3
		        list(window) +                # size=4 (y1, x1, y2, x2) in image coordinates
		        [scale] +                     # size=1
		        list(active_class_ids)        # size=num_classes
		    )
        */
		float[] classesArray = new float[nClasses];
		int metaSize = 1 + originalImShape.length + finalShape.length + window.length + 1 + nClasses;
		float[] meta = new float[metaSize];
		int i = 0;
		meta[i ++] = id;
		
		for (int c = 0; i < originalImShape.length; c ++)
			meta[i ++] = originalImShape[c];
		
		for (int c = 0; i < finalShape.length; c ++)
			meta[i ++] = finalShape[c];
		
		for (int c = 0; i < window.length; c ++)
			meta[i ++] = window[c];
		
		meta[i ++] = scale;
		
		for (int c = 0; i < classesArray.length; c ++)
			meta[i ++] = classesArray[c];
		return meta;		
	}
	
	public static float[][] getAnchors(int[] imShape) {
		// Returns the anchor of pyramid for the given image size
		float[][] backboneShapes  = computeBackboneShapes(imShape);
		float[][] anchors = generatePyramidAnchors(
								                RPN_ANCHOR_SCALES,
								                RPN_ANCHOR_RATIOS,
								                backboneShapes,
								                BACKBONE_STRIDES,
								                RPN_ANCHOR_STRIDE);
		return anchors;
	}
	
	public static float[][] computeBackboneShapes(int[] imShape) {
		//Computes the width and height of each stage of the backbone network.
		// Currently supports resnet only
		float[][] backboneShapes = new float[BACKBONE_STRIDES.length][2];
		int c = 0;
		for (float bs : BACKBONE_STRIDES) {
			backboneShapes[c][0] = (float) Math.ceil(imShape[0] / bs);
			backboneShapes[c++][1] = (float) Math.ceil(imShape[0] / bs);
		}
		return backboneShapes;
	}
	
	public static float[][] generatePyramidAnchors(float[] scales, float[]ratios, float[][] featureShapes,
											  float[]featureStride, float anchorStride) {
		/*Returns:
    	anchors: [N, (y1, x1, y2, x2)]. All generated anchors in one array. Sorted
        with the same order of the given scales. So, anchors of scale[0] come
        first, then anchors of scale[1], and so on.
		 */
		int nAnchors = 0;
		int nRatios = ratios.length;
		for (int i = 0; i < scales.length; i ++) {
			int xStrides = (int) Math.floor(featureShapes[i][1] / anchorStride);
			int yStrides = (int) Math.floor(featureShapes[i][0] / anchorStride);
			nAnchors += nRatios * xStrides * yStrides;
		}
		
		float[][] anchors = new float[nAnchors][4];
		for (int i = 0; i < scales.length; i ++) {
			float[][] sAnchors = generateAnchors(scales[i], ratios, featureShapes[i], featureStride[i], anchorStride);
			System.arraycopy(sAnchors, 0, anchors, nAnchors * i, nAnchors * (i + 1));
		}
		return anchors;
	}
	
	public static float[][] generateAnchors(float scale, float[]ratios, float[] shape,
											  float featureStride, float anchorStride) {
		
		float[] heights = new float[ratios.length];
		float[] widths = new float[ratios.length];
		for (int i = 1; i < ratios.length; i ++) {
			heights[i] = (float) ((double)scale / Math.sqrt((double)ratios[i]));
			widths[i] = (float) ((double)scale * Math.sqrt((double)ratios[i]));
		}
		float[] shiftsY = arange(0, shape[0], anchorStride);
		for (int i = 0; i < shiftsY.length; i ++)
			shiftsY[i] = shiftsY[i] * featureStride;
		float[] shiftsX = arange(0, shape[1], anchorStride);
		for (int i = 0; i < shiftsX.length; i ++)
			shiftsX[i] = shiftsX[i] * featureStride;
		
		float[] aux_x = new float[shiftsX.length * shiftsY.length];
		float[] aux_y = new float[shiftsX.length * shiftsY.length];
		int count = 0;
		for (float y : shiftsY) {
			for (float x : shiftsX) {
				aux_x[count] = x;
				aux_y[count ++] = y;
			}
		}
		count = 0;
		float[] boxCentersX = new float[aux_x.length * widths.length];
		float[] boxWidths = new float[aux_x.length * widths.length];
		for (float x : aux_x) {
			for (float w : widths) {
				boxWidths[count] = w;
				boxCentersX[count ++] = x;
			}
		}
		count = 0;
		float[] boxCentersY = new float[aux_y.length * heights.length];
		float[] boxHeights = new float[aux_y.length * heights.length];
		for (float y : aux_y) {
			for (float h : heights) {
				boxHeights[count] = h;
				boxCentersY[count ++] = y;
			}
		}
		// Create the matrix of anchors and normalise it
		float scaleY = shape[0] - 1;
		float scaleX = shape[1] - 1;
		float shift = 1;
		float[][] boxes = new float[boxCentersY.length][4];
		for (int i = 0; i < boxes.length; i ++) {
			boxes[i][0] = ((float) (boxCentersY[i] - boxHeights[i] * 0.5)) / scaleY;
			boxes[i][1] = ((float) (boxCentersX[i] - boxWidths[i] * 0.5)) / scaleX;
			boxes[i][2] = ((float) (boxCentersY[i] + boxHeights[i] * 0.5) - shift) / scaleY;
			boxes[i][3] = ((float) (boxCentersX[i] + boxWidths[i] * 0.5) - shift) / scaleX;
		}
		
		return boxes;
	}
	
	private static float[] arange(float start, float end, float space) {
		int nComponents = (int) Math.floor((double)(end - start) / space);
		float[] arr = new float[nComponents];
		for (int i = 0; start < end; i ++) {
			start += (float) i * space; 
			arr[i] = start; 
		}
		return arr;
	}
	
	
	
	
	
	
	
}

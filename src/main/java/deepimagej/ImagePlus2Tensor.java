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

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.LongStream;


import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;


public class ImagePlus2Tensor {
	// TODO allow other types of tensors
	// TODO allow batch size != 1
	// Methods to transform a DJL Pytorch and TF tensors into ImageJ ImagePlus
	
	public static < T extends NumericType< T > & RealType< T > >  RandomAccessibleInterval< T > imPlus2tensor(ImagePlus img, String form){
		// Convert ImagePlus into tensor calling the corresponding
		// method depending on the dimensions of the required tensor 
		// Find the number of dimensions of the tensor
		int[] tensorDimOrder = Tensor.convertToTensorDimOrder(form);
		
		// TODO allow different batch sizes
        // Create a cursor
        int[] tensorDims = getTensorCompleteTensorDimensions(img.getDimensions(), tensorDimOrder);
		// Find the correspondence between the sequence axes order and
		// the tensor axes order
		int[] orderCorrespondence = getSequenceDimOrder(tensorDimOrder);
		// Make sure the array is written from last dimension to first dimension.
		// For example, for CYX we first iterate over all the X, then over the Y and then 
		// over the C
		int[] auxCounter = new int[5];
		final ArrayImgFactory< FloatType > factory = new ArrayImgFactory<>(new FloatType());
		long[] tensorSize = LongStream.range(0, tensorDimOrder.length).map(i -> tensorDims[(int) i]).toArray();
        final Img< FloatType > tensor = factory.create( tensorSize );
        Cursor<FloatType> tensorCursor = tensor.cursor();
        while (tensorCursor.hasNext()) {
        	tensorCursor.fwd();
        	long[] position = tensorCursor.positionAsLongArray();
        	for (int i = 0; i < position.length; i ++) {
        		auxCounter[i] = (int) position[i];
        	}
        	// TODO remove
        	int[] icyInd = {auxCounter[orderCorrespondence[0]], auxCounter[orderCorrespondence[1]], auxCounter[orderCorrespondence[2]], auxCounter[orderCorrespondence[3]], auxCounter[orderCorrespondence[4]]};
        	
        	img.setPositionWithoutUpdate(icyInd[2] + 1, icyInd[3] + 1, icyInd[4] + 1);
        	ImageProcessor ip = img.getProcessor();
        	float val = ip.getPixelValue(icyInd[0], icyInd[1]);
        	tensorCursor.get().set(val);
        }
		return (RandomAccessibleInterval<T>) tensor;
    }
	
	// TODO make specific for different types
	public static < T extends NumericType< T > & RealType< T > > ImagePlus tensor2ImagePlus(RandomAccessibleInterval<T> data, String form) {
		// This method copies the information from the tensor to a matrix. At first only works
		// if the batch size is 1
		
		// ImagePlus dimensions in the TensorFlow style. In this case we consider B as T,
		// as for the moment both are going to be 1
		
		// TODO adapt to several batch sizes
    	long[] dataShape = data.dimensionsAsLongArray();
    	
		if (dataShape.length != form.length())
			throw new IllegalArgumentException("Tensor has " + dataShape.length + " dimensions "
					+ "whereas the specified axes have " + form.length() + " (" + form + ").");
		int[] axesOrder = Tensor.convertToTensorDimOrder(form);
		Type<T> dtype = Util.getTypeFromInterval(data);
		// Check if the axes order is valid
		checkTensorDimOrder(dataShape, axesOrder);
		// Add missing dimensions to the tensor axes order. The missing dimensions
		// are added at the end
		int[] completeDimOrder = completeImageDimensions(axesOrder);
		// Get the order of the tensor with respect to the axes of an ImageJ sequence
        int[] seqDimOrder = getSequenceDimOrder(completeDimOrder);
        // GEt the size of the tensor for every dimension existing in an Icy sequence
        int[] seqSize = getSequenceSize(axesOrder, dataShape);
		// Create result sequence
        ImagePlus sequence = IJ.createHyperStack("output", seqSize[0], seqSize[1], seqSize[2], seqSize[3],
        		seqSize[4], 32);
        // Create an array with the shape of the tensor for every dimension in Icy
        // REcall that Icy axes are organized as [xyzbc] but in this plugin
        // to keep the convention with ImageJ and Fiji, we will always act as
        // they were [xyczb]. That is why in the following command, after
        // tensorSize[seqDimOrder[1]], it goes tensorSize[seqDimOrder[4]],
        // instead of tensorSize[seqDimOrder[2]], because seqSize uses 
        // Icy axes, but seqDimOrder refers to the tensor from ImageJ axes
        int[] tensorShape = new int[5];
        tensorShape[seqDimOrder[0]] = seqSize[0]; tensorShape[seqDimOrder[1]] = seqSize[1];
        tensorShape[seqDimOrder[2]] = seqSize[2]; tensorShape[seqDimOrder[3]] = seqSize[3];
        tensorShape[seqDimOrder[4]] = seqSize[4];
		int[] auxInd = {0, 0, 0, 0, 0};
		Cursor<FloatType> tensorCursor;
		if (data instanceof IntervalView)
			tensorCursor = ((IntervalView<FloatType>) data).cursor();
		else if (data instanceof Img)
			tensorCursor = ((Img<FloatType>) data).cursor();
		else if (data instanceof ArrayImg)
			tensorCursor = ((ArrayImg<FloatType, ?>) data).cursor();
		else
			throw new IllegalArgumentException("First parameter has to be an instance of " + Img.class 
					+ " or " + IntervalView.class + " or " + ArrayImg.class);
		while (tensorCursor.hasNext()) {
			tensorCursor.fwd();
			long[] cursorPos = tensorCursor.positionAsLongArray();
        	for (int i = 0; i < cursorPos.length; i ++) {
        		auxInd[i] = (int) cursorPos[i];
        	}
        	float val = tensorCursor.get().getRealFloat();
        	int[] icyInd = {auxInd[seqDimOrder[0]], auxInd[seqDimOrder[1]], auxInd[seqDimOrder[2]], auxInd[seqDimOrder[3]], auxInd[seqDimOrder[4]]};
        	sequence.setPositionWithoutUpdate(icyInd[2] + 1, icyInd[3] + 1, icyInd[4] + 1);
        	ImageProcessor ip = sequence.getProcessor();
        	ip.putPixelValue(icyInd[0], icyInd[1], (float) val);
        }
    	return sequence;
	}

	/**
	 * Create an array where each position corresponds to the size 
	 * of the tensor that will be created. The array has all the possible
	 * dimensions of a sequence. For example for a sequence with X->255,
	 * Y->256, C->3, Z->1, B->1, and axes order "BYXC" (which will be represented
	 * by the variable 'arrayDimOrder' as [4, 1, 0, 2]), the resulting array
	 * would be [1, 256, 256, 3, 1]
	 * @param sequence: image from which the tensor will be created
	 * @param arrayDimOrder: axes order of the tensor
	 * @return array with the size of each tensor in the corresponding dimension
	 */
    private static int[] getTensorCompleteTensorDimensions(int[] dims, int[] arrayDimOrder)
    {
    	// Map the dimensions integer (ie x->0, y->1, c->2, z->3, t->4)
		HashMap<Integer, Integer> dimsMap = new HashMap<Integer, Integer>();
		dimsMap.put(0, dims[0]);
		dimsMap.put(1, dims[1]);
		dimsMap.put(2, dims[2]);
		dimsMap.put(3, dims[3]);
		dimsMap.put(4, dims[4]);
		int[] tensorDims = new int[] {1, 1, 1, 1, 1};
		for (int i = 0; i < arrayDimOrder.length; i ++)
			tensorDims[i] = dimsMap.get(arrayDimOrder[i]);
    	return tensorDims;
    }

    /**
     * Computes the sequence dimension order with respect to the tensor dimensions.
     * 
     * @param tensorDimOrder
     *        The Tensor dimension order.
     * @return The sequence dimension order.
     */
    private static int[] getSequenceDimOrder(int[] tensorDimOrder)
    {
    	tensorDimOrder = tensorDimOrderAllDims(tensorDimOrder);
        int[] imgDimOrder = new int[] {-1, -1, -1, -1, -1};
        for (int i = 0; i < tensorDimOrder.length; i++)
        {
            imgDimOrder[tensorDimOrder[i]] = i;
        }
        return imgDimOrder;
    }
    
    /**
     * Create a dimensions (axes) order array that contains all the possible dimensions,
     * adding the ones missing from the tensor at the end of the array
     * @param tensorDimOrder
     * 	the tensor axes order in array form
     * @return the tensor axes order but with all the possible dims
     */
    private static int[] tensorDimOrderAllDims(int[] tensorDimOrder) {
    	int[] longDimOrder = new int[5];
    	// Auxiliary array with dimensions ordered
    	int[] auxArr = new int[] {0,1,2,3,4};
    	int i;
    	for (i = 0; i < tensorDimOrder.length; i ++) {
    		longDimOrder[i] = tensorDimOrder[i];
    		auxArr[tensorDimOrder[i]] = -1;
    	}
    	
    	for (int aa : auxArr) {
    		if (aa != -1) 
    			longDimOrder[i ++] = aa;
    	}
    	return longDimOrder;
    }
	
	/**
	 * Check that the dimensions order provided is compatible with
	 * the output array given. If it is not, the method throws an exception,
	 * if it is, nothing happens
	 * @param dataShape
	 * 	shape of the data array
	 * @param tensorDimOrder
	 * 	dimensions (axes) order given
	 * @throws IllegalArgumentException if the dimensions do not have the same length
	 */
    private static void checkTensorDimOrder(long[] dataShape, int[] tensorDimOrder)
            throws IllegalArgumentException
    {
        if (tensorDimOrder.length != dataShape.length)
        {
            throw new IllegalArgumentException(
                    "Tensor dim order array length is different than number of dimensions in tensor ("
                            + tensorDimOrder.length + " != " + dataShape.length + ")");
        }
    }
    
    // TODO improve efficiency
    /**
     * Add to the tensor axes order array the dimensions missing,
     * the dimensions are always added at the end.
     * For example, for a tensor with axes [byxc], its tensorDimOrder
     * would be transformed from [4,1,0,2] to [4,1,0,2,3] ([byxcz])
     * @param tensorDimOrder; axes order of the tensor
     * @return new axes order with dimensions at the end
     */
    private static int[] completeImageDimensions(int[] tensorDimOrder) {
    	int nTotalImageDims = 5;
    	int nTensorDims = tensorDimOrder.length;
    	int missingDims = nTotalImageDims - nTensorDims;
    	int[] missingDimsArr = new int[missingDims];
    	int c = 0;
    	for (int ii : new int[] {0, 1, 2, 3, 4}) {
    		if (Arrays.stream(tensorDimOrder).noneMatch(i -> i == ii))
    			missingDimsArr[c ++] = ii;
    	}
    	int[] completeDims = new int[nTotalImageDims];
        System.arraycopy(tensorDimOrder, 0, completeDims, 0, tensorDimOrder.length);
        System.arraycopy(missingDimsArr, 0, completeDims, tensorDimOrder.length, missingDimsArr.length);
    	return completeDims;
    }

    /**
     * Get the size of each of the dimensions expressed in an array that
     * follows the ImageJ axes order -> xyczt
     * @param seqDimOrder
     * 	order of the dimensions of the Icy sequence with respect to the tensor
     * @param shape
     * 	shape of the dimensions of the data
     * @return array containing the size for each dimension
     */
    private static int[] getSequenceSize(int[] seqDimOrder, long[] shape)
    {
        int[] dims = new int[] {1, 1, 1, 1, 1};
        for (int i = 0; i < seqDimOrder.length; i ++) {
        	dims[seqDimOrder[i]] = (int) shape[i];
        }
        return dims;
    }
}

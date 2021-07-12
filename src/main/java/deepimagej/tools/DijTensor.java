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

package deepimagej.tools;

import java.util.ArrayList;
import java.util.List;

public class DijTensor {
	// For the moment only consider images as input
	// Name of the input
	public String 		name;
	// Tensor dimensions of the input. Ex: [-1,-1, -1, 1]
	public int[]		tensor_shape;
	// Organization of the input. Ex: "BYXC"
	public String		form;
	// Minimum size of a patch (in all dimensions). Ex: [1,8,8,1]
	// minimum of 8 for Y and X and 1 for B and C
	public int[]		minimum_size;
	// Whether each of the dimensions is fixed or not. Ex: [0, 1, 1, 0]
	// means that H and W are not and can increase by 1 pixel fixed.
	public int[]	step;
	// Size of the padding in every axis. Ex: [null, 50, 50, 0]
	// means removing 50 pixels in H and W. The organisation is the same as 
	// in inputForm
	public int[]		halo;
	// Size of the patch in every dimension for the example image
	public int[]		recommended_patch;
	// Data range
	public double[]		dataRange = new double[2];
	// Size of the step between convolutions
	public int[]		offset;
	public float[] 		scale;
	// For an image output tensor. Reference that we take to make the output image
	public String 		referenceImage		= "";
	// String referring to the type of input or output the model should expect
	// Possible input types:
	// 	 - Image, a normal ImageJ ImagePlus
	//   - Parameter, the parameter has to be an array provided as the output
	//     to a external java processing method
	// Possible output types
	//   - Image, a normal ImageJ ImagePlus
	//   - Label, a number in an Excel file
	//   - List, a list in an Excel file
	//   - Ignore, the tensor is not used and discarded
	public String		tensorType;
	/*
	 * Parameter that indicates that all the needed information for a
	 *  tensor to work is completed. Used in the InputTensorStamp and 
	 *  the OutputTensorStamp
	 */
	public boolean		finished = false;

	/*
	 * Size of the output in Pyramidal Networks. It will always be fixed
	 * to the dimensions set by the developer
	 */
	public int[] sizeOutputPyramid;
	/*
	 * Size in pixels of each of the input images used as examples to
	 * create the bundled model
	 */
	public String inputTestSize = null;
	/*
	 * Size in pixels of each of the input images used as examples to
	 * create the bundled model
	 */
	public String outputTestSize = null;
	/*
	 * Pixel size in X of the input image used to create the bundled model
	 */
	public String inputPixelSizeX;
	/*
	 * Pixel size in Y of the input image used to create the bundled model
	 */
	public String inputPixelSizeY;
	/*
	 * Pixel size in Z of the input image used to create the bundled model
	 */
	public String inputPixelSizeZ;
	/*
	 * Data type of the tensor
	 */
	public String dataType;
	/*
	 * Name of the image used as input while creating the model.
	 */
	public String exampleInput = null;
	/*
	 * Copy of the tensor form for type list outputs. This
	 * is used because the in lists the letters used change to
	 * R (rows) and C (columns)
	 */
	public String auxForm;
	
	/*
	 * Contructor to create a tensor
	 */
	public DijTensor(String name) {
		this.name = name;
	}

	/*
	 * Auxiliary constructor to create a copy of a tensor with a few 
	 * specific parameters. This is done to know if the tensors have changed  
	 * at @see deepimagej.stamp.InputDimensionStamp#init or @see deepimagej.stamp.OutputDimensionStamp#init
	 */
	public DijTensor(DijTensor original) {
	    this.name = original.name;  
	    this.form = original.form;  
	    this.tensorType = original.tensorType;   
	  }
	
	public void setInDimensions(int[] inDimensions) {
		this.tensor_shape = inDimensions;
	}
	
	public void setInputForm(String inputForm) {
		this.form = inputForm;
	}
	
	public void setMinimumSize(int[] minimumSize) {
		this.minimum_size = minimumSize;
	}
	
	public void setPadding(int[] padding) {
		this.halo = padding;
	}
	
	public void setPatch(int[] patch) {
		this.recommended_patch = patch;
	}
	
	public static DijTensor retrieveByName(String name, List<DijTensor> tensors) {
		DijTensor wantedTensor = null;
		for (DijTensor tensor: tensors) {
			if (tensor.name.equals(name) == true) {
				wantedTensor = tensor;
				break;
			}
		}
		return wantedTensor;
	}
	
	public static int[] getWorkingDimValues(String form, int[] values) {
		String[] splitForm = form.split("");
		int batchInd = Index.indexOf(splitForm, "B");
		if (batchInd != -1) {
			int c = 0;
			int[] newValues = new int[values.length - 1];
			for (int i = 0; i < values.length; i ++) {
				if (batchInd != i) {
					newValues[c++] = values[i];
				}
			}
			values = newValues;
		}
		return values;
	}
	
	public static String[] getWorkingDims(String form) {
		String[] splitForm = form.split("");
		int batchInd = Index.indexOf(splitForm, "B");
		if (batchInd != -1) {
			form = form.substring(0, batchInd) + form.substring(batchInd + 1);
			splitForm = form.split("");
		}
		return splitForm;
	}
	
	public static int getBatchInd(String form) {
		String[] splitForm = form.split("");
		int batchInd = Index.indexOf(splitForm, "B");
		return batchInd;
	}
	
	public static List<DijTensor> getImageTensors(List<DijTensor> tensorList) {
		List<DijTensor> imageTensors = new ArrayList<DijTensor>(); 
		for (DijTensor tensor : tensorList) {
			if (tensor.tensorType.contains("image"))
				imageTensors.add(tensor);
		}
		return imageTensors;
	}
	
	public static int getImageTensorInd(List<DijTensor> tensorList) {
		List<DijTensor> imageTensors = new ArrayList<DijTensor>(); 
		int i = 0;
		for (DijTensor tensor : tensorList) {
			if (tensor.tensorType.contains("image"))
				return i;
			i ++;
		}
		return -1;
	}
	
	public static List<DijTensor> copyTensorList(List<DijTensor> inputList){
		List<DijTensor> newTensors = new ArrayList<DijTensor>();
		for (DijTensor tt : inputList)
			newTensors.add(new DijTensor(tt));
		return newTensors;
	}

}

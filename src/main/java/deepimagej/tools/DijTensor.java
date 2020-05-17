package deepimagej.tools;

import java.util.ArrayList;
import java.util.List;

public class DijTensor {
	// For the moment only consider images as input
	// Name of the input
	public String 		name;
	// Tensor dimensions of the input. Ex: [-1,-1, -1, 1]
	public int[]		tensor_shape;
	// Organization of the input. Ex: "NHWC"
	public String		form;
	// Minimum size of a patch (in all dimensions). Ex: [-1,8,8,1]
	// the same dimension organization is kept
	public int[]		minimum_size;
	// Whether each of the dimensions is fixed or not. Ex: [0, 1, 1, 0]
	// means that H and W are fixed.
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
	// Path only needed for input tensors that correspond to parameters.
	// Contains the path to the file that contains the info needed to construct
	// the tensor
	public String		parameterPath;
	/*
	 * Whether the java interface method that it is used to
	 * retrieve the parameter tensor has an ImagePlus as argument
	 * or not
	 */
	public boolean		useImage;
	/*
	 * Parameter that indicates that all the needed information for a
	 *  tensor to work is completed. Used in the InputTensorStamp and 
	 *  the OutputTensorStamp
	 */
	public boolean		finished = false;
	
	public DijTensor(String name) {
		this.name = name;
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
		int batchInd = Index.indexOf(splitForm, "N");
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
		int batchInd = Index.indexOf(splitForm, "N");
		if (batchInd != -1) {
			form = form.substring(0, batchInd) + form.substring(batchInd + 1);
			splitForm = form.split("");
		}
		return splitForm;
	}
	
	public static int getBatchInd(String form) {
		String[] splitForm = form.split("");
		int batchInd = Index.indexOf(splitForm, "N");
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

}

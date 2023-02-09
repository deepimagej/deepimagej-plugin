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

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import ij.IJ;

public class DeepLearningModel {

	public static int nChannelsOrSlices(DijTensor tensor, String channelsOrSlices) {
		// Find the number of channels or slices in the corresponding tensor
		String letter = "";
		if (channelsOrSlices.equals("channels")) {
			letter = "C";
		} else {
			letter = "Z";
		}
		
		int nChannels;
		String inputForm = tensor.form;
		int ind = Index.indexOf(inputForm.split(""), letter);
		if (ind == -1) {
			nChannels = 1;
		}
		else {
			nChannels = tensor.minimum_size[ind];
		}
		return nChannels;
	}
	
	public static String hSize(Parameters params, String inputForm) {
		// Find the number of channels in the input
		String nChannels;
		int ind = Index.indexOf(inputForm.split(""), "Y");
		if (ind == -1) {
			nChannels = "-1";
		}
		else {
			nChannels = Integer.toString(params.inputList.get(0).tensor_shape[ind]);
		}
		return nChannels;
	}
	
	public static String wSize(Parameters params, String inputForm) {
		// Find the number of channels in the input
		String nChannels;
		int ind = Index.indexOf(inputForm.split(""), "X");
		if (ind == -1) {
			nChannels = "-1";
		}
		else {
			nChannels = Integer.toString(params.inputList.get(0).tensor_shape[ind]);
		}
		return nChannels;
	}
	
	// Method added to allow multiple possible batch sizes
	public static String nBatch(int[] dims, String inputForm) {
		// Find the number of channels in the input
		String inBatch;
		int ind = Index.indexOf(inputForm.split(""), "B");
		if (ind == -1) {
			inBatch = "1";
		} else {
			inBatch = Integer.toString(dims[ind]);
		}
		if (inBatch.equals("-1")) {
			inBatch = "1";
		}
		return inBatch;
	}
	
	// TODO group Tf and Pytorch methods regarding versions
	/*
	 * Find if the CUDA and Tf versions are compatible
	 */
	public static String PytorchCUDACompatibility(String ptVersion, String CUDAVersion) {
		String errMessage = "";
		if (CUDAVersion.equals("nocuda")) {
				errMessage = "";
		} else if (ptVersion.contains("1.7.0") && !(CUDAVersion.contains("10.2") || CUDAVersion.contains("10.1") || CUDAVersion.contains("11.0"))) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with DJL Pytorch 1.7.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install either CUDA 10.1, CUDA 10.2 or CUDA 11.0.\n";
		} else if (ptVersion.contains("1.6.0") && !(CUDAVersion.contains("10.2") || CUDAVersion.contains("10.1"))) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with DJL Pytorch  1.6.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 10.1, CUDA 10.2.\n";
		} else if (ptVersion.contains("1.5.0") && !(CUDAVersion.contains("10.1")  || CUDAVersion.contains("10.2") || CUDAVersion.contains("9.2"))) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with DJL Pytorch  1.5.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 9.2, CUDA 10.1 or CUDA 10.2.\n";
		} else if (ptVersion.contains("1.4.0") && !(CUDAVersion.contains("10.1")  || CUDAVersion.contains("9.2"))) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with DJL Pytorch  1.4.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 9.2 or CUDA 10.1.\n";
		} else if (!CUDAVersion.toLowerCase().contains("nocuda") && !ptVersion.equals("")) {
			errMessage = "Make sure that the DJL Pytorch version is compatible with the installed CUDA version.\n"
					+ "Check the DeepImageJ Wiki for more information";
		}
		return errMessage;
	}
	
	/*
	 * Find if the CUDA and Tf versions are compatible
	 */
	public static String TensorflowCUDACompatibility(String tfVersion, String CUDAVersion) {
		String errMessage = "";
		if (tfVersion.contains("1.15.0") && !CUDAVersion.contains("10.0")) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with tf 1.15.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 10.0.\n";
		} else if (tfVersion.contains("1.14.0") && !CUDAVersion.contains("10.0")) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with tf 1.14.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 10.0.\n";
		} else if (tfVersion.contains("1.13.0") && !CUDAVersion.contains("10.0")) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with tf 1.13.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 10.0.\n";
		} else if (tfVersion.contains("1.12.0") && !CUDAVersion.contains("9.0")) {
			errMessage = "Installed CUDA version " + CUDAVersion + " is not compatible with tf 1.12.0.\n"
					+ "The plugin might not be able to run on GPU.\n"
					+ "For optimal performance please install CUDA 9.0.\n";
		} else if (!tfVersion.contains("1.15.0") && !tfVersion.contains("1.14.0") && !tfVersion.contains("1.13.0") && !tfVersion.contains("1.12.0")) {
			errMessage = "Make sure that the Tensorflow version is compatible with the installed CUDA version.\n";
		}
		return errMessage;
	}

}

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

package deepimagej.processing;

import java.util.ArrayList;
import java.util.HashMap;


public interface PreProcessingInterface {
	
	public String configFile = "";

	/**
	 * Method containing the whole Java pre-processing routine. 
	 * @param map: inputs to be pre-processed. It is provided by deepImageJ. The keys
	 * correspond to name given by the model to the inputs. And the values are the images
	 * selected to be applied to the model and any ResultsTable that is called as any of
	 * the parameter inputs of the model
	 * @return this method has to return HashMap whose keys are the inputs to the model as
	 * named by the model. The values types depend on the input type of tensor. For images,
	 * they should correspond to an ImagePlus. FOr parameters, the output provided should be either
	 * a Tensorflow tensor or a DJL NDArray
	 * Here is some documentation about creating Tensorflow tensors from Java Arrays:
	 * See <a href="https://www.tensorflow.org/api_docs/java/org/tensorflow/Tensors#public-static-tensorfloat-create-float[][][]-data">https://www.tensorflow.org/api_docs/java/org/tensorflow/Tensors#public-static-tensorfloat-create-float[][][]-data</a>
	 * 
	 * To create DJL NDArrays:
	 * See <a href="https://javadoc.io/doc/ai.djl/api/latest/ai/djl/ndarray/NDManager.html">https://javadoc.io/doc/ai.djl/api/latest/ai/djl/ndarray/NDManager.html</a>
	 */
	public HashMap<String, Object> deepimagejPreprocessing(HashMap<String, Object> map);
	
	/**
	 * Auxiliary method to be able to change some pre-processing parameters without
	 * having to change the code. DeepImageJ gives the option of providing a .txt or .ijm
	 * file in the pre-processing which can act both as a macro and as a config file.
	 * It can act as a config file because the needed parameters can be specified in
	 * a comment block and the parsed by the pre-processing method
	 * @param configFile: macro file which might contain parameters for the pre-processing 
	 */
	public void setConfigFiles(ArrayList<String> files);
	
	/**
	 * Method that recovers an error message from the pre-processing execution
	 * @return
	 */
	public String error();


}

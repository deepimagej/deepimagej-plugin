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


package deepimagej.processing;

import java.util.ArrayList;
import java.util.HashMap;


public interface PostProcessingInterface {

	public String configFile = "";

	/**
	 * Method containing the whole Java post-processing routine. 
	 * @param map: outputs to be post-processed. It is provided by deepImageJ. The keys
	 * correspond to name given by the model to the outputs. And the values are the images and 
	 * ResultsTables outputes by the model.
	 * @return this method has to return a HashMap with the post-processing results.
	 */
	public HashMap<String, Object> deepimagejPostprocessing(HashMap<String, Object> map);
	
	/**
	 * Auxiliary method to be able to change some post-processing parameters without
	 * having to change the code. DeepImageJ gives the option of providing a .txt or .ijm
	 * file in the post-processing which can act both as a macro and as a config file.
	 * It can act as a config file because the needed parameters can be specified in
	 * a comment block and the parsed by the post-processing method
	 * @param configFile: macro file which might contain parameters for the post-processing 
	 */
	public void setConfigFiles(ArrayList<String> files);
	
	/**
	 * Method that recovers an error message from the post-processing execution
	 * @return
	 */
	public String error();
}

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

package deepimagej;

import java.io.File;
import java.util.Map;
import java.util.Set;

import deepimagej.tools.XmlUtils;
import ij.ImagePlus;

public class Parameters {

	// Directory where the model is actually located
	public String path2Model;
	// Parameter that checks if the plugin is for developers or users
	public boolean developer;
	// Strings containing the macros used in the test, only useful for developer
	// plugin
	public String postmacro = "";
	public String premacro = "";
	// Images for testing the model
	// Image used to test the model
	public ImagePlus testImage;
	// Copy created to return the original image in the case
	// that the model fails after applying the macros
	public ImagePlus testImageBackup;
	// ImagePlus produced after testing the model
	public ImagePlus testResultImage;
	
	// Directory specified by the user to save the model
	public String saveDir;
	
	// Boolean informing if the config file contains the ModelCharacteristics
	// parameters, needed to load the model
	public boolean completeConfig = true;
		
	// in ModelInformation
	public String		name						= "";
	public String		author					= "";
	public String		url						= "";
	public String		credit					= "";
	public String		version					= "";
	public String		date						= "";
	public String		reference				= "";

	// in ModelTest
	public String		inputSize				= "";
	public String		outputSize				= "";
	public String		memoryPeak				= "";
	public String		runtime					= "";

	public String		tag						= "";
	public String		graph					= "";
	public Set<String> 	graphSet;

	// Parameters for the correct execution of the model on an image.
	// They range from the needed dimensions and dimensions organization
	// to the size of the patches and overlap needed for the network to work
	// properly and not to crash because of different issues such as memory.
	// They also regard the requirements for the input image

	public int[]			inDimensions;
	public int[]			outDimensions;
	public String[]		inputForm				= new String[1];
	public String[]		outputForm				= new String[1];
	public String[]		inputs;
	public String[]		outputs;
	public int			nInputs				= -1;
	public int			nOutputs			= -1;
	
	public String		minimumSize;
	public boolean		fixedPatch			= false;
	public boolean		fixedPadding			= true;
	public int			padding				= -1;
	public int			patch				= -1;

	// Set one channel as default
	public String		channels				= "1";

	// This parameter is predefined and unmodifiable as 3d models are still not accepted
	public String		slices					= "1";


	public Parameters(boolean valid, String path, boolean isDeveloper) {
		// If the model is not valid or we are in the developer plugin,
		// we cannot read the parameters from anywhere as there is no
		// config file
		developer = isDeveloper;
		if (!valid || developer)
			return;
		String xml = path + File.separator + "config.xml";
		Map<String, String> config = (Map<String, String>) XmlUtils.readXML(xml);

		name = config.get("Name") != null ? config.get("Name") : "";
		author = config.get("Author") != null ? config.get("Author") : "";
		url = config.get("URL") != null ? config.get("URL") : "";
		credit = config.get("Credit") != null ? config.get("Credit") : "";
		version = config.get("Version") != null ? config.get("Version") : "";
		date = config.get("Date") != null ? config.get("Date") : "";
		reference = config.get("Reference") != null ? config.get("Reference") : "";
		inputSize = config.get("InputSize") != null ? config.get("InputSize") : "";
		outputSize = config.get("OutputSize") != null ? config.get("OutputSize") : "";
		memoryPeak = config.get("MemoryPeak") != null ? config.get("MemoryPeak") : "";
		runtime = config.get("Runtime") != null ? config.get("Runtime") : "";

		nInputs = Integer.parseInt(config.get("NumberOfInputs")==null ? "0" : config.get("NumberOfInputs"));
		nOutputs = Integer.parseInt(config.get("NumberOfOutputs")==null ? "0" : config.get("NumberOfOutputs"));
		inputs = new String[nInputs];
		outputs = new String[nOutputs];
		inputForm = new String[nInputs];
		outputForm = new String[nOutputs];
		readInOutSet(config);
		minimumSize = config.get("MinimumSize");
		tag = config.get("ModelTag");
		graph = config.get("SignatureDefinition");
		padding = Integer.parseInt(config.get("Padding")==null ? "-1" : config.get("Padding"));
		inDimensions = string2tensorDims(config.get("InputTensorDimensions"));
		fixedPatch = Boolean.parseBoolean(config.get("FixedPatch") == null ? "true" : config.get("FixedPatch"));
		patch = Integer.parseInt(config.get("PatchSize")==null ? "-1" : config.get("PatchSize"));
		fixedPadding = Boolean.parseBoolean(config.get("fixedPadding") == null ? "true" : config.get("fixedPadding"));
		channels = config.get("Channels");// == null ? "1" : config.get("Channels");
		//slices = Integer.parseInt(config.get("slices"));
		
		// Now check that all the Model Characteristic parameters are
		// present in the config file, if not the model will not be loaded
		if (nInputs == 0 || nOutputs == 0 || inputs == null || outputs == null || inputForm == null ||
			inputForm == null || outputForm == null || minimumSize == null || tag == null || graph == null ||
			padding == -1 || inDimensions == null || patch == -1 || channels == null) {
			completeConfig = false;
		}
	}

	private void readInOutSet(Map<String, String> model) {
		for (int i = 0; i < nInputs; i++) {
			String in_name = "InputNames" + String.valueOf(i);
			String in_dims = "InputOrganization" + String.valueOf(i);
			inputs[i] = model.get(in_name);
			inputForm[i] = model.get(in_dims);
			if (inputs[i] == null || inputForm[i] == null) {
				inputs = null;
				inputForm = null;
				outputs = null;
				outputForm = null;
				return;
			}
		}
		for (int i = 0; i < nOutputs; i++) {
			String in_name = "OutputNames" + String.valueOf(i);
			String in_dims = "OutputOrganization" + String.valueOf(i);
			outputs[i] = model.get(in_name);
			outputForm[i] = model.get(in_dims);
			if (outputs[i] == null || outputForm[i] == null) {
				inputs = null;
				inputForm = null;
				outputs = null;
				outputForm = null;
				return;
			}
		}
	}

	private int[] string2tensorDims(String string) {
		// This method separates a string into an array of the int
		// represented by each character of the string separated by comas.
		// Example: ",1,2,3,4,-5,"-->[1,2,3,4,-5]
		String[] array = string.split(",");
		int[] tensor_dims = new int[array.length - 1];
		int array_counter = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals("") == false) {
				tensor_dims[array_counter] = Integer.parseInt(array[i]);
				array_counter++;
			}
		}
		return tensor_dims;
	}
}

package deepimagej;

import java.io.File;
import java.util.Map;

import deepimagej.tools.XmlUtils;
import ij.ImagePlus;

public class Parameters {

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

	// Parameters for the correct execution of the model on an image.
	// They range from the needed dimensions and dimensions organization
	// to the size of the patches and overlap needed for the network to work
	// properly and not to crash because of different issues such as memory.
	// They also regard the requirements for the input image

	public int[]		in_dimensions;
	public int[]		out_dimensions;
	public String[]		input_form				= new String[1];
	public String[]		output_form				= new String[1];
	public String[]		inputs;
	public String[]		outputs;
	public int			n_inputs;
	public int			n_outputs;

	public String		minimum_patch_size;
	public boolean		fixedPatch		= false;
	public int			overlap					= 0;
	public int			patch;

	// Set one channel as default
	public String		channels				= "1";

	// This parameter is predefined and unmodifiable as 3d models are still not
	// accepted
	public int			slices					= 1;

	public Parameters(boolean valid, String path) {
		if (!valid)
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

		n_inputs = Integer.parseInt(config.get("NumberOfInputs"));
		n_outputs = Integer.parseInt(config.get("NumberOfOutputs"));
		inputs = new String[n_inputs];
		outputs = new String[n_outputs];
		input_form = new String[n_inputs];
		output_form = new String[n_outputs];
		readInOutSet(config);
		minimum_patch_size = config.get("MinimumMultipleOfPatches");
		tag = config.get("ModelTag");
		graph = config.get("SignatureDefinition");
		overlap = Integer.parseInt(config.get("FalseInformationBecauseCorners"));
		in_dimensions = string2tensorDims(config.get("InputTensorDimensions"));
		fixedPatch = Boolean.parseBoolean(config.get("FixedPatch"));
		patch = Integer.parseInt(config.get("PatchSize"));

		channels = config.get("Channels");
		slices = Integer.parseInt(config.get("slices"));
	}

	private void readInOutSet(Map<String, String> model) {
		for (int i = 0; i < n_inputs; i++) {
			String in_name = "InputNames" + String.valueOf(i);
			String in_dims = "InputOrganization" + String.valueOf(i);
			inputs[i] = model.get(in_name);
			input_form[i] = model.get(in_dims);
		}
		for (int i = 0; i < n_outputs; i++) {
			String in_name = "OutputNames" + String.valueOf(i);
			String in_dims = "OutputOrganization" + String.valueOf(i);
			outputs[i] = model.get(in_name);
			output_form[i] = model.get(in_dims);
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

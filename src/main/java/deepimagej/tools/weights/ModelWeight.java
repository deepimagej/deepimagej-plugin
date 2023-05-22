package deepimagej.tools.weights;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The model weights information for the current model.
 * 
 * @author Carlos Garcia Lopez de Haro and Daniel Felipe Gonzalez Obando 
 */
public class ModelWeight
{
	/**
	 * String representing the selected weight by the user in the DeepIcy GUI
	 */
    private String selectedEngine;
	/**
	 * String representing the selected weight version by the user in the DeepIcy GUI
	 */
    private String selectedVersion;
	/**
	 * Object containing the information about the weights selected
	 */
    private WeightFormatInterface selectedWeights;
	/**
	 * Object containing the information about the weights loaded
	 */
    private static Map<String, WeightFormatInterface> loadedWeights = new HashMap<String, WeightFormatInterface>();
	/**
	 * Map with all the engines defined in the rdf.yaml
	 */
    private HashMap<String, WeightFormatInterface> weightsDic; 
    private static String kerasIdentifier = "keras_hdf5";
    private static String onnxIdentifier = "onnx";
    private static String torchIdentifier = "pytorch_state_dict";
    private static String tfIdentifier = "tensorflow_saved_model_bundle";
    private static String tfJsIdentifier = "tensorflow_js";
    private static String torchscriptIdentifier = "torchscript";
    private static String bioengineIdentifier = "bioengine";
    private static String gpuSuffix = " (supports gpu)";
    /**
     * The key for the weights that are going to be used in the BioEngine
     */
    private String bioEngineWeightsKey;
    /**
     * List of all the not supported Deep Learning frameworks by DeepIcy
     */
    private static ArrayList<String> supported = 
    		new ArrayList<String>(Arrays.asList(torchscriptIdentifier, tfIdentifier, onnxIdentifier));
    /**
     * Suffix added to the engine version when the engine version is not installed
     */
    private static String missingVersion = " (please install)";
    /**
     * Suffix added to the engine version when the engine version is not installed
     */
    private static String notSupported = " (not supported)";
    /**
     * Suffix added to the engine version when another version of 
     * the same engine has already been loaded
     */
    private static String alreadyLoaded = " (restart Icy)";
    /**
     * Builds a weight information element from the element map.
     * 
     * @param yamlFieldElements
     *        The element map.
     * @return The model weight information instance.
     */
    public static ModelWeight build(Map<String, Object> yamlFieldElements)
    {
        ModelWeight model = new ModelWeight();
        Set<String> weightsFormats = yamlFieldElements.keySet();
        // Reset the list with the inlcuded frameworks
        model.weightsDic = new HashMap<String, WeightFormatInterface>();
        for (String ww : weightsFormats) {
        	Map<String, Object> weights = (Map<String, Object>) yamlFieldElements.get(ww);
	        if (ww.contentEquals(kerasIdentifier)) {
	        	KerasWeights weightsObject = new KerasWeights(weights);
	        	model.weightsDic.put(model.kerasEngineName(weightsObject), weightsObject);
	    	} else if (ww.contentEquals(onnxIdentifier)) {
	    		OnnxWeights weightsObject = new OnnxWeights(weights);
	    		model.weightsDic.put(model.onnxEngineName(weightsObject), weightsObject);
	    	} else if (ww.contentEquals(torchIdentifier)) {
	    		PytorchWeights weightsObject = new PytorchWeights(weights);
	    		model.weightsDic.put(model.torchEngineName(weightsObject), weightsObject);
	    	} else if (ww.contentEquals(tfIdentifier)) {
	    		TfWeights weightsObject = new TfWeights(weights);
	    		model.weightsDic.put(model.tfEngineName(weightsObject), weightsObject);
	    	} else if (ww.contentEquals(tfJsIdentifier)) {
	    		TfJsWeights weightsObject = new TfJsWeights(weights);
	    		model.weightsDic.put(model.tfJsEngineName(weightsObject), weightsObject);
	    	} else if (ww.contentEquals(torchscriptIdentifier)
	    			|| ww.contentEquals("pytorch_script")) {
	    		TorchscriptWeights weightsObject = new TorchscriptWeights(weights);
	    		model.weightsDic.put(model.torchscriptEngineName(weightsObject), weightsObject);
	    	}
        }
        return model;
    }
    
    /**
     * Identifies the weights that are compatible with the Bioengine. The BioEngine
     * canot run Tf 1 weights
     */
    public void findBioEngineWeights() {
    	for (Entry<String, WeightFormatInterface> entry : weightsDic.entrySet()) {
    		if (entry.getValue().getWeightsFormat().equals(kerasIdentifier)) {
    			bioEngineWeightsKey = kerasIdentifier;
    			return;
    		} else if (entry.getValue().getWeightsFormat().equals(onnxIdentifier)) {
    			bioEngineWeightsKey = onnxIdentifier;
    			return;
    		} else if (entry.getValue().getWeightsFormat().equals(torchscriptIdentifier)) {
    			bioEngineWeightsKey = torchscriptIdentifier;
    			return;
    		}
    	}
    }
    
    /**
     * Return the key for the Bioengine weights that are going to be used
     * @return the key for the bioengine weights
     */
    public String getBioEngineWeightsKey() {
    	return this.bioEngineWeightsKey;
    }

	/**
     * Return the corresponding weight format
     * @return the corresponding weight format.
     * @throws Exception if the set of wanted weights is not present
     */
    public WeightFormatInterface getWeightsByIdentifier(String weightsFormat) throws IOException
    {
    	WeightFormatInterface ww = weightsDic.get(weightsFormat);
    	
    	if (ww == null) {
    		throw new IOException("The selected model does not contain "
    				+ "a set of " + weightsFormat + " weights.");
    	}
    	return ww;
    }
    
    /**
     * Return a list containing all the frameworks (engines) where the model has weights
     * @return list of supported Deep Learning frameworks with the corresponding version
     */
    public List<String> getEnginesListWithVersions(){
    	return this.weightsDic.keySet().stream().collect(Collectors.toList());
    }
    
    /**
     * Get list with the supported Deep Learning frameworks. Does not the same framework
     * several times if it is repeated.
     * @return
     */
    public List<String> getSupportedDLFrameworks() {
    	return weightsDic.entrySet().stream().
    			map(i -> i.getValue().getWeightsFormat()).
    			distinct().collect(Collectors.toList());
    }

	/**
	 * Get the weights format selected to make inference.
	 * For models that contain several sets of weights
	 * from different frameworks in the
	 * same model folder
	 * 
	 * @return the selected weights engine
	 */
	public String getSelectedWeightsIdentifier() {
		return selectedEngine;
	}
	
	/**
	 * GEt the training version of the selected weights
	 * @return the training version of the selected weights
	 * @throws IOException if the weights do not exist
	 */
	public String getWeightsSelectedVersion() throws IOException {
		return selectedVersion;
	}
	
	/**
	 * Return the object containing the information about the selected weights
	 * @return the yaml information about the selected weights
	 */
	public WeightFormatInterface getSelectedWeights() {
		return this.selectedWeights;
	}

	/**
	 * Sets the Deep Learning framework of the weights of the
	 * model selected. 
	 * For models that contain several sets of weights
	 * from different frameworks in the
	 * 
	 * @param selectedWeights the format (framework) of the weights 
	 * @throws IOException if the weights are not found in the avaiable ones
	 */
	public void setSelectedWeightsFormat(String selectedWeights) throws IOException {
		if (selectedWeights.startsWith(kerasIdentifier)) {
			this.selectedEngine = kerasIdentifier;
		} else if (selectedWeights.startsWith(onnxIdentifier)) {
			this.selectedEngine = onnxIdentifier;
		} else if (selectedWeights.startsWith(torchIdentifier)) {
			this.selectedEngine = torchIdentifier;
		} else if (selectedWeights.startsWith(tfIdentifier)) {
			this.selectedEngine = tfIdentifier;
		} else if (selectedWeights.startsWith(tfJsIdentifier)) {
			this.selectedEngine = tfJsIdentifier;
		} else if (selectedWeights.startsWith(torchscriptIdentifier)) {
			this.selectedEngine = torchscriptIdentifier;
		} else if (selectedWeights.startsWith(bioengineIdentifier)) {
			this.selectedEngine = bioengineIdentifier;
		} else {
			throw new IllegalArgumentException("Unsupported Deep Learning framework in DeepIcy.");
		}
		setSelectedVersion(selectedWeights);
		setSelectedWeights(selectedWeights);
	}
	
	/**
	 * Sets the Deep Learning engine version selected by the user
	 * @param selectedWeights
	 * 	the selected weights format and version by the user in the GUI
	 */
	private void setSelectedVersion(String selectedWeights) {
		if (selectedWeights.equals(bioengineIdentifier)) {
			this.selectedVersion =  "";
			return;
		}
		String preffix = this.selectedEngine + "_v";
		this.selectedVersion = selectedWeights.substring(preffix.length());
		
	}
	
	/**
	 * Set the pair of weights selected by the user by saving the object that contains the info
	 * about them
	 * @param selectedWeights
	 * 	the string selected by the user as weights
	 * @throws IOException if the weights are not found in the avaiable ones
	 */
	private void setSelectedWeights(String selectedWeights) throws IOException {
		this.selectedWeights = getWeightsByIdentifier(selectedWeights);
	}
	
	/**
	 * Set the weights as loaded. Once there are loaded weights, no other weights of
	 * that same engine can be loaded
	 */
	public void setWeightsAsLoaded() {
		loadedWeights.put(selectedWeights.getWeightsFormat(), selectedWeights);
	}

    /** TODO finish when the BioEngine is better defined in the BioImage.io
     * Create the name for the BioEngine weights. The name contains the name of the BioEngine
     * 
     * @return the complete weights name
     */
    private String bioEngineName(String server) {
    	if (server.startsWith("https://"))
    		server = server.substring("https://".length());
    	else if (server.startsWith("http://"))
    		server = server.substring("http://".length());
    	
    	String name = bioengineIdentifier + " (" + server + ")";
		return name;
	}

    /**
     * Create the name of a pair of torchscript names. The name contains the name of the weights and
     * version number. If no version is provided, "Unknown" is used as version identifier
     * @param ww
     * 	weights object
     * @return the complete weights name
     */
    private String torchscriptEngineName(TorchscriptWeights ww) {
    	String name = torchscriptIdentifier + "_v";
    	String suffix = ww.getTrainingVersion();
    	if (suffix == null) {
    		boolean exist = true;
    		suffix = "Unknown";
    		int c = 0;
    		while (exist) {
    			if (!this.weightsDic.keySet().contains(name + suffix + c)) {
    				suffix = suffix + c;
    				exist = false;
    			}
    			c ++;
    		}
    	}
		return name + suffix;
	}

    /**
     * Create the name of a pair of torchscript names. The name contains the name of the weights and
     * version number. If no version is provided, "Unknown" is used as version identifier
     * @param ww
     * 	weights object
     * @return the complete weights name
     */
	private String tfJsEngineName(TfJsWeights ww) {
    	String name = tfJsIdentifier + "_v";
    	String suffix = ww.getTrainingVersion();
    	if (suffix == null) {
    		boolean exist = true;
    		suffix = "Unknown";
    		int c = 0;
    		while (exist) {
    			if (!this.weightsDic.keySet().contains(name + suffix + c)) {
    				suffix = suffix + c;
    				exist = false;
    			}
    			c ++;
    		}
    	}
		return name + suffix;
	}

    /**
     * Create the name of a pair of torchscript names. The name contains the name of the weights and
     * version number. If no version is provided, "Unknown" is used as version identifier
     * @param ww
     * 	weights object
     * @return the complete weights name
     */
	private String onnxEngineName(OnnxWeights ww) {
    	String name = onnxIdentifier + "_v";
    	String suffix = ww.getTrainingVersion();
    	if (suffix == null) {
    		boolean exist = true;
    		suffix = "Unknown";
    		int c = 0;
    		while (exist) {
    			if (!this.weightsDic.keySet().contains(name + suffix + c)) {
    				suffix = suffix + c;
    				exist = false;
    			}
    			c ++;
    		}
    	}
		return name + suffix;
	}

    /**
     * Create the name of a pair of torchscript names. The name contains the name of the weights and
     * version number. If no version is provided, "Unknown" is used as version identifier
     * @param ww
     * 	weights object
     * @return the complete weights name
     */
	private String tfEngineName(TfWeights ww) {
    	String name = tfIdentifier + "_v";
    	String suffix = ww.getTrainingVersion();
    	if (suffix == null) {
    		boolean exist = true;
    		suffix = "Unknown";
    		int c = 0;
    		while (exist) {
    			if (!weightsDic.keySet().contains(name + suffix + c)) {
    				suffix = suffix + c;
    				exist = false;
    			}
    			c ++;
    		}
    	}
		return name + suffix;
	}

    /**
     * Create the name of a pair of torchscript names. The name contains the name of the weights and
     * version number. If no version is provided, "Unknown" is used as version identifier
     * @param ww
     * 	weights object
     * @return the complete weights name
     */
	private String torchEngineName(PytorchWeights ww) {
    	String name = torchIdentifier + "_v";
    	String suffix = ww.getTrainingVersion();
    	if (suffix == null) {
    		boolean exist = true;
    		suffix = "Unknown";
    		int c = 0;
    		while (exist) {
    			if (!weightsDic.keySet().contains(name + suffix + c)) {
    				suffix = suffix + c;
    				exist = false;
    			}
    			c ++;
    		}
    	}
		return name + suffix;
	}

    /**
     * Create the name of a pair of torchscript names. The name contains the name of the weights and
     * version number. If no version is provided, "Unknown" is used as version identifier
     * @param ww
     * 	weights object
     * @return the complete weights name
     */
	private String kerasEngineName(KerasWeights ww) {
    	String name = kerasIdentifier + "_v";
    	String suffix = ww.getTrainingVersion();
    	if (suffix == null) {
    		boolean exist = true;
    		suffix = "Unknown";
    		int c = 0;
    		while (exist) {
    			if (!weightsDic.keySet().contains(name + suffix + c)) {
    				suffix = suffix + c;
    				exist = false;
    			}
    			c ++;
    		}
    	}
		return name + suffix;
	}
	
	/**
	 * REturn the tag used to identify Deep Learning engines that are not present
	 * in the local engines repo
	 * @return 
	 */
	public static String getMissingEngineTag() {
		return missingVersion;
	}
	
	/**
	 * REturn the tag used to identify Deep Learning engines that are not supported by DeepIcy
	 * @return 
	 */
	public static String getNotSupportedEngineTag() {
		return notSupported;
	}
	
	/**
	 * REturn the tag used to identify Deep Learning engines where another
	 * version oof the engine has been loaded
	 * @return 
	 */
	public static String getAlreadyLoadedEngineTag() {
		return alreadyLoaded;
	}
	
	/**
	 * REturn the tag used to identify Deep Learning engines that support GPU
	 * @return 
	 */
	public static String getGPUSuffix() {
		return gpuSuffix;
	}

	/**
	 * 
	 * @return the identifier key used for the Keras Deep Learning framework
	 */
	public static String getKerasID() {
		return kerasIdentifier;
	}

	/**
	 * 
	 * @return the identifier key used for the Onnx Deep Learning framework
	 */
	public static String getOnnxID() {
		return onnxIdentifier;
	}

	/**
	 * 
	 * @return the identifier key used for the Pytorch Deep Learning framework
	 */
	public static String getPytorchID() {
		return torchIdentifier;
	}

	/**
	 * 
	 * @return the identifier key used for the Tensorflow JS Deep Learning framework
	 */
	public static String getTensorflowJsID() {
		return tfJsIdentifier;
	}

	/**
	 * 
	 * @return the identifier key used for the Tensorflow Deep Learning framework
	 */
	public static String getTensorflowID() {
		return tfIdentifier;
	}

	/**
	 * 
	 * @return the identifier key used for the torchscript Deep Learning framework
	 */
	public static String getTorchscriptID() {
		return torchscriptIdentifier;
	}

	/**
	 * 
	 * @return the identifier key used for the Bioengine Deep Learning framework
	 */
	public static String getBioengineID() {
		return bioengineIdentifier;
	}
}

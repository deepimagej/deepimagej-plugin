package deepimagej.tools.weights;

import java.util.List;
import java.util.Map;

/**
 * Interface that contains all the methods needed to create a 
 * Bioimage.io weight specification for any format.
 * For more info go to: 
 * https://github.com/bioimage-io/spec-bioimage-io/blob/gh-pages/weight_formats_spec_0_4.md
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public interface WeightFormatInterface {
	
	/**
	 * Retrieve the version of the framework used to train the
	 * weights
	 * 
	 * @return version used to train the weights
	 */
	public String getTrainingVersion();
	
	/**
	 * Format of the weights of the model. The supported weights by the Bioimage.io are:
	 * keras_hdf5, onnx, pytorch_state_dict, tensorflow_js, tensorflow_saved_model_bundle
	 * and torchscript
	 * 
	 * @return the Deep Learning framework of the model
	 */
	public String getWeightsFormat();
	
	/**
	 * SHA256 checksum of the source file
	 * 
	 * @return SHA256 checksum of the source file
	 */
	public String getSha256();
	
	/**
	 * REturn URL to the weights in the local machine
	 * @return URL to the weights in the local machine
	 */
	public String getSource();
	
	/**
	 * REturn name of the source file for the weights. Does not include the path to the directory
	 * @return name of the file containing the weights
	 */
	public String getSourceFileName();
	
	/**
	 * List of the authors that have trained the model in the case there is no
	 * parent model, or list of authors that converted the weights into this format
	 * 
	 * @return the authors of the weights
	 */
	public List<String> getAuthors();
	
	/** REturn the attachments needed for the weights 
	 * 
	 * @return the attachements. Attachments consists of a dictionary 
	 * of text keys and list values (that may contain any valid yaml) to 
	 * additional, relevant files that are specific to the current 
	 * weight format. A list of URIs can be listed under the files key 
	 * to included additional files for generating the model package 
	 */
	public Map<String, Object> getAttachments();
	
	/**
	 * Returns the parent of the weights. 
	 * @return the parent of the weights. This is the source weights
	 * used as input for converting the weights to this format. For 
	 * example, if the weights were converted from the format 
	 * pytorch_state_dict to torchscript, the parent is pytorch_state_dict. 
	 * All weight entries except one (the initial set of weights 
	 * resulting from training the model), need to have this field.
	 */
	public String getParent();

	/**
	 * Source code of the model architecture that either points to a local 
	 * implementation: <relative path to file>:<identifier of implementation 
	 * within the file> or the implementation in an available dependency: 
	 * <root-dependency>.<sub-dependency>.<identifier>. For example: 
	 * my_function.py:MyImplementation or bioimageio.core.some_module.some_class_or_function.
	 * 
	 * @return the archiecture used to train the weights
	 */
	public String getArchitecture();
	
	/**
	 * SHA256 of the architecture
	 * @return the SHA256 of the architecture
	 */
	public String getArchitectureSha256();
	
	/**
	 * Method to set whether the engine used for this weights supports GPU or not
	 * @return
	 */
	public void supportGPU(boolean support);
	
	/**
	 * Method to know whether the engine used for this weights supports GPU or not
	 * @return
	 */
	public boolean isSupportGPU();
}

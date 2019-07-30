package deepimagej;

import java.util.List;
import java.util.concurrent.Callable;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

public class Runner implements Callable<ImagePlus> {

	private ImagePlus		imp;
	private DeepPlugin		dp;
	private RunnerProgress	rp;
	private Log				log;
	private int				currentPatch	= 0;
	private int				totalPatch	= 0;

	public Runner(DeepPlugin dp, RunnerProgress rp, ImagePlus imp, Log log) {
		this.dp = dp;
		this.rp = rp;
		this.log = log;
		this.imp = imp;
	}

	public Runner(DeepPlugin dp, RunnerProgress rp, Log log) {
		this.dp = dp;
		this.rp = rp;
		this.log = log;
		this.imp = null;
	}

	@Override
	public ImagePlus call() {
		if (imp == null)
			imp = WindowManager.getCurrentImage();
		if (imp == null) {
			rp.stop();
			return null;
		}
		if (log.getLevel() >= 1)
			rp.setVisible(true);

		int nx = imp.getWidth();
		int ny = imp.getHeight();
		Parameters params = dp.params;

		if (3 * nx < params.patch || 3 * ny < params.patch) {
			IJ.log("Error patch size is too large " + params.patch);
			rp.stop();
			return null;
		}

		// Create the image that is going to be fed to the graph
		ImagePlus impatch = null;
		String in1 = params.inputs[0];
		String outputTitle = dp.getName() + " of " + imp.getTitle();
		int[] dim = params.in_dimensions;
		String[] outputs = params.outputs;

		SavedModelBundle model = dp.getModel();
		ImagePlus out = null;
		SignatureDef sig = graph2SigDef(model, dp.params.graph);

		// Order of the dimensions. For example "NHWC"-->Batch size, Height, Width, Channels
		String inputForm = params.input_form[0];
		// Order of the dimensions. For example "NHWC"-->Batch size, Height, Width, Channels
		String outputForm = params.output_form[0];
		int nChannels = Integer.parseInt((String) params.channels);
		int overlap = params.overlap;

		int channel_pos = ArrayOperations.indexOf(inputForm.split(""), "C");
		int[] inDim = imp.getDimensions();
		if (inDim[2] != nChannels && dim[channel_pos] != -1) {
			IJ.log("Error in nChannel " + nChannels);
			rp.stop();
			return out;
		}

		int padding = ArrayOperations.paddingSize(nx, ny, params.patch, overlap);
		ImagePlus mirrorImage = ArrayOperations.createMirroredImage(imp, padding, nx, ny, nChannels);

		int roi = params.patch - overlap * 2;
		int npx = nx / roi;
		int npy = ny / roi;
		if (nx % roi != 0)
			npx++;
		if (ny % roi != 0)
			npy++;
		currentPatch = 0;
		totalPatch = npx * npy;

		String outputName;
		for (int i = 0; i < npx; i++) {
			for (int j = 0; j < npy; j++) {
				currentPatch++;
				if (rp.isStopped()) {
					rp.stop();
					return out;
				}
				int x = padding + roi * i;
				int y = padding + roi * j;
				ImagePlus patch = ArrayOperations.extractPatch(mirrorImage, x, y, roi, overlap, dim, nChannels);
				log.print("Extract Patch (" + (i + 1) + ", " + (j + 1) + ") patch size: " + patch.getWidth() + "x" + patch.getHeight() + " pixels");
				Tensor<?> inputTensor = ImagePlus2Tensor.imPlus2tensor(patch, inputForm, nChannels);
				Session.Runner sess = model.session().runner();
				sess = sess.feed(opName(sig.getInputsOrThrow(in1)), inputTensor);
				for (int k = 0; k < outputs.length; k++) {
					outputName = outputs[k];
					sess = sess.fetch(opName(sig.getOutputsOrThrow(outputName)));
					log.print("Session fetch " + k);
				}
				try {
					List<Tensor<?>> fetches = sess.run();
					for (int counter = 0; counter < outputs.length; counter++) {
						log.print("Session run " + (counter+1) + "/"  + outputs.length);
						Tensor<?> result = fetches.get(counter);
						impatch = ImagePlus2Tensor.tensor2ImagePlus(result, outputForm);
						counter++;
					}
				}
				catch (Exception ex) {
					IJ.log("Error in the TensorFlow library");
					IJ.log(ex.getMessage());
					rp.stop();
					return out;
				}
				if (out == null) {
					int[] dims = impatch.getDimensions();
					out = IJ.createHyperStack(outputTitle, nx, ny, dims[2], dims[3], dims[4], 32);
					out.getProcessor().resetMinAndMax();
					out.show();
				}
				ArrayOperations.imagePlusReconstructor(out, impatch, roi * i, roi * j, roi, overlap);
				log.print("Create Output ");
				if (out != null)
					out.getProcessor().resetMinAndMax();
				if (rp.isStopped()) {
					rp.stop();
					return out;
				}
			}
		}
		rp.stop();
		return out;
	}

	public SignatureDef graph2SigDef(SavedModelBundle model, String key) {
		byte[] byte_graph = model.metaGraphDef();

		SignatureDef sig = null;
		try {
			sig = MetaGraphDef.parseFrom(byte_graph).getSignatureDefOrThrow(key);
		}
		catch (Exception e) {
			System.out.println("Invalid graph");
		}
		return sig;
	}

	private String opName(final TensorInfo t) {
		final String n = t.getName();
		if (n.endsWith(":0")) {
			return n.substring(0, n.lastIndexOf(":0"));
		}
		return n;
	}

	public String[] assignCharacter(String form, int[] expanded_dim, int[] im_plus_dim) {
		int rank = expanded_dim.length;
		String aux_key = "empty";
		String[] aux_array = createAuxArr(rank, aux_key);
		int start2find = 0;
		for (int i = 0; i < rank; i++) {
			char dim = form.charAt(i);
			int value = valueOfChar(im_plus_dim, dim);
			aux_array = namePosition(dim, expanded_dim, value, aux_array, aux_key, start2find);
		}
		return aux_array;
	}

	public String[] namePosition(char dim_name, int[] image_dims, int dim_value, String[] out_array, String key_word, int start) {
		// This method writes a character representing a dimension in the position where
		// it corresponds.
		// Names for the dimensions: "W"-->nx; "H"-->ny; "C"-->nc; "D"--> nz, "N"-->nb.
		// Example: image_dims= [256, 128, 3], dim_name = 'C', dim_value = 3, out_array
		// = ["nul, "nul", "nul"].
		// The output will be out_array = ["nul", "nul", "C"]

		int index = ArrayOperations.indexOf(image_dims, dim_value, start);
		if (out_array[index] == key_word) {
			out_array[index] = String.valueOf(dim_name);
		}
		else {
			out_array = namePosition(dim_name, image_dims, dim_value, out_array, key_word, index + 1);
		}
		return out_array;
	}

	public int valueOfChar(int[] im_plus_dim, char dim_name) {
		// This method takes advantage that the dimensions retrieved from an ImagePlus
		// always have the shape [nx, ny, nc, nz, nt] in order to retrieve the value
		// for the dimension specified by the given character. It also assumes that the
		// batch
		// size is always 1.
		// "W"-->nx; "H"-->ny; "C"-->nc; "D"--> nz, "N" = 1.
		int value = 0;
		if (dim_name == 'W') {
			value = im_plus_dim[0];
		}
		else if (dim_name == 'H') {
			value = im_plus_dim[1];
		}
		else if (dim_name == 'C') {
			value = im_plus_dim[2];
		}
		else if (dim_name == 'D') {
			value = im_plus_dim[3];
		}
		else if (dim_name == 'N') {
			value = 1;
		}
		return value;
	}

	public String[] createAuxArr(int size, String keyword) {
		// This method creates an auxiliar< string array with the where every entry is
		// the word inputs as keyword
		String[] aux_array = new String[size];
		for (int i = 0; i < size; i++) {
			aux_array[i] = keyword;
		}
		return aux_array;
	}

	public int getCurrentPatch() {
		return currentPatch;
	}

	public int getTotalPatch() {
		return totalPatch;
	}

}

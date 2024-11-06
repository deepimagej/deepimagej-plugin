package deepimagej;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import deepimagej.gui.adapter.ImageAdapter;
import deepimagej.tools.ImPlusRaiManager;
import ij.IJ;
import ij.ImagePlus;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.TensorSpec;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class IjAdapter implements ImageAdapter {

	@Override
	public <T extends RealType<T> & NativeType<T>> List<Tensor<T>> getInputTensors(ModelDescriptor descriptor) {
		ImagePlus imp = IJ.getImage();
		List<Tensor<T>> list = Arrays.asList(buildTensor(imp, descriptor.getInputTensors().get(0)));
		return list;
	}
	
	@Override
	public <T extends RealType<T> & NativeType<T>> List<Tensor<T>> 
	convertToInputTensors(Map<String, Object> inputs, ModelDescriptor descriptor) {
		List<Tensor<T>> inputTensors = new ArrayList<Tensor<T>>();
		for (TensorSpec tt : descriptor.getInputTensors()) {
			Object im = inputs.get(tt.getName());
			if (im == null)
				throw new IllegalArgumentException("Missing input tensor: " + tt.getName());
			else if (!(im instanceof ImagePlus))
				throw new IllegalArgumentException("Input object should be an ImagePlus: " + tt.getName());
			inputTensors.add(buildTensor((ImagePlus) im, tt));
		}
		return inputTensors;
	}
	
	private static <T extends RealType<T> & NativeType<T>> Tensor<T> buildTensor(ImagePlus imp, TensorSpec tensorSpec) {
		RandomAccessibleInterval<T> rai = ImageJFunctions.wrap(imp);
		String axesOrder = "xy";
		if (imp.getNChannels() != 1)
			axesOrder += "c";
		if (imp.getNSlices() != 1)
			axesOrder += "z";
		if (imp.getNFrames() != 1)
			axesOrder += "b";
		RandomAccessibleInterval<T> nRai = ImPlusRaiManager.permute(rai, axesOrder, tensorSpec.getAxesOrder());
		return Tensor.build(tensorSpec.getName(), tensorSpec.getAxesOrder(), nRai);
	}

}

package deepimagej;

import java.util.Arrays;
import java.util.List;

import deepimagej.gui.adapter.ImageAdapter;
import deepimagej.tools.ImPlusRaiManager;
import ij.IJ;
import ij.ImagePlus;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class IjAdapter implements ImageAdapter {

	@Override
	public <T extends RealType<T> & NativeType<T>> List<Tensor<T>> getInputTensors(ModelDescriptor descriptor) {
		ImagePlus imp = IJ.getImage();
		RandomAccessibleInterval<T> rai = ImageJFunctions.wrap(imp);
		String axesOrder = "xy";
		if (imp.getNChannels() != 1)
			axesOrder += "c";
		if (imp.getNSlices() != 1)
			axesOrder += "z";
		if (imp.getNFrames() != 1)
			axesOrder += "b";
		RandomAccessibleInterval<T> nRai = ImPlusRaiManager.permute(rai, axesOrder, descriptor.getInputTensors().get(0).getAxesOrder());
		List<Tensor<T>> list = Arrays.asList(Tensor.build(descriptor.getInputTensors().get(0).getName(), 
															descriptor.getInputTensors().get(0).getAxesOrder(), 
															nRai));
		return list;
	}

}

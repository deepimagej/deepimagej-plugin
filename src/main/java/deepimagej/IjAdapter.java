package deepimagej;

import deepimagej.adapter.gui.ImageAdapter;
import ij.IJ;
import ij.ImagePlus;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class IjAdapter implements ImageAdapter {

	@Override
	public <T extends RealType<T> & NativeType<T>> Tensor<T> getCurrentTensor() {
		ImagePlus imp = IJ.getImage();
		RandomAccessibleInterval<T> rai = ImageJFunctions.wrap(imp);
		String axesOrder = "xy";
		if (imp.getNChannels() != 1)
			axesOrder += "c";
		if (imp.getNSlices() != 1)
			axesOrder += "z";
		if (imp.getNFrames() != 1)
			axesOrder += "b";
		return Tensor.build("imput_0", axesOrder, rai);
	}

}

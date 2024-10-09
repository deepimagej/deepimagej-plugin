package deepimagej.tools;

import ij.ImagePlus;
import io.bioimage.modelrunner.tensor.Utils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ImPlusRaiManager {
	
	private static final String IJ_AXES_ORDER = "xyczb";

	public static <T extends RealType<T> & NativeType<T>>
	ImagePlus convert(RandomAccessibleInterval<T> rai, String axesOrder) {
		String newImAxesOrder = addExtraDims(rai, axesOrder, IJ_AXES_ORDER);
		transposeToAxesOrder(rai, newImAxesOrder, IJ_AXES_ORDER);
		return ImageJFunctions.show(rai);
	}

	public static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<T> convert(ImagePlus imp, String axesOrder) {
		Img<T> rai = ImageJFunctions.wrap(imp);
		String impAxesOrder = "";
		String[] ijAxesOrder = IJ_AXES_ORDER.split("");
		for (int i = 0; i < imp.getDimensions().length; i ++) {
			if (imp.getDimensions()[i] != 1)
				impAxesOrder += ijAxesOrder[i];
		}
		String newImAxesOrder = addExtraDims(rai, impAxesOrder, axesOrder);
		transposeToAxesOrder(rai, newImAxesOrder, axesOrder);
		return rai;
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	String addExtraDims(RandomAccessibleInterval<T> rai, String ogAxes, String targetAxes) {
		for (String ax : targetAxes.split("")) {
			if (ogAxes.contains(ax))
				continue;
			rai = Views.addDimension(rai, 0, 0);
			ogAxes += ax;
		}
		return ogAxes;
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	void transposeToAxesOrder(RandomAccessibleInterval<T> rai, String ogAxes, String targetAxes) {
		int[] transformation = new int[ogAxes.length()];
		int c = 0;
		for (String ss : targetAxes.split("")) {
			transformation[c ++] = ogAxes.indexOf(ss);
		}
		Utils.rearangeAxes(rai, transformation);
	}
	
}

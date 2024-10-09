package deepimagej.tools;

import ij.ImagePlus;
import io.bioimage.modelrunner.tensor.Utils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ImPlusRaiManager {
	
	private static final String IJ_AXES_ORDER = "xyczb";

	public static <T extends RealType<T> & NativeType<T>>
	ImagePlus convert(RandomAccessibleInterval<T> rai, String axesOrder) {
		String newImAxesOrder = removeExtraDims(rai, IJ_AXES_ORDER, axesOrder);
		rai = transposeToAxesOrder(rai, axesOrder, newImAxesOrder);
		return ImageJFunctions.show(rai);
	}

	public static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<T> convert(ImagePlus imp, String axesOrder) {
		RandomAccessibleInterval<T> rai = ImageJFunctions.wrap(imp);
		String impAxesOrder = "";
		String[] ijAxesOrder = IJ_AXES_ORDER.split("");
		for (int i = 0; i < imp.getDimensions().length; i ++) {
			if (imp.getDimensions()[i] != 1)
				impAxesOrder += ijAxesOrder[i];
		}
		String newImAxesOrder = addExtraDims(rai, impAxesOrder, axesOrder);
		for (int i = 0; i < newImAxesOrder.length() - impAxesOrder.length(); i ++)
			rai = Views.addDimension(rai, 0, 0);
		rai = transposeToAxesOrder(rai, newImAxesOrder, axesOrder);
		return rai;
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	String removeExtraDims(RandomAccessibleInterval<T> rai, String ogAxes, String targetAxes) {
		String nAxes = "";
		for (String ax : ogAxes.split("")) {
			if (!targetAxes.contains(ax))
				continue;
			nAxes += ax;
		}
		return nAxes;
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	String addExtraDims(RandomAccessibleInterval<T> rai, String ogAxes, String targetAxes) {
		for (String ax : targetAxes.split("")) {
			if (ogAxes.contains(ax))
				continue;
			ogAxes += ax;
		}
		return ogAxes;
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<T> transposeToAxesOrder(RandomAccessibleInterval<T> rai, String ogAxes, String targetAxes) {
		int[] transformation = new int[ogAxes.length()];
		int c = 0;
		for (String ss : targetAxes.split("")) {
			transformation[c ++] = ogAxes.indexOf(ss);
		}
		return Utils.rearangeAxes(rai, transformation);
	}
	
}

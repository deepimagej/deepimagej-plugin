package deepimagej.tools;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import ij.ImagePlus;
import io.bioimage.modelrunner.tensor.Utils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ImPlusRaiManager {
	
	public static final String IJ_AXES_ORDER = "xyczb";

	public static <T extends RealType<T> & NativeType<T>>
	ImagePlus convert(RandomAccessibleInterval<T> rai, String axesOrder) {
		axesOrder = axesOrder.toLowerCase().replaceAll("t", "b");
		rai = convertToAxesOrder(rai, axesOrder, IJ_AXES_ORDER);
		return ImageJFunctions.wrap(rai, UUID.randomUUID().toString(), ( ExecutorService ) null);
	}

	public static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<T> convertToAxesOrder(RandomAccessibleInterval<T> rai, String axesOrder, String targetAxesOrder) {
		int ogLength = axesOrder.length();
		for (int i = ogLength - 1; i >= 0; i --) {
			if (targetAxesOrder.indexOf(axesOrder.split("")[i]) != -1)
				continue;
			axesOrder = axesOrder.substring(0, i) + axesOrder.substring(i + 1, axesOrder.length());
			rai = Views.hyperSlice(rai, i, 0);
		}
		String newImAxesOrder = addExtraDims(axesOrder, targetAxesOrder);
		for (int i = 0; i < (newImAxesOrder.length() - axesOrder.length()); i ++)
			rai = Views.addDimension(rai, 0, 0);
		rai = transposeToAxesOrder(rai, newImAxesOrder, IJ_AXES_ORDER);
		return rai;
	}

	public static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<T> convert(ImagePlus imp, String axesOrder) {
		axesOrder = axesOrder.toLowerCase().replace("t", "b");
		RandomAccessibleInterval<T> rai = ImageJFunctions.wrap(imp);
		String impAxesOrder = "";
		String[] ijAxesOrder = IJ_AXES_ORDER.split("");
		for (int i = 0; i < imp.getDimensions().length; i ++) {
			if (imp.getDimensions()[i] != 1)
				impAxesOrder += ijAxesOrder[i];
		}
		String newImAxesOrder = addExtraDims(impAxesOrder, axesOrder);
		for (int i = 0; i < newImAxesOrder.length() - impAxesOrder.length(); i ++)
			rai = Views.addDimension(rai, 0, 0);
		rai = transposeToAxesOrder(rai, newImAxesOrder, axesOrder);
		return rai;
	}

	public static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<T> permute(RandomAccessibleInterval<T> rai, String ogAxesOrder, String targetAxesOrder) {
		int n = ogAxesOrder.length();
		for (int i = n - 1; i >= 0; i --) {
			if (targetAxesOrder.contains(ogAxesOrder.split("")[i]))
				continue;
			rai = Views.hyperSlice(rai, i, 0);
			ogAxesOrder = ogAxesOrder.substring(0, i) + ogAxesOrder.substring(i + 1);
		}
		String newImAxesOrder = addExtraDims(ogAxesOrder, targetAxesOrder);
		for (int i = 0; i < newImAxesOrder.length() - ogAxesOrder.length(); i ++)
			rai = Views.addDimension(rai, 0, 0);
		
		return transposeToAxesOrder(rai, newImAxesOrder, targetAxesOrder);
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	String addExtraDims(String ogAxes, String targetAxes) {
		ogAxes = ogAxes.toLowerCase().replace("t", "b");
		targetAxes = targetAxes.toLowerCase().replace("t", "b");
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

package deepimagej.gui;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import deepimagej.Runner;
import deepimagej.tools.ImPlusRaiManager;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.CompositeConverter;
import ij.plugin.frame.Recorder;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.TensorSpec;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.gui.adapter.GuiAdapter;
import io.bioimage.modelrunner.gui.adapter.RunnerAdapter;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ImageJGui implements GuiAdapter {

	@Override
	public String getSoftwareName() {
		return "deepImageJ";
	}

	@Override
	public String getSoftwareDescription() {
		return "The Fiji/ImageJ Plugin for AI";
	}
	
	@Override
	public Color getTitleColor() {
		return new Color(110, 38, 14);
	}
	
	@Override
	public Color getSubtitleColor() {
		return Color.black;
	}
	
	@Override
	public Color getHeaderColor() {
		return Color.black;
	}

	@Override
	public String getIconPath() {
		return "dij_imgs/deepimagej_icon.png";
	}

	@Override
	public String getModelsDir() {
		return new File(deepimagej.Constants.FIJI_FOLDER + File.separator + "models").getAbsolutePath();
	}

	@Override
	public String getEnginesDir() {
		return new File(deepimagej.Constants.FIJI_FOLDER + File.separator + "engines").getAbsolutePath();
	}
	
	@Override
	public void notifyModelUsed(String modelAbsPath) {
		if (!Recorder.record)
			return;
		
		String macro = String.format("run(\"DeepImageJ Run\", \"model_path=[%s]", modelAbsPath) + "\")" + System.lineSeparator();
		Recorder.recordString(macro);
	}

	@Override
	public RunnerAdapter createRunner(ModelDescriptor descriptor) throws IOException, LoadEngineException {
		return Runner.create(descriptor);
	}

	@Override
	public RunnerAdapter createRunner(ModelDescriptor descriptor, String enginesPath) throws IOException, LoadEngineException {
		return Runner.create(descriptor, enginesPath);
	}

	@Override
	public List<String> getInputImageNames() {
		List<String> names = new ArrayList<String>();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return names;
		names.add(imp.getTitle());
		return names;
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> void displayRai(RandomAccessibleInterval<T> rai, String axesOrder, String imTitle) {
		ImagePlus im = ImPlusRaiManager.convert(rai, axesOrder);
		if (WindowManager.getWindow(imTitle) != null) {
	    	String noExtension = imTitle;
	    	String extension = ".tif";
    		int pointInd = imTitle.lastIndexOf(".");
	    	if (pointInd != -1) {
	    		noExtension = imTitle.substring(0, pointInd);
	    		extension = imTitle.substring(pointInd);
	    	}
	    	int c = 1;
	    	while (WindowManager.getWindow(imTitle) != null) {
	    		imTitle = noExtension + "-" + c + extension;
	    	}
		}
		im.setTitle(imTitle);
		im.getProcessor().resetMinAndMax();;
		SwingUtilities.invokeLater(() -> im.show());
		
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> List<Tensor<T>> getInputTensors(ModelDescriptor descriptor) {
		ImagePlus imp = WindowManager.getCurrentImage();
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
				throw new IllegalArgumentException("Input object should be a Sequence: " + tt.getName());
			ImagePlus imp = (ImagePlus) im;
			inputTensors.add(buildTensor(imp, tt));
		}
		return inputTensors;
	}
	
	private static <T extends RealType<T> & NativeType<T>> Tensor<T> buildTensor(ImagePlus imp, TensorSpec tensorSpec) {
		boolean isColorRGB = imp.getType() == ImagePlus.COLOR_RGB;
		imp = isColorRGB ? CompositeConverter.makeComposite(imp) : imp;
		RandomAccessibleInterval<T> rai = Views.dropSingletonDimensions((RandomAccessibleInterval<T>) ImageJFunctions.wrap(imp));
		String axesOrder = "xy";
		if (imp.getNChannels() != 1 && (tensorSpec.getAxesOrder().toLowerCase().contains("c") || imp.getNFrames() != 1))
			axesOrder += "c";
		if (imp.getNChannels() != 1 && !tensorSpec.getAxesOrder().toLowerCase().contains("c") && imp.getNFrames() == 1)
			axesOrder += "b";
		if (imp.getNSlices() != 1 && (tensorSpec.getAxesOrder().toLowerCase().contains("z") || imp.getNFrames() != 1) || axesOrder.contains("b"))
			axesOrder += "z";
		if (imp.getNSlices() != 1 && !tensorSpec.getAxesOrder().toLowerCase().contains("z") && imp.getNFrames() == 1 && !axesOrder.contains("b"))
			axesOrder += "b";
		if (imp.getNFrames() != 1)
			axesOrder += "b";
		for (String ax : tensorSpec.getAxesOrder().split("")) {
			if (axesOrder.contains(ax))
				continue;
			rai = Views.addDimension(rai, 0, 0);
			axesOrder += ax;
		}
		RandomAccessibleInterval<T> nRai = ImPlusRaiManager.permute(rai, axesOrder, tensorSpec.getAxesOrder());
		return Tensor.build(tensorSpec.getName(), tensorSpec.getAxesOrder(), nRai);
	}

}

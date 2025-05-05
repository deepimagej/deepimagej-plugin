package deepimagej.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import deepimagej.Runner;
import deepimagej.tools.ImPlusRaiManager;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.CompositeConverter;
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
	public String getIconPath() {
		return "dij_imgs/deepimagej_icon.png";
	}

	@Override
	public String getModelsDir() {
		return new File("models").getAbsolutePath();
	}

	@Override
	public String getEnginesDir() {
		return new File("engines").getAbsolutePath();
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
		names.add(IJ.getImage().getTitle());
		return names;
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> void displayRai(RandomAccessibleInterval<T> rai, String axesOrder, String imTitle) {
		ImagePlus im = ImPlusRaiManager.convert(rai, axesOrder);
		im.setTitle(imTitle);
		SwingUtilities.invokeLater(() -> im.show());
		
	}

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

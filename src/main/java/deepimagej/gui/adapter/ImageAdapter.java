package deepimagej.gui.adapter;

import java.util.List;
import java.util.Map;

import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface ImageAdapter {

	public <T extends RealType<T> & NativeType<T>> List<Tensor<T>> getInputTensors(ModelDescriptor descriptor);
	
	public <T extends RealType<T> & NativeType<T>> List<Tensor<T>> convertToInputTensors(Map<String, Object> inputs, ModelDescriptor descriptor);
}

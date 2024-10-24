package deepimagej.adapter.gui;

import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface ImageAdapter {

	public <T extends RealType<T> & NativeType<T>> Tensor<T> getCurrentTensor();
}

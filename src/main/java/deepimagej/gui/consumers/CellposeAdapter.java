package deepimagej.gui.consumers;

import java.util.List;

import javax.swing.JComponent;

import ij.ImageListener;
import ij.ImagePlus;
import io.bioimage.modelrunner.gui.ConsumerInterface;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class CellposeAdapter extends ConsumerInterface implements ImageListener {

	@Override
	public void setListenersForComponents(List<JComponent> components) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getFocusedImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getFocusedImageAsRai() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> void display(RandomAccessibleInterval<T> rai, String axes,
			String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void imageOpened(ImagePlus imp) {
		if (!imp.getWindow().isFocused())
			return;
		
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		if (!imp.getWindow().isFocused())
			return;
		
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (!imp.getWindow().isFocused())
			return;
		
	}

}

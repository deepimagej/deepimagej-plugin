package deepimagej.gui.consumers;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.JComponent;

import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import io.bioimage.modelrunner.gui.ConsumerInterface;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class CellposeAdapter extends ConsumerInterface implements ImageListener {
	
	public CellposeAdapter() {
		int[] ids = WindowManager.getIDList();
        if (ids == null) return;

        for (int id : ids) {
            ImagePlus imp = WindowManager.getImage(id);
            if (imp == null) continue;
            ImageWindow win = imp.getWindow();
            if (win == null) continue;
            win.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                	updateComboBox(imp);
                }
                @Override
                public void focusLost(FocusEvent e) {
                }
            });
            if (win.isFocusOwner()) updateComboBox(imp);
        }
	}

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
	
	private void updateComboBox(ImagePlus imp) {
		
	}

	@Override
	public void imageOpened(ImagePlus imp) {
        ImageWindow win = imp.getWindow();
        if (win != null) {
            win.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    updateComboBox(imp);
                }
                @Override
                public void focusLost(FocusEvent e) {
                }
            });
        }
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

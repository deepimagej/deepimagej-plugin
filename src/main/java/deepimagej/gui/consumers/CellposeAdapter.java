/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package deepimagej.gui.consumers;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import deepimagej.tools.ImPlusRaiManager;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.plugin.CompositeConverter;
import io.bioimage.modelrunner.gui.ConsumerInterface;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * @author Carlos Garcia
 */
public class CellposeAdapter extends ConsumerInterface implements ImageListener {
	
	private JComboBox<String> cbox;
	
	public CellposeAdapter() {
        ImagePlus.addImageListener(this);
		int[] ids = WindowManager.getIDList();
        if (ids == null) return;

        for (int id : ids) {
            ImagePlus imp = WindowManager.getImage(id);
            if (imp == null) continue;
            ImageWindow win = imp.getWindow();
            if (win == null) continue;
            win.addWindowFocusListener(new WindowFocusListener() {

				@Override
				public void windowGainedFocus(WindowEvent e) {
                	updateComboBox(imp);
				}

				@Override
				public void windowLostFocus(WindowEvent e) {
				}
            	
            });
            if (win.isFocusOwner()) updateComboBox(imp);
        }
	}

	@Override
	public String getModelsDir() {
		return new File("models").getAbsolutePath();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setComponents(List<JComponent> components) {
		this.componentsGui = components;
		if (varNames != null && varNames.indexOf("Channel:") != -1) {
			this.cbox = (JComboBox<String>) componentsGui.get(varNames.indexOf("Channel:"));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setVarNames(List<String> componentNames) {
		this.varNames = componentNames;
		if (componentsGui != null && varNames.indexOf("Channel:") != -1) {
			this.cbox = (JComboBox<String>) componentsGui.get(varNames.indexOf("Channel:"));
		}
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> void display(RandomAccessibleInterval<T> rai, String axes,
			String name) {
		ImagePlus imp = ImPlusRaiManager.convert(rai, axes);
		imp.setTitle(name);
		imp.show();
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getFocusedImageAsRai() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return null;
		boolean isColorRGB = imp.getType() == ImagePlus.COLOR_RGB;
		RandomAccessibleInterval<T> rai = 
				ImPlusRaiManager.convert(isColorRGB ? CompositeConverter.makeComposite(imp) : imp, "xyczt");
		// We only allow multichannel, single slice images. If there are several frames, 
		// they will be processed sequentially
		if (imp.getNFrames() == 1) {
			return Views.hyperSlice(Views.hyperSlice(rai, 4, 0), 3, 0);
		}
		return Views.hyperSlice(rai, 3, 0);
	}

	@Override
	public Object getFocusedImage() {
		return WindowManager.getCurrentImage();
	}

	@Override
	public String getFocusedImageName() {
		return WindowManager.getCurrentImage().getTitle();
	}

	@Override
	public Integer getFocusedImageChannels() {
		ImagePlus imp = (ImagePlus) getFocusedImage();
		if(imp == null)
			return null;
		return imp.getNChannels();
	}

	@Override
	public Integer getFocusedImageSlices() {
		ImagePlus imp = (ImagePlus) getFocusedImage();
		if(imp == null)
			return null;
		return imp.getNSlices();
	}

	@Override
	public Integer getFocusedImageFrames() {
		ImagePlus imp = (ImagePlus) getFocusedImage();
		if(imp == null)
			return null;
		return imp.getNFrames();
	}

	@Override
	public Integer getFocusedImageWidth() {
		ImagePlus imp = (ImagePlus) getFocusedImage();
		if(imp == null)
			return null;
		return imp.getWidth();
	}

	@Override
	public Integer getFocusedImageHeight() {
		ImagePlus imp = (ImagePlus) getFocusedImage();
		if(imp == null)
			return null;
		return imp.getHeight();
	}
	
	private void updateComboBox(ImagePlus imp) {
		if (cbox == null) return;
		if ((imp.getType() == ImagePlus.COLOR_RGB || imp.getNChannels() == 3) && cbox.getItemCount() != 2) {
	        cbox.setModel(new DefaultComboBoxModel<>(new String[] {"[2,3]", "[2,1]"}));
		} else if (imp.getNChannels() == 1 && cbox.getItemCount() != 1) {
	        cbox.setModel(new DefaultComboBoxModel<>(new String[] {"[0,0]"}));
		} else if (imp.getNChannels() != 1 && imp.getNChannels() == 3) {
	        cbox.setModel(new DefaultComboBoxModel<>(new String[] {"[0,0]", "[2,3]", "[2,1]"}));
		}
	}

	@Override
	public void imageOpened(ImagePlus imp) {
        ImageWindow win = imp.getWindow();
        if (win != null) {
            win.addWindowFocusListener(new WindowFocusListener() {

				@Override
				public void windowGainedFocus(WindowEvent e) {
                	updateComboBox(imp);
				}

				@Override
				public void windowLostFocus(WindowEvent e) {
				}
            	
            });
        }
        if (win.isFocused()) updateComboBox(imp);
	}

	@Override
	public void imageClosed(ImagePlus imp) {
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
	}

}

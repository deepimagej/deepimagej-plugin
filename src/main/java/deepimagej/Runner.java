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

package deepimagej;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import deepimagej.tools.ImPlusRaiManager;
import ij.IJ;
import ij.ImagePlus;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.TensorSpec;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.gui.adapter.RunnerAdapter;
import io.bioimage.modelrunner.numpy.DecodeNumpy;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class Runner extends RunnerAdapter {
	
	List<String> inputNames;

	private Runner(ModelDescriptor descriptor) throws IOException, LoadEngineException {
		super(descriptor, Runner.class.getClassLoader());
	}

	private Runner(ModelDescriptor descriptor, String enginesPath) 
			throws IOException, LoadEngineException {
		super(descriptor, enginesPath, Runner.class.getClassLoader());
	}
	
	public static Runner create(ModelDescriptor descriptor) throws IOException, LoadEngineException {
		return new Runner(descriptor);
	}
	
	public static Runner create(ModelDescriptor descriptor, String enginesPath) throws IOException, LoadEngineException {
		return new Runner(descriptor, enginesPath);
	}
	
	@Override
	protected <T extends RealType<T> & NativeType<T>>
	LinkedHashMap<TensorSpec, RandomAccessibleInterval<T>> displayTestInputs(LinkedHashMap<TensorSpec, String> testInputs) {
		LinkedHashMap<TensorSpec, RandomAccessibleInterval<T>> inputRais = new LinkedHashMap<TensorSpec, RandomAccessibleInterval<T>>();
		for (Entry<TensorSpec, String> input : testInputs.entrySet()) {
			RandomAccessibleInterval<T> rai;
			if (input.getValue().endsWith(".npy")) {
				try {
					rai = DecodeNumpy.loadNpy(input.getValue());
					ImagePlus image = ImPlusRaiManager.convert(rai, input.getKey().getAxesOrder());
					SwingUtilities.invokeLater(() -> image.show()); 
				} catch (IOException e) {
					throw new RuntimeException("Unexpected error reading .npy file.");
				}
			} else {
				ImagePlus imp = IJ.openImage(input.getValue());
				rai = ImPlusRaiManager.convert(imp, input.getKey().getAxesOrder());
				SwingUtilities.invokeLater(() -> imp.show());
			}
			inputRais.put(input.getKey(), rai);
		}
		return inputRais;
	}
	
	@Override
	protected LinkedHashMap<TensorSpec, String> getTestInputs() {
		LinkedHashMap<TensorSpec, String> testInputs = new LinkedHashMap<TensorSpec, String>();
		for (TensorSpec tt : this.descriptor.getInputTensors()) {
			String sampleFile = tt.getTestTensorName();
			if (sampleFile == null)
				sampleFile = tt.getSampleTensorName();
			if (sampleFile == null)
				throw new RuntimeException("Sample/Test files for input tensor '" + tt.getName() + "' are missing, please download them.");
			File ff = new File(sampleFile);
			if (!ff.isFile())
				ff = new File(model.getModelFolder() + File.separator + sampleFile);
			if (!ff.isFile())
				throw new RuntimeException("Sample/Test files for input tensor '" + tt.getName() + "' are missing, please download them.");
			testInputs.put(tt, ff.getAbsolutePath());
		}
		return testInputs;
	}
}

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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.TensorSpec;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.exceptions.LoadEngineException;
import io.bioimage.modelrunner.exceptions.LoadModelException;
import io.bioimage.modelrunner.exceptions.RunModelException;
import io.bioimage.modelrunner.model.Model;
import io.bioimage.modelrunner.tensor.Tensor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class Runner implements Closeable {
	
	private final ModelDescriptor descriptor;
	
	private final Model model;
	
	private boolean closed = false;

	public Runner(ModelDescriptor descriptor) {
		this.descriptor = descriptor;
		try {
			this.model = Model.createBioimageioModel(new File(descriptor.getModelPath()).getParentFile().getAbsolutePath());
		} catch (ModelSpecsException | LoadEngineException | IOException e) {
			throw new IllegalArgumentException("Something has happened, the model wanted does not "
					+ "seem to exist anymore or it has been moved.");
		}
	}
	
	public void load() throws LoadModelException {
		if (closed)
			throw new RuntimeException("The model has already been closed");
		this.model.loadModel();
	}
	
	public <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	List<Tensor<R>> run(List<Tensor<T>> inputTensors) throws FileNotFoundException, ModelSpecsException, RunModelException, IOException {
		if (closed)
			throw new RuntimeException("The model has already been closed");
		if (!this.model.isLoaded())
			throw new RuntimeException("Please first load the model");
		return model.runBMZ(inputTensors);
	}
	
	public <T extends RealType<T> & NativeType<T>, R extends RealType<R> & NativeType<R>> 
	List<Tensor<R>> runOnTestImages() {
		LinkedHashMap<TensorSpec, String> testInputs = getTestInputs();
		LinkedHashMap<TensorSpec, RandomAccessibleInterval<T>> inputRais = displayTestOutputs(testInputs);
		List<Tensor<T>> inputTensors = createTestTensorList(inputRais);
		return model.runBMZ(inputTensors);
	}
	
	private LinkedHashMap<TensorSpec, String> getTestInputs() {
		LinkedHashMap<TensorSpec, String> testInputs = new LinkedHashMap<TensorSpec, String>();
		for (TensorSpec tt : this.descriptor.getInputTensors()) {
			String sampleFile = tt.getSampleTensorName();
			if (sampleFile == null)
				sampleFile = tt.getTestTensorName();
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

	@Override
	public void close() throws IOException {
		model.close();
		closed = true;
		
	}
}

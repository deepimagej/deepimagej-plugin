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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;

import ai.djl.ndarray.NDArray;
import deepimagej.exceptions.BatchSizeBiggerThanOne;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.CompactMirroring;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import deepimagej.tools.NumFormat;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;

public class RunnerTf implements Callable<HashMap<String, Object>> {

	private HashMap<String,Object> 	inputMap;
	private DeepImageJ				dp;
	private RunnerProgress			rp;
	private Log						log;
	private int						currentPatch = 0;
	private int						totalPatch = 0;
	public String 					error = "";

	public RunnerTf(DeepImageJ dp, RunnerProgress rp,HashMap<String,Object> inputMap, Log log) {
		this.dp = dp;
		this.rp = rp;
		this.log = log;
		this.inputMap = inputMap;
		log.print("constructor runner");
	}

	@Override
	public HashMap<String, Object> call() {
		
		if (rp != null)
			rp.setInfoTag("applyModel");
		if (rp != null && log.getLevel() >= 1) {
			log.print("call runner");
			rp.setVisible(true);
		}


		Parameters params = dp.params;
		// Load the model first
		SavedModelBundle model = dp.getTfModel();
		
		String sigeDefTag = params.developer ? params.graph : DeepLearningModel.returnStringSig(params.graph);
		SignatureDef sig = DeepLearningModel.getSignatureFromGraph(model, DeepLearningModel.returnStringSig(sigeDefTag));
		
		if (log.getLevel() >= 1) {
			log.print("model " + (model == null));
			log.print("sig " + (sig == null));
		}
		
		if (!params.developer) {
			String[] inputs = DeepLearningModel.returnTfInputs(sig);
			for (int i = 0; i < inputs.length; i ++) {
				if (DijTensor.retrieveByName(inputs[i], params.inputList) == null) {
					DijTensor inp = new DijTensor(inputs[i]);
					inp.tensorType = "parameter";
					inp.setInDimensions(DeepLearningModel.modelTfEntryDimensions(sig, inputs[i]));
					params.inputList.add(inp);
				}
			}
		}
		// Map that contains the input tensors that are not images.
		// TODO restrict patching (or not) if the input contains parameters
        HashMap<String, Object> parameterMap = new HashMap<String, Object>(); 
		ImagePlus imp = null;
		// Auxiliary array with the same number of images as output tensors
		int c = 0;
		int inputImageInd = 0;
		for (DijTensor tensor : params.inputList) {
			if (tensor.tensorType.contains("image")) {
				imp = getImageFromMap(inputMap, tensor);
				if (imp == null) {
					// TODO maybe we should allow running models without images
					error = "No image provided.";
					return null;
				}
				String inputPixelSizeX = ((float) imp.getCalibration().pixelWidth) + " " + imp.getCalibration().getUnit();
				String inputPixelSizeY = ((float) imp.getCalibration().pixelHeight) + " " + imp.getCalibration().getUnit();
				String inputPixelSizeZ = ((float) imp.getCalibration().pixelDepth) + " " + imp.getCalibration().getUnit();
				int[] dims = imp.getDimensions();
				params.inputList.get(c).inputTestSize = Integer.toString(dims[0]) + " x " + Integer.toString(dims[1]) + " x " + Integer.toString(dims[2]) + " x " + Integer.toString(dims[3]);;
				params.inputList.get(c).inputPixelSizeX = inputPixelSizeX;
				params.inputList.get(c).inputPixelSizeY = inputPixelSizeY;
				params.inputList.get(c).inputPixelSizeZ = inputPixelSizeZ;
				inputImageInd = c;
			} else if (tensor.tensorType.contains("parameter")){
				Object tensorVal = getTensorFromMap(inputMap, tensor);
				if (tensorVal == null) {
					error = "The input tensor '" + tensor.name + "' should be given by"
							+ "the preprocessing but it is not.";
					IJ.error(error);
					return null;
				} else if (tensorVal instanceof Tensor<?>) {
					parameterMap.put(tensor.name, (Tensor<?>) tensorVal);
				} else if (tensorVal instanceof NDArray) {
					parameterMap.put(tensor.name, (NDArray) tensorVal);
				} else  {
					// TODO improve error message and review what can be a preprocessing output
					error = "Output of the preprocessing should be either a Tensor object"
							+ " or a NDArray object";
					IJ.error(error);
					return null;
				}
			}
			c ++;
		}
		
		int outputImagesCount = 0;
		for (DijTensor tensor : params.outputList) {
			if (tensor.tensorType.contains("image"))
				outputImagesCount ++;
		}
		ImagePlus[] outputImages = new ImagePlus[outputImagesCount];
		List<ResultsTable> outputTables = new ArrayList<ResultsTable>();
		
		if (imp == null) {
			// TODO maybe we should allow running models without images
			error = "No image provided.";
			return null;
		}
		int nx = imp.getWidth();
		int ny = imp.getHeight();
		int nc = imp.getNChannels();
		int nz = imp.getNSlices();
		
		if (log.getLevel() >= 1)
			log.print("image size " + nx + "x" + ny + "x" + nz);
		
		int[] indices = new int[4];
		String[] dimLetters = "XYCZ".split("");
		for  (int i = 0; i < dimLetters.length; i ++)
			indices[i] = Index.indexOf(params.inputList.get(inputImageInd).form.split(""), dimLetters[i]);

		int[] patchSize = {1, 1, 1, 1};
		int[] step = {1, 1, 1, 1};
		int[] minSize = {1, 1, 1, 1};
		for (int i = 0; i < indices.length; i ++) {
			if (indices[i] != -1) {
				patchSize[i] = params.inputList.get(inputImageInd).recommended_patch[indices[i]];
				step[i] = params.inputList.get(inputImageInd).step[indices[i]];
				minSize[i] = params.inputList.get(inputImageInd).minimum_size[indices[i]];
			}
		}
		
		// TODO improve
		if (params.pyramidalNetwork || !params.allowPatching) {
			for (c = 0; c < patchSize.length; c ++) {
				if (step[c] != 0 && patchSize[c] != imp.getDimensions()[c]) {
					patchSize[c] = (int) Math.ceil((double) (imp.getDimensions()[c] - minSize[c]) / step[c]) * step[c] + minSize[c];
				} else if (patchSize[c] < imp.getDimensions()[c] && step[c] == 0) {
					String errorMsg = "This model only accepts images with input size smaller or equal to:";
					for (int i = 0; i < dimLetters.length; i ++) {
						errorMsg += "\n" + dimLetters[i] + " : " + patchSize[i];
					}
					IJ.error(errorMsg);
					return null;
				}
			}
		}
		
		int px = patchSize[0]; int py = patchSize[1]; int pc = patchSize[2]; int pz = patchSize[3]; 
		
		if (!ArrayOperations.isImageSizeAcceptable(new int[] {nx, ny, nc, nz}, patchSize, params.inputList.get(inputImageInd).form)) {
			if (rp != null)
				rp.stop();
			return null;
		}
		
		if (log.getLevel() >= 1)
			log.print("patch size " + "X: " +  px + ", Y: " +  py + ", Z: " +  pz + ", C: " +  pc);
		
		// To define the runtime for config.xml. Starting time
		long startingTime = System.nanoTime();
		// Create the image that is going to be fed to the graph
		ImagePlus[] impatch = new ImagePlus[outputImages.length];
		
		String[] outputTitles = new String[params.outputList.size()];
		// Reset the counter to 0 use it again
		c = 0;
		int extensionInd = imp.getTitle().lastIndexOf('.');
		String imName = extensionInd  == -1 ? imp.getTitle() : imp.getTitle().substring(0, extensionInd);
		for (DijTensor outName: params.outputList) 
			outputTitles[c++] = dp.getName() + "_" + outName.name  + "_" + imName;

		// Order of the dimensions. For example "NHWC"-->Batch size, Height, Width, Channels
		String inputForm = params.inputList.get(inputImageInd).form;
		int[] inputDims = params.inputList.get(inputImageInd).tensor_shape;
		int channelPos = Index.indexOf(inputForm.split(""), "C");
		int[] inDim = imp.getDimensions();
		if (inDim[2] != inputDims[channelPos] && inputDims[channelPos] != -1) {
			error = "The number of channels of the input image is incorrect.\n"
					   + "The models requires " + inputDims[channelPos] + "channels "
				   		+ "but the input image provided has " + inDim[2];
			IJ.error(error);
			return null;
		}
		// Get the padding in case the image needs any
		int[] padding = new int[4];
		if (!params.pyramidalNetwork) {
			padding = findTotalPadding(params.outputList);
		}
		int roiX = px - padding[0] * 2;
		int roiY = py - padding[1] * 2;
		int roiZ = pz - padding[3] * 2;
		int roiC = pc - padding[2] * 2;
		int npx = (int) Math.ceil((double)nx / (double)roiX);
		int npy = (int) Math.ceil((double)ny / (double)roiY);
		int npc = (int) Math.ceil((double)nc / (double)roiC);
		int npz = (int) Math.ceil((double)nz / (double)roiZ);
		if (!params.allowPatching) {
			npx = 1; npy = 1; npz = 1; npc = 1;
		}
		currentPatch = 0;
		totalPatch = npx * npy * npz * npc;

		int[] roi = {roiX, roiY, roiC, roiZ};
		int[] size = {nx, ny, nc, nz};
		int[][] mirrorPixels = ArrayOperations.findAddedPixels(size, padding, roi);
		ImagePlus mirrorImage = CompactMirroring.mirrorXY(imp, mirrorPixels[0][0], mirrorPixels[1][0],
														  	   mirrorPixels[0][1], mirrorPixels[1][1],
														       mirrorPixels[0][3], mirrorPixels[1][3]);
		if (log.getLevel() == 2) {
			mirrorImage.setTitle("Extended image");
			mirrorImage.getProcessor().resetMinAndMax();
			mirrorImage.show();
		}
		
		// If the roi of the patch is bigger than the actual image wanted, consider all the
		// remaining pixels as overlap (padding). Consider that now there might be then different
		// padding for X and Y
		int overlapX = mirrorPixels[0][0];
		if (roiX > nx) {
			roiX = nx;
			padding[0] = (px - nx) / 2;
			overlapX = (px - nx) / 2;
		}
		
		int overlapY = mirrorPixels[0][1];
		if (roiY > ny) {
			roiY = ny;
			padding[1] = (py - ny) / 2;
			overlapY = (py - ny) / 2;
		}
		
		int overlapZ = mirrorPixels[0][3];
		if (roiZ > nz) {
			roiZ = nz;
			padding[3] = (pz - nz) / 2;
			overlapZ = (pz - nz) / 2;
		}

		if (log.getLevel() >= 1)
			log.print("start " + npx + "x" + npy);
		
		for (int i = 0; i < npx; i++) {
			for (int j = 0; j < npy; j++) {
				for (int z = 0; z < npz; z++) {
					// TODO reduce this mega big loop to something more modular
					currentPatch++;
					System.out.println("[DEBUG] (Tensorflow) Patch " + currentPatch + "/" + totalPatch);
					if (log.getLevel() >= 1)
						log.print("currentPatch " + currentPatch);
					if (rp != null && rp.isStopped()) {
						rp.stop();
						return null;
					}
					// Variables to track when the roi starts in the mirror image
					int xMirrorStartPatch;
					int yMirrorStartPatch;
					int zMirrorStartPatch;
					
					// Variables to track when the roi starts in the patch
					int xImageStartPatch;
					int xImageEndPatch;
					int yImageStartPatch;
					int yImageEndPatch;
					int zImageStartPatch;
					int zImageEndPatch;
					int leftoverPixelsX;
					int leftoverPixelsY;
					int leftoverPixelsZ;
					if (i < npx -1 || npx == 1) {
						xMirrorStartPatch = padding[0] + roiX*i;
	
						xImageStartPatch = roiX*i;
						xImageEndPatch = roiX*(i + 1);
						leftoverPixelsX = overlapX;
					} else {
						xMirrorStartPatch = nx + padding[0] - roiX;
	
						xImageStartPatch = roiX*i;
						xImageEndPatch = nx;
						leftoverPixelsX = overlapX + roiX - (xImageEndPatch - xImageStartPatch);
					}
					
					if (j < npy - 1 || npy == 1) {
						yMirrorStartPatch = padding[1] + roiY*j;
	
						yImageStartPatch = roiY*j;
						yImageEndPatch = roiY*(j + 1);
						leftoverPixelsY = overlapY;
					} else {
						yMirrorStartPatch = ny + padding[1] - roiY;
	
						yImageStartPatch = roiY*j;
						yImageEndPatch = ny;
						leftoverPixelsY = overlapY + roiY - (yImageEndPatch - yImageStartPatch);
					}
					
					if (z < npz - 1 || npz == 1) {
						zMirrorStartPatch = padding[3] + roiZ*z;
	
						zImageStartPatch = roiZ*z;
						zImageEndPatch = roiZ*(z + 1);
						leftoverPixelsZ = overlapZ;
					} else {
						zMirrorStartPatch = nz + padding[3] - roiZ;
	
						zImageStartPatch = roiZ*z;
						zImageEndPatch = nz;
						leftoverPixelsZ = overlapZ + roiZ- (zImageEndPatch - zImageStartPatch);
					}
					
					// TODO mirar en profundidad. Que pasa cuando el mirror no es igual de grande que le patch
					// Observé que se compensaba erroneamente
					ImagePlus patch = ArrayOperations.extractPatch(mirrorImage, patchSize, xMirrorStartPatch, yMirrorStartPatch,
																	zMirrorStartPatch, overlapX, overlapY, overlapZ);
					if (log.getLevel() >= 1)
						log.print("Extract Patch (" + (i + 1) + ", " + (j + 1) + ") patch size: " + patch.getWidth() + "x" + patch.getHeight() + " pixels");
					if (log.getLevel() == 2) {
						patch.setTitle("Patch (" + i + "," + j + ")");
						patch.getProcessor().resetMinAndMax();
					}
					
					Tensor<?>[] inputTensors = getInputTensors(params.inputList, parameterMap,  patch, pc);
					Session.Runner sess = model.session().runner();
					
					for (int k = 0; k < params.inputList.size(); k++) {
						// The thread cannot be stopped while loading a model, thus block the button
						// while executing the task
						if (rp != null )
							rp.allowStopping(false);
						sess = sess.feed(opName(sig.getInputsOrThrow(params.inputList.get(k).name)), inputTensors[k]);
						if (rp != null )
							rp.allowStopping(true);
						// Check if the user has tried to stop the execution while loading the model
						// If they have return false and stop
						if (rp != null  && rp.isStopped())
							return null;
					}
					// Reinitialise the counter
					c = 1;
					for (DijTensor outTensor : params.outputList) {
						// The thread cannot be stopped while loading a model, thus block the button
						// while executing the task
						if (rp != null )
							rp.allowStopping(false);
						sess = sess.fetch(opName(sig.getOutputsOrThrow(outTensor.name)));
						if (log.getLevel() >= 1)
							log.print("Session fetch " + (c ++));
						if (rp != null )
							rp.allowStopping(true);
						// Check if the user has tried to stop the execution while loading the model
						// If they have return false and stop
						if(rp != null && rp.isStopped())
							return null;
					}
					try {
						// The thread cannot be stopped while loading a model, thus block the button
						// while executing the task
						if (rp != null )
							rp.allowStopping(false);
						List<Tensor<?>> fetches = sess.run();
						if (rp != null )
							rp.allowStopping(true);
						// Check if the user has tried to stop the execution while loading the model
						// If they have return false and stop
						if (rp != null && rp.isStopped())
							return null;
						// Reinitialise counter
						c = 0;
						int imCounter = 0;
						for (DijTensor outTensor : params.outputList) {
							if (log.getLevel() >= 1)
								log.print("Session run " + (c+1) + "/"  + params.outputList.size());
							Tensor<?> result = fetches.get(c);
							if (outTensor.tensorType.contains("image") && !params.pyramidalNetwork && params.allowPatching) {
								impatch[imCounter] = ImagePlus2Tensor.tensor2ImagePlus(result, outTensor.form, outTensor.name);
								imCounter ++;
								c ++;
							} else if (outTensor.tensorType.contains("image") && (params.pyramidalNetwork  || !params.allowPatching)) {
								outputImages[imCounter] = ImagePlus2Tensor.tensor2ImagePlus(result, outTensor.form, outTensor.name);
								outputImages[imCounter].setTitle(outputTitles[c ++]);
								outputImages[imCounter].show();
								imCounter ++;
							} else if (outTensor.tensorType.contains("list")){
								ResultsTable table = Table2Tensor.tensorToTable(result, outTensor.form, outTensor.name);
								outputTables.add(table);
								table.show(outputTitles[c ++]);
							}
							result.close();
							// TODO put in a method
							// Check if the user has tried to stop the execution while loading the model
							// If they have return false and stop
							if (rp != null && rp.isStopped()) {
								// Close every tensor and stop
								// Close input tensors
								for (int ii = 0; ii < inputTensors.length; ii ++) 
									inputTensors[ii].close();
								for (Tensor<?> oo : fetches)
									oo.close();
								return null;
							}
							
						}
						// Close input tensors
						for (int ii = 0; ii < inputTensors.length; ii ++) {
							inputTensors[ii].close();
						}		
					}				
					catch(IllegalArgumentException ex) {
						ex.printStackTrace();	
						error = "Incorrect input dimensions";
						IJ.log("Error applying the model");
						IJ.log("The dimensions of the input are incorrect.");
						IJ.log("The model might require only specific input sizes.");
						IJ.log("Another of the possible options is that the model has an encoder decoder\n"
								+ "architecture that requires input to be divisible a certain amount of times.");
						IJ.log("Please review the model architecture and the step and patch parameters.");
						return null;
					} catch(BatchSizeBiggerThanOne ex) {
						ex.printStackTrace();	
						error = "Output batch size bigger than 1 for tensor '" + ex.getName() + "'.\n Batch_size > 1 not supported by this version of DeepImageJ";
						IJ.log("Error applying the model");
						IJ.log(error);
						IJ.log(ex.toString());
						return null;
					} catch(IllegalStateException ex) {
						ex.printStackTrace();	
						error = "Missing weights";
						IJ.log("Error applying the model");
						IJ.log("Uninitialized weights.");
						IJ.log("Check that the variables/weights folder contains a correct version of the weights");
						return null;
					}
					catch (Exception ex) {
						// TODO MAKE THIS EXCEPTION MORE ESPECIFIC
						ex.printStackTrace();	
						IJ.log("Error applying the model");
						return null;
					}
					int[][] allOffsets = findOutputOffset(params.outputList);
					int imCounter = 0;
					for (int counter = 0; counter < params.outputList.size(); counter++) {
						if (params.outputList.get(counter).tensorType.contains("image") && !params.pyramidalNetwork && params.allowPatching) {
							float[] outSize = findOutputSize(size, params.outputList.get(counter), params.inputList, impatch[imCounter].getDimensions());
							if (outputImages[imCounter] == null) {
								int[] dims = impatch[imCounter].getDimensions();
								outputImages[imCounter] = IJ.createHyperStack(outputTitles[counter], (int)outSize[0], (int)outSize[1], (int)outSize[2], (int)outSize[3], dims[4], 32);
								outputImages[imCounter].getProcessor().resetMinAndMax();
								outputImages[imCounter].show();
							}
							float scaleX = outSize[0] / nx; float scaleY = outSize[1] / ny; float scaleZ = outSize[3] / nz;
							ArrayOperations.imagePlusReconstructor(outputImages[imCounter], impatch[imCounter], (int) (xImageStartPatch * scaleX),
									(int) (xImageEndPatch * scaleX), (int) (yImageStartPatch * scaleY), (int) (yImageEndPatch * scaleY),
									(int) (zImageStartPatch * scaleZ), (int) (zImageEndPatch * scaleZ),(int)(leftoverPixelsX * scaleX) + allOffsets[imCounter][0],
									(int)(leftoverPixelsY * scaleY) + allOffsets[imCounter][1], (int)(leftoverPixelsZ * scaleZ) + allOffsets[imCounter][3]);
							if (outputImages[imCounter] != null)
								outputImages[imCounter].getProcessor().resetMinAndMax();
							if (rp != null && rp.isStopped()) {
								rp.stop();
								return null;
							}
							imCounter ++;
						} else if (params.outputList.get(counter).tensorType.contains("image") && params.pyramidalNetwork) {
							// TODO improve
							int[] outPatchDims = outputImages[imCounter].getDimensions();
							String[] ijForm = "XYCZB".split("");
							String dijForm = params.outputList.get(counter).form;
							int[] pyramidOut = params.outputList.get(counter).sizeOutputPyramid;
							for (int dd = 0; dd < ijForm.length; dd ++) {
								int idx = dijForm.indexOf(ijForm[dd]);
								if (idx == -1 && outPatchDims[dd] == 1) {
									continue;
								} else if (idx != -1 && outPatchDims[dd] == pyramidOut[idx]) {
									continue;
								}
								IJ.error("The dimensions of the output image do not coincide\n"
										+ "with the dimensions specified previously:\n"
										+ "Specified output dimensions: dimension order -> " + dijForm + ", dimension size -> " + Arrays.toString(pyramidOut) 
										+ "Actual output dimensions: dimension order -> XYCZB, dimension size -> " + Arrays.toString(outPatchDims));
								error = "Error specifying output dimensions.";
								return null;
							}
							if (rp != null && rp.isStopped()) {
								rp.stop();
								return null;
							}
							imCounter ++;
						} else if (params.outputList.get(counter).tensorType.contains("image") && !params.pyramidalNetwork && !params.allowPatching) {
							// TODO improve
							int[] outPatchDims = outputImages[imCounter].getDimensions();
							String[] ijForm = "XYCZB".split("");
							String dijForm = params.outputList.get(counter).form;
							float[] scale = params.outputList.get(counter).scale;
							int[] offset = params.outputList.get(counter).offset;
							// TODO adapt for more inputs
							// We take the mirrored image as the reference, because that is what ends
							// up going into the model
							int[] refSize = mirrorImage.getDimensions();
							String thSizeStr = "[";
							for (int dd = 0; dd < ijForm.length; dd ++) {
								int idx = dijForm.indexOf(ijForm[dd]);
								if (idx == -1 && outPatchDims[dd] == scale[idx]) {
									thSizeStr += scale[idx] + ",";
									continue;
								} else if (idx != -1 && outPatchDims[dd] == (int)(refSize[dd] * scale[idx]) + 2 * offset[idx]) {
									thSizeStr += ((int)(refSize[dd] * scale[idx]) + 2 * offset[idx]) + ",";
									continue;
								}
								for (dd ++; dd < ijForm.length;) {
									idx = dijForm.indexOf(ijForm[dd]);
									if (idx == -1) {
										thSizeStr += scale[idx] + ",";
									} else if (idx != -1) {
										thSizeStr += ((int)(refSize[dd] * scale[idx]) + 2 * offset[idx]) + ",";
									}
								}
								thSizeStr = thSizeStr.substring(0, thSizeStr.length() - 1) + "]";
								IJ.error("The dimensions of the output image do not coincide\n"
										+ "with the dimensions specified previously:\n"
										+ "Specified output dimensions: dimension order -> XYCZB, dimension size -> " + thSizeStr 
										+ "Actual output dimensions: dimension order -> XYCZB, dimension size -> " + Arrays.toString(outPatchDims));
								error = "Error specifying output dimensions.";
								return null;
							}
							if (rp != null && rp.isStopped()) {
								rp.stop();
								return null;
							}
							imCounter ++;
						}
					}
					if (log.getLevel() >= 1)
						log.print("Create Output ");
				}
			}
		}
		
		// To define the runtime. End time
		long endTime = System.nanoTime();
		params.runtime = NumFormat.seconds(endTime - startingTime);
		// Set Parameter params.memoryPeak
		if (rp != null )
			params.memoryPeak = NumFormat.bytes(rp.getPeakmem());
		// Set Parameter  params.outputSize
		HashMap<String, Object> outputMap = new HashMap<String, Object>();
		int imageCount = 0;
		int tableCount = 0;
		c = 0;
		for (DijTensor tensor : params.outputList) {
			if (tensor.tensorType.contains("image")) {
				ImagePlus im = outputImages[imageCount];
				// Add the image to the output map
				outputMap.put(tensor.name, im);
			} else if (tensor.tensorType.contains("list")) {
				// Add the results table to the output map
				outputMap.put(tensor.name, outputTables.get(tableCount ++));
				}
		}
		
		
		return outputMap;
	}
	
	private static ImagePlus getImageFromMap(HashMap<String, Object> inputMap, DijTensor tensor) {
		if (!inputMap.containsKey(tensor.name)){
			IJ.error("Preprocessing should provide a HashMap with\n"
					+ "the key " + tensor.name);
			return null;
		} else if (!(inputMap.get(tensor.name) instanceof ImagePlus)) {
			IJ.error("The input " + tensor.name + " should"
					+ " be an instance of an ImagePlus.");
			return null;
		}
		ImagePlus imp = (ImagePlus) inputMap.get(tensor.name);
		return imp;
	}
	
	private static Object getTensorFromMap(HashMap<String, Object> inputMap, DijTensor tensor){
		if (!inputMap.containsKey(tensor.name)){
			IJ.error("Preprocessing should provide a HashMap with\n"
					+ "the key " + tensor.name);
			return null;
		} else if (!(inputMap.get(tensor.name) instanceof Tensor<?>)) {
			IJ.error("The input " + tensor.name + " should"
					+ " be an instance of a Tensor.");
			return null;
		}
		return inputMap.get(tensor.name);
	}
	
	private static Tensor<?>[] getInputTensors(List<DijTensor> inputTensors, HashMap<String, Object> paramsMap,
												ImagePlus im, int pc){
		Tensor<?>[] tensorsArray = new Tensor<?>[inputTensors.size()];
		int c = 0;
		for (DijTensor tensor : inputTensors) {
			if (tensor.tensorType.contains("parameter") && paramsMap.get(tensor.name) instanceof Tensor<?>) {
				tensorsArray[c ++] = (Tensor<?>) paramsMap.get(tensor.name);
			} else if (tensor.tensorType.contains("parameter") && paramsMap.get(tensor.name) instanceof NDArray) {
				NDArray t = (NDArray) paramsMap.get(tensor.name);
				final float[] out = t.toFloatArray();
				FloatBuffer outBuff = FloatBuffer.wrap(out);
				tensorsArray[c ++] = Tensor.create(t.getShape().getShape(), outBuff);
			} else {
				tensorsArray[c ++] = ImagePlus2Tensor.implus2TensorFloat(im, tensor.form);
			}
		}
		return tensorsArray;
	}
	
	private static float[] findOutputSize(int[] inpSize, DijTensor outTensor, List<DijTensor> inputList, int[] patchSize) {
		String refForOutput = outTensor.referenceImage;
		DijTensor refTensor = DijTensor.retrieveByName(refForOutput, inputList);
		float[] outSize = new float[inpSize.length];
		String[] standarForm = "XYCZ".split("");
		for (int i = 0; i < outSize.length; i ++) {
			int indOut = Index.indexOf(outTensor.form.split(""), standarForm[i]);
			int indInp = Index.indexOf(refTensor.form.split(""), standarForm[i]);
			if (indOut != -1 && indInp != -1) {
				if (standarForm[i].toLowerCase().equals("c"))
					outSize[i] = inpSize[i] * outTensor.scale[indOut] + 2*outTensor.offset[indOut];
				else
					outSize[i] = inpSize[i] * outTensor.scale[indOut];
			} else if (indOut != -1 && indInp == -1) {
				outSize[i] = patchSize[i];
			} else {
				outSize[i] = 1;
			}
		}
		return outSize;
	}

	private String opName(final TensorInfo t) {
		final String n = t.getName();
		if (n.endsWith(":0")) {
			return n.substring(0, n.lastIndexOf(":0"));
		}
		return n;
	}
	
	public static int[] findTotalPadding(List<DijTensor> outputs) {
		// Create an object of int[] that contains the output dimensions
		// of each patch.
		// This dimensions are always of the form [x, y, c, d]
		int[] padding = {0, 0, 0, 0};
		String[] form = "XYCZ".split("");
		for (DijTensor out: outputs) {
			if (!out.tensorType.equals("image"))
				continue;
			for (int i = 0; i < form.length; i ++) {
				int ind = Index.indexOf(out.form.split(""), form[i]);
				if (out.tensorType.contains("image") && ind != -1 && !form[i].equals("B") && !form[i].equals("C")) {
					double totalPad = Math.ceil(-1 * (double)out.offset[ind] / (double)out.scale[ind]) + Math.ceil((double)out.halo[ind] / (double)out.scale[ind]);
					if ((int) totalPad > padding[i]) {
						padding[i] = (int) totalPad;
					}
				}
			}
		}
		return padding;
	}
	
	// TODO clean up method (line 559) Make it stable for pyramidal
	public static int[][] findOutputOffset(List<DijTensor> outputs) {
		// Create an object of int[] that contains the output dimensions
		// of each patch.
		// This dimensions are always of the form [x, y, c, d]
		int[][] offsets = new int[outputs.size()][4];
		String[] form = "XYCZ".split("");
		int c1 = 0;
		for (DijTensor out: outputs) {
			if (!out.tensorType.toLowerCase().equals("image"))
				continue;
			int c2 = 0;
			for (int i = 0; i < offsets[0].length; i ++) {
				int ind = Index.indexOf(out.form.split(""), form[i]);
				if (ind != -1 && out.offset != null) {
					offsets[c1][c2] = out.offset[ind];
				}
				c2 ++;
			}
			c1 ++;
		}
		return offsets;
	}
	
	public int getCurrentPatch() {
		return currentPatch;
	}

	public int getTotalPatch() {
		return totalPatch;
	}

}

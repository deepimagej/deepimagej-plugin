/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we expect you to include adequate citations and acknowledgments whenever you 
 * present or publish results that are based on it.
 * 
 * Reference: DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, L. Donati, M. Unser, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 *
 * Corresponding authors: mamunozb@ing.uc3m.es, daniel.sage@epfl.ch
 *
 */

/*
 * Copyright 2019. Universidad Carlos III, Madrid, Spain and EPFL, Lausanne, Switzerland.
 * 
 * This file is part of DeepImageJ.
 * 
 * DeepImageJ is free software: you can redistribute it and/or modify it under the terms of 
 * the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with DeepImageJ. 
 * If not, see <http://www.gnu.org/licenses/>.
 */

package deepimagej;

import java.util.List;
import java.util.concurrent.Callable;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.framework.SignatureDef;
import org.tensorflow.framework.TensorInfo;

import deepimagej.tools.ArrayOperations;
import deepimagej.tools.CompactMirroring;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Index;
import deepimagej.tools.Log;
import deepimagej.tools.NumFormat;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

public class Runner implements Callable<ImagePlus[]> {

	private ImagePlus		imp;
	private DeepImageJ		dp;
	private RunnerProgress	rp;
	private Log				log;
	private int				currentPatch = 0;
	private int				totalPatch = 0;

	public Runner(DeepImageJ dp, RunnerProgress rp, ImagePlus imp, Log log) {
		this.dp = dp;
		this.rp = rp;
		this.log = log;
		this.imp = imp;
		log.print("constructor runner");
	}

	public Runner(DeepImageJ dp, RunnerProgress rp, Log log) {
		this.dp = dp;
		this.rp = rp;
		this.log = log;
		this.imp = null;
	}

	@Override
	public ImagePlus[] call() {
		log.print("call runner");

		Parameters params = dp.params;
		
		// Auxiliary array with the same number of images as output tensors
		ImagePlus[] outputImages = new ImagePlus[params.outputList.size()];

		// TODO REMOVE String varName = params.inputList.get(0).isImage;
		
		if (imp == null) {
			//TODO remove imp = (ImagePlus) DijVariable.retrieveByName(varName, params.vars).memoryContent;
			imp = WindowManager.getCurrentImage();
		}
		if (imp == null) {
			rp.stop();
			return null;
		}
		if (log.getLevel() >= 1)
			rp.setVisible(true);

		int nx = imp.getWidth();
		int ny = imp.getHeight();
		int nc = imp.getNChannels();
		int nz = imp.getNSlices();
		log.print("image size " + nx + "x" + ny + "x" + nz);
		
		// Now check if the image is an RGB, if it is make it composite,
		// so ImageJ can see the 3 channels of the RGB image
		if (imp.getType() == 4){
			IJ.run(imp, "Make Composite", "");
			//imp = WindowManager.getCurrentImage();
		}
		
		
		int xInd = Index.indexOf(params.inputList.get(0).form.split(""), "W");
		int yInd = Index.indexOf(params.inputList.get(0).form.split(""), "H");
		int cInd = Index.indexOf(params.inputList.get(0).form.split(""), "C");
		int zInd = Index.indexOf(params.inputList.get(0).form.split(""), "D");

		int[] indices = {xInd, yInd, cInd, zInd};
		int[] patchSize = {1, 1, 1, 1};
		for (int i = 0; i < indices.length; i ++) {
			if (indices[i] != -1) {
				patchSize[i] = params.inputList.get(0).recommended_patch[indices[i]];
			}
		}
		
		int px = patchSize[0]; int py = patchSize[1]; int pc = patchSize[2]; int pz = patchSize[3]; 

		if (3 * nx < px || 3 * ny <py || 3 * nz < pz) {
			IJ.log("Error patch size is too large.\n"
					+ "Image Size: X = " + nx + ", Y = " + ny + ", Z = " + nz
					+ "\n Patch Size: X = " + px + ", Y = " + py + ", Z = " + pz);
			rp.stop();
			return null;
		}
		log.print("patch size " + "X: " +  px + ", Y: " +  py + ", Z: " +  pz + ", C: " +  pc);
		params.inputSize = new String[1];
		params.inputSize[0] = Integer.toString(nx) + "x" + Integer.toString(ny) + "x" + Integer.toString(nz);
		
		// To define the runtime for config.xml. Starting time
		long startingTime = System.nanoTime();
		// Create the image that is going to be fed to the graph
		ImagePlus[] impatch = new ImagePlus[params.outputList.size()];
		String in1 = params.inputList.get(0).name;
		
		String[] outputTitles = new String[params.outputList.size()];
		int c = 0;
		for (DijTensor outName: params.outputList) {
			outputTitles[c++] = outName.name  + " " + dp.getName() + " of " + imp.getTitle();
		}
		int[] inputDims = params.inputList.get(0).tensor_shape;

		SavedModelBundle model = dp.getModel();
		log.print("model " + (model == null));
		
		SignatureDef sig = TensorFlowModel.getSignatureFromGraph(model, TensorFlowModel.returnStringSig(dp.params.graph));
		log.print("sig " + (sig == null));

		// Order of the dimensions. For example "NHWC"-->Batch size, Height, Width, Channels
		String inputForm = params.inputList.get(0).form;
		String[] outForms = new String[params.outputList.size()];
		c = 0;
		for (DijTensor tensor: params.outputList) {
			outForms[c++] = tensor.form;
		}

		int channelPos = Index.indexOf(inputForm.split(""), "C");
		int[] inDim = imp.getDimensions();
		if (inDim[2] != inputDims[channelPos] && inputDims[channelPos] != -1) {
			IJ.log("Error in nChannel.\n"
					+ "Image should have " + inputDims[channelPos] 
							+ " instead of " + inDim[2]);
			rp.stop();
			return outputImages;
		}
		int[] padding;
		if (params.final_halo == null) {
			padding = findTotalPadding(params.outputList);
		} else {
			// TODO decide what to do with padding, if it is always fixed
			// this field is not needed anymore
			//padding = params.final_halo;
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
		currentPatch = 0;
		totalPatch = npx * npy * npz * npc;

		int[] roi = {roiX, roiY, roiC, roiZ};
		int[] size = {nx, ny, nc, nz};
		int[][] mirrorPixels = ArrayOperations.findAddedPixels(size, padding, roi);
		ImagePlus mirrorImage = CompactMirroring.mirrorXY(imp, mirrorPixels[0][0], mirrorPixels[1][0],
														  	   mirrorPixels[0][1], mirrorPixels[1][1],
														       mirrorPixels[0][3], mirrorPixels[1][3]);
		if (log.getLevel() == 3) {
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

		String outputName;
		log.print("start " + npx + "x" + npy);
		
		for (int i = 0; i < npx; i++) {
			for (int j = 0; j < npy; j++) {
				for (int z = 0; z < npz; z++) {
					
					currentPatch++;
					log.print("currentPatch " + currentPatch);
					if (rp.isStopped()) {
						rp.stop();
						return outputImages;
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
					
					ImagePlus patch = ArrayOperations.extractPatch(mirrorImage, patchSize, xMirrorStartPatch, yMirrorStartPatch,
																	zMirrorStartPatch, overlapX, overlapY, overlapZ);
					log.print("Extract Patch (" + (i + 1) + ", " + (j + 1) + ") patch size: " + patch.getWidth() + "x" + patch.getHeight() + " pixels");
					if (log.getLevel() == 3) {
						patch.setTitle("Patch (" + i + "," + j + ")");
						patch.getProcessor().resetMinAndMax();
						//patch.show();
					}
					Tensor<?> inputTensor = ImagePlus2Tensor.imPlus2tensor(patch, inputForm, pc);
					Session.Runner sess = model.session().runner();
					sess = sess.feed(opName(sig.getInputsOrThrow(in1)), inputTensor);
					for (int k = 0; k < params.outputList.size(); k++) {
						outputName = params.outputList.get(k).name;
						sess = sess.fetch(opName(sig.getOutputsOrThrow(outputName)));
						log.print("Session fetch " + k);
					}
					try {
						List<Tensor<?>> fetches = sess.run();
						for (int counter = 0; counter < params.outputList.size(); counter++) {
							log.print("Session run " + (counter+1) + "/"  + params.outputList.size());
							Tensor<?> result = fetches.get(counter);
							String outputForm = outForms[counter];
							impatch[counter] = ImagePlus2Tensor.tensor2ImagePlus(result, outputForm);
						}
					}
					catch (Exception ex) {
						IJ.log("Error in the TensorFlow library");
						IJ.log(ex.getMessage());
						rp.stop();
						return outputImages;
					}
					int[][] allOffsets = findOutputOffset(params.outputList);
					for (int counter = 0; counter < params.outputList.size(); counter++) {
						float[] outSize = findOutputSize(size, params.outputList.get(counter));
						if (outputImages[counter] == null) {
							int[] dims = impatch[counter].getDimensions();
							outputImages[counter] = IJ.createHyperStack(outputTitles[counter], (int)outSize[0], (int)outSize[1], (int)outSize[2], (int)outSize[3], dims[4], 32);
							outputImages[counter].getProcessor().resetMinAndMax();
							outputImages[counter].show();
						}
						float scaleX = outSize[0] / nx; float scaleY = outSize[1] / ny; float scaleZ = outSize[3] / nz;
						ArrayOperations.imagePlusReconstructor(outputImages[counter], impatch[counter], (int) (xImageStartPatch * scaleX),
								(int) (xImageEndPatch * scaleX), (int) (yImageStartPatch * scaleY), (int) (yImageEndPatch * scaleY),
								(int) (zImageStartPatch * scaleZ), (int) (zImageEndPatch * scaleZ),(int)(leftoverPixelsX * scaleX) - allOffsets[counter][0],
								(int)(leftoverPixelsY * scaleY) - allOffsets[counter][1], (int)(leftoverPixelsZ * scaleZ) - allOffsets[counter][3]);
						if (outputImages[counter] != null)
							outputImages[counter].getProcessor().resetMinAndMax();
						if (rp.isStopped()) {
							rp.stop();
							return outputImages;
						}
					}
					log.print("Create Output ");
				}
			}
		}
		//modelOutputTensor(outputImages);
		
		// To define the runtime. End time
		long endTime = System.nanoTime();
		params.runtime = NumFormat.seconds(endTime - startingTime);
		// Set Parameter params.memoryPeak
		params.memoryPeak = NumFormat.bytes(rp.getPeakmem());
		// Set Parameter  params.outputSize
		params.outputSize = new String[params.outputList.size()];
		int i = 0;
		for (ImagePlus im : outputImages) {
			int[] dims = im.getDimensions();
			params.outputSize[i++] = Integer.toString(dims[0]) + "x" + Integer.toString(dims[1]) + "x" + Integer.toString(dims[3]);
		}
		rp.stop();
		
		return outputImages;
	}
	
	private static float[] findOutputSize(int[] inpSize, DijTensor outTensor) {
		float[] outSize = new float[inpSize.length];
		String[] standarForm = "WHCD".split("");
		for (int i = 0; i < outSize.length; i ++) {
			int ind = Index.indexOf(outTensor.form.split(""), standarForm[i]);
			if (ind != -1) {
				outSize[i] = inpSize[i] * outTensor.scale[ind];
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
		String[] form = "WHCD".split("");
		for (DijTensor out: outputs) {
			for (int i = 0; i < form.length; i ++) {
				int ind = Index.indexOf(out.form.split(""), form[i]);
				if (ind != -1 && form[i].equals("N") == false) {
					double totalPad = Math.ceil((double)out.offset[ind] / (double)out.scale[ind]) + Math.ceil((double)out.halo[ind] / (double)out.scale[ind]);
					if ((int) totalPad > padding[i]) {
						padding[i] = (int) totalPad;
					}
				}
			}
		}
		return padding;
	}
	
	public static int[][] findOutputOffset(List<DijTensor> outputs) {
		// Create an object of int[] that contains the output dimensions
		// of each patch.
		// This dimensions are always of the form [x, y, c, d]
		int[][] offsets = new int[outputs.size()][4];
		String[] form = "WHCD".split("");
		int c1 = 0;
		for (DijTensor out: outputs) {
			int c2 = 0;
			for (int i = 0; i < offsets[0].length; i ++) {
				int ind = Index.indexOf(out.form.split(""), form[i]);
				if (ind != -1) {
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

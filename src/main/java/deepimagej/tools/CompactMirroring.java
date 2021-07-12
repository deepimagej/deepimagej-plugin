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

package deepimagej.tools;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class CompactMirroring {
	
	public static ImagePlus mirrorXY(ImagePlus imp, 
							   int paddingXLeft, int paddingXRight,
							   int paddingYTop, int paddingYBottom,
							   int paddingZFront, int paddingZBack) {
		int nx = imp.getWidth();
		int ny = imp.getHeight();
		int nc = imp.getNChannels();
		int nz = imp.getNSlices();
		int nt = imp.getNFrames();
		ImagePlus out = IJ.createImage("Mirror", "32-bits", nx + paddingXLeft + paddingXRight,
										ny + paddingYTop + paddingYBottom, nc, nz + paddingZFront + paddingZBack, nt);
		for(int c=0; c<nc; c++) {
			for(int z=0; z<nz; z++) {
				for(int t=0; t<nt; t++) {
					imp.setPositionWithoutUpdate(c + 1, z + 1, t + 1);
					out.setPositionWithoutUpdate(c + 1, z + paddingZFront + 1, t + 1);
					ImageProcessor ip = imp.getProcessor();
					ImageProcessor op = mirrorXY(ip, paddingXLeft, paddingXRight,
							   					 paddingYTop, paddingYBottom);
					out.setProcessor(op);
					if (z < paddingZFront) {
						out.setPositionWithoutUpdate(c + 1, z + 1, t + 1);
						out.setProcessor(mirrorXY(ip, paddingXLeft, paddingXRight,
			   					 paddingYTop, paddingYBottom));
					} else if (z >= nz - paddingZBack) {
						int sliceZBack = 2 * nz - z;
						out.setPositionWithoutUpdate(c + 1, sliceZBack + 1, t + 1);
						out.setProcessor(mirrorXY(ip, paddingXLeft, paddingXRight,
			   					 paddingYTop, paddingYBottom));
					}
				}
			}
		}
		return out;
	}
	
	private static ImageProcessor mirrorXY(ImageProcessor ip,
								   int paddingXLeft, int paddingXRight,
								   int paddingYTop, int paddingYBottom) {
		int nx = ip.getWidth();
		int ny = ip.getHeight();
		FloatProcessor fp = new FloatProcessor(nx + paddingXLeft + paddingXRight,
												ny + paddingYTop + paddingYBottom);
		int periodX = 2*nx - 2;
		int periodY = 2*ny - 2;
		int xm, ym;
		for(int x = -paddingXLeft; x<nx+paddingXRight; x++) {
			xm = mirror(x, nx, periodX);
			for(int y = -paddingYTop; y<ny+paddingYBottom; y++) {
				ym = mirror(y, ny, periodY);
				double v = ip.getPixelValue(xm, ym);
				fp.putPixelValue(x+paddingXLeft, y+paddingYTop, v);
			}
		}
		return fp;
	}
	
	private static int mirror(int a, int n, int period) {
		while (a < 0)
			a += period;
		while (a >= n) {
			a = period - a;
			a = (a < 0 ? -a : a);
		}
		return a;
	}
}

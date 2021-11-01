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

package deepimagej.validation;

import java.text.DecimalFormat;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ImageProcessor;

public class LAP extends AbstractLoss {


	public static void main(String arg[]) {
		ImagePlus ref = IJ.createImage("ref", 32, 200, 202, 32);
		ImagePlus test = IJ.createImage("test", 32, 200, 202, 32);
		ref.setRoi(new Roi(20, 30, 50, 50));
		ref.getProcessor().fill();

	}
	
	@Override
	public String getName() {
		return "LAP";
	}
	@Override
	public ArrayList<Double> compute(ImagePlus reference, ImagePlus test,Constants setting) {
		
		int nxr = reference.getWidth();
		int nyr = reference.getHeight();
		double sigma = 0.5;
		
			
		ArrayList<Double> res = new ArrayList<Double>(); 	
		
	
		int nzr = reference.getStack().getSize();
		int nzt = test.getStack().getSize();
		double s, g,s_l,g_l,sum=0.0;
		
		for (int z=1; z<=Math.max(nzr, nzt); z++) {
			int ir = Math.min(z, nzr);
			int it = Math.min(z, nzt);
			ImageProcessor ipt = test.getStack().getProcessor(it);
			ImageProcessor ipr = reference.getStack().getProcessor(ir);
			
			sum=0.0;
			//int n=0;
			for(int l=0; l<4 ;l++){
				ImageProcessor ipt_l = diffOfGaussian(ipt,sigma*Math.pow(2, l));
				ImageProcessor ipr_l = diffOfGaussian(ipr,sigma*Math.pow(2, l));
				
				//ipr_l.copyBits(ipr, 0, 0, Blitter.SUBTRACT);
				for (int x = 0; x < nxr; x++) {
					for (int y = 0; y < nyr; y++) {
						
						s =  ipr_l.getPixelValue(x, y);
						g = ipt_l.getPixelValue(x, y);

						if (!Double.isNaN(g))
							if (!Double.isNaN(s)) {
								sum+=Math.abs(g-s);
							}
					}
				}
			}
			res.add(sum);	
		}
		
		
		return res ;
	}
	
	public ImageProcessor diffOfGaussian(ImageProcessor im, double sigma) {
		ImageProcessor blur1= im.duplicate();
		ImageProcessor blur2= im.duplicate();
		blur1.blurGaussian(sigma);
		blur2.blurGaussian(sigma*Math.sqrt(2.0));
		blur1.copyBits(blur2, 0, 0, Blitter.SUBTRACT);
		return blur1;
	}

	@Override
	public ArrayList<Double> compose(ArrayList<Double> loss1, double w_1, ArrayList<Double> loss2, double w_2) {
		return null;
	}
	
	@Override
	public Boolean getSegmented() {
		return false;
	}

	@Override
	public String check(ImagePlus reference, ImagePlus test, Constants setting) {
		return "Valid";
	}
}



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
import ij.process.ImageProcessor;

public class SSIM extends AbstractLoss {

	public static void main(String arg[]) {
		ImagePlus ref = IJ.createImage("ref", 32, 200, 202, 32);
		ImagePlus test = IJ.createImage("test", 32, 200, 202, 32);
		ref.setRoi(new Roi(20, 30, 50, 50));
		ref.getProcessor().fill();
		
	}
	
	@Override
	public String getName() {
		return "SSIM";
	}
	@Override
	public ArrayList<Double> compute(ImagePlus reference, ImagePlus test,Constants setting) {
		
		int nxr = reference.getWidth();
		int nyr = reference.getHeight();
		
		double k1=0.01;
		double k2=0.03;
		double c1,c2;
		c1=(k1*255)*(k1*255);
		c2=(k2*255)*(k2*255);
		int L = (int) setting.wd_ssim;
		int pixwin=L*L;
			
		ArrayList<Double> res = new ArrayList<Double>(); 	
		
	
		int nzr = reference.getStack().getSize();
		int nzt = test.getStack().getSize();
		
		for (int z=1; z<=Math.max(nzr, nzt); z++) {
			int ir = Math.min(z, nzr);
			int it = Math.min(z, nzt);
			ImageProcessor ipt = test.getStack().getProcessor(it);
			ImageProcessor ipr = reference.getStack().getProcessor(ir);
			int n=0;
			double ssim=0;
			double maxSignal = -Double.MAX_VALUE;
			for (int x = 0; x < nxr; x++) {
				for (int y = 0; y < nyr; y++) {
					double[][] blockr = new double[L][L];
					ipr.getNeighborhood(x, y,blockr);
					double[][] blockt = new double[L][L];
					ipt.getNeighborhood(x, y,blockr);
					
					double sumx=0.0 , sumy=0.0;
					
					for (int k=0; k<blockr.length; k++)
						for (int l=0; l<blockr.length; l++) {
							sumx+=blockr[k][l];
							sumy+=blockt[k][l];
						}
					sumx/=pixwin;
					sumy/=pixwin;
					
					double sigmax=0.0 , sigmay=0.0, sigmaxy=0.0;
					
					for (int k=0; k<blockr.length; k++)
						for (int l=0; l<blockr.length; l++) {
							sigmax+=(blockr[k][l]-sumx)*(blockr[k][l]-sumx);
							sigmay+=(blockt[k][l]-sumy)*(blockt[k][l]-sumy);
							sigmaxy+=(blockr[k][l]-sumx)*(blockt[k][l]-sumy);
						}
					
					sigmax/=(pixwin-1);
					sigmay/=(pixwin-1);
					sigmaxy/=(pixwin-1);
					
					ssim+=(2*sumx*sumy+c1)*(2*sigmaxy+c2)/(sumx*sumx+sumy*sumy+c1)*(sigmax*sigmax+sigmay*sigmay+c2);
					n++;
				}
			}
			ssim/=n;
			res.add(ssim);
					
		}
		
		
		return res ;
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


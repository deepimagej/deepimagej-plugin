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


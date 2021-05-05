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



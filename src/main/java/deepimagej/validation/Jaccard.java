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

public class Jaccard extends AbstractLoss {


	public static void main(String arg[]) {
		ImagePlus ref = IJ.createImage("ref", 32, 200, 202, 32);
		ImagePlus test = IJ.createImage("test", 32, 200, 202, 32);
		ref.setRoi(new Roi(20, 30, 50, 50));
		ref.getProcessor().fill();
		
	}
	
	@Override
	public String getName() {
		return "Jaccard";
	}
	@Override
	public ArrayList<Double> compute(ImagePlus reference, ImagePlus test,Constants setting) {
		
		int nxr = reference.getWidth();
		int nyr = reference.getHeight();
			
		ArrayList<Double> res = new ArrayList<Double>(); 	
		
	
		int nzr = reference.getStack().getSize();
		int nzt = test.getStack().getSize();
		
		for (int z=1; z<=Math.max(nzr, nzt); z++) {
			int ir = Math.min(z, nzr);
			int it = Math.min(z, nzt);
			ImageProcessor ipt = test.getStack().getProcessor(it);
			ImageProcessor ipr = reference.getStack().getProcessor(ir);
			int difval = (int)ipt.getMax();
			int n=0;
			double s, g, sum = 0.0, jaccard = 0.0, intersection=0.0, smooth=1.0,union=0.0,meanjacc=0.0;
			
			for(int v=0; v < difval ; v++) {
				for (int x = 0; x < nxr; x++) {
					for (int y = 0; y < nyr; y++) {
						
						s =  ipr.getPixelValue(x, y);
						g = ipt.getPixelValue(x, y);
						if( s== (v+1) && g== (v+1)) {
							if (!Double.isNaN(g))
								if (!Double.isNaN(s)) {
									intersection += 1;
									sum += 2;
									
								}
						}
						else if(s== (v+1) || g== (v+1)){
							if (!Double.isNaN(g))
								if (!Double.isNaN(s)) {
									sum += 1;
								}
						}
						
					}
				}
				union=sum-intersection;
				jaccard=(intersection + smooth)/(union + smooth);
				res.add(1-jaccard);
				meanjacc+=1-jaccard;
			}
			meanjacc/=difval;
			res.add(meanjacc);
					
		}
		
		
		return res ;
	}


	@Override
	public ArrayList<Double> compose(ArrayList<Double> loss1, double w_1, ArrayList<Double> loss2, double w_2) {
		return null;
	}

	@Override
	public Boolean getSegmented() {
		return true;
	}

	@Override
	public String check(ImagePlus reference, ImagePlus test, Constants setting) {
		// TODO Auto-generated method stub
		
		//get the max of the first stack of the images
		double min_im1 = MinMax.getminimum(reference.getStack().getProcessor(1));
		double min_im2 = MinMax.getminimum(test.getStack().getProcessor(1));
		
		//get the first stack of the images
		ImageProcessor ipt = reference.getStack().getProcessor(1);
		ImageProcessor ipr = test.getStack().getProcessor(1);
		
		int nxr = reference.getWidth();
		int nyr = test.getHeight();
		
		if((min_im1<0)||(min_im2<0)) {
			
			return "For Jaccard, values must be positive";
		}
		double s,g;
		for (int x = 0; x < nxr; x++) {
			for (int y = 0; y < nyr; y++) {
				
				s =  ipr.getPixelValue(x, y);
				g = ipt.getPixelValue(x, y);
				if ((s%1)!=0.0||(g%1)!=0.0) {
					return "For Jaccard, values must be integer";
				}
			}
		}
		return "Valid";
	}
}



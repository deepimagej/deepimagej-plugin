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

import java.awt.Color;


import java.awt.Font;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import deepimagej.validation.AbstractLoss;
import deepimagej.validation.Bce;
import deepimagej.validation.DiceLoss;
import deepimagej.validation.Jaccard;
import deepimagej.validation.LAP;
import deepimagej.validation.MAE;
import deepimagej.validation.NormL1;
import deepimagej.validation.NormL2;
import deepimagej.validation.PSNR;
import deepimagej.validation.RMSE;
import deepimagej.validation.RegressSNR;
import deepimagej.validation.SNR;
import deepimagej.validation.SSIM;
import deepimagej.validation.Constants;

public class Settings implements PlugInFilter {
	
	private GenericDialog gd ;
	
	public Constants run(String arg) {
		
		//import function that can be used for composed function
		ArrayList<AbstractLoss> functions = new ArrayList<AbstractLoss>();
		functions.add(new NormL1());
		functions.add(new NormL2());
		functions.add(new Bce());
		functions.add(new RMSE());
		functions.add(new MAE());
		functions.add(new SNR());
		functions.add(new PSNR());
		functions.add(new DiceLoss());
		functions.add(new Jaccard());
		functions.add(new SSIM());
		functions.add(new RegressSNR());
		functions.add(new LAP());
		
		String name[] = new String[functions.size()];
		
		int j=0;
		for(AbstractLoss function : functions) {
			name[j]=function.getName();
			j++;
		}
		
		// create the box of dialog
		gd = new GenericDialog("Settings");
		//IJ.log("Hello"); 
		Font Title = new Font("TimesRoman", Font.PLAIN, 20);
		//value that the users can change for the different settings for the functions
		gd.addMessage("LAP", Title, Color.black);
		gd.addNumericField("Starting Sigma:", 1, 0);
		gd.addMessage("SSIM", Title, Color.black);
		gd.addNumericField("Window Size:", 1, 0);
		gd.addMessage("Composed Function", Title, Color.black);
		gd.addChoice("First loss of Composed Function:", name, name[0]);
		gd.addNumericField("Coefficient:", 0, 2);
		gd.addChoice("Second loss of Composed Function:", name, name[0]);
		gd.addNumericField("Coefficient:", 0, 2);
		
		gd.showDialog();
		
		//get the values written by the user
		Constants setting = new Constants();
		setting.sig_lap=gd.getNextNumber();
		setting.wd_ssim=gd.getNextNumber();
		setting.w1_composed=gd.getNextNumber();
		setting.w2_composed=gd.getNextNumber();
		int index1 = gd.getNextChoiceIndex();
		setting.title1 = name[index1];
		int index2 = gd.getNextChoiceIndex();
		setting.title2 = name[index2];
		
		return setting;
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		IJ.log("setup");
		return 0;
	}

	@Override
	public void run(ImageProcessor ip) {
		// TODO Auto-generated method stub
		IJ.log("run");
	}

}

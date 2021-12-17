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

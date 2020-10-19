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

import java.io.File;
import java.io.IOException;

import deepimagej.BuildDialog;
import deepimagej.TensorFlowModel;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class DeepImageJ_Build_BundledModel implements PlugIn {

	public void run(String arg) {
		// If there is no models directory inside Fiji folder, create it
		String path = IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
		
		if (!(new File(path).isDirectory()))
				new File(path).mkdirs();

		BuildDialog bd = new BuildDialog();
		bd.showDialog();
	}

	public static void main(String args[]) throws IOException {
		ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models\\exemplary-image-data\\tribolium.tif");
		if (imp != null)
			imp.show();
		new ImageJ();
		BuildDialog bd = new BuildDialog();
		bd.showDialog();/*
		if (WindowManager.getCurrentImage() == null) {
			IJ.error("There should be an image open.");
		} else {
			BuildDialog bd = new BuildDialog();
			bd.showDialog();
		}*/
	}
}
import java.io.File;
import java.io.IOException;

import deepimagej.RunDialog;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

public class DeepImageJ_Run_Swing implements PlugIn {
	
	public void run(String arg) {
  		String path = IJ.getDirectory("imagej") + File.separator + "models" + File.separator;
		if (!(new File(path).isDirectory()))
			new File(path).mkdirs();
		RunDialog dlg = new RunDialog();
		Boolean bool = dlg.initialParameters();
		if (bool == true) {
			dlg.showDialog("");
		}
	}

	public static void main(String args[]) throws IOException {
		ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\Videos\\Fiji.app\\models\\exemplary-image-data\\tribolium.tif");
		if (imp != null)
			imp.show();
		new ImageJ();
		new DeepImageJ_Run_Swing().run("");
	}
	
}

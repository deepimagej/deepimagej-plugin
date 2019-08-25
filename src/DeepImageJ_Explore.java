import java.io.File;

import javax.swing.JDialog;

import deepimagej.ExploreDialog;
import ij.IJ;
import ij.plugin.PlugIn;

public class DeepImageJ_Explore extends JDialog implements PlugIn {

	public static void main(String arg[]) {
		new ExploreDialog(
				System.getProperty("user.home") + File.separator + "Google Drive" + File.separator + "ImageJ" + File.separator + "models" + File.separator);
	}

	@Override
	public void run(String arg) {
		new ExploreDialog(IJ.getDirectory("imagej") + File.separator + "models" + File.separator);
	}

}

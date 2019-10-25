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

package deepimagej.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {

	static public String getFolderSizeKb(String dir) {
		return String.format("%3.2f Mb", (getFolderSize(dir) / (1024 * 1024.0)));
	}

	static public long getFolderSize(String dir) {
		File folder = new File(dir);
		long length = 0;
		File[] files = folder.listFiles();

		if (files == null)
			return 0;
		int count = files.length;
		for (int i = 0; i < count; i++) {
			if (files[i].isFile())
				length += files[i].length();
			else
				length += getFolderSize(files[i].getAbsolutePath());
		}

		return length;
	}

	// TODO Remove this method (substituted by Daniel method). Now the model has to
	// be
	// directly inside the folder
	public static String folderWithModel(String ogFolder) {
		// Use this method to extract the folder that directly contains the
		// protobuffer file and the weights
		int targetFiles = 2;
		String architecture = "saved_model.pb";
		String weights = "variables";
		String modelPath = null;
		File folder = new File(ogFolder);
		File[] file_fileList = folder.listFiles();
		int nFiles = file_fileList.length;
		int i = 0;
		while (i < nFiles && targetFiles != 0) {
			String file_path = file_fileList[i].getAbsolutePath();
			if (file_path.contains(architecture) == true && file_fileList[i].isFile() == true) {
				targetFiles--;
			}
			if (file_fileList[i].isFile() == false && file_path.contains(weights) == true) {
				targetFiles--;
			}
			i++;
		}
		if (targetFiles == 0) {
			modelPath = ogFolder;
		}
		else {
			int i2 = 0;
			while (modelPath == null && i2 < nFiles) {
				if (file_fileList[i2].isDirectory() == true) {
					String subfolder_path = file_fileList[i2].getAbsolutePath();
					modelPath = folderWithModel(subfolder_path);
				}
				i2++;
			}
		}
		return modelPath;
	}

	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdir();
		}
		if (!sourceFile.exists()) {
			return;
		}
		// If we are trying to copy a file into itself, do nothing
		//i.e, the sorce file and the destination are the same
		if (sourceFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
			return;
		}
		
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		FileChannel source = null;
		FileChannel destination = null;
		source = new FileInputStream(sourceFile).getChannel();
		destination = new FileOutputStream(destFile).getChannel();
		if (destination != null && source != null) {
			destination.transferFrom(source, 0, source.size());
		}
		if (source != null) {
			source.close();
		}
		if (destination != null) {
			destination.close();
		}

	}
}

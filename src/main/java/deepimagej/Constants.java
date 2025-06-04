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

package deepimagej;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import io.bioimage.modelrunner.system.PlatformDetection;


public class Constants {

	public static String url = "https://deepimagej.github.io/deepimagej/";
	public static String DIJ_VERSION = getVersion();
	public static String DIJ_NAME = "deepImageJ";
	
	public static final String FIJI_FOLDER;
	static {
		FIJI_FOLDER = getFijiFolder();
		System.err.println("Fiji folder: " + FIJI_FOLDER);
	}
	
    private static String getVersion() {
        try (InputStream input = Constants.class.getResourceAsStream("/.deepimagej_properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("version");
        } catch (Exception | Error ex) {
            return "unknown";
        }
    }
    
	private static String getFijiFolder() {
		File jvmFolder = new File(System.getProperty("java.home"));
		String imageJExecutable;
		if (PlatformDetection.isWindows())
			imageJExecutable = "fiji-windows-x64.exe";
		else if (PlatformDetection.isLinux())
			imageJExecutable = "fiji-linux-x64";
		else if (PlatformDetection.isMacOS() && PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64))
			imageJExecutable = "Fiji.App/Contents/MacOS/fiji-macos-arm64";
		else if (PlatformDetection.isMacOS())
			imageJExecutable = "Fiji.App/Contents/MacOS/fiji-macos-x64";
		else
			throw new IllegalArgumentException("Unsupported Operating System");
		while (true && jvmFolder != null) {
			jvmFolder = jvmFolder.getParentFile();
			if (new File(jvmFolder + File.separator + imageJExecutable).isFile())
				return jvmFolder.getAbsolutePath();
		}
		return getImageJFolder();
	}
    
	private static String getImageJFolder() {
		File jvmFolder = new File(System.getProperty("java.home"));
		String imageJExecutable;
		if (PlatformDetection.isWindows())
			imageJExecutable = "ImageJ-win64.exe";
		else if (PlatformDetection.isLinux())
			imageJExecutable = "ImageJ-linux64";
		else if (PlatformDetection.isMacOS())
			imageJExecutable = "Contents/MacOS/ImageJ-macosx";
		else
			throw new IllegalArgumentException("Unsupported Operating System");
		while (true && jvmFolder != null) {
			jvmFolder = jvmFolder.getParentFile();
			if (new File(jvmFolder + File.separator + imageJExecutable).isFile())
				return jvmFolder.getAbsolutePath();
		}
		return new File("").getAbsolutePath();
		// TODO remove throw new RuntimeException("Unable to find the path to the ImageJ/Fiji being used.");
	}
    
}

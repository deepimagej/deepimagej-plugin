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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileTools {

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
	    Path originalPath = sourceFile.toPath();
		Path copied = destFile.toPath();
	    Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);

	}
	
	public static void copyFolderFiles(File sourceFolder, File destFolder) throws IOException {
		// Copy only the files from a folder to the other one
		if (sourceFolder.isDirectory() == false) {
			return;
		}
		if (destFolder.isDirectory() == false) {
			return;
		}
		File[] listOfFiles = sourceFolder.listFiles();
		for (File file : listOfFiles) {
			if (file.isFile() == true) {
				String sourceFile = file.getAbsolutePath();
				String fileName = file.getName();
				String destFile = destFolder.getAbsolutePath() + File.separator + fileName;
				copyFile(new File(sourceFile), new File(destFile));
			}
		}
	}
	
	public static void unzipJar(String destinationDir, String jarPath) throws IOException {
		File file = new File(jarPath);
		JarFile jar = new JarFile(file);
 
		// fist get all directories,
		// then make those directory on the destination Path
		for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
			JarEntry entry = (JarEntry) enums.nextElement();
 
			String fileName = destinationDir + File.separator + entry.getName();
			File f = new File(fileName);
 
			if (fileName.endsWith("/")) {
				f.mkdirs();
			}
 
		}
 
		//now create all files
		for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
			JarEntry entry = (JarEntry) enums.nextElement();
 
			String fileName = destinationDir + File.separator + entry.getName();
			File f = new File(fileName);
 
			if (!fileName.endsWith("/")) {
				InputStream src = jar.getInputStream(entry);
				FileOutputStream dst = new FileOutputStream(f);
				copy(src, dst);
			}
		}
		jar.close();
	}
	
	private static long copy(InputStream src, FileOutputStream dst) throws IOException {
	    try {
	      byte[] buffer = new byte[1 << 20]; // 1MB
	      long ret = 0;
	      int n = 0;
	      while ((n = src.read(buffer)) >= 0) {
	        dst.write(buffer, 0, n);
	        ret += n;
	      }
	      return ret;
	    } finally {
	      dst.close();
	      src.close();
	    }
	  }
	
	public static boolean deleteDir(File element) {
	    if (element.isDirectory()) {
	        for (File sub : element.listFiles()) {
	        	boolean deleted = deleteDir(sub);
	        	if (!deleted) {
	        		return deleted;
	        	}
	        }
	    }
	    boolean deleted = element.delete();

	    return deleted;
	}
	
	public static void zipFolder(File srcFolder, File destZipFile) throws Exception {
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
                ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
            addFileToZip("", srcFolder, zip);
            //fileWriter.close();
        }
    }
	
    private static void addFileToZip(String rootPath, File srcFile, ZipOutputStream zip) throws Exception {

        if (srcFile.isDirectory()) {
            addFolderToZip(srcFile.getName(), srcFile, zip);
        } else {
            byte[] buf = new byte[4096];
            int len;
            try (FileInputStream in = new FileInputStream(srcFile)) {
                String name = srcFile.getName();
                name = rootPath + File.separator + name;
                zip.putNextEntry(new ZipEntry(name));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
                in.close();
            }
        }
    }

    private static void addFolderToZip(String rootPath, File srcFolder, ZipOutputStream zip) throws Exception {
        for (File fileName : srcFolder.listFiles()) {
            addFileToZip(rootPath, fileName, zip);
        }
    }
	
    /*
     * Unzip zip file 'source' into a file in the path 'outPath'
     */
    public static void unzipFolder(File source, String outPath) throws IOException, InterruptedException {
 	    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source))) {

 	        ZipEntry entry = zis.getNextEntry();

 	        while (entry != null) {

 	            File file = new File(outPath, entry.getName());
 	            

 	            // Check if entry is directory (if the entry name ends with '\' or '/'
 	            if (entry.isDirectory()) {
 	                file.mkdirs();
 	            } else {
 	                File parent = file.getParentFile();

 	                if (!parent.exists()) {
 	                    parent.mkdirs();
 	                }

 	                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {

 	                    int bufferSize = Math.toIntExact(entry.getSize());
 	                    byte[] buffer = new byte[bufferSize > 0 ? bufferSize : 4096];
 	                    int location;
 	                    
 	                    while ((location = zis.read(buffer)) != -1 && !Thread.interrupted()) {
 	                        bos.write(buffer, 0, location);
 	                    }
 	                    
 	                    // If the exit of the while loo has been due to a
 	                    // thread interruption, throw exception
 	     	           if ((location = zis.read(buffer)) != -1) {
 	     	        	    throw new InterruptedException();
 	     	        	}
 	                }
 	            }
 	            entry = zis.getNextEntry();
 	        }
 	    }
 	}
    
    /**
     * Compresses a list of files to a destination zip file
     * @param listFiles A collection of files and directories
     * @param destZipFile The path of the destination zip file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void zip(List<File> listFiles, String destZipFile) throws FileNotFoundException,
            IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZipFile));
        for (File file : listFiles) {
            if (file.isDirectory()) {
                zipDirectory(file, file.getName(), zos);
            } else {
                zipFile(file, zos);
            }
        }
        zos.flush();
        zos.close();
    }
    /**
     * Compresses files represented in an array of paths
     * @param files a String array containing file paths
     * @param destZipFile The path of the destination zip file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void zip(String[] files, String destZipFile) throws FileNotFoundException, IOException {
        List<File> listFiles = new ArrayList<File>();
        for (int i = 0; i < files.length; i++) {
            listFiles.add(new File(files[i]));
        }
        zip(listFiles, destZipFile);
    }
    /**
     * Adds a directory to the current zip output stream
     * @param folder the directory to be  added
     * @param parentFolder the path of parent directory
     * @param zos the current zip output stream
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void zipDirectory(File folder, String parentFolder,
            ZipOutputStream zos) throws FileNotFoundException, IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(file));
            long bytesRead = 0;
            byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = bis.read(bytesIn)) != -1) {
                zos.write(bytesIn, 0, read);
                bytesRead += read;
            }
            zos.closeEntry();
            bis.close();
        }
    }
    /**
     * Adds a file to the current zip output stream
     * @param file the file to be added
     * @param zos the current zip output stream
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void zipFile(File file, ZipOutputStream zos)
            throws FileNotFoundException, IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                file));
        long bytesRead = 0;
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = bis.read(bytesIn)) != -1) {
            zos.write(bytesIn, 0, read);
            bytesRead += read;
        }
        zos.closeEntry();
        bis.close();
    }
    
    public static String createSHA256(String fileName) throws  IOException {
        byte[] buffer= new byte[8192];
        int count;
        MessageDigest digest;
        String sha256 = "error";
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return "";
		}
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
        while ((count = bis.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }
        bis.close();

        byte[] hash = digest.digest();
        sha256 = bytesToHex(hash);
        return sha256;
    }
    
	private static String bytesToHex(byte[] hash) {
	    StringBuffer hexString = new StringBuffer();
	    for (int i = 0; i < hash.length; i++) {
	    String hex = Integer.toHexString(0xff & hash[i]);
	    if(hex.length() == 1) hexString.append('0');
	        hexString.append(hex);
	    }
	    return hexString.toString();
	}
}

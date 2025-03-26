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

package deepimagej.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class FileTools {

	static public String getFolderSizeKb(String dir) {
		return String.format("%3.2f Mb", (getFolderSize(dir) / (1024 * 1024.0)));
	}

	static public long getFolderSize(String dir) {
		File folder = new File(dir);
		if (!folder.exists())
			return 0;
		if (folder.isFile())
			return folder.length();
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

	/**
	 * @deprecated
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
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
				copyFile(sourceFile, destFile);
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
			Thread thread = Thread.currentThread();
			if (!fileName.endsWith("/")) {
				InputStream src = jar.getInputStream(entry);
				FileOutputStream dst = new FileOutputStream(f);
				copy(src, dst, thread);
			}
		}
		jar.close();
	}
	
    public static void copyFile(String sourcePath, String destPath) throws IOException {
    	copyFile(sourcePath, destPath, Thread.currentThread());
    }
	
    public static void copyFile(String sourcePath, String destPath, Thread parentThread) throws IOException {
        try (FileInputStream src = new FileInputStream(sourcePath);
             FileOutputStream dst = new FileOutputStream(destPath)) {
            copy(src, dst, parentThread);
        }
    }
	
    private static long copy(InputStream src, FileOutputStream dst, Thread parentThread) throws IOException {
        try {
            byte[] buffer = new byte[1 << 20]; // 1MB
            long ret = 0;
            int n = 0;
            while ((n = src.read(buffer)) >= 0 && parentThread.isAlive()) {
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
    
    public static boolean unzipFolder(File source, String outPath) throws IOException, InterruptedException {
    	return unzipFolder(source, outPath, null);
    }
    
    public static boolean unzipFolder(File source, String outPath, Thread parentThread) throws IOException, InterruptedException {
        if (parentThread == null) {
            parentThread = Thread.currentThread();
        }

        // REMOVED hardcoded path: Use the 'source' parameter passed to the method
        // source = new File("/home/carlos/git/deepimagej-plugin/models/embryo-stardist-cat.zip");

        // Use ZipFile which reads the central directory and might be more lenient
        // Use try-with-resources to ensure the ZipFile is closed automatically
        try (ZipFile zipFile = new ZipFile(source)) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                // Check for interruption before processing the next entry
                if (!parentThread.isAlive()) {
                    throw new InterruptedException("Unzipping interrupted by parent thread.");
                }

                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(outPath, entry.getName());

                // Create parent directories if they don't exist
                if (!entry.isDirectory()) {
                    File parentDir = entryDestination.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        if (!parentDir.mkdirs()) {
                            throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                        }
                    }
                }

                if (entry.isDirectory()) {
                    // Create directory entry
                    if (!entryDestination.exists()) {
                        if (!entryDestination.mkdirs()) {
                           throw new IOException("Failed to create directory: " + entryDestination.getAbsolutePath());
                        }
                    }
                } else {
                    // Extract file entry
                    // Use try-with-resources for the streams
                    boolean interrupted = false;
                    try (InputStream in = zipFile.getInputStream(entry);
                         BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(entryDestination))) {

                        // Use a fixed-size buffer instead of entry.getSize(), which can be unreliable (-1)
                        // or too large causing OutOfMemoryError. 8KB is common.
                        byte[] buffer = new byte[8192];
                        int bytesRead;

                        while ((bytesRead = in.read(buffer)) != -1) {
                            // Check for interruption within the read loop
                            if (!parentThread.isAlive()) {
                                interrupted = true;
                                break;
                            }
                            bos.write(buffer, 0, bytesRead);
                        }
                        bos.flush(); // Ensure buffer is flushed before closing

                    } catch (IOException e) {
                        // Handle potential errors reading a specific entry or writing its file
                        System.err.println("Error processing entry " + entry.getName() + ": " + e.getMessage());
                        // Clean up potentially partially written file
                        entryDestination.delete();
                        // Depending on requirements, you might want to continue with the next entry
                        // or stop entirely. Here, we stop by returning false.
                        return false;
                    }

                    // If the inner loop was exited due to interruption, throw the exception
                    if (interrupted) {
                         // Clean up partially written file
                         entryDestination.delete();
                        throw new InterruptedException("Unzipping interrupted by parent thread while processing entry: " + entry.getName());
                    }
                }
            } // end while loop over entries

            // Final check after processing all entries
            if (!parentThread.isAlive()) {
                 throw new InterruptedException("Unzipping interrupted by parent thread after processing entries.");
             }

        } catch (ZipException e) {
            // Catch ZipException specifically (might still occur if ZipFile detects other corruption)
            System.err.println("Error opening or reading ZIP file (ZipException): " + source.getAbsolutePath() + " - " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
            return false; // Indicate failure
        } catch (IOException e) {
            // Catch other IOExceptions (e.g., file not found, permissions)
             System.err.println("IO error during unzip: " + e.getMessage());
            throw e; // Re-throw other IOExceptions as they might indicate different problems
        }

        return true; // If we reached here without exceptions or returning false, it was successful
    }
	
    /*
     * Unzip zip file 'source' into a file in the path 'outPath'
     */
    public static boolean unzipFolder2(File source, String outPath, Thread parentThread) throws IOException, InterruptedException {
 	    if (parentThread == null)
 	    	parentThread = Thread.currentThread();
 	    source = new File("/home/carlos/git/deepimagej-plugin/models/embryo-stardist-cat.zip");
    	FileInputStream fis = new FileInputStream(source);
    	ZipInputStream zis = new ZipInputStream(fis);

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
                    
                    while ((location = zis.read(buffer)) != -1 && parentThread.isAlive()) {
                        bos.write(buffer, 0, location);
                    }
                    
                    // If the exit of the while loo has been due to a
                    // thread interruption, throw exception
     	           if ((location = zis.read(buffer)) != -1) {
     	        	    throw new InterruptedException();
     	        	}
     	           bos.close();
                } catch (ZipException e) {
    				e.printStackTrace();
    		 	    zis.close();
    		 	    fis.close();
    				return false;
    			}
            }
            entry = zis.getNextEntry();
        }
 	    zis.close();
 	    fis.close();
 	    return true;
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
            byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = bis.read(bytesIn)) != -1) {
                zos.write(bytesIn, 0, read);
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
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = bis.read(bytesIn)) != -1) {
            zos.write(bytesIn, 0, read);
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

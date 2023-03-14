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
package deepimagej.modelrunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import deepimagej.InstallerDialog;
import deepimagej.tools.ModelDownloader;
import io.bioimage.modelrunner.engine.EngineInfo;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.versionmanagement.AvailableDeepLearningVersions;
import io.bioimage.modelrunner.versionmanagement.DeepLearningVersion;

/**
 * Class that manages the dl-modelrunner engines.
 * This class checks that the required engines are installed and installs them if they are not.
 * There is one required engine per DL framework. It can be either the latest one or the one specified
 * in the variable {@link EngineManagement#ENGINES_VERSIONS}.
 * This class also contains the methods to install engines on demand.
 * @author Carlos Garcia Lopez de Haro
 *
 */
public class EngineManagement {
	/**
	 * Directory where the engines shold be installed
	 */
	private static final String ENGINES_DIR = new File("engines").getAbsolutePath();
	/**
	 * Keyword used to identify the engine being installed
	 */
	public static final String PROGRESS_ENGINE_KEYWORD = "Engine: ";
	/**
	 * Keyword used to identify the JAR file being downloaded
	 */
	public static final String PROGRESS_JAR_KEYWORD = "JAR: ";
	/**
	 * Keyword used to identify the size of the JAR file being downloaded
	 */
	public static final String PROGRESS_SIZE_KEYWORD = "Size: ";
	/**
	 * Keyword used to signal that the installation of the engines has finished
	 */
	public static final String PROGRESS_DONE_KEYWORD = "DONE";
	/**
	 * Map containing which version should always be installed per framework
	 */
	private static final HashMap<String, String> ENGINES_VERSIONS = new HashMap<String, String>();
	
	static {
		ENGINES_VERSIONS.put(EngineInfo.getTensorflowKey() + "_2", "2.7.1");
		ENGINES_VERSIONS.put(EngineInfo.getTensorflowKey() + "_1", "1.15.0");
		ENGINES_VERSIONS.put(EngineInfo.getOnnxKey() + "_1", "1.11.0");
		ENGINES_VERSIONS.put(EngineInfo.getPytorchKey() + "_1", "1.13.0");
	}
	/**
	 * Key word that substitutes the OS part of the engine folder name in some
	 * of the engines installed from the update site
	 * In order to reduce repetition and to reduce the number of deps downloaded
	 * by the user when deepImageJ is installed, a selection of engines is created.
	 * There are some of the engines that are the same for every operating system,
	 * for example TF1 and Pytorch.
	 * To reduce the number of deps donwloaded, the TF1 and Pytorch engines are
	 * installed as for example:
	 *  - "tensorflow-1.15.0-1.15.0" + {@link #GENERAL_KEYWORD}
	 * Just one of the above folder is downloaded for all the OS.
	 * If it was not done like this, as the Fiji update site does not recognise the
	 * OS of the user, the three following engines would be required:
	 *  - "tensorflow-1.15.0-1.15.0-windows-x86_64-cpu-gpu"
	 *  - "tensorflow-1.15.0-1.15.0-linux-x86_64-cpu-gpu"
	 *  - "tensorflow-1.15.0-1.15.0-macos-x86_64-cpu"
	 * however, the 3 engines would contain exactly the same deps.
	 * This is the reason why we make the workaround to download a single
	 * engine for certain engines and then rename it follwoing the corresponding
	 * convention
	 * 
	 * Regard, that for certain engines, downloading all the OS depending engines
	 * is necessary, as the dependencies vary from one system to another. 
	 */
	private static final String GENERAL_KEYWORD = "-general";
	/**
	 * Whether the minimum required engines are installed or not
	 */
	private boolean everythingInstalled = false;
	/**
	 * Which of the required engines are not installed
	 */
	private Map<String, String> missingEngineFolders;
	/**
	 * String that communicates the progress made downloading engines
	 */
	private String progressString;
	
	/**
	 * Constructor that checks whether the minimum engines are installed
	 * or not.
	 * In order to reduce repetition and to reduce the number of deps downloaded
	 * by the user when deepImageJ is installed, a selection of engines is created.
	 * There are some of the engines that are the same for every operating system,
	 * for example TF1 and Pytorch.
	 * To reduce the number of deps donwloaded, the TF1 and Pytorch engines are
	 * installed as for example:
	 *  - "tensorflow-1.15.0-1.15.0" + {@link #GENERAL_KEYWORD}
	 * Just one of the above folder is downloaded for all the OS.
	 * If it was not done like this, as the Fiji update site does not recognise the
	 * OS of the user, the three following engines would be required:
	 *  - "tensorflow-1.15.0-1.15.0-windows-x86_64-cpu-gpu"
	 *  - "tensorflow-1.15.0-1.15.0-linux-x86_64-cpu-gpu"
	 *  - "tensorflow-1.15.0-1.15.0-macos-x86_64-cpu"
	 * however, the 3 engines would contain exactly the same deps.
	 * This is the reason why we make the workaround to download a single
	 * engine for certain engines and then rename it follwoing the corresponding
	 * convention
	 * 
	 * Regard, that for certain engines, downloading all the OS depending engines
	 * is necessary, as the dependencies vary from one system to another. 
	 */
	private EngineManagement() {
	}
	
	/**
	 * Creates an {@link EngineManagement} object to check if the required engines are installed.
	 * 
	 * In order to reduce repetition and to reduce the number of deps downloaded
	 * by the user when deepImageJ is installed, a selection of engines is created.
	 * There are some of the engines that are the same for every operating system,
	 * for example TF1 and Pytorch.
	 * To reduce the number of deps donwloaded, the TF1 and Pytorch engines are
	 * installed as for example:
	 *  - "tensorflow-1.15.0-1.15.0" + {@link #GENERAL_KEYWORD}
	 * Just one of the above folder is downloaded for all the OS.
	 * If it was not done like this, as the Fiji update site does not recognise the
	 * OS of the user, the three following engines would be required:
	 *  - "tensorflow-1.15.0-1.15.0-windows-x86_64-cpu-gpu"
	 *  - "tensorflow-1.15.0-1.15.0-linux-x86_64-cpu-gpu"
	 *  - "tensorflow-1.15.0-1.15.0-macos-x86_64-cpu"
	 * however, the 3 engines would contain exactly the same deps.
	 * This is the reason why we make the workaround to download a single
	 * engine for certain engines and then rename it follwoing the corresponding
	 * convention
	 * 
	 * Regard, that for certain engines, downloading all the OS depending engines
	 * is necessary, as the dependencies vary from one system to another. 
	 * @return
	 */
	public static EngineManagement createManager() {
		return new EngineManagement();
	}
	
	/**
	 * Checks if the minimal required engines to execute the majority of models
	 * are installed, if not it manages the installation of the missing ones.
	 *  
	 * In order to reduce repetition and to reduce the number of deps downloaded
	 * by the user when deepImageJ is installed, a selection of engines is created.
	 * There are some of the engines that are the same for every operating system,
	 * for example TF1 and Pytorch.
	 * To reduce the number of deps donwloaded, the TF1 and Pytorch engines are
	 * installed as for example:
	 *  - "tensorflow-1.15.0-1.15.0" + {@link #GENERAL_KEYWORD}
	 * Just one of the above folder is downloaded for all the OS.
	 * If it was not done like this, as the Fiji update site does not recognise the
	 * OS of the user, the three following engines would be required:
	 *  - "tensorflow-1.15.0-1.15.0-windows-x86_64-cpu-gpu"
	 *  - "tensorflow-1.15.0-1.15.0-linux-x86_64-cpu-gpu"
	 *  - "tensorflow-1.15.0-1.15.0-macos-x86_64-cpu"
	 * however, the 3 engines would contain exactly the same deps.
	 * This is the reason why we make the workaround to download a single
	 * engine for certain engines and then rename it follwoing the corresponding
	 * convention
	 * 
	 * Regard, that for certain engines, downloading all the OS depending engines
	 * is necessary, as the dependencies vary from one system to another. 
	 */
	public void checkMinimalEngineInstallation() {
		readEnginesJSON();
		checkEnginesInstalled();
		if (this.everythingInstalled)
			manageMissingEngines();
		this.progressString = PROGRESS_DONE_KEYWORD;
	}
	
	/**
	 * Returns the required engines that have not been yet correctly installed
	 * @return a list with the required engines for the plugin that have not been installed
	 * 	yet.
	 */
	public ArrayList<String> getMissingEngines() {
		if (missingEngineFolders == null)
			checkEnginesInstalled();
		return new ArrayList<>(missingEngineFolders.keySet());
	}
	
	/**
	 * Read the engines JSON and finds if there is any new framework that is not required
	 * by the {@link #ENGINES_VERSIONS} dictionary. 
	 * If a new framework is found, the {@link #ENGINES_VERSIONS} dictionary is updated with
	 * the latest version available in the JSON for that framework.
	 * Regard that there is a workaround for Onnx versions due to its weird versioning,
	 * instead of being 1.1.0, 1.2.1, 1.3.0, it changes as 1, 2, 3, 4...
	 * 
	 * The engines JSOn is located at {@code /src/main/resources/availableDLVersions.json}
	 * at the JAR file {@code dl-modelrunner-X-Y-Z.jar}.
	 * The link to a github repo is: https://raw.githubusercontent.com/bioimage-io/model-runner-java/main/src/main/resources/availableDLVersions.json
	 */
	private void readEnginesJSON() {
		 Map<String, String> versionsNotInRequired = AvailableDeepLearningVersions
				.loadCompatibleOnly().getVersions().stream()
				.filter( v -> !v.getEngine().startsWith(EngineInfo.getOnnxKey())
						&& !ENGINES_VERSIONS.keySet().contains( v.getEngine() 
						+ "_" + v.getPythonVersion().substring(0, v.getPythonVersion().indexOf(".")) ) )
				.collect(Collectors.toMap(
						v -> v.getEngine() + "_" + v.getPythonVersion(), v -> v.getPythonVersion()));
		 List<String> uniqueFrameworks = versionsNotInRequired.keySet().stream()
				 .map(f -> f.substring(0, f.lastIndexOf("_"))).distinct()
				 .collect(Collectors.toList());
		 Comparator<String> versionComparator = (v1, v2) -> {
			 // Multiply by -1 because we want to return 1 if v1 is bigger and -1 otherwise
			 // and the used method does the opposite
			 return DeepLearningVersion.stringVersionComparator(v1, v2) * -1;
	        };
		 for (String f : uniqueFrameworks) {
			 String selectedVersion = versionsNotInRequired.entrySet().stream()
					 .filter( v -> v.getKey().startsWith(f + "_"))
					 .map(v -> v.getValue()).max(versionComparator).orElse(null);
			 ENGINES_VERSIONS.put(f + "_" + selectedVersion.indexOf("."), selectedVersion);
		 }
		 
		
	}
	
	/**
	 * Checks which of the required engines are not installed.
	 */
	public void checkEnginesInstalled() {
		Map<String, String> engineFolders = ENGINES_VERSIONS.entrySet().stream()
				.collect(Collectors.toMap( v -> v.getKey(), v -> {
					String framework = v.getKey().substring(0, v.getKey().lastIndexOf("_"));
					String pythonVersion = v.getValue();
					try {
						EngineInfo engineInfo = 
							EngineInfo.defineDLEngine(framework, pythonVersion, ENGINES_DIR, true, true);
						return engineInfo.getDeepLearningVersionJarsDirectory();
					} catch (Exception ex) {
						return null;
					}
					}));

		missingEngineFolders = engineFolders.entrySet().stream()
				.filter( dir -> (dir.getValue() != null) && !(new File(dir.getValue()).isDirectory()) )
				.collect(Collectors.toMap(dir -> dir.getKey(), dir -> dir.getValue()));
		
		if (missingEngineFolders.entrySet().size() == 0)
			everythingInstalled = true;
	}
	
	/**
	 * Manages the missing engines, either renaming the OS-general engine folder
	 * to OS-specific or directly installing the engine from scratch.
	 * 
	 * This method tries to find if there is any engine with the {@link #GENERAL_KEYWORD}
	 * tag and renames it following the dl-modelrunner naming convention.
	 * 
	 * In order to reduce repetition and to reduce the number of deps downloaded
	 * by the user when deepImageJ is installed, a selection of engines is created.
	 * There are some of the engines that are the same for every operating system,
	 * for example TF1 and Pytorch.
	 * To reduce the number of deps donwloaded, the TF1 and Pytorch engines are
	 * installed as for example:
	 *  - "tensorflow-1.15.0-1.15.0" + {@link #GENERAL_KEYWORD}
	 * Just one of the above folder is downloaded for all the OS.
	 * If it was not done like this, as the Fiji update site does not recognise the
	 * OS of the user, the three following engines would be required:
	 *  -"tensorflow-1.15.0-1.15.0-windows-x86_64-cpu-gpu"
	 *  -"tensorflow-1.15.0-1.15.0-linux-x86_64-cpu-gpu"
	 *  -"tensorflow-1.15.0-1.15.0-macos-x86_64-cpu"
	 * however, the 3 engines would contain exactly the same deps.
	 * This is the reason why we make the workaround to download a single
	 * engine for certain engines and then rename it follwoing the corresponding
	 * convention
	 * 
	 * Regard, that for certain engines, downloading all the OS depending engines
	 * is necessary, as the dependencies vary from one system to another. 
	 * 
	 */
	public void manageMissingEngines() {
		if (missingEngineFolders == null)
			checkEnginesInstalled();
		if (missingEngineFolders.entrySet().size() == 0)
			return;
		missingEngineFolders = missingEngineFolders.entrySet().stream()
			.filter(v -> {
				String value = v.getValue();
				String generalName = value.substring(0, value.indexOf(new PlatformDetection().getOs()));
				generalName += GENERAL_KEYWORD;
				File generalFile = new File(generalName);
				if (generalFile.isDirectory() && generalFile.renameTo(new File(v.getValue())))
					return false;
				return true;
			}).collect(Collectors.toMap(v -> v.getKey(), v -> v.getValue()));
		installMissingEngines();
	}
	
	/**
	 * Install the missing engines from scratch
	 */
	public void installMissingEngines() {
		if (missingEngineFolders == null)
			checkEnginesInstalled();
		if (missingEngineFolders.entrySet().size() == 0)
			return;
		missingEngineFolders = missingEngineFolders.entrySet().stream()
				.filter(v -> !installSpecificEngine(v.getValue(), getInstallationProgressConsumer()))
				.collect(Collectors.toMap(v -> v.getKey(), v -> v.getValue()));
	}
	
	/**
	 * Install the engine that should be located in the engine dir specified
	 * @param engineDir
	 * 	directory where the specific engine shuold be installed. Regard that this 
	 * 	is the whole path to the folder, and that the folder name should follow the 
	 * 	dl-modelrunner naming convention (https://github.com/bioimage-io/model-runner-java#readme)
	 * @return true if the installation was successful and false otherwise
	 */
	public static boolean installSpecificEngine(String engineDir) {
		return installSpecificEngine(engineDir, null);
	}
	
	/**
	 * Install the engine that should be located in the engine dir specified
	 * @param engineDir
	 * 	directory where the specific engine shuold be installed. Regard that this 
	 * 	is the whole path to the folder, and that the folder name should follow the 
	 * 	dl-modelrunner naming convention (https://github.com/bioimage-io/model-runner-java#readme)
	 * @param consumer
	 * 	consumer used to communicate the progress made donwloading files. It can be null
	 * @return true if the installation was successful and false otherwise
	 */
	public static boolean installSpecificEngine(String engineDir, Consumer<String> consumer) {
		File engineFileDir = new File(engineDir);
		if (engineFileDir.mkdirs() == false)
			return false;
		DeepLearningVersion dlVersion;
		try {
			dlVersion = DeepLearningVersion.fromFile(engineFileDir);
			return installEngine(dlVersion, consumer);
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Install the engine specified by the {@link DeepLearningVersion} object
	 * @param engine
	 * 	the {@link DeepLearningVersion} object specifying the wanted engine
	 * @return true if the installation was successful and false otherwise
	 */
	public static boolean installEngine(DeepLearningVersion engine) {
		return installEngine(engine, null);
	}
	
	/**
	 * Install the engine specified by the {@link DeepLearningVersion} object
	 * @param engine
	 * 	the {@link DeepLearningVersion} object specifying the wanted engine
	 * @param consumer
	 * 	consumer used to communicate the progress made donwloading files
	 * @return true if the installation was successful and false otherwise
	 */
	public static boolean installEngine(DeepLearningVersion engine, Consumer<String> consumer) {
		addProgress(consumer, PROGRESS_ENGINE_KEYWORD + engine.folderName());
		try {
			for (String jar : engine.getJars()) {
				URL website = new URL(jar);
				long size = InstallerDialog.getFileSize(website);
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				// Create the new model file as a zip
				Path filePath = Paths.get(website.getPath()).getFileName();
				FileOutputStream fos = new FileOutputStream(new File(ENGINES_DIR, filePath.toString()));
				addProgress(consumer, PROGRESS_JAR_KEYWORD + ENGINES_DIR + File.separator
						+ filePath.toString());
				addProgress(consumer, PROGRESS_SIZE_KEYWORD + size);
				ModelDownloader downloader = new ModelDownloader(rbc, fos);
				downloader.call();
			}
		} catch (IOException ex) {
			return false;
		}
		return true;
	}
	
	/**
	 * Install the engine specified by the arguments of the method
	 * @param framework
	 * 	DL framework as specified by the Bioimage.io model zoo ()https://github.com/bioimage-io/spec-bioimage-io/blob/gh-pages/weight_formats_spec_0_4.md)
	 * @param version
	 * 	the version of the framework
	 * @param cpu
	 * 	whether the engine supports cpu or not
	 * @param gpu
	 * 	whether the engine supports gpu or not
	 * @return true if the installation was successful and false otherwise
	 */
	public static  boolean installEngineForSystemOs(String framework, String version, boolean cpu, boolean gpu) {
		return installEngineForSystemOs(framework, version, cpu, gpu, null);
	}
	
	/**
	 * Install the engine specified by the arguments of the method
	 * @param framework
	 * 	DL framework as specified by the Bioimage.io model zoo ()https://github.com/bioimage-io/spec-bioimage-io/blob/gh-pages/weight_formats_spec_0_4.md)
	 * @param version
	 * 	the version of the framework
	 * @param cpu
	 * 	whether the engine supports cpu or not
	 * @param gpu
	 * 	whether the engine supports gpu or not
	 * @param consumer
	 * 	consumer used to communicate the progress made donwloading files
	 * @return true if the installation was successful and false otherwise
	 */
	public static  boolean installEngineForSystemOs(String framework, String version, 
			boolean cpu, boolean gpu, Consumer<String> consumer) {
		if (AvailableDeepLearningVersions.getEngineKeys().get(framework) != null)
			framework = AvailableDeepLearningVersions.getEngineKeys().get(framework);
		DeepLearningVersion engine = AvailableDeepLearningVersions.getAvailableVersionsForEngine(framework).getVersions()
				.stream().filter(v -> (v.getPythonVersion() == version)
					&& (v.getCPU() == cpu)
					&& (v.getGPU() == gpu)).findFirst().orElse(null);
		return installEngine(engine, consumer);
	}
	
	private static void addProgress(Consumer<String> consumer, String str) {
		if (consumer == null)
			return;
		consumer.accept(str + System.lineSeparator());
	}
    
    private Consumer<String> getInstallationProgressConsumer() {
    	progressString = "";
    	Consumer<String> progressConsumer = (String b) -> {
    		progressString += b;
    		};
		return progressConsumer;
    }
    
    /**
     * Retrieve the progress String
     * @return progress String that updates the progress about installing engines
     */
    public String getProgressString() {
    	return progressString;
    }
    
    /**
     * Get the download information provided by {@link #progressString} 
     * @return a meaningful String containing info about the installation
     */
    public String manageProgress() {
    	if (progressString.equals(PROGRESS_DONE_KEYWORD))
    		return progressString;
    	int caret = 0;
    	String infoStr = "";
    	while (progressString.substring(caret).indexOf(PROGRESS_ENGINE_KEYWORD) == -1) {
    		int engineStart = progressString.substring(caret).indexOf(PROGRESS_ENGINE_KEYWORD) + PROGRESS_ENGINE_KEYWORD.length();
    		int engineEnd = progressString.substring(engineStart).indexOf(System.lineSeparator());
    		String engine = progressString.substring(engineStart, engineStart + engineEnd).trim();
    		caret += engineStart + engineEnd;
    		infoStr += "Installing: " + engine + System.lineSeparator();
    		while (progressString.substring(caret).indexOf(PROGRESS_JAR_KEYWORD) == -1
    				&& progressString.substring(caret).indexOf(PROGRESS_ENGINE_KEYWORD) != -1
    				&& progressString.substring(caret).indexOf(PROGRESS_ENGINE_KEYWORD) < 
    						progressString.substring(caret).indexOf(PROGRESS_JAR_KEYWORD)) {
	    		int jarStart = progressString.substring(caret).indexOf(PROGRESS_JAR_KEYWORD) + PROGRESS_JAR_KEYWORD.length();
	    		int jarEnd = progressString.substring(caret + jarStart).indexOf(System.lineSeparator());
	    		String jar = progressString.substring(jarStart, jarStart + jarEnd).trim();
	    		caret += jarStart + jarEnd;
	    		int sizeStart = progressString.substring(caret).indexOf(PROGRESS_JAR_KEYWORD) + PROGRESS_JAR_KEYWORD.length();
	    		int sizeEnd = progressString.substring(caret + sizeStart).indexOf(System.lineSeparator());
	    		String sizeStr = progressString.substring(sizeStart, sizeStart + sizeEnd).trim();
	    		caret += sizeStart + sizeEnd;
	    		long size = Long.parseLong(sizeStr);
	    		File jarFile = new File(ENGINES_DIR, jar);
	    		if (jarFile.isFile())
	    			infoStr += jar + ": " + (100 * jarFile.length() / size) + "%" + System.lineSeparator();
	    		else
	    			infoStr += jar + ": " + "unknown%" + System.lineSeparator();
    		}
    	}
    	return infoStr;
    }
}

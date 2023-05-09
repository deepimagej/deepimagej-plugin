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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import deepimagej.InstallerDialog;
import deepimagej.tools.ModelDownloader;
import io.bioimage.modelrunner.engine.EngineInfo;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.versionmanagement.AvailableEngines;
import io.bioimage.modelrunner.versionmanagement.DeepLearningVersion;

/**
 * Class that manages the dl-modelrunner engines.
 * This class checks that the required engines are installed and installs them if they are not.
 * There is one required engine per DL framework. It can be either the latest one or the one specified
 * in the variable {@link EngineManagement#ENGINES_VERSIONS}.
 * This class also contains the methods to install engines on demand.
 * @author Carlos Garcia Lopez de Haro, Ivan Estevez Albuja and Caterina Fuster Barcelo
 *
 */
public class EngineManagement_old {
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
	 * Keyword used to identify the time at whihc the engine started being downloaded
	 */
	public static final String PROGRESS_ENGINE_TIME_KEYWORD = "Engine time start: ";
	/**
	 * Keyword used to identify the time at whihc the engine started being downloaded
	 */
	public static final String PROGRESS_JAR_TIME_KEYWORD = "JAR time start: ";
	/**
	 * Map containing which version should always be installed per framework
	 */
	public static HashMap<String, String> ENGINES_VERSIONS = new HashMap<String, String>();
	
	static {
		ENGINES_VERSIONS.put(EngineInfo.getTensorflowKey() + "_2", "2.7.0");
		ENGINES_VERSIONS.put(EngineInfo.getTensorflowKey() + "_1", "1.15.0");
		ENGINES_VERSIONS.put(EngineInfo.getOnnxKey() + "_17", "17");
		ENGINES_VERSIONS.put(EngineInfo.getPytorchKey() + "_1", "1.13.1");
	}
	/**
	 * Map containing the reference from bioimage.io key to the engine key used
	 * to name the engine folder
	 */
	private static final Map<String, String> ENGINES_MAP = 
			AvailableEngines.bioimageioToModelRunnerKeysMap().entrySet()
			.stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
	
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
	 * Flag to communicate if the management of engines is already finished
	 */
	private boolean isManagementFinished = false;
	
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
	private EngineManagement_old() {
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
	public static EngineManagement_old createManager() {
		return new EngineManagement_old();
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
		isManagementFinished = false;
		readEnginesJSON();
		checkEnginesInstalled();
		if (!this.everythingInstalled)
			manageMissingEngines();
		isManagementFinished = true;
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
		Map<String, String> versionsNotInRequired = getListOfSingleVersionsPerFrameworkNotInRequired();
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
	 * Method to retrieve a list of single python versions for the system OS. If there exist GPU
	 * and CPU versions, it chooses the GPU one
	 * @return a list of all the versions available for each framework that is not contained in 
	 * the {@link #ENGINES_MAP} map of required versions
	 */
	public static Map<String, String> getListOfSingleVersionsPerFrameworkNotInRequired() {
		List<DeepLearningVersion> vList = AvailableEngines
				.loadCompatibleOnly().getVersions().stream()
				.filter( v -> !v.getEngine().startsWith(EngineInfo.getOnnxKey())
						&& !ENGINES_VERSIONS.keySet().contains( v.getEngine() 
						+ "_" + v.getPythonVersion().substring(0, v.getPythonVersion().indexOf(".")) ) 
						&& v.getOs().equals(new PlatformDetection().toString()))
				.collect(Collectors.groupingBy(DeepLearningVersion::getPythonVersion)).values().stream()
			    .flatMap(sizeGroup -> {
			        List<DeepLearningVersion> uniquePythonVersions = sizeGroup.stream()
			            .filter(v -> sizeGroup.stream()
			            		.noneMatch(otherV -> v != otherV && v.getPythonVersion().equals(otherV.getPythonVersion())))
			            .collect(Collectors.toList());

			        List<DeepLearningVersion> guVersions = sizeGroup.stream()
			            .filter(obj -> obj.getGPU()).limit(1).collect(Collectors.toList());

			        uniquePythonVersions.addAll(guVersions);
			        return uniquePythonVersions.stream();
			    })
			    .collect(Collectors.toList());

		Map<String, String> versionsNotInRequired = vList.stream().collect(Collectors.toMap(
							v -> v.getEngine() + "_" + v.getPythonVersion(), v -> v.getPythonVersion()));;
		return versionsNotInRequired;
	}
	
	/**
	 * Checks which of the required engines are not installed.
	 */
	public void checkEnginesInstalled() {
		Map<String, String> engineFolders = ENGINES_VERSIONS.entrySet().stream()
				.collect(Collectors.toMap( v -> v.getKey(), v -> {
					String framework = v.getKey().substring(0, v.getKey().lastIndexOf("_"));
					if (ENGINES_MAP.get(framework) != null)
						framework = ENGINES_MAP.get(framework);
					String pythonVersion = v.getValue();
					try {
						boolean gpu = true;
						if (!isEngineSupported(framework, pythonVersion, true, gpu))
							gpu = false;
						EngineInfo engineInfo = 
							EngineInfo.defineDLEngine(framework, pythonVersion, ENGINES_DIR, true, gpu);
						return engineInfo.getDeepLearningVersionJarsDirectory();
					} catch (Exception ex) {
						return "";
					}
					}));

		missingEngineFolders = engineFolders.entrySet().stream()
				.filter( dir -> {
					try {
						File dirFile = new File(dir.getValue());
						return !dirFile.isDirectory() || DeepLearningVersion.fromFile(dirFile).checkMissingJars().size() != 0;
					} catch (Exception e) {
						return true;
					}
				} ).collect(Collectors.toMap(dir -> dir.getKey(), dir -> dir.getValue()));
		
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
				String generalName = value.substring(0, value.indexOf(new PlatformDetection().toString()) - 1);
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
		Consumer<String> consumer = getInstallationProgressConsumer();
		missingEngineFolders = missingEngineFolders.entrySet().stream()
				.filter(v -> !installSpecificEngine(v.getValue(), consumer))
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
		if (!engineFileDir.isDirectory() && engineFileDir.mkdirs() == false)
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
		Date now = new Date();
		addProgress(consumer, PROGRESS_ENGINE_TIME_KEYWORD + new SimpleDateFormat("HH:mm:ss").format(now));
		ReadableByteChannel rbc = null;
		FileOutputStream fos = null;
		try {
			for (String jar : engine.getJars()) {
				URL website = new URL(jar);
				long size = InstallerDialog.getFileSize(website);
				rbc = Channels.newChannel(website.openStream());
				// Create the new model file as a zip
				Path filePath = Paths.get(website.getPath()).getFileName();
				String engineDir = ENGINES_DIR + File.separator + engine.folderName();
				fos = new FileOutputStream(new File(engineDir, filePath.toString()));
				addProgress(consumer, PROGRESS_JAR_KEYWORD + engineDir + File.separator
						+ filePath.toString());
				addProgress(consumer, PROGRESS_SIZE_KEYWORD + size);
				addProgress(consumer, PROGRESS_JAR_TIME_KEYWORD + new SimpleDateFormat("HH:mm:ss").format(now));
				ModelDownloader downloader = new ModelDownloader(rbc, fos);
				downloader.call();
				rbc.close();
				fos.close();
			}
		} catch (IOException ex) {
			try {
				if (rbc != null)
					rbc.close();
			} catch (IOException e) {
			}
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
			}
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
		if (AvailableEngines.bioimageioToModelRunnerKeysMap().get(framework) != null)
			framework = AvailableEngines.bioimageioToModelRunnerKeysMap().get(framework);
		DeepLearningVersion engine = AvailableEngines.getAvailableVersionsForEngine(framework).getVersions()
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
     * Check whether the management of the engines is finished or not
     * @return true if it is finished or false otherwise
     */
    public boolean isManagementDone() {
    	return isManagementFinished;
    }
    
    /**
     * Get the download information provided by {@link #progressString} 
     * @return a meaningful String containing info about the installation
     */
    public String manageProgress() {
    	String str = "" + this.progressString;
    	if (this.isManagementFinished)
    		return "";
    	int caret = 0;
    	String infoStr = "";
    	while (str.substring(caret).indexOf(PROGRESS_ENGINE_KEYWORD) != -1) {
    		int engineStart = str.substring(caret).indexOf(PROGRESS_ENGINE_KEYWORD) + PROGRESS_ENGINE_KEYWORD.length();
    		int engineEnd = str.substring(caret + engineStart).indexOf(System.lineSeparator());
    		String engine = str.substring(caret + engineStart, caret + engineStart + engineEnd).trim();
    		caret += engineStart + engineEnd;
    		int engineTimeStart = str.substring(caret).indexOf(PROGRESS_ENGINE_TIME_KEYWORD) + PROGRESS_ENGINE_TIME_KEYWORD.length();
    		int engineTimeEnd = str.substring(caret + engineTimeStart).indexOf(System.lineSeparator());
    		String time = str.substring(caret + engineTimeStart, caret + engineTimeStart + engineTimeEnd).trim();
    		caret += engineTimeStart + engineTimeEnd;
    		infoStr += " - " + time + " -- Installing: " + engine + System.lineSeparator();
    		while (str.substring(caret).indexOf(PROGRESS_JAR_KEYWORD) != -1) {
    			if (str.substring(caret).indexOf(PROGRESS_ENGINE_KEYWORD) != -1
        				&& str.substring(caret).indexOf(PROGRESS_ENGINE_KEYWORD) < 
						str.substring(caret).indexOf(PROGRESS_JAR_KEYWORD)) {
    				break;
    			}
	    		int jarStart = str.substring(caret).indexOf(PROGRESS_JAR_KEYWORD) + PROGRESS_JAR_KEYWORD.length();
	    		int jarEnd = str.substring(caret + jarStart).indexOf(System.lineSeparator());
	    		String jar = str.substring(caret + jarStart, caret + jarStart + jarEnd).trim();
	    		caret += jarStart + jarEnd;
	    		int sizeStart = str.substring(caret).indexOf(PROGRESS_SIZE_KEYWORD) + PROGRESS_SIZE_KEYWORD.length();
	    		int sizeEnd = str.substring(caret + sizeStart).indexOf(System.lineSeparator());
	    		String sizeStr = str.substring(caret + sizeStart, caret + sizeStart + sizeEnd).trim();
	    		caret += sizeStart + sizeEnd;
	    		int jarTimeStart = str.substring(caret).indexOf(PROGRESS_JAR_TIME_KEYWORD) + PROGRESS_JAR_TIME_KEYWORD.length();
	    		int jarTimeEnd = str.substring(caret + jarTimeStart).indexOf(System.lineSeparator());
	    		String jarTime = str.substring(caret + jarTimeStart, caret + jarTimeStart + jarTimeEnd).trim();
	    		caret += jarTimeStart + jarTimeEnd;
	    		long size = Long.parseLong(sizeStr);
	    		File jarFile = new File(jar);
	    		jar = jarFile.getName();
	    		if (jarFile.isFile())
	    			infoStr += " - " + jarTime + " -- " + jar + ":   " + (100 * jarFile.length() / size) + "%" + System.lineSeparator();
	    		else
	    			infoStr += " - " + jarTime + " -- " + jar + ":   " + "unknown%" + System.lineSeparator();
    		}
    	}
    	return infoStr;
    }
    
    /**
     * Check if an engine is supported by the dl-modelrunner or not
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
     * @return true if the engine exists and false otherwise
     */
    public static boolean isEngineSupported(String framework, String version, boolean cpu, boolean gpu) {
    	if (ENGINES_MAP.get(framework) != null)
			framework = AvailableEngines.bioimageioToModelRunnerKeysMap().get(framework);
    	DeepLearningVersion engine = AvailableEngines.getAvailableVersionsForEngine(framework).getVersions()
				.stream().filter(v -> v.getPythonVersion().equals(version) 
						&& v.getOs().equals(new PlatformDetection().toString())
						&& v.getCPU() == cpu
						&& v.getGPU() == gpu).findFirst().orElse(null);
		if (engine == null) 
			return false;
		return true;
    }
}

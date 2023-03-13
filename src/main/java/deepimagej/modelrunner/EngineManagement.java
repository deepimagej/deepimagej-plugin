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
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.bioimage.modelrunner.engine.EngineInfo;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.versionmanagement.DeepLearningVersion;

public class EngineManagement {
	
	private static final String ENGINES_DIR = new File("engines").getAbsolutePath();
	
	private static final HashMap<String, String> ENGINES_VERSIONS = new HashMap<String, String>();
	
	private static final String GENERAL_KEYWORD = "-general";
	
	static {
		ENGINES_VERSIONS.put(EngineInfo.getTensorflowKey() + "_2", "2.7.1");
		ENGINES_VERSIONS.put(EngineInfo.getTensorflowKey() + "_1", "1.15.0");
		ENGINES_VERSIONS.put(EngineInfo.getOnnxKey() + "_1", "1.11.0");
		ENGINES_VERSIONS.put(EngineInfo.getPytorchKey() + "_1", "1.13.0");
	}
	
	private boolean everythingInstalled = false;
	
	private Map<String, String> missingEngineFolders;
	
	
	private EngineManagement() {
		readEnginesJSON();
		checkEnginesInstalled();
		if (this.everythingInstalled)
			manageMissingEngines();
	}
	
	public static EngineManagement manage() {
		return new EngineManagement();
	}
	
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
	
	public void installMissingEngines() {
		if (missingEngineFolders == null)
			checkEnginesInstalled();
		if (missingEngineFolders.entrySet().size() == 0)
			return;
		missingEngineFolders = missingEngineFolders.entrySet().stream()
				.filter(v -> !installSpecificEngine(v.getValue()))
				.collect(Collectors.toMap(v -> v.getKey(), v -> v.getValue()));
		
	}
	
	public boolean installSpecificEngine(String engineDir) {
		File engineFileDir = new File(engineDir);
		if (engineFileDir.mkdirs() == false)
			return false;
		DeepLearningVersion dlVersion;
		try {
			dlVersion = DeepLearningVersion.fromFile(engineFileDir);
			return installEngine(dlVersion);
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean installEngine(DeepLearningVersion engine) {
		return false;
	}
	
	public boolean installEngineForSystemOs(String framework, String version, boolean cpu, boolean gpu) {
		return false;
	}
}

package deepimagej.modelrunner;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.bioimage.modelrunner.engine.EngineInfo;

public class EngineManagement {
	
	private static final String ENGINES_DIR = new File("engines").getAbsolutePath();
	
	private static final HashMap<String, String> ENGINES_VERSIONS = new HashMap<String, String>();
	
	static {
		ENGINES_VERSIONS.put(EngineInfo.getTensorflowKey() + "_2", "2.7.1");
		ENGINES_VERSIONS.put(EngineInfo.getTensorflowKey() + "_1", "1.15.0");
		ENGINES_VERSIONS.put(EngineInfo.getOnnxKey() + "_1", "1.11.0");
		ENGINES_VERSIONS.put(EngineInfo.getPytorchKey() + "_1", "1.13.0");
	}
	
	private boolean everythingInstalled = false;
	
	private List<String> missingEngineFolders;
	
	
	private EngineManagement() {
		readEnginesJSON();
		checkEnginesInstalled();
		if (this.notInstalled)
			installEngines();
	}
	
	public static EngineManagement manage() {
		return new EngineManagement();
	}
	
	public void checkEnginesInstalled() {
		List<String> engineFolders = ENGINES_VERSIONS.entrySet().stream().map(v -> {
			String framework = v.getKey().substring(0, v.getKey().lastIndexOf("_"));
			String pythonVersion = v.getValue();
			try {
				EngineInfo engineInfo = 
					EngineInfo.defineDLEngine(framework, pythonVersion, ENGINES_DIR, true, true);
				return engineInfo.getDeepLearningVersionJarsDirectory();
			} catch (Exception e) {
				return null;
			}
			}).collect(Collectors.toList());
		
		missingEngineFolders = engineFolders.stream()
				.filter( dir -> !(new File(dir).isDirectory()) ).collect(Collectors.toList());
		
		if (missingEngineFolders.size() == 0)
			everythingInstalled = true;
	}
}

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.junit.Test;

import io.bioimage.modelrunner.bioimageio.BioimageioRepo;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptorFactory;
import io.bioimage.modelrunner.bioimageio.description.exceptions.ModelSpecsException;
import io.bioimage.modelrunner.bioimageio.download.DownloadModel;
import io.bioimage.modelrunner.engine.installation.EngineInstall;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.utils.CommonUtils;
import io.bioimage.modelrunner.utils.Constants;
import io.bioimage.modelrunner.utils.ZipUtils;

public class TestRunEveryFramework {
	
	private List<String> modelPaths;
	
	private static final String MACRO_FORMAT = ""
			+ "run(\"DeepImageJ Run\", "
			+ "\""
			+ "modelPath=%s "
			+ "inputPath=%s "
			+ "outputFolder=%s "
			+ "displayOutput=null"
			+ "\")";
	
	private static final File FIJI_DIR = new File("fiji");
	private static final File ENGINES_DIR = new File(FIJI_DIR.getAbsolutePath(), "engines");
	private static final File MODELS_DIR = new File(FIJI_DIR.getAbsolutePath(), "models");
	
	private static final Map<String, String> MODELS;
	
	static {
		MODELS = new HashMap<String, String>();
		MODELS.put("pytorch1", "hiding-tiger");
		MODELS.put("pytorch2", "hiding-tiger");
		MODELS.put("tensorflow1", "chatty-frog");
		MODELS.put("tensorflow2", "placid-llama");
		MODELS.put("onnx", "polite-pig");
	}
	
	private static final Map<String, String> FIJI_URL = new HashMap<String, String>();
	static {
		FIJI_URL.put(PlatformDetection.OS_WINDOWS, "https://downloads.imagej.net/fiji/latest/fiji-win64.zip");
		FIJI_URL.put(PlatformDetection.OS_LINUX, "https://downloads.imagej.net/fiji/latest/fiji-linux64.zip");
		FIJI_URL.put(PlatformDetection.OS_OSX, "https://downloads.imagej.net/fiji/latest/fiji-macosx.zip");
	}
	
	@Test
	public void checkTestResults() {
		
	}

    @BeforeAll
    public void setUp() throws InterruptedException, IOException, ModelSpecsException {
    	downloadAndTrackFiji();
    	installEngines();
    	installModels();
    	createMacros();
    }
    
    private static void downloadAndTrackFiji() throws InterruptedException, IOException {
    	String url = FIJI_URL.get(PlatformDetection.getOs());
    	URL website = new URL(url);
    	long fijiSize = DownloadModel.getFileSize(website);
		Path filePath = Paths.get(website.getPath()).getFileName();
    	File targetFile = new File(FIJI_DIR.getAbsolutePath(), filePath.toString());
    	
    	Thread parentThread = Thread.currentThread();
    	Thread dnwldthread = new Thread(() -> {
    		try {
				downloadFiji(website, targetFile, parentThread);
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
    	});
    	dnwldthread.start();
    	
    	while (dnwldthread.isAlive()) {
    		Thread.sleep(300);
    		System.out.println("Download progress: " + (targetFile.length() / (double) fijiSize) + "%");
    	}
    	
    	if (targetFile.length() != fijiSize)
    		throw new RuntimeException("Size of downloaded Fiji zip is different than the expected.");
    	ZipUtils.unzipFolder(targetFile.getAbsolutePath(), FIJI_DIR.getAbsolutePath());
    	
    }
    
    private static void downloadFiji(URL website, File targetFile, Thread parentThread) throws InterruptedException, IOException {
        HttpURLConnection conn = (HttpURLConnection) website.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", CommonUtils.getJDLLUserAgent());
		try (ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
				FileOutputStream fos = new FileOutputStream(targetFile)){
				FileDownloader downloader = new FileDownloader(rbc, fos);
				downloader.call(parentThread);
		}
		conn.disconnect();
    }
    
    private static void installEngines() {
    	EngineInstall installer = EngineInstall.createInstaller(ENGINES_DIR.getAbsolutePath());
    	installer.basicEngineInstallation();
    }
    
    private void installModels() throws IOException, InterruptedException {
    	modelPaths = new ArrayList<String>();
    	List<String> dwnldModels = MODELS.entrySet().stream().map(ee -> ee.getValue()).collect(Collectors.toList());
    	
    	BioimageioRepo br = BioimageioRepo.connect();
    	for (String mm : dwnldModels) {
    		String path = br.downloadModelByID(mm, MODELS_DIR.getAbsolutePath());
    		modelPaths.add(path);
    	}
    }
    
    private void createMacros() throws FileNotFoundException, ModelSpecsException, IOException {
    	for (String mm : modelPaths) {
    		ModelDescriptor model = ModelDescriptorFactory.readFromLocalFile(mm + File.separator + Constants.RDF_FNAME);
    		String modelPath = model.getModelPath();
    		String samplePath = modelPath + File.separator +  model.getInputTensors().get(0).getSampleTensorName();
    		String macro = String.format(MACRO_FORMAT, modelPath, samplePath, modelPath);
    		try (FileWriter writer = new FileWriter(selectedFile)) {
                writer.write(macro);
                writer.close();
            }
    	}
    }
    
}

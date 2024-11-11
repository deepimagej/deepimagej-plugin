import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.bioimage.modelrunner.bioimageio.download.DownloadModel;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.utils.CommonUtils;

public class TestRunEveryFramework {
	
	private static final File FIJI_DIR = new File("fiji");
	
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
    public void testRun() {
    }
    
    private static void downloadAndTrackFiji() throws MalformedURLException {
    	String url = FIJI_URL.get(PlatformDetection.getOs());
    	URL website = new URL(url);
    	DownloadModel.getFileSize(website);
		Path filePath = Paths.get(website.getPath()).getFileName();
    	File targetFile = new File(FIJI_DIR.getAbsoluteFile(), filePath.toString());
    	
    	Thread parentThread = Thread.currentThread();
    	Thread dnwldthread = new Thread(() -> {
    		try {
				downloadFiji(website, targetFile, parentThread);
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
    	});
    	dnwldthread.start();
    	
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
}

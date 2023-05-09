package deepimagej.modelrunner;

import java.util.Map;

import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;

public class EngineInstaller {
	
	/**
	 * Create a String that summarizes the informatio about the download of the
	 * engines specifies by the parameter 'basicEng' and the real time information
	 * about the download contained in the consumer
	 * @param basicEng
	 * @param consumer
	 * @return
	 */
	public static String basicEnginesInstallationProgress(Map<String, String> basicEng,
			TwoParameterConsumer<String, Double> consumer) {
		return "";
	}

}

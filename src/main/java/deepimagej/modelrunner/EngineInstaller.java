package deepimagej.modelrunner;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.bioimage.modelrunner.bioimageio.download.DownloadTracker;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;

public class EngineInstaller {

	private static String TOTAL_PROGRESS_STRING = "";
	private static String TOTAL_REMAINING_STRING = "";
	private static int N_BINS = 10;
	
	static {
		for (int i = 0; i < N_BINS; i ++) {
			TOTAL_PROGRESS_STRING += "#";
			TOTAL_REMAINING_STRING += ".";
		}
	}
	
	/**
	 * Create a String that summarizes the information about the download of the
	 * engines specifies by the parameter 'basicEng' and the real time information
	 * about the download contained in the consumer
	 * @param consumers
	 * @return
	 */
	public static String basicEnginesInstallationProgress(
			Map<String, TwoParameterConsumer<String, Double>> consumers) {
		String progress = "";
		for (Entry<String, TwoParameterConsumer<String, Double>> entry : consumers.entrySet()) {
			progress += System.lineSeparator();
			TwoParameterConsumer<String, Double> consumer = entry.getValue();
			double totalProgress = consumer.get().keySet().contains(DownloadTracker.TOTAL_PROGRESS_KEY) ? 
					consumer.get().get(DownloadTracker.TOTAL_PROGRESS_KEY) : 0.0;
			if (totalProgress == 0.0)
				continue;
			progress += "- Installing: " + new File(entry.getKey()).getName();
			progress += " " + getProgressPerc(totalProgress) + System.lineSeparator();
			for (Entry<String, Double> fEntry : consumer.get().entrySet()) {
				if (fEntry.getKey().equals(DownloadTracker.TOTAL_PROGRESS_KEY))
					continue;
				progress += " -- " + new File(fEntry.getKey()).getName();
				progress += " " + getProgressPerc(fEntry.getValue()) + System.lineSeparator();
				
			}
		}
		return progress;
	}
	
	private static String getProgressPerc(double progress) {
		String progressStr = "[" + Math.round(progress * 100) + "%]";
		return progressStr;
	}
	
	private static String getProgressBar(double progress) {
		int nProgressBar = (int) (progress * N_BINS);
		String progressStr = "[" + TOTAL_PROGRESS_STRING.substring(0, nProgressBar) 
		+ TOTAL_REMAINING_STRING.substring(nProgressBar) + "] " + Math.round(progress * 100) + "%";
		return progressStr;
	}

}

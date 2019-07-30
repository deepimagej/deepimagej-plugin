package deepimagej.tools;

import java.io.File;

public class FileUtils {

	static public String getFolderSizeKb(String dir) {
		return String.format("%3.2f Mb", (getFolderSize(dir)/(1024*1024.0)));
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
}

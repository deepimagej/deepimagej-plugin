package deepimagej.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Callable;

import deepimagej.DownloadProgress;

public class ModelDownloader {
	private ReadableByteChannel rbc;
	private FileOutputStream fos;
	
	public ModelDownloader(ReadableByteChannel rbc, FileOutputStream fos) {
		this.rbc = rbc;
		this.fos = fos;
	}

	public void call() throws IOException  {
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
	}

}

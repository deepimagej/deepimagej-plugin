package deepimagej.gui;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ImageLoaderWorker extends SwingWorker<ImageIcon, Void> {
	
	private volatile URLConnection connection;
	
	private final URL url;
	
	private final int width;
	
	private final int height;
	
	private final ImageLoadCallback callback;
	
	private ImageLoaderWorker(URL url, int width, int height, ImageLoadCallback callback) {
		this.url = url;
		this.height = height;
		this.width = width;
		this.callback = callback;
	}
	
	public static void main(String[] args) throws MalformedURLException, InterruptedException, ExecutionException {
		URL url2 = new URL("https://fastly.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U");
		URL url1 = new URL("https://upload.wikimedia.org/wikipedia/commons/9/94/Ti_cd2708n2l_mcmaster_mz_mit20x.jpg");
		ImageLoadCallback callback = new ImageLoadCallback() {
            @Override
            public void onImageLoaded(ImageIcon icon) {
            		return;
            }
		};
		ImageLoaderWorker a1 = create(url1, 100, 100, callback);
		ImageLoaderWorker a2 = create(url2, 10, 10, callback);
		a1.execute();
		a2.execute();
		a2.get();
		//a1.cancelBackground();
	    Thread.sleep(100000);
	}
	
	public static ImageLoaderWorker create(URL url, int width, int height, ImageLoadCallback callback) {
		return new ImageLoaderWorker(url, width, height, callback);
	}
	
    @Override
    protected ImageIcon doInBackground() throws Exception {
        connection = url.openConnection();
        
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            httpConnection.setConnectTimeout(5000);
            httpConnection.setReadTimeout(5000);
        }
        
        try {
            return createScaledIcon(connection, width, height);
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }

    @Override
    protected void done() {
        try {
        	System.out.println(url);
            ImageIcon icon = get();
            if (icon == null) {
            	System.out.println("null icon " + url);
            	return;
            }
            SwingUtilities.invokeLater(() -> callback.onImageLoaded(icon));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> callback.onImageLoadFailed(e));
        }
    }

    public boolean cancelBackground() {
        if (connection != null && connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).disconnect();
        }
        return cancel(true);
    }

    @FunctionalInterface
    public interface ImageLoadCallback {
        void onImageLoaded(ImageIcon icon);
        
        default void onImageLoadFailed(Exception e) {
            System.err.println("Failed to load image: " + e.getMessage());
        }
    }
    
    protected static ImageIcon createScaledIcon(URL imagePath, int logoWidth, int logoHeight) {
        if (imagePath == null) {
            return DefaultIcon.getDefaultIcon(logoWidth, logoHeight);
        }
        try (ImageInputStream iis = ImageIO.createImageInputStream(imagePath.openStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return DefaultIcon.getDefaultIcon(logoWidth, logoHeight);
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);
            if (isAnimatedGif(reader)) {
                return createScaledAnimatedGif(imagePath, logoWidth, logoHeight);
            } else {
                return createScaledStaticImage(reader, logoWidth, logoHeight);
            }
        } catch (IOException e) {
            return DefaultIcon.getDefaultIcon(logoWidth, logoHeight);
        }
    }
    
    protected ImageIcon createScaledIcon(URLConnection connection, int logoWidth, int logoHeight) {
        if (connection == null) {
            return DefaultIcon.getDefaultIcon(logoWidth, logoHeight);
        }
        try (ImageInputStream iis = ImageIO.createImageInputStream(connection.getInputStream())) {
        	if (this.isCancelled())
        		return null;
        		
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        	if (this.isCancelled())
        		return null;
            if (!readers.hasNext()) {
                return DefaultIcon.getDefaultIcon(logoWidth, logoHeight);
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);
        	if (this.isCancelled())
        		return null;
            if (isAnimatedGif(reader)) {
                return createScaledAnimatedGif(connection.getURL(), logoWidth, logoHeight);
            } else {
                return createScaledStaticImage(reader, logoWidth, logoHeight);
            }
        } catch (IOException e) {
            return DefaultIcon.getDefaultIcon(logoWidth, logoHeight);
        }
    }

    private static boolean isAnimatedGif(ImageReader reader) throws IOException {
        return reader.getFormatName().equalsIgnoreCase("gif") && reader.getNumImages(true) > 1;
    }

    private static ImageIcon createScaledAnimatedGif(URL imagePath, int width, int height) {
        ImageIcon originalIcon = new ImageIcon(imagePath);
        Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }
    
    private static ImageIcon createScaledStaticImage(ImageReader reader, int width, int height) throws IOException {
        BufferedImage originalImage = reader.read(0);

        if (originalImage == null) {
            return DefaultIcon.getDefaultIcon(width, height);
        }

        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = AffineTransform.getScaleInstance(
                (double) width / originalImage.getWidth(), 
                (double) height / originalImage.getHeight()
        );
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scaleOp.filter(originalImage, scaledImage);
        
        return new ImageIcon(scaledImage);
    }

    public static ImageIcon createScaledStaticImage(URL imagePath, int width, int height) throws IOException {
        BufferedImage originalImage = ImageIO.read(imagePath);
        if (originalImage == null) {
            return DefaultIcon.getDefaultIcon(width, height);
        }
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = AffineTransform.getScaleInstance(
                (double) width / originalImage.getWidth(), 
                (double) height / originalImage.getHeight()
        );
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        scaleOp.filter(originalImage, scaledImage);
        //Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }
}
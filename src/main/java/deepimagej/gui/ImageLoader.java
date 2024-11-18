package deepimagej.gui;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ImageLoader {

    public static void loadImageIconFromURL(URL url, int width, int height, ImageLoadCallback callback) {
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                return createScaledIcon(url, width, height);
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    SwingUtilities.invokeLater(() -> callback.onImageLoaded(icon));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> callback.onImageLoadFailed(e));
                }
            }
        };
        
        worker.execute();
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

    private static ImageIcon createScaledStaticImage(URL imagePath, int width, int height) throws IOException {
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
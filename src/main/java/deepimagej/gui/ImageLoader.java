package deepimagej.gui;

import java.net.MalformedURLException;
import java.net.URL;

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
                return Gui.createScaledIcon(url, width, height);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Internet Image");
            JLabel label = new JLabel("Loading...");
            frame.add(label);
            frame.setSize(300, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            URL imageUrl;
			try {
				imageUrl = new URL("https://example.com/path/to/image.jpg");
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
            loadImageIconFromURL(imageUrl, 10, 10, new ImageLoadCallback() {
                @Override
                public void onImageLoaded(ImageIcon icon) {
                    label.setIcon(icon);
                    label.setText("");
                    frame.pack();
                }

                @Override
                public void onImageLoadFailed(Exception e) {
                    label.setText("Failed to load image: " + e.getMessage());
                }
            });
        });
    }
}
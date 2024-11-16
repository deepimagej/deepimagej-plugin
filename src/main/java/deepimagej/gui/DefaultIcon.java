package deepimagej.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class DefaultIcon {
	
    private static final Map<Dimension, ImageIcon> SIZE_CACHE = new ConcurrentHashMap<>();
    
    // Approach 2: Cache single BufferedImage
    private static final BufferedImage MASTER_IMAGE;

    private static final ImageIcon LOADING_ICON;
    
    // Initialize the loading icon once when the class is loaded
    static {
        // You can use any loading icon image you have
        // This example assumes the loading.gif is in the resources folder
        LOADING_ICON = initializeLoadingIcon();
    }

    static ImageIcon getDefaultIcon(int width, int height) {
        try {
            URL defaultIconUrl = Gui.class.getClassLoader().getResource(Gui.DIJ_ICON_PATH);
            if (defaultIconUrl == null) {
                return null;
            }
            BufferedImage defaultImage = ImageIO.read(defaultIconUrl);
            Image scaledDefaultImage = defaultImage.getScaledInstance(width, height, Image.SCALE_FAST);
            return new ImageIcon(scaledDefaultImage);
        } catch (IOException e) {
            return null;
        }
    }
}

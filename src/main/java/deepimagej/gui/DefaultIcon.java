package deepimagej.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

public class DefaultIcon {

    protected static final String DIJ_ICON_PATH = "dij_imgs/deepimagej_icon.png";
    
    private static final Map<Dimension, CompletableFuture<ImageIcon>> PENDING_ICONS = new ConcurrentHashMap<>();
    private static Map<Dimension, ImageIcon> ICONS_CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService scaleExecutor = Executors.newFixedThreadPool(2);
    // Approach 2: Cache single BufferedImage
    private static final BufferedImage MASTER_IMAGE;

    
    // Initialize the loading icon once when the class is loaded
    static {
        // You can use any loading icon image you have
        // This example assumes the loading.gif is in the resources folder
    	MASTER_IMAGE = initializeMasterImage();
    }
    
    private static BufferedImage initializeMasterImage() {
        try {
            URL defaultIconUrl = DefaultIcon.class.getClassLoader().getResource(DIJ_ICON_PATH);
            if (defaultIconUrl == null) {
                throw new IOException();
            }
            return ImageIO.read(defaultIconUrl);
        } catch (IOException e) {
            // Fallback to creating a simple buffered image
            BufferedImage bi = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            g.setColor(Color.GRAY);
            g.drawString("Loading...", 5, 25);
            g.dispose();
            return bi;
        }
    }

    static ImageIcon getDefaultIcon(int width, int height) {
        try {
            URL defaultIconUrl = Gui.class.getClassLoader().getResource(DIJ_ICON_PATH);
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
    
    

    public static ImageIcon getLoadingIcon(int width, int height) {
        Dimension size = new Dimension(width, height);
        
        // Check if already cached
        ImageIcon cached = ICONS_CACHE.get(size);
        if (cached != null) {
            return cached;
        }
        
        // Check if already being processed
        CompletableFuture<ImageIcon> pending = PENDING_ICONS.get(size);
        if (pending == null && !scaleExecutor.isShutdown()) {
            // Start new scaling operation
            pending = CompletableFuture.supplyAsync(() -> {
                Image scaledImage = MASTER_IMAGE.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaledImage);
                ICONS_CACHE.put(size, icon);
                PENDING_ICONS.remove(size);
                return icon;
            }, scaleExecutor);
            PENDING_ICONS.put(size, pending);
        }
        
        // Return immediately with nearest size while scaling happens
        return createTransparentIcon(width, height);
    }
    
    private static ImageIcon createTransparentIcon(int width, int height) {
        return new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }
    
    // Create a simple placeholder instantly
    /**
     * TODO test if this or {@link #createTransparentIcon(int, int)} is faster
     * @param width
     * @param height
     * @return
     */
    private static ImageIcon createPlaceholderIcon(int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // Optionally draw something simple like a border or loading text
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawRect(0, 0, width - 1, height - 1);
        g2d.dispose();
        return new ImageIcon(bi);
    }
    
    
    // For components that want to update when the exact size is ready
    public static void getLoadingIconWithCallback(int width, int height, Consumer<ImageIcon> callback) {
        ImageIcon immediate = getLoadingIcon(width, height);
        Dimension size = new Dimension(width, height);
        
        CompletableFuture<ImageIcon> pending = PENDING_ICONS.get(size);
        if (pending != null && !scaleExecutor.isShutdown()) {
            pending.thenAcceptAsync(icon -> {
                SwingUtilities.invokeLater(() -> callback.accept(icon));
            }, scaleExecutor);
        }
        
        // Return immediate result
        callback.accept(immediate);
    }
    
    public static void closeThreads() {
    	if (scaleExecutor != null)
    		scaleExecutor.shutdown();
    }
}

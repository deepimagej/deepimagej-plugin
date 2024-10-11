package deepimagej.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ModernSearchBar extends JPanel {
    private static final long serialVersionUID = -1741389221668683293L;
	private JTextField searchField;
    private JButton searchButton;
    private static final int ICON_SIZE = 60; // Define the icon size
    protected static final String SEARCH_ICON_PATH = "dij_imgs/search_logo.png";

    public ModernSearchBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(600, 80));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true));

        // Create the search icon
        ImageIcon originalIcon = new ImageIcon(getClass().getClassLoader().getResource(SEARCH_ICON_PATH)); // Replace with your icon path
        Image scaledImage = getScaledImage(originalIcon.getImage(), ICON_SIZE, ICON_SIZE);
        JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Create the search field
        searchField = new JTextField();
        searchField.setBorder(null);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));

        // Create the search button
        searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(80, 30));
        searchButton.setBackground(new Color(0, 120, 215));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Add components to the panel
        add(iconLabel, BorderLayout.WEST);
        add(searchField, BorderLayout.CENTER);
        add(searchButton, BorderLayout.EAST);

        // Add action listener to the search button
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });
    }

    private Image getScaledImage(Image srcImg, int width, int height) {
        BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, width, height, null);
        g2.dispose();
        return resizedImg;
    }

    private void performSearch() {
        String searchText = searchField.getText();
        // Implement your search logic here
        System.out.println("Searching for: " + searchText);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Modern Search Bar");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(new ModernSearchBar());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
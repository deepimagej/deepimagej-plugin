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
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SearchBar extends JPanel {
    private static final long serialVersionUID = -1741389221668683293L;
	private JTextField searchField;
    private JButton searchButton;
    private long parentHeight;
    private long parentWidth;
    private static final double H_RATIO = 1;
    private static final double V_RATIO = 0.05;
    private static final double ICON_VRATIO = 1.0;
    private static final double ICON_HRATIO = 0.05; 
    private static final double SEARCH_VRATIO = 1.0;
    private static final double SEARCH_HRATIO = 0.2; 
    protected static final String SEARCH_ICON_PATH = "dij_imgs/search_logo.png";
    

    protected SearchBar(long parentWidth, long parentHeight) {
    	this.parentHeight = parentHeight;
    	this.parentWidth = parentWidth;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension((int) (parentWidth * H_RATIO), (int) (parentHeight * V_RATIO)));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true));

        // Create the search icon
        URL iconPath = getClass().getClassLoader().getResource(SEARCH_ICON_PATH);
        int iconH = (int) (parentHeight * V_RATIO * ICON_VRATIO);
        int iconW = (int) (parentWidth * H_RATIO * ICON_HRATIO);
        ImageIcon scaledImage = Gui.createScaledIcon(iconPath, iconW, iconH);
        //Image scaledImage = getScaledImage(originalIcon.getImage(), (int) (parentWidth * H_RATIO), ICON_SIZE);
        JLabel iconLabel = new JLabel(scaledImage);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Create the search field
        searchField = new JTextField();
        searchField.setBorder(null);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));

        // Create the search button
        searchButton = new JButton("Search");
        int searchH = (int) (parentHeight * V_RATIO * SEARCH_VRATIO);
        int searchW = (int) (parentWidth * H_RATIO * SEARCH_HRATIO);
        searchButton.setPreferredSize(new Dimension(searchW, searchH));
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
                frame.getContentPane().add(new SearchBar(600, 800));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
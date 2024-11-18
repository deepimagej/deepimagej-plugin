package deepimagej.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import deepimagej.tools.WebBrowser;
import io.bioimage.modelrunner.bioimageio.BioimageioRepo;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;
import io.bioimage.modelrunner.bioimageio.description.ModelDescriptorFactory;
import io.bioimage.modelrunner.utils.Constants;

public class SearchBar extends JPanel {
    private static final long serialVersionUID = -1741389221668683293L;
    protected JTextField searchField;
	protected JButton searchButton;
    protected JButton switchButton;
    private long parentHeight;
    private long parentWidth;
    private List<ModelDescriptor> bmzModels;
    private int nModels;
    private static final double H_RATIO = 1;
    private static final double V_RATIO = 0.05;
    private static final double ICON_VRATIO = 1.0;
    private static final double ICON_HRATIO = 0.05;
    private static final double SEARCH_VRATIO = 1.0;
    private static final double SEARCH_HRATIO = 0.15;
    private static final double SWITCH_VRATIO = 1.0;
    private static final double SWITCH_HRATIO = 0.15;
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
        ImageIcon scaledImage = ImageLoader.createScaledIcon(iconPath, iconW, iconH);
        JLabel iconLabel = new JLabel(scaledImage);
        iconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    WebBrowser.open("https://www.bioimage.io");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
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

        // Create the switch button
        switchButton = new JButton(Gui.BIOIMAGEIO_STR);
        int switchH = (int) (parentHeight * V_RATIO * SWITCH_VRATIO);
        int switchW = (int) (parentWidth * H_RATIO * SWITCH_HRATIO);
        switchButton.setPreferredSize(new Dimension(switchW, switchH));
        switchButton.setBackground(new Color(255, 140, 0));
        switchButton.setForeground(Color.BLACK);
        switchButton.setFocusPainted(false);
        switchButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel wrapperPanel = new JPanel(new GridLayout(1, 2));
        wrapperPanel.add(searchButton);
        wrapperPanel.add(switchButton);

        // Add components to the panel
        add(iconLabel, BorderLayout.WEST);
        add(searchField, BorderLayout.CENTER);
        //add(searchButton, BorderLayout.EAST);
        add(wrapperPanel, BorderLayout.EAST);

    }

    protected List<ModelDescriptor> performSearch() {
        String searchText = searchField.getText().trim();
        return this.bmzModels.stream().filter(mm -> {
        	if (mm == null) return false;
        	return mm.getName().contains(searchText) || mm.getDescription().contains(searchText)
        			|| mm.getNickname().contains(searchText) || mm.getTags().contains(searchText);
        }).collect(Collectors.toList());
    }
    
    protected int countBMZModels() throws InterruptedException {
    	return countBMZModels(false);
    }
    
    protected int countBMZModels(boolean recount) throws InterruptedException {
    	if (!recount)
    		return nModels;
    	BioimageioRepo.connect();
    	nModels = BioimageioRepo.getModelIDs().size();
    	return nModels;
    }
    
    protected List<ModelDescriptor> findBMZModels() throws InterruptedException {
    	bmzModels = new ArrayList<ModelDescriptor>();
    	for (String url : BioimageioRepo.getModelIDs()) {
    		ModelDescriptor descriptor = BioimageioRepo.retreiveDescriptorFromURL(BioimageioRepo.getModelURL(url) + Constants.RDF_FNAME);
    		bmzModels.add(descriptor);
    	}
    	return bmzModels;
    }
    
    protected void findLocalModels(String dir) {
    	bmzModels = ModelDescriptorFactory.getModelsAtLocalRepo(dir);
    }
    
    protected List<ModelDescriptor> getBMZModels() {
    	return this.bmzModels;
    }
    
    protected void setModels(List<ModelDescriptor> models) {
    	this.bmzModels = models;
    }
    
    protected boolean isBMZPArsingDone() {
    	return nModels == bmzModels.size();
    }
    
    protected void changeButtonToLocal() {
    	this.switchButton.setText(Gui.LOCAL_STR);
    }
    
    protected void changeButtonToBMZ() {
    	this.switchButton.setText(Gui.BIOIMAGEIO_STR);
    }
    
    protected boolean isBarOnLocal() {
    	return this.switchButton.getText().equals(Gui.BIOIMAGEIO_STR);
    }
    
    protected void setBarEnabled(boolean enabled) {
    	this.searchButton.setEnabled(enabled);
    	this.switchButton.setEnabled(enabled);
    	this.searchField.setEnabled(enabled);
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
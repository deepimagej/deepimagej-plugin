package deepimagej.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class Layout extends GridBagLayout {
	
	private static final long serialVersionUID = 1816507461370536813L;
	
	private GridBagConstraints gbc = new GridBagConstraints();
	
	private boolean isVertical = false;
	
	private final double[] weigths;

	protected Layout(double[] weigths, boolean isVertical) {
		super();
		this.isVertical = isVertical;
		this.weigths = weigths;
	}
	
	public Layout createVertical(double[] weights) {
		Layout layout = new Layout(weights, true);
		return layout;
	}
	
	public Layout createHorizontal(double[] weights) {
		Layout layout = new Layout(weights, false);
		return layout;
	}
		
	public GridBagConstraints get(int n) {
		if (n > this.weigths.length)
			throw new IllegalArgumentException("The interface does not have as many components.");

        // Common constraints
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;

        // Title Panel (10% height)
        titlePanel = createPanel("Title Panel", new Color(200, 200, 200));
        gbc.gridy = 0;
        gbc.weighty = 0.1;
        mainPanel.add(titlePanel, gbc);

        // Search Bar Panel (10% height)
        searchBarPanel = createPanel("Search Bar", new Color(220, 220, 220));
        gbc.gridy = 1;
        gbc.weighty = 0.1;
        mainPanel.add(searchBarPanel, gbc);

        // Model Selection Panel (30% height)
        modelSelectionPanel = createPanel("Model Selection", new Color(240, 240, 240));
        gbc.gridy = 2;
        gbc.weighty = 0.3;
        mainPanel.add(modelSelectionPanel, gbc);

        // Content Panel (40% height)
        contentPanel = createPanel("Content", new Color(255, 255, 255));
        gbc.gridy = 3;
        gbc.weighty = 0.4;
        mainPanel.add(contentPanel, gbc);

        // Footer Panel (10% height)
        footerPanel = createPanel("Footer", new Color(180, 180, 180));
        gbc.gridy = 4;
        gbc.weighty = 0.1;
        mainPanel.add(footerPanel, gbc);

	}

}

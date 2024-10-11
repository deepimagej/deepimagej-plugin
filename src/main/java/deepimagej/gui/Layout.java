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
	
	public static Layout createVertical(double[] weights) {
		Layout layout = new Layout(weights, true);
		return layout;
	}
	
	public static Layout createHorizontal(double[] weights) {
		Layout layout = new Layout(weights, false);
		return layout;
	}
		
	public GridBagConstraints get(int n) {
		if (n >= this.weigths.length)
			throw new IllegalArgumentException("The interface does not have as many components.");

        // Common constraints
        gbc.fill = GridBagConstraints.BOTH;
        if (this.isVertical) {
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
        } else {
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridy = 0;
            gbc.gridheight = GridBagConstraints.REMAINDER;
            gbc.weighty = 1.0;
        }
        
        if (this.isVertical) {
            gbc.gridy = n;
            gbc.weighty = this.weigths[n];
        } else {
            gbc.gridx = n;
            gbc.weightx = this.weigths[n];
        }
        return gbc;
	}

}

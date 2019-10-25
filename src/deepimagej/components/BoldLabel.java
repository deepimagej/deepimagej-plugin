package deepimagej.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

public class BoldLabel extends JLabel {

	public BoldLabel(String text) {
		super(text);
		Border b = BorderFactory.createEtchedBorder();
		Border m = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		setBorder(new CompoundBorder(b, m));
	}
	
	@Override
	public void setText(String text) {
		super.setText("<html><h3>&nbsp;" + text + "&nbsp;</h3></html>");
	}
	
}

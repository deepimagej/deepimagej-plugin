package deepimagej.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

public class BorderLabel extends JLabel {

	public BorderLabel(String text) {
		super("");
		Border b = BorderFactory.createEtchedBorder();
		Border m = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		setBorder(new CompoundBorder(b, m));
	}
}

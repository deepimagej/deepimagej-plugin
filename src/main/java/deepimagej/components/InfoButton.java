package deepimagej.components;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;

public class InfoButton extends JButton {

	public InfoButton() {
		super("<html><b>&#9432;</b></html>");
		setPreferredSize(new Dimension(25, 25));
		setBorder(BorderFactory.createEtchedBorder());
	}
}

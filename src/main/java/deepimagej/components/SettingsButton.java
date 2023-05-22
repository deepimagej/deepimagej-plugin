package deepimagej.components;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;

public class SettingsButton extends JButton {

	public SettingsButton() {
		super("<html><b>&#9881;</b></html>");
		setPreferredSize(new Dimension(25, 25));
		setBorder(BorderFactory.createEtchedBorder());
	}
}

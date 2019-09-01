package deepimagej.components;

import deepimagej.Constants;

public class TitleHTMLPane extends HTMLPane {

	public TitleHTMLPane() {
		super(Constants.width, 100);
		this.append("h1", "Deep ImageJ");
		this.append("p", "This user allows to buid a <i>deep</i> plugins for ImageJ");
		this.append("p", "bla bla");
	}
	
}

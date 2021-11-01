/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package deepimagej.components;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * This class extends the Java JEditorPane to make a easy to use panel to
 * display HTML information.
 * 
 * @author Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland.
 * 
 */
public class HTMLPane extends JEditorPane {

	private String		html		= "";
	private String		header		= "";
	private String		footer		= "";
	private Dimension	dim;
	private String		font		= "verdana";
	private String		color		= "#222222";
	private String		background	= "#f8f8f8";

	public HTMLPane() {
		create();
	}

	public HTMLPane(String font) {
		this.font = font;
		create();
	}

	public HTMLPane(int width, int height) {
		this.dim = new Dimension(width, height);
		create();
	}

	public HTMLPane(String font, int width, int height) {
		this.font = font;
		this.dim = new Dimension(width, height);
		create();
	}

	public HTMLPane(String font, String color, String background, int width, int height) {
		this.font = font;
		this.dim = new Dimension(width, height);
		this.color = color;
		this.background = background;
		create();
	}

	/* TODO
	// Allows wrap of the text
	@Override
	public boolean getScrollableTracksViewportWidth() {
		return getUI().getPreferredSize(this).width <= getParent().getSize().width;
	}
	*/

	public void enableHyperLink() {
		addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (IOException | URISyntaxException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});

	}

	@Override
	public String getText() {
		Document doc = this.getDocument();
		try {
			return doc.getText(0, doc.getLength());
		}
		catch (BadLocationException e) {
			e.printStackTrace();
			return getText();
		}
	}

	public void clear() {
		html = "";
		append("");
	}

	private void create() {
		header += "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\n";
		header += "<html><head>\n";
		header += "<style>body {background-color:" + background + "; color:" + color + "; font-family: " + font + ";margin:4px}</style>\n";
		header += "<style>h1 {color:#555555; font-size:1.0em; font-weight:bold; padding:1px; margin:1px;}</style>\n";
		header += "<style>h2 {color:#333333; font-size:0.9em; font-weight:bold; padding:1px; margin:1px;}</style>\n";
		header += "<style>h3 {color:#000000; font-size:0.9em; font-weight:italic; padding:1px; margin:1px;}</style>\n";
		header += "<style>p  {color:" + color + "; font-size:0.9em; padding:1px; margin:0px;}</style>\n";
		header += "</head>\n";
		header += "<body>\n";
		footer += "</body></html>\n";
		setEditable(false);
		setContentType("text/html; charset=ISO-8859-1");
	}

	public void append(String content) {
		html += content;
		setText(header + html + footer);
		if (dim != null) {
			setPreferredSize(dim);
		}
		setCaretPosition(0);
	}

	public void appendLink(String link, String content) {
		try {
			URL url = new URL(link);
			if (url != null)
				html += "<p><a href=\"" + link + "\">" + content + "</a></p>";

		} catch (IOException e) {
			html += "<p>" + content + ": " + link + "</p>";
		}
		setText(header + html + footer);
		if (dim != null) {
			setPreferredSize(dim);
		}
		setCaretPosition(0);
	}

	public void append(String tag, String content) {
		html += "<" + tag + ">" + content + "</" + tag + ">";
		setText(header + html + footer);
		if (dim != null) {
			setPreferredSize(dim);
		}
		setCaretPosition(0);
	}
	
	public void append(String tag1, String tag2, String content) {
		html += "<" + tag1 + ">" + "<" + tag2 + ">"+ content + "</" + tag2 + ">" + "</" + tag1 + ">";
		setText(header + html + footer);
		if (dim != null) {
			setPreferredSize(dim);
		}
		setCaretPosition(0);
	}

	public JScrollPane getPane() {
		JScrollPane scroll = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setPreferredSize(dim);
		return scroll;
	}
}
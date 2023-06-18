package deepimagej.components;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.Label;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import ij.gui.GenericDialog;

public class Hyperlink implements MouseListener {
	
	private String name;
	private Font font;
	private Label label;
	private GenericDialog dlg;
	
	private static final String JDLL_LINK = "https://arxiv.org/abs/2306.04796";
	
	private static final String DIJ_LINK = "https://www.nature.com/articles/s41592-021-01262-9";
	
	private Hyperlink(Label label, GenericDialog dlg) {
		name = label.getText();
		font = label.getFont();
		this.label = label;
		this.dlg = dlg;
	}
	
	public static Hyperlink createHyperlink(Label label, GenericDialog dlg) {
		return new Hyperlink(label, dlg);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (name.toLowerCase().contains("de haro")) {
			openWebpage(JDLL_LINK);
		} else if (name.toLowerCase().contains("de mariscal")) {
			openWebpage(DIJ_LINK);
		}
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
		fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		Font boldUnderline = new Font(this.font.getFontName(), Font.ITALIC,
				this.font.getSize()).deriveFont(fontAttributes);
		SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
				label.setFont(boldUnderline);
				label.repaint();
				label.revalidate();
				//dlg.repaint();
		    }});
	}

	@Override
	public void mouseExited(MouseEvent e) {
		label.setFont(this.font);
		
	}
	
	public static boolean openWebpage(URI uri) {
	    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
	    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
	        try {
	            desktop.browse(uri);
	            return true;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    return false;
	}

	public static boolean openWebpage(String url) {
	    try {
	        return openWebpage(new URL(url).toURI());
	    } catch (IOException | URISyntaxException e) {
	        e.printStackTrace();
	    }
	    return false;
	}

}

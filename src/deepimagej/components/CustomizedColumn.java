/*
 * bilib --- Java Bioimaging Library ---
 * 
 * Author: Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland
 * 
 * Conditions of use: You are free to use this software for research or
 * educational purposes. In addition, we expect you to include adequate
 * citations and acknowledgments whenever you present or publish results that
 * are based on it.
 */


package deepimagej.components;

/**
 * This class allows to customized the columns of the CustomizedTable tables.
 * 
 * @author Daniel Sage
 * 
 */
public class CustomizedColumn {

	private Class<?>		columnClasse; // usually it is a String
	private String	 	header;
	private boolean		editable;
	private int	     	width;
	private String[]		choices;	  // ComboBox
	private String	 	button;	     // Button
	private String	 	tooltip;

	public CustomizedColumn(String header, Class<?> columnClasse, int width, boolean editable) {
		this.columnClasse = columnClasse;
		this.header = header;
		this.width = width;
		this.editable = editable;
	}

	public CustomizedColumn(String header, Class<?> classe, int width, String[] choices, String tooltip) {
		this.columnClasse = classe;
		this.header = header;
		this.width = width;
		this.editable = true;
		this.choices = choices;
		this.tooltip = tooltip;
	}

	public CustomizedColumn(String header, Class<?> columnClasse, int width, String button, String tooltip) {
		this.columnClasse = columnClasse;
		this.header = header;
		this.width = width;
		this.editable = false;
		this.button = button;
		this.tooltip = tooltip;
	}

	public Class<?> getColumnClass() {
		return columnClasse;
	}
	
	public String getHeader() {
		return header;
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public int	getWidth()  {
		return width;
	}
	
	public String[] getChoices()  {
		return choices;
	}
	
	public String getButton()  {
		return button;
	} 
	
	public String getTooltip()  {
		return tooltip;
	} 
}

/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we strongly encourage you to include adequate citations and acknowledgments 
 * whenever you present or publish results that are based on it.
 * 
 * Reference: DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, L. Donati, M. Unser, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 *
 * Corresponding authors: mamunozb@ing.uc3m.es, daniel.sage@epfl.ch
 *
 */

/*
 * Copyright 2019. Universidad Carlos III, Madrid, Spain and EPFL, Lausanne, Switzerland.
 * 
 * This file is part of DeepImageJ.
 * 
 * DeepImageJ is an open source software (OSS): you can redistribute it and/or modify it under 
 * the terms of the BSD 2-Clause License.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 * You should have received a copy of the BSD 2-Clause License along with DeepImageJ. 
 * If not, see <https://opensource.org/licenses/bsd-license.php>.
 */package deepimagej.components;

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

/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we expect you to include adequate citations and acknowledgments whenever you 
 * present or publish results that are based on it.
 * 
 * Reference: DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ
 * E. Gómez-de-Mariscal, C. García-López-de-Haro, L. Donati, M. Unser, A. Muñoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique fédérale de Lausanne (EPFL), Switzerland
 *
 * Corresponding authors: mamunozb@ing.uc3m.es, daniel.sage@epfl.ch
 *
 */

/*
 * Copyright 2019. Universidad Carlos III, Madrid, Spain and EPFL, Lausanne, Switzerland.
 * 
 * This file is part of DeepImageJ.
 * 
 * DeepImageJ is free software: you can redistribute it and/or modify it under the terms of 
 * the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with DeepImageJ. 
 * If not, see <http://www.gnu.org/licenses/>.
 */
package additionaluserinterface;

import java.awt.*;
import javax.swing.*;
import java.text.*;
import javax.swing.event.*;
import javax.swing.table.*; 

/**
 * This class extends JFrame and draw a simple table. All values are in 2D double arrays 
 * 
 * @author Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland.
 *
 */  

public class NumericTable extends JFrame {

	private JTable table;
	private DefaultTableModel model;
	
	public NumericTable(String title, String[] headings, Dimension dim) {
		super(title);
		setMinimumSize(dim);
		setSize(dim);
		setPreferredSize(dim);
		
		JScrollPane pane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		model= new DefaultTableModel();
		table = new JTable(model);
		for (int i=0; i<headings.length; i++) {
			model.addColumn(headings[i]);
		}
		
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		pane.getViewport().add(table, null);
		add(pane);
	}

	public void setData(double data[][]) {
		int nrow = data.length;
		int ncol = data[0].length;
		String s[] = new String[ncol];
		for (int r=0; r<nrow; r++) {
			for (int c=0; c<ncol; c++) 
				s[c] = "" + data[r][c];
			model.addRow(s);
		}
	
	}

	public void setData(double data[][], String[] formats) {
		int nrow = data.length;
		int ncol = data[0].length;
		String s[] = new String[ncol];
		for (int r=0; r<nrow; r++) {
			for (int c=0; c<ncol; c++) 
				s[c] = (new DecimalFormat(formats[c])).format(data[r][c]);
			model.addRow(s);
		}
	
	}
	
	public void setColumnSize(int width[]) {
		for (int i=0; i<width.length; i++) {
			TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(width[i]);
		}
	}
	
	public void show(int posx, int posy) {
		pack();
		setLocation(new Point(posx, posy));
		setVisible(true);
	}
	
}

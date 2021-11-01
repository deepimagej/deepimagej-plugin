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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * This class build a table by extending JTable. Usually the constructor expects
 * a list of CustomizedColumn objects that defines the columns of the table.
 * 
 * @author Daniel Sage
 */

public class CustomizedTable extends JTable {

	private JScrollPane	                 pane	= null;
	private ArrayList<CustomizedColumn>	columns;

	public CustomizedTable(ArrayList<CustomizedColumn> columns, boolean sortable) {
		create(columns);
		setAutoCreateRowSorter(sortable);
		setRowHeight(20);
	}
	
	public CustomizedTable(String headers[], boolean sortable) {
		ArrayList<CustomizedColumn> colums = new ArrayList<CustomizedColumn>();
		for (int i = 0; i < headers.length; i++)
			colums.add(new CustomizedColumn(headers[i], String.class, 150, false));
		create(colums);
		setAutoCreateRowSorter(sortable);
		setRowHeight(20);
	}

	private void create(ArrayList<CustomizedColumn> column) {
		columns = column;
		DefaultTableModel model = new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int col) {
				return columns.get(col).isEditable();
			}

			@Override
			public Class<?> getColumnClass(int col) {
				return columns.get(col).getColumnClass();
			}
		};

		setModel(model);
		int n = columns.size();
		String headers[] = new String[n];
		for (int col = 0; col < n; col++)
			headers[col] = columns.get(col).getHeader();

		model.setColumnIdentifiers(headers);
		setFillsViewportHeight(true);

		for (int col = 0; col < n; col++) {
			TableColumn tc = getColumnModel().getColumn(col);
			tc.setPreferredWidth(columns.get(col).getWidth());

			if (columns.get(col).getChoices() != null) {
				JComboBox<String> cmb = new JComboBox<String>();
				for (String p : columns.get(col).getChoices()) {
					cmb.addItem(p);
					cmb.setToolTipText(columns.get(col).getTooltip());
					tc.setCellEditor(new DefaultCellEditor(cmb));
				}
			}
			if (columns.get(col).getButton() != null) {
				ButtonRenderer bn = new ButtonRenderer();
				bn.setToolTipText(columns.get(col).getTooltip());
				tc.setCellRenderer(bn);
			}
		}
		getTableHeader().setReorderingAllowed(false);
	}

	public void setPreferredSize(int width, int height) {
		if (pane != null)
			pane.setPreferredSize(new Dimension(width, height));
	}

	/**
	 * Removes one specify row from the table.
	 * 
	 * @param row	Row to remove
	 */
	public void removeRow(int row) {
		if (row >= 0 && row < getRowCount())
			((DefaultTableModel) getModel()).removeRow(row);
	}

	/**
	 * Removes all rows of the table.
	 */
	public void removeRows() {
		while (getRowCount() > 0)
			((DefaultTableModel) getModel()).removeRow(0);
	}

	public String[] getRow(int row) {
		if (row >= 0) {
			int ncol = getColumnCount();
			String items[] = new String[ncol];
			for (int col = 0; col < ncol; col++)
				items[col] = (String) getModel().getValueAt(row, col);
			return items;
		}
		return new String[1];
	}

	public String getCell(int row, int col) {
		if (row >= 0 && col >= 0) {
			return (String) getModel().getValueAt(row, col);
		}
		return "";
	}

	public void setCell(int row, int col, String value) {
		if (row >= 0 && col >= 0) {
			getModel().setValueAt(value, row, col);
		}
	}

	public String getRowCSV(int row, String seperator) {
		if (row >= 0) {
			int ncol = getColumnCount();
			String items = "";
			for (int col = 0; col < ncol - 1; col++) {
				if ((String) getModel().getValueAt(row, col) == null)
					items += "" + seperator;
				else
					items += (String) getModel().getValueAt(row, col) + seperator;
			}
			if (ncol >= 1)
				items += (String) getModel().getValueAt(row, ncol - 1);
			return items;
		}
		return "";
	}

	/**
	 * Saves the table in a CSV file
	 * 
	 * @param filename	Complete path and filename
	 */
	public void saveCSV(String filename) {
		File file = new File(filename);
		try {
			BufferedWriter buffer = new BufferedWriter(new FileWriter(file));
			int nrows = getRowCount();
			int ncols = getColumnCount();

			String row = "";
			for (int c = 0; c < columns.size(); c++)
				row += columns.get(c).getHeader() + (c == columns.size() - 1 ? "" : ", ");
			buffer.write(row + "\n");

			for (int r = 0; r < nrows; r++) {
				row = "";
				for (int c = 0; c < ncols; c++)
					row += this.getCell(r, c) + (c == ncols - 1 ? "" : ", ");
				buffer.write(row + "\n");
			}
			buffer.close();
		}
		catch (IOException ex) {
		}
	}

	public String getSelectedAtColumn(int col) {
		int row = getSelectedRow();
		if (row >= 0)
			return (String) getModel().getValueAt(row, col);
		else
			return "";
	}

	public void setSelectedAtColumn(int col, String selection) {
		int nrows = this.getRowCount();
		for (int i = 0; i < nrows; i++) {
			String name = (String) getModel().getValueAt(i, col);
			if (name.equals(selection))
				this.setRowSelectionInterval(i, i + 1);
		}
	}

	/**
	 * Add a row at the end of the table.
	 * 
	 * @param row
	 */
	public void append(Object[] row) {
		DefaultTableModel model = (DefaultTableModel) getModel();
		int i = 0;
		try {
			model.addRow(row);
			i = getRowCount() - 1;
			if (i >= 0) {
				getSelectionModel().setSelectionInterval(i, i);
				scrollRectToVisible(new Rectangle(getCellRect(i, 0, true)));
			}
		}
		catch (Exception e) {
		}
		repaint();
	}

	/**
	 * Add a row at the top of the table.
	 * 
	 * @param row
	 */
	public void insert(Object[] row) {
		DefaultTableModel model = (DefaultTableModel) getModel();
		int i = 0;
		try {
			model.insertRow(0, row);
			getSelectionModel().setSelectionInterval(i, i);
			scrollRectToVisible(new Rectangle(getCellRect(i, 0, true)));
		}
		catch (Exception e) {
		}
		repaint();
	}

	@Override
	public int getSelectedRow() {
		int row = super.getSelectedRow();
		if (row < 0) {
			if (getRowCount() > 0) {
				setRowSelectionInterval(0, 0);
				row = super.getSelectedRow();
			}
			return row;
		}
		return row;
	}

	/**
	 * Replaces all the content of the table by a content private as list of String[].
	 * 
	 * @param data
	 */
	public void update(ArrayList<String[]> data) {
		DefaultTableModel model = (DefaultTableModel) getModel();
		model.getDataVector().removeAllElements();
		for (String[] row : data)
			model.addRow(row);
		repaint();
	}

	public JScrollPane getPane(int width, int height) {
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		setPreferredScrollableViewportSize(new Dimension(width, height));
		setFillsViewportHeight(true);
		pane = new JScrollPane(this);
		return pane;
	}

	public JScrollPane getMinimumPane(int width, int height) {
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		setMinimumSize(new Dimension(width, height));
		setShowVerticalLines(true);
		setPreferredScrollableViewportSize(new Dimension(width, height));
		setFillsViewportHeight(true);
		return new JScrollPane(this);
	}

	public JFrame show(String title, int w, int h) {
		JFrame frame = new JFrame(title);
		frame.add(getPane(w, h));
		frame.pack();
		frame.setVisible(true);
		return frame;
	}

	public class ButtonRenderer extends JButton implements TableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			setText((String) value);
			setMargin(new Insets(1, 1, 1, 1));
			return this;
		}
	}

}

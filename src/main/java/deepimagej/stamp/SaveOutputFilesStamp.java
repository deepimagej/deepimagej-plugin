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

package deepimagej.stamp;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.Parameters;
import deepimagej.components.HTMLPane;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

public class SaveOutputFilesStamp extends AbstractStamp implements ActionListener {

	private JList<String>			openedList;
	private DefaultListModel<String> 	listModel;
	private JButton						refreshBtn = new JButton("Refresh");
	private HashMap<String, String> 	imOrResultsTable;
	private HTMLPane					info;

	public SaveOutputFilesStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	@Override
	public void buildPanel() {
		info = new HTMLPane(Constants.width, 70);
		info.append("h2", "Model output selection");
		info.append("p", "Select the images and tables that you want to save in"
				+ " the Bundled Model as the output of an example execution.");
		info.append("p", "<i>The input is saved automatically</i>.");
		listModel = new DefaultListModel<String>();
		listModel.addElement("example");
		openedList = new JList<String>(listModel);
		
		openedList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		openedList.setLayoutOrientation(JList.VERTICAL);
		openedList.setVisibleRowCount(-1);
		openedList.setSelectionModel(new DefaultListSelectionModel() {
		    @Override
		    public void setSelectionInterval(int index0, int index1) {
		        if(super.isSelectedIndex(index0)) {
		            super.removeSelectionInterval(index0, index1);
		        }
		        else {
		            super.addSelectionInterval(index0, index1);
		        }
		    }
		});
		JScrollPane listScroller = new JScrollPane(openedList);
		listScroller.setPreferredSize(new Dimension(Constants.width, panel.getPreferredSize().height));
		
		JPanel main = new JPanel(new BorderLayout());
		main.add(info.getPane(), BorderLayout.NORTH);
		main.add(listScroller, BorderLayout.CENTER);
		main.add(refreshBtn, BorderLayout.SOUTH);
		panel.add(main);
		
		refreshBtn.addActionListener(this);
	}

	@Override
	public void init() {
		updateOutputList();	
		openedList.revalidate();
		openedList.repaint();
		
	}
	
	@Override
	public boolean finish() {
		Parameters params = parent.getDeepPlugin().params;
		List<String> selections = openedList.getSelectedValuesList();
		
		if (selections.size() == 0) {
			IJ.error("You need to select at least one element from the list.");
			return false;
		} else {
			// Save the names of the outputs to save
			params.savedOutputs = new ArrayList<HashMap<String, String>>();
			for (String name : selections) {
				HashMap<String, String> out = new HashMap<String, String>();
				String type = imOrResultsTable.get(name);
				out.put("name", name);
				out.put("type", type);
				
				// Write info about the selected info. This info includes
				// the name, the type of object and the size
				String size = "";
				if (imOrResultsTable.get(name).contains("image")) {
					ImagePlus im = WindowManager.getImage(name);
					int[] dims = im.getDimensions();
					size = Integer.toString(dims[0]) + " x " + Integer.toString(dims[1]) + " x " + Integer.toString(dims[2]) + " x " + Integer.toString(dims[3]);
				} else if (imOrResultsTable.get(name).contains("ResultsTable")) {
					Frame f = WindowManager.getFrame(name);
			        if (f!=null && (f instanceof TextWindow)) {
			        	 ResultsTable resultstable = ((TextWindow)f).getResultsTable();
			        	 int cols = resultstable.getLastColumn() + 1;
			        	 if (cols == 0)
			        		 cols = 1;
						size = Integer.toString(resultstable.size()) + " x " + Integer.toString(resultstable.getLastColumn());
			        }
				}
				if (!size.equals("")) {
					out.put("size", size);
			    } else {
		        	IJ.error("Cannot save the " + imOrResultsTable.get(name) + " " + name + 
		        			"\nbecause it has already been closed.");
		        	updateOutputList();
		        	return false;
		        }
				
								
				params.savedOutputs.add(out);
			}
		}
		
		return true;
	}
	
	public void updateOutputList(){

		imOrResultsTable = new HashMap<String, String>();
		listModel = new DefaultListModel<String>();
		// Add the elements to the list
		String[] imageTitles = WindowManager.getImageTitles();
		for (String title : imageTitles) {
			listModel.addElement(title);
			imOrResultsTable.put(title, "image");
		}
		Frame[] nonImageWindows = WindowManager.getNonImageWindows();
		for (Frame f : nonImageWindows) {
	        if (f!=null && (f instanceof TextWindow)) {
	        	String tableTitle = f.getTitle();
	        	listModel.addElement(tableTitle);
				imOrResultsTable.put(tableTitle, "ResultsTable");
	        }
		}
		openedList.setModel(listModel);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == refreshBtn) {
			updateOutputList();
			openedList.revalidate();
			openedList.repaint();
		}
		
	}
}

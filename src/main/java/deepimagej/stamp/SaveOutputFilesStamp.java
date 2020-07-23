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
		info.append("h2", "Pyramidal Pooling Network selection");
		info.append("p", "Usually, complex architectures for which more than one input is "
				+ "required or for which the output is multidimensional. These networks are "
				+ "used in the detection, panoptic or instance segmentation, for which the "
				+ "combination of bounding boxes and segmentation is needed. The most famous "
				+ "examples are RetinaNet, R-CNN, Fast-RCNN, Mask-RCNN or PanopticFPN.");
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
				out.put("name", name);
				out.put("type", imOrResultsTable.get(name));
				
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

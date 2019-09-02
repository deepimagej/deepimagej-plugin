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
package deepimagej;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import additionaluserinterface.GridPanel;
import deepimagej.stamp.FileStamp;
import deepimagej.stamp.ModelStamp;
import deepimagej.stamp.PatchesStamp;
import deepimagej.stamp.PostprocessStamp;
import deepimagej.stamp.PreprocessStamp;
import deepimagej.stamp.SaveStamp;
import deepimagej.stamp.TensorStamp;
import deepimagej.Log;
import deepimagej.components.TitleHTMLPane;
import deepimagej.exceptions.IncorrectPatchSize;
import deepimagej.exceptions.InvalidTensorForm;
import deepimagej.exceptions.MacrosError;
import deepimagej.exceptions.TensorDimensionsException;
import deepimagej.tools.WebBrowser;
import deepimagej.Parameters;
import deepimagej.tools.XmlUtils;
import deepimagej.DeepPlugin;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GUI;

public class BuildDialog extends JDialog implements ActionListener {
	
	public JButton bnNext = new JButton("Load Model");
	public JButton bnBack = new JButton("Back");
	public JButton bnClose = new JButton("Close");
	public JButton bnHelp = new JButton("Help");
	public JButton bnTest = new JButton("Test");
	private JPanel pnCards	 = new JPanel(new CardLayout());
	
	private FileStamp file;
	private ModelStamp model;
	private TensorStamp tensor;
	private PatchesStamp patches;
	private PreprocessStamp preprocess;
	private PostprocessStamp postprocess;
	private SaveStamp save;
	private Parameters params = new Parameters(false, "", true);
	
	private DeepPlugin dp;
	private Log	log	= new Log();
	
	public BuildDialog() {
		super(new JFrame(), "Create Deep Plugin");
	}/*
	
		
*/	public void showDialog() {	
		file = new FileStamp(params, this, "Model file", log);
		model = new ModelStamp(params, this, "Model definition");
		tensor = new TensorStamp(params, this, "Tensor structure");
		patches = new PatchesStamp(params, this, "Patch organization");
		preprocess = new PreprocessStamp(params, this, "Preprocessing");
		postprocess = new PostprocessStamp(params,this, "Postprocessing");
		save = new SaveStamp(params, this, "Save Deep Plugin", log);

		GridPanel pnTitle = new GridPanel(true);
		pnTitle.place(1, 1, 5, 1, new TitleHTMLPane());

		JPanel pnButtons = new JPanel(new GridLayout(1,4));
		pnButtons.add(bnClose);
		pnButtons.add(bnHelp);
		pnButtons.add(bnBack);
		pnButtons.add(bnNext);
		pnButtons.add(bnTest);
		

		
		pnCards.add(file.getPanel(), "1");
		pnCards.add(model.getPanel(), "2");
		pnCards.add(tensor.getPanel(), "3");
		pnCards.add(patches.getPanel(), "4");
		pnCards.add(preprocess.getPanel(), "5");
		pnCards.add(postprocess.getPanel(), "6");
		pnCards.add(save.getPanel(), "7");

		setLayout(new BorderLayout());
		add(pnTitle, BorderLayout.NORTH);
		add(pnCards, BorderLayout.CENTER);
		add(pnButtons, BorderLayout.SOUTH);
		
		bnNext.addActionListener(this);
		bnBack.addActionListener(this);
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);
		bnTest.addActionListener(this);
		
		bnTest.setVisible(false);
		bnTest.setEnabled(false);
		
		setResizable(false);
		pack();
		GUI.center(this);
		setVisible(true);
		updateInterface();
	}

	private void setCard(String name) {
		CardLayout cl = (CardLayout) (pnCards.getLayout());
		cl.show(pnCards, name);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
			
		if (e.getSource() == bnNext) {
			// Remark that we are in the developer plugin for the specialç
			// parameters with respect to the user plugin
			params.developer = true;
			
			// First screen: load model. If the folder loaded
			// is a TensorFlow model, load the model and obtain
			// the parameters for the next screen
			if (params.card == 1) {
				dp = file.validate(dp);
				if (dp != null && dp.getValid() == true){
					// Initialise the parameters for the next screen
					model.init();
				}
			}
			
			if (params.card == 2) {
				try {
					// If the model is already loaded (== the model tag is known)
					if (dp.getModel() != null) {
						model.validate(dp);
						// Once the model is validated create the next screen.
						tensor.resetPanel();
						tensor.init();
					} else {
						// Get the model tag introduced by the user, load the model
						// with it and obtain teh SigDefs
						model.getTag(dp);
						params.card --;
					}
				} catch (TensorDimensionsException e1) {
					IJ.error("This model requires a tensor with more than 4 dimensions" + 
							 ". This kind of models are not supported by this version of the plugin" + 
							 ", only input tensors with 4 dimensions at most are supported");
					params.card = 0;
				}
			}

			if (params.card == 3) {
				try {
					tensor.validate();
					patches.init();
				} catch (IncorrectPatchSize e1) {
					IJ.error("Introuce a positivite into the patch size field.");
					// Substract one so in the next iteration, the program goes back to this stamp
					params.card --;
				} catch (NumberFormatException e1) {
					IJ.error("Introuce a positivite into the patch size field.");
					// Substract one so in the next iteration, the program goes back to this stamp
					params.card --;
				} catch (InvalidTensorForm e1) {
					IJ.error("The dimensions spedified should not repeat \nthemselves."
							+ " Each letter should appear just once.");
					// Substract one so in the next iteration, the program goes back to this stamp
					params.card --;
				}
			}
			
			if (params.card == 4) {
				try {
					patches.validate();
				} catch (IncorrectPatchSize e1) {
					IJ.error("Introuce a correct patch size.\n"
							+ "It has to be a multiple of " + params.minimumSize + ".");
					// Substract one so in the next iteration, the program goes back to this stamp
					params.card --;
				} catch (NumberFormatException e1) {
					IJ.error("Introuce a positivite INTEGER into the field.");
					// Substract one so in the next iteration, the program goes back to this stamp
					params.card --;
				}
			}
			
			if (params.card == 5) {
				preprocess.validate();
			}
			if (params.card == 6) {
				postprocess.validate();
				// Reset the parameter params.testResultImage
				// to avoid errors
				params.testResultImage = null;
			}

			if (params.card == 7) {
				
				save.validate(dp);
				
			}
			params.card = Math.min(7, params.card + 1);
			setCard(""+params.card);
		}
		if (e.getSource() == bnBack) {
			params.card = Math.max(1, params.card-1);
			setCard(""+params.card);
		}
		if (e.getSource() == bnClose) {
			dispose();
		}
		if (e.getSource() == bnHelp) {
			WebBrowser.open("http://bigwww.epfl.ch/");
		}
		if (e.getSource() == bnTest) {
			params.testResultImage = save.test(dp);
			//ImageProcessing.presentImage(params.testResultImage);
		}
			
		updateInterface();
	}
		

	public void updateInterface() {
		
		if (params.card == 1) {
			//bnNext.setEnabled(dp.getValid());
			bnNext.setText("Load Model");
			bnTest.setEnabled(false);
			bnTest.setVisible(false);
		}
		if (params.card == 2) {
			bnNext.setText("Next");
			bnTest.setEnabled(false);
			bnTest.setVisible(false);
		}
		if (params.card == 3) {
			bnNext.setEnabled(true);
			bnNext.setText("Next");
			bnTest.setEnabled(false);
			bnTest.setVisible(false);	
		}
		if (params.card == 4) {
			bnNext.setEnabled(true);
			bnNext.setText("Next");
			bnTest.setEnabled(false);
			bnTest.setVisible(false);
		}
		if (params.card == 5) {
			bnNext.setEnabled(true);
			bnNext.setText("Next");
			bnTest.setEnabled(false);
			bnTest.setVisible(false);
		}
		if (params.card == 6) {
			bnNext.setEnabled(true);
			bnNext.setText("Next");
			bnTest.setEnabled(false);
			bnTest.setVisible(false);
		}
		if (params.card == 7) {
			bnNext.setText("Save");
			bnTest.setEnabled(true);
			bnTest.setVisible(true);
			//bnNext.setEnabled(params.isTested);
		}
		
		bnBack.setEnabled(params.card !=1 );
	}

}

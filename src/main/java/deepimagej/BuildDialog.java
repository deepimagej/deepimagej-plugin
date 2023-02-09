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

package deepimagej;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import deepimagej.components.TitleHTMLPane;
import deepimagej.stamp.InputDimensionStamp;
import deepimagej.stamp.JavaPostprocessingStamp;
import deepimagej.stamp.JavaPreprocessingStamp;
import deepimagej.stamp.LoadPytorchStamp;
import deepimagej.stamp.InformationStamp;
import deepimagej.stamp.LoadTFStamp;
import deepimagej.stamp.OutputDimensionStamp;
import deepimagej.stamp.PtSaveStamp;
import deepimagej.stamp.SaveOutputFilesStamp;
import deepimagej.stamp.TfSaveStamp;
import deepimagej.stamp.SelectPyramidalStamp;
import deepimagej.stamp.TensorPytorchTmpStamp;
import deepimagej.stamp.TensorStamp;
import deepimagej.stamp.TestStamp;
import deepimagej.stamp.WelcomeStamp;
import deepimagej.tools.WebBrowser;
import ij.IJ;
import ij.gui.GUI;

public class BuildDialog extends JDialog implements ActionListener {

	public JButton				bnNext	= new JButton("Next");
	public JButton				bnBack	= new JButton("Back");
	public JButton				bnClose	= new JButton("Cancel");
	public JButton				bnHelp	= new JButton("Help");
	private JPanel				pnCards	= new JPanel(new CardLayout());

	private WelcomeStamp			welcome = null;
	private LoadTFStamp				loaderTf = null;
	private LoadPytorchStamp		loaderPt = null;
	private SelectPyramidalStamp	selectPyramid = null;
	private InputDimensionStamp		dim3 = null;
	private OutputDimensionStamp 	outputDim = null;
	private TensorStamp				tensorTf = null;
	private TensorPytorchTmpStamp	tensorPt = null;
	private InformationStamp		info = null;
	private JavaPreprocessingStamp  javaPreproc = null;
	private JavaPostprocessingStamp  javaPostproc = null;
	private TestStamp				test2 = null;
	private SaveOutputFilesStamp	outputSelection = null;
	private TfSaveStamp				tfSave = null;
	private PtSaveStamp				ptSave = null;
	private DeepImageJ				dp		= null;
	private int						card	= 1;
	private String					GPU_TF 	= "CPU";
	private String					GPU_PT 	= "CPU";
	private boolean					Fiji	= false;

	public BuildDialog() {
		super(new JFrame(), "Build Bundled Model [" + Constants.version + "]");
	}

	public void showDialog() {

		welcome = new WelcomeStamp(this);
		loaderTf = new LoadTFStamp(this);
		loaderPt = new LoadPytorchStamp(this);
		selectPyramid = new SelectPyramidalStamp(this);
		dim3 = new InputDimensionStamp(this);
		tensorTf = new TensorStamp(this);
		tensorPt = new TensorPytorchTmpStamp(this);
		info = new InformationStamp(this);
		outputDim = new OutputDimensionStamp(this);
		javaPreproc = new JavaPreprocessingStamp(this);
		javaPostproc = new JavaPostprocessingStamp(this);
		test2 = new TestStamp(this);
		outputSelection = new SaveOutputFilesStamp(this);
		tfSave = new TfSaveStamp(this);
		ptSave = new PtSaveStamp(this);

		JPanel pnButtons = new JPanel(new GridLayout(1, 4));
		pnButtons.add(bnClose);
		pnButtons.add(bnHelp);
		pnButtons.add(bnBack);
		pnButtons.add(bnNext);

		pnCards.add(welcome.getPanel(), "1");
		pnCards.add(loaderTf.getPanel(), "2");
		pnCards.add(loaderPt.getPanel(), "20");
		pnCards.add(selectPyramid.getPanel(), "3");
		pnCards.add(tensorTf.getPanel(), "4");
		pnCards.add(tensorPt.getPanel(), "40");
		pnCards.add(dim3.getPanel(), "5");
		pnCards.add(outputDim.getPanel(), "6");
		pnCards.add(info.getPanel(), "7");
		pnCards.add(javaPreproc.getPanel(), "8");
		pnCards.add(javaPostproc.getPanel(), "9");
		pnCards.add(test2.getPanel(), "10");
		pnCards.add(outputSelection.getPanel(), "11");
		pnCards.add(tfSave.getPanel(), "12");
		pnCards.add(ptSave.getPanel(), "120");

		setLayout(new BorderLayout());
		add(new TitleHTMLPane().getPane(), BorderLayout.NORTH);
		add(pnCards, BorderLayout.CENTER);
		add(pnButtons, BorderLayout.SOUTH);

		bnNext.addActionListener(this);
		bnBack.addActionListener(this);
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);

		setResizable(true);
		pack();
		setPreferredSize(new Dimension(Constants.width, 300));
		GUI.center(this);
		setVisible(true);
		bnBack.setEnabled(false);

		// Close model when the plugin is closed
		this.addWindowListener(new WindowAdapter() 
		{
		  public void windowClosed(WindowEvent e) {
			  // Release every component of each stamp
			  pnCards.removeAll();
			  removeAll();
			  if (dp == null)
				  return;
			  if (getDeepPlugin().getModel() != null) { 
				  getDeepPlugin().getModel().closeModel();
			  }
		  }
		  public void windowClosing(WindowEvent e) {
			  // Release every component of each stamp
			  pnCards.removeAll();
			  removeAll();
			  if (dp == null)
				  return;
			  if (getDeepPlugin().getModel() != null) { 
				  getDeepPlugin().getModel().closeModel();
			  }
		  }
		});

	}

	private void setCard(String name) {
		CardLayout cl = (CardLayout) (pnCards.getLayout());
		if (name.equals("2") && dp.params.framework.equals("pytorch"))
			name = "20";
		else if (name.equals("4") && dp.params.framework.equals("pytorch"))
			name = "40";
		else if (name.equals("12") && dp.params.framework.equals("pytorch"))
			name = "120";
		cl.show(pnCards, name);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		bnNext.setText("Next");
		bnNext.setEnabled(true);
		if (e.getSource() == bnNext) {
			switch (card) {
			case 1:
				if (welcome.finish()) {
					card = 2;
					String path = welcome.getModelDir();
					String name = welcome.getModelName();
					if (path != null) {
						dp = new DeepImageJ(path, name, true);
						if (getDeepPlugin().getModel() != null) {
							getDeepPlugin().getModel().closeModel();
						}
						if (dp != null) {
							dp.params.path2Model = path + File.separator + name + File.separator;
							if (dp.getValid() && dp.params.framework.contains("tensorflow")) {
								loaderTf.init();
							} else if (dp.getValid() && dp.params.framework.contains("pytorch")) {
								loaderPt.init();
							} else if (!dp.getValid()) {
								IJ.error("Please select a correct model");
								card = 1;
							}
						}
					}
				}
				break;
			case 2:
				if (dp.params.framework.contains("tensorflow")) {
					card = loaderTf.finish() ? card+1 : card;
				} else if (dp.params.framework.contains("pytorch")) {
					card = loaderPt.finish() ? card+1 : card;
				}
				break;
			case 3:
				card = selectPyramid.finish() ? card+1 : card;
				break;
			case 4:
				if (dp.params.framework.contains("tensorflow")) {
					card = tensorTf.finish() ? card+1 : card;
				} else if (dp.params.framework.contains("pytorch")) {
					card = tensorPt.finish() ? card+1 : card;
				}
				break;
			case 5:
				card = dim3.finish() ? card+1 : card;
				break;
			case 6:
				card = outputDim.finish() ? card+1 : card;
				break;
			case 7:
				card = info.finish() ? card+1 : card;
				break;
			case 8:
				card = javaPreproc.finish() ? card+1 : card;
				break;
			case 9:
				card = javaPostproc.finish() ? card+1 : card;
				break;
			case 11:
				card = outputSelection.finish() ? card+1 : card;
				break;
			case 12:
				dispose();
			default:
				card = Math.min(12, card + 1);
			}
		}
		if (e.getSource() == bnBack) {
			card = Math.max(1, card - 1);
		}
		if (e.getSource() == bnClose) {
			dispose();
		}
		if (e.getSource() == bnHelp) {
			WebBrowser.openDeepImageJ();
		}

		setCard("" + card);
		bnBack.setEnabled(card > 1);
		if (card == 4 && dp.params.framework.contains("tensorflow"))
			tensorTf.init();
		else if (card == 4 && dp.params.framework.contains("pytorch"))
			tensorPt.init();
		else if (card == 5)
			dim3.init();
		else if (card == 6)
			outputDim.init();
		else if (card == 7)
			info.init();
		else if (card == 8)
			javaPreproc.init();
		else if (card == 9)
			javaPostproc.init();
		else if (card == 10)
			test2.init();
		else if (card == 11)
			outputSelection.init();
		else if (card == 12)
			setEnabledBackNext(true);

		bnNext.setText(card == 12 ? "Finish" : "Next");
	}

	public void setEnabledBackNext(boolean b) {
		bnBack.setEnabled(b);
		bnNext.setEnabled(b);
	}

	public void setEnabledNext(boolean b) {
		bnNext.setEnabled(b);
	}

	public void setEnabledBack(boolean b) {
		bnBack.setEnabled(b);
	}

	public void endsTest() {
		bnBack.setEnabled(true);
		bnNext.setEnabled(true);
		bnNext.setText("Next");
	}
	
	public DeepImageJ getDeepPlugin() {
		return dp;
	}
	
	public String getGPUTf() {
		return GPU_TF;
	}
	
	public void setGPUTf(String info) {
		GPU_TF = info;
	}
	
	public String getGPUPt() {
		return GPU_PT;
	}
	
	public void setGPUPt(String info) {
		GPU_PT = info;
	}
	
	public boolean getFiji() {
		return Fiji;
	}
	
	public void setFiji(boolean fiji) {
		Fiji = fiji;
	}
}

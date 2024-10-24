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

import java.io.File;
import java.util.Map;

import javax.swing.SwingUtilities;

import deepimagej.IjAdapter;
import deepimagej.gui.Gui;
import ij.plugin.PlugIn;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.engine.installation.EngineInstall;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class DeepImageJ_Run implements PlugIn {
	
	private static final String ENGINES_DIR = new File("engines").getAbsolutePath();
	/**
	 * Message containing the references to the plugin
	 */
	private static final String REF_MSG = "References: Please cite the model developer and";
	/**
	 * Message containing the references to the plugin
	 */
	private static final String REF_1 = "[1] E. Gómez de Mariscal, DeepImageJ, Nature Methods, 2021";
	/**
	 * Message containing the references to the plugin
	 */
	private static final String REF_2 = "[2] C. García López de Haro, JDLL, arXiv, 2023";
	
	
	static public void main(String args[]) {
		new DeepImageJ_Run().run("");
	}

	Map<String, TwoParameterConsumer<String, Double>> consumersMap;
	@Override
	public void run(String arg) {
		long tt = System.currentTimeMillis();
		File modelsDir = new File("models");
		if (!modelsDir.isDirectory() && !modelsDir.mkdir())
			throw new RuntimeException("Unable to create 'models' folder inside ImageJ/Fiji directory. Please create it yourself.");
	    final Gui[] guiRef = new Gui[1];
	    if (SwingUtilities.isEventDispatchThread())
	    	guiRef[0] = new Gui(new IjAdapter());
	    else {
		    SwingUtilities.invokeLater(() -> {
		        guiRef[0] = new Gui(new IjAdapter());
		    });
	    }
	    /**
	    new Thread(() -> {
	        List<ModelDescriptor> models = ModelDescriptorFactory.getModelsAtLocalRepo(modelsDir.getAbsolutePath());
            if (guiRef[0] != null)
                guiRef[0].setModels(models);
            System.out.println(System.currentTimeMillis() - tt);
	    }).start();
	    */
	    
	    
	    new Thread(() -> {
	        EngineInstall installer = EngineInstall.createInstaller(ENGINES_DIR);
	        installer.checkBasicEngineInstallation();
	        consumersMap = installer.getBasicEnginesProgress();
	        installer.basicEngineInstallation();
	    }).start();
	    
	    new Thread(() -> {
	    	while (guiRef[0] == null || consumersMap == null) {
	    		try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return;
				}
	    	}
	        guiRef[0].trackEngineInstallation(consumersMap);
	    }).start();
	    
		
	}

}
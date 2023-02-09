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

package deepimagej.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.DeepLearningModel;
import ij.IJ;

public class YAMLUtils {
	
	public static Map<String, Object> readConfig(String yamlFile) {
		File initialFile = new File(yamlFile);
		InputStream targetStream = null;
	    try {
			targetStream = new FileInputStream(initialFile);
			Yaml yaml = new Yaml();
			Map<String, Object> obj = yaml.load(targetStream);
			targetStream.close();
			
			return obj;
		} catch (FileNotFoundException e) {
			IJ.error("Invalid YAML file");
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * Method to write the output of the yaml file. The fields written
	 * depend on the type of network that we are defining.
	 */
	public static Map<String, Object> getOutput(DijTensor out, boolean pyramidal, boolean allowPatching){
		Map<String, Object> outputTensorMap = new LinkedHashMap<>();
		outputTensorMap.put("name", out.name);
		
		if (!pyramidal && out.tensorType.contains("image")) {
			outputTensorMap.put("axes", out.form.toLowerCase());
			outputTensorMap.put("data_type", "float32");
			outputTensorMap.put("data_range", Arrays.toString(out.dataRange));
			outputTensorMap.put("halo",  Arrays.toString(out.halo));
			Map<String, Object> shape = new LinkedHashMap<>();
			shape.put("reference_tensor", out.referenceImage);
			shape.put("scale", Arrays.toString(out.scale));
			shape.put("offset", Arrays.toString(out.offset));
			outputTensorMap.put("shape", shape);
			
		} else if (pyramidal && out.tensorType.contains("image")) {
			outputTensorMap.put("axes", out.form.toLowerCase());
			outputTensorMap.put("data_type", "float32");
			outputTensorMap.put("data_range", Arrays.toString(out.dataRange));
			outputTensorMap.put("shape", Arrays.toString(out.sizeOutputPyramid));
			
		}else if (out.tensorType.contains("list")) {
			if (out.form.toLowerCase().contains("r"))
				out.form = out.form.toLowerCase().replace("r", "i");
			outputTensorMap.put("axes", out.form.toLowerCase());
			outputTensorMap.put("shape", Arrays.toString(out.tensor_shape));
			outputTensorMap.put("data_type", "float32");
			outputTensorMap.put("data_range", Arrays.toString(out.dataRange));
		}
		// TODO what to do with postprocesing
		outputTensorMap.put("postprocessing", null);
		return outputTensorMap;
	}
	
	public static void removeQuotes(File file) throws FileNotFoundException {

		Scanner scanner = new Scanner(file);       // create scanner to read

	    // do something with that line
	    String newLine = "";
		while(scanner.hasNextLine()){  // while there is a next line
		    String line = scanner.nextLine();  // line = that next line
		
		    // Replace Infinity by inf
		    line = line.replace("Infinity", "inf");
		    // Replace '-   ' by '  - ' and '- ' by '  - '
		    if (line.contains("-   ")) {
			    line = line.replace("-   ", "  - ");
		    } else if (line.contains("- ")) {
		    	line = line.replace("- ", "  - ");
		    }
		    // replace a character
		    for (int i = 0; i < line.length(); i++){
		        if (line.charAt(i) != '\'') {  // or anything other character you chose
		            newLine += line.charAt(i);
		        }
		    }
		    newLine += '\n';
		
		}
		scanner.close();
		PrintWriter writer = new PrintWriter(file.getAbsolutePath()); // create file to write to
		writer.print(newLine);
		writer.close();
	}
}
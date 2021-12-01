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

package deepimagej.processing;

import deepimagej.exceptions.MacrosError;
import ij.IJ;

/**
 * This class is used to retrieve the commands executed by anIJ Macro
 * or Pyimagej script and provide them to the plugin in a readable way
 * @author Carlos Garcia @carlosuc3m
 *
 */
public class HeadlessProcessing {
	
	private static String headlessModelsDirExtraVar = "models_dir";
	
	/**
	 * This method retrieves an array of Strings corresponding to the
	 * variable values defined in the macro
	 * @param macroArg: macro string with all the commands
	 * @param varNames: names of the variables of interest
	 * @return array with the values of the variables of interest
	 * @throws MacrosError: error showing which variable is missing
	 */
	public static String[] retrieveArguments(String macroArg, String[] varNames) throws MacrosError {
		// Decide what to do with the optional, only used for headless,
		// Macro parameter 'models_dir'
		String[] varNamesAux = adaptVars2HeadlessOptionalParam(varNames, macroArg);
		macroArg = adaptArgs2HeadlessOptionalParam(varNames, macroArg);
		// Now check that all the needed variables are present
		checkVariables(macroArg, varNamesAux);
		// Create an array that will contain the macro variables' values
		String[] varValues = new String[varNamesAux.length];
		
		// Parse the values from the macro String. It will have the form
		// of: "varName1= varValue1 varName2= varValue2..."
		for (int i = 0; i < varValues.length - 1; i ++) {
			// Index where the variable of interest starts
			int varInd = macroArg.indexOf(varNamesAux[i]);
			if (varInd == -1) {
				IJ.error("Invalid Macro call: missing argument '" + varNamesAux[i] + "'.");
				return null;
			}
			// Index where the following variable to the variable of interest starts	
			int nextVarInd = macroArg.indexOf(varNamesAux[i + 1]);
			varValues[i] = macroArg.substring(varInd + varNamesAux[i].length() + 1, nextVarInd).trim();
			// The macro recorder adds "[" and "]" when there is a black space in the command.
			// Delete it if it has been added
			int index = varValues[i].indexOf(' ');
			if (index != -1 && varValues[i].startsWith("[") && varValues[i].endsWith("]")) {
				varValues[i] = varValues[i].substring(1, varValues[i].length() - 1);
			}
		}
		// Get the value of the last variable
		varValues[varNamesAux.length - 1] = macroArg.substring(macroArg.lastIndexOf("=") + 1).trim();
		return varValues;
	}
	
	/**
	 * Check that all teh variables needed are present on the Macro call
	 * @param macroArg: macro argument used to call the plugin
	 * @param varNames: names of the variables needed
	 * @throws MacrosError: error showing which variable is missing
	 */
	public static void checkVariables(String macroArg, String[] varNames) throws MacrosError {
		for (String vv : varNames) {
			if (!macroArg.contains(vv))
				throw new MacrosError(vv);
		}
	}
	
	/**
	 * Read the variables and Macro arguments provided and arranges the variables
	 * so there are no conflicts with respect to the optional MAcro parameter 'models_dir'.
	 * If the 'models_dir' variable is  within the variables, but it is not in the Macro
	 * argument, remove it from the variables.
	 * 
	 * In order to use DeepImageJ using PyImageJ is preferable to provide the optional
	 * parameter 'models_dir' in the Macro argument as it avoids possible errors not 
	 * finding the models directory. This happens because with PyImageJ can launch 
	 * ImageJ/Fiji from any directory making it impossible to he plugin to find
	 * from which directory it has been launched. This optional parameter helps avoiding 
	 * this error providing the path to the models directory. HOwever, this parameter is
	 * optional, so some errors from bad practices can appear. This method tries to 
	 * avoid those errors
	 * @param varNames
	 * 	variable names expected
	 * @param macroArg
	 * 	argument provided in the macro
	 * @return the variables String adapted
	 */
	public static String[] adaptVars2HeadlessOptionalParam(String[] varNames, String macroArg) {
		// If the Macro argument does not contain the models_dir 
		// variable, remove it from the variables array to avoid errors
		boolean headlessVarInVars = varNames[varNames.length - 1].contentEquals(headlessModelsDirExtraVar);
		int modelsDirInd = macroArg.lastIndexOf(" " + headlessVarInVars + "=");
		String[] varNamesAux = null;
		if (headlessVarInVars && modelsDirInd == -1) {
			// If the models_dir variable is present in the variables but not
			// in the Macro argument, remove it from the variables
			varNamesAux = new String[varNames.length - 1];
			System.arraycopy(varNames, 0, varNamesAux, 0, varNamesAux.length);
		} else {
			// if not copy the variables into the auxiliary array
			varNamesAux = new String[varNames.length];
			System.arraycopy(varNames, 0, varNamesAux, 0, varNamesAux.length);
		}
		return varNamesAux;
	}
	
	/**
	 * Read the variables and Macro arguments provided and arranges the Macro args so 
	 * there are no conflicts with respect to the optional Macro parameter 'models_dir'.
	 * If the 'models_dir' variable is not within the variables, but it is in the Macro
	 * argument, remove it from the MAcro argument.
	 * 
	 * In order to use DeepImageJ using PyImageJ is preferable to provide the optional
	 * parameter 'models_dir' in the Macro argument as it avoids possible errors not 
	 * finding the models directory. This happens because with PyImageJ can launch 
	 * ImageJ/Fiji from any directory making it impossible to he plugin to find
	 * from which directory it has been launched. This optional parameter helps avoiding 
	 * this error providing the path to the models directory. HOwever, this parameter is
	 * optional, so some errors from bad practices can appear. This method tries to 
	 * avoid those errors
	 * @param varNames
	 * 	variable names expected
	 * @param macroArg
	 * 	argument provided in the macro
	 * @return the Macro argument adapted
	 */
	public static String adaptArgs2HeadlessOptionalParam(String[] varNames, String macroArg) {
		// If the Macro argument does not contain the models_dir 
		// variable, remove it from the variables array to avoid errors
		boolean headlessVarInVars = varNames[varNames.length - 1].contentEquals(headlessModelsDirExtraVar);
		int modelsDirInd = macroArg.lastIndexOf(" " + headlessVarInVars + "=");
		// If the Macro argument contains the models_dir variable
		// but the variable is not in the variables array, remove it
		// from the Macro argument to avoid errors
		if (!headlessVarInVars && modelsDirInd != -1) {
			macroArg = macroArg.substring(0, modelsDirInd);
		}
		return macroArg;
	}
}

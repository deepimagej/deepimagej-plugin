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
import java.awt.event.*;
import javax.swing.*;

/**
 * This class extends the generic JSpinner of Java for a 
 * specific JSpinner for float. It handles float type.
 * 
 * @author Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland.
 *
 */  
public class SpinnerFloat extends JSpinner {

	private SpinnerNumberModel	model;
	
	private float defValue;
	private float minValue;
	private float maxValue;
	private float incValue;
	
	/**
	* Constructor.
	*/
	public SpinnerFloat(float defValue, float minValue, float maxValue, float incValue) {
		super();
		this.defValue = defValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.incValue = incValue;
		
		Float def = new Float(defValue);
		Float min = new Float(minValue);
		Float max = new Float(maxValue);
		Float inc = new Float(incValue);
		model = new SpinnerNumberModel(def, min, max, inc);
		setModel(model);
	}
	
	/**
	* Set the minimal and the maximal limit.
	*/
	public void setLimit(float minValue, float maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		float value = get();
		Float min = new Float(minValue);
		Float max = new Float(maxValue);
		Float inc = new Float(incValue);
		defValue = (value > maxValue ? maxValue : (value < minValue ? minValue : value));
		Float def = new Float(defValue);
		model = new SpinnerNumberModel(def, min, max, inc);
		setModel(model);
	}
	
	/**
	* Set the incremental step.
	*/
	public void setIncrement(float incValue) {
		this.incValue = incValue;
		Float def = (Float)getModel().getValue();
		Float min = new Float(minValue);
		Float max = new Float(maxValue);
		Float inc = new Float(incValue);
		model = new SpinnerNumberModel(def, min, max, inc);
		setModel(model);
	}

	/**
	* Returns the incremental step.
	*/
	public float getIncrement() {
		return incValue;
	}
	
	/**
	* Set the value in the JSpinner with clipping in the range [min..max].
	*/
	public void set(float value) {
		value = (value > maxValue ? maxValue : (value < minValue ? minValue : value));
		model.setValue(value);
	}
	
	/**
	* Return the value without clipping the value in the range [min..max].
	*/
	public float get() {		
		if (model.getValue() instanceof Integer) {
			Integer i = (Integer)model.getValue();
			float ii = (float)i.intValue();
			return (ii > maxValue ? maxValue : (ii < minValue ? minValue : ii));
		}
		else if (model.getValue() instanceof Double) {
			Double i = (Double)model.getValue();
			float ii = (float)i.doubleValue();
			return (ii > maxValue ? maxValue : (ii < minValue ? minValue : ii));
		}
		else if (model.getValue() instanceof Float) {
			Float i = (Float)model.getValue();
			float ii = i.floatValue();
			return (ii > maxValue ? maxValue : (ii < minValue ? minValue : ii));
		}
		return 0f;
	}
	

}

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

import javax.swing.*;

/**
 * This class extends the generic JSpinner of Java for a 
 * specific JSpinner for integer. It handles int type.
 * 
 * @author Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland.
 *
 */  
public class SpinnerInteger extends JSpinner {

	private SpinnerNumberModel	model;
	
	private int defValue;
	private int minValue;
	private int maxValue;
	private int incValue;
	
	/**
	* Constructor.
	*/
	public SpinnerInteger(int defValue, int minValue, int maxValue, int incValue) {
		super();
		this.defValue = defValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.incValue = incValue;
		
		Integer def = new Integer(defValue);
		Integer min = new Integer(minValue);
		Integer max = new Integer(maxValue);
		Integer inc = new Integer(incValue);
		model = new SpinnerNumberModel(def, min, max, inc);
		setModel(model);
	}
	
	/**
	* Set the minimal and the maximal limit.
	*/
	public void setLimit(int minValue, int maxValue) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		int value = get();
		Integer min = new Integer(minValue);
		Integer max = new Integer(maxValue);
		Integer inc = new Integer(incValue);
		defValue = (value > maxValue ? maxValue : (value < minValue ? minValue : value));
		Integer def = new Integer(defValue);
		model = new SpinnerNumberModel(def, min, max, inc);
		setModel(model);
	}
	
	/**
	* Set the incremental step.
	*/
	public void setIncrement(int incValue) {
		this.incValue = incValue;
		Integer def = (Integer)getModel().getValue();
		Integer min = new Integer(minValue);
		Integer max = new Integer(maxValue);
		Integer inc = new Integer(incValue);
		model = new SpinnerNumberModel(def, min, max, inc);
		setModel(model);
	}

	/**
	* Returns the incremental step.
	*/
	public int getIncrement() {
		return incValue;
	}
	
	/**
	* Set the value in the JSpinner with clipping in the range [min..max].
	*/
	public void set(int value) {
		value = (value > maxValue ? maxValue : (value < minValue ? minValue : value));
		model.setValue(value);
	}
	
	/**
	* Return the value without clipping the value in the range [min..max].
	*/
	public int get() {
		if (model.getValue() instanceof Integer) {
			Integer i = (Integer)model.getValue();
			int ii = i.intValue();
			return (ii > maxValue ? maxValue : (ii < minValue ? minValue : ii));
		}
		else if (model.getValue() instanceof Double) {
			Double i = (Double)model.getValue();
			int ii = (int)i.doubleValue();
			return (ii > maxValue ? maxValue : (ii < minValue ? minValue : ii));
		}
		else if (model.getValue() instanceof Float) {
			Float i = (Float)model.getValue();
			int ii = (int)i.floatValue();
			return (ii > maxValue ? maxValue : (ii < minValue ? minValue : ii));
		}
		return 0;
	}
}

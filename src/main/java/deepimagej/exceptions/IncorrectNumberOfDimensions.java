package deepimagej.exceptions;

import java.util.Arrays;

public class IncorrectNumberOfDimensions extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	long[] shape;
	String dims;
	String name;
	
	public IncorrectNumberOfDimensions(long[] shape, String dims, String name) {
		this.shape = shape;
		this.dims = dims;
		this.name = name;
	}
	
	public String toString() {
		return "Only " + dims.length() + " dimensions specified (" + dims + 
				") for tensor '" + name + "' with " + shape.length + " dimensions and shape "
				+ Arrays.toString(shape);
	}
	
	public String getDims() {
		return this.dims;
	}
	
	public long[] getShape() {
		return this.shape;
	}
	
	public String getName() {
		return this.name;
	}

}
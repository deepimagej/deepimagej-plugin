package deepimagej.exceptions;

import java.util.Arrays;

// TODO remove when DeepImageJ batch sizes bigger than 1
public class BatchSizeBiggerThanOne extends Exception {
	/**
	 * Exception thrown when the output tensor has batch_size>1
	 */
	private static final long serialVersionUID = 1L;
	long[] shape;
	String dims;
	String name;
	
	public BatchSizeBiggerThanOne(long[] shape, String dims, String name) {
		this.shape = shape;
		this.dims = dims;
		this.name = name;
	}
	
	public String toString() {
		return "Output tensor '" + name + "' with tensor organization -> " + dims + ", and dimensions -> " + Arrays.toString(shape) +
				", has a batch of " + batchSize() + ". This version of DeepImageJ only supports batch sizes of 1.";
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
	
	public int batchSize() {
		int bInd = dims.indexOf("B");
		return (int) shape[bInd];
	}

}
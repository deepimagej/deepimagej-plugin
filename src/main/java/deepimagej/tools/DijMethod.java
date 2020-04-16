package deepimagej.tools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DijMethod {
	// For the moment only consider images as input
	// Name of the method
	public String 				name;
	// Method 
	public Method 				method;
	// Input arguments
	public List<DijVariable>	apply = new ArrayList<>();
	// Input arguments index in the list of variables
	public int[]				inputInd;
	// Output returns
	public DijVariable  		output;
	// Output returns index in the list of variables
	public int			  		outputInd;
	// Flag indicating if the method needs an image or not
	public int		  			needsImage = -1;
	
	public DijMethod(String name) {
		this.name = name;
	}
	
	public void addInput(DijVariable input) {
		this.apply.add(input);
		if (input.flag == "input_image") {
			this.needsImage = this.apply.size() - 1;
		}
	}
	
	public void setOutput(DijVariable output) {
		this.output = output;
	}

}

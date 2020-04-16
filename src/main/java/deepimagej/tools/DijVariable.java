package deepimagej.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DijVariable {
	// For the moment only consider images as input
	// Name of the input
	public String 		argumentName;
	// Variable type
	public String		type;
	// Variable flag
	public String		flag;
	// Value of the variables, in some cases it represents the name of the variable that
	// represents the actual data (normally tensors, arrays and imagesPlus) and in other cases
	// it contains the actual value ( for example if the value is an int parameter)
	public String		value;
	// Actual value of each variable
	public Object		memoryContent;
	public String[] 	flagList = {"yaml_defined", "developer_defined", "input_tensor", "output_tensor",
									"preprocessing_output", "postprocessing_output", "input_image"};
	
	public DijVariable(String value) {
		this.value = value;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setFlag(String flag) {
		this.flag = flag;
	}
	
	public void setVal(String value) {
		this.value = value;
	}
	
	public String getType() {
		return this.type;
	}
	
	public String getFlag() {
		return this.flag;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public static String[] retrieveTypes(List<DijVariable> allVars, String type) {
		List<String> argsType = new ArrayList<>();
		for (DijVariable var: allVars) {
			if (var.type.equals(type) == true) {
				argsType.add(var.value);
			}
		}
		if (argsType.isEmpty() == true) {
			argsType.add("");
		}
		String[] argsArray = Arrays.copyOf(argsType.toArray(), argsType.size(), String[].class);
		return argsArray;
	}
	
	public static DijVariable retrieveByName(String name, List<DijVariable> vars) {
		DijVariable wantedVar = null;
		for (DijVariable var: vars) {
			if (var.value.equals(name) == true) {
				wantedVar = var;
				break;
			}
		}
		return wantedVar;
	}
	
	public static int retrieveIndByName(String name, List<DijVariable> vars) {
		int ind = -1;
		for (DijVariable var: vars) {
			ind++;
			if (var.value.equals(name) == true) {
				break;
			}
		}
		return ind;
	}
	
	public static int retrieveNameInd(String name, List<DijVariable> vars) {
		DijVariable wantedVar = null;
		int count = -1;
		for (DijVariable var: vars) {
			if (var.value.equals(name) == true) {
				wantedVar = var;
				count++;
				break;
			}
		}
		return count;
	}
	
	public static String[] retrieveNames(List<DijVariable> allVars) {
		List<String> argsType = new ArrayList<>();
		for (DijVariable var: allVars) {
			argsType.add(var.value);
		}
		if (argsType.isEmpty() == true) {
			argsType.add("");
		}
		String[] argsArray = Arrays.copyOf(argsType.toArray(), argsType.size(), String[].class);
		return argsArray;
	}

}

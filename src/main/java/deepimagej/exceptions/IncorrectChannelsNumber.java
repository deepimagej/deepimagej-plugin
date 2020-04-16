package deepimagej.exceptions;

public class IncorrectChannelsNumber extends Exception {
	int needed_channels;
	int provided_channels;
	
	public IncorrectChannelsNumber(int x, int y) {
		needed_channels = x;
		provided_channels = y;
	}
	
	public String toString() {
		return "Image with " + provided_channels + 
				"channels provided instead of the required "
				+ provided_channels;
	}

}
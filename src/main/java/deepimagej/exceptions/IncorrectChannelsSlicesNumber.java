package deepimagej.exceptions;

public class IncorrectChannelsSlicesNumber extends Exception {
	int needed_channels;
	int provided_channels;
	String channelsOrSlices;
	
	public IncorrectChannelsSlicesNumber(int x, int y, String target) {
		needed_channels = x;
		provided_channels = y;
		channelsOrSlices = target;
	}
	
	public String toString() {
		return "Image with " + provided_channels + " " + channelsOrSlices +
				" provided instead of the required "
				+ provided_channels;
	}
	
	public String getExceptionType() {
		return this.channelsOrSlices;
	}

}
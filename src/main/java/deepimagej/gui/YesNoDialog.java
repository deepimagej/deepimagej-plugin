package deepimagej.gui;

import javax.swing.JOptionPane;

public class YesNoDialog {
	
    public static boolean askQuestion(String title, String message) {
        // Show the Yes/No dialog
        int response = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION ? true : false;
    }
}

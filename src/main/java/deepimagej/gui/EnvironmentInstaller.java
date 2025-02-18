package deepimagej.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import deepimagej.gui.workers.InstallEnvWorker;

public class EnvironmentInstaller extends JPanel {

    private static final long serialVersionUID = -459646711339061371L;
    
    private InstallEnvWorker worker;
    private Consumer<String> consumer;
    private JTextPane htmlPane;
    private JButton cancelButton;
    private Point initialClick;
    private int loadingCharInd = 0;
    
    private static final String[] LOADING_CHAR = new String[] {"/", "--", "\\", "|", "/", "--", "\\",};
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private static final String HTML_STYLE = ""
    		+ "<!DOCTYPE html>" + System.lineSeparator()
    		+ "<html lang=\"en\">" + System.lineSeparator()
    		+ "  <head>" + System.lineSeparator()
    		+ "    <meta charset=\"UTF-8\">" + System.lineSeparator()
    		+ "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + System.lineSeparator()
    		+ "    <title>Installation Console</title>" + System.lineSeparator()
    		+ "    <style>" + System.lineSeparator()
    		+ "      body {" + System.lineSeparator()
    		+ "        font-family: 'SF Mono', 'Fira Code', monospace;" + System.lineSeparator()
    		+ "      }" + System.lineSeparator()
    		+ "    </style>" + System.lineSeparator()
    		+ "  </head>" + System.lineSeparator()
    		+ "  <body>" + System.lineSeparator()
    		+ "    <div class=\"console-panel\">" + System.lineSeparator()
    		+ "      <h1>%s</h1>" + System.lineSeparator()
    		+ "    </div>" + System.lineSeparator()
    		+ "  </body>" + System.lineSeparator()
    		+ "</html>" + System.lineSeparator();
    
    private static final String LOADING_STR = "<p class='logline';'>%s -- Installation in progress...%s</p>";

    private static final String LOADING_REGEX = "<p class='logline';'>([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d -- Installation in progress\\.\\.\\.(.)</p>";


    private EnvironmentInstaller(InstallEnvWorker worker) {
    	this.worker = worker;
        
        consumer = (str) -> {
        	updateText(str, Color.BLACK);
        	System.out.println(str);
        };
        worker.setConsumer(consumer);
        setLayout(new BorderLayout());

        // Set up the HTML pane
        htmlPane = new JTextPane();
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false); 
        htmlPane.setText(String.format(HTML_STYLE, worker.getDescriptor().getModelFamily() ));
        // Add HTML content pane
        JScrollPane scrollPane = new JScrollPane(htmlPane);
        add(scrollPane, BorderLayout.CENTER);

        // Set up Cancel button
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelInstallation());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Enable dragging of the window
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                // Get the new location of the window
                int x = e.getXOnScreen() - initialClick.x;
                int y = e.getYOnScreen() - initialClick.y;
                SwingUtilities.getWindowAncestor(EnvironmentInstaller.this).setLocation(x, y);
            }
        });
    }
    
    public static EnvironmentInstaller create(InstallEnvWorker worker) {
        EnvironmentInstaller installer = new EnvironmentInstaller(worker);
        return installer;
    }
    
    public Consumer<String> getConsumer(){
    	return consumer;
    }
    
    public void cancelInstallation() {
        worker.stopBackground();
        // Close the installation window
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    public void addToFrame(JDialog parentFrame) {
    	// Create the installer panel and show it in a floating window
        //frame.setTitle("Installing " + descriptor.getName());
        //frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    	parentFrame.setContentPane(this);
        //frame.setSize(400, 300);
        //frame.setUndecorated(true); // Optionally remove window decorations (title bar)
    	parentFrame.setLocationRelativeTo(null); // Center the window

        // Handle closing via the red X by adding a window listener to the JDialog
    	parentFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelInstallation(); // Call cancelInstallation when window is closed
            }
        });

    	parentFrame.setVisible(true);
    }

	// Method to append text with a specific color to the JEditorPane
    private void updateText(String text, Color color) {
    	text = text.trim();
    	if (text.equals("") || text.equals(null))
    		updateWait();
    	else
    		appendText(text, color);
    }
    
    private void appendText(String text, Color color) {
    	formatInput(text);
        // Get the editor kit and document from the htmlPane.
        HTMLEditorKit kit = (HTMLEditorKit) htmlPane.getEditorKit();
        HTMLDocument doc = (HTMLDocument) htmlPane.getDocument();
        
        // Convert the Color to a hex string (e.g., "#ff0000")
        String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        
        // Create an HTML snippet that uses the pre-defined class and overrides the color.
        String htmlSnippet = "<p class='logline' style='color:" + hexColor + ";'>" + text + "</p>";
        
        try {
            // Insert the HTML snippet at the end of the document.
            kit.insertHTML(doc, doc.getLength(), htmlSnippet, 0, 0, null);
        } catch (BadLocationException | IOException e) {
            e.printStackTrace();
        }
        
        // Optionally, scroll the pane to the end.
        htmlPane.setCaretPosition(doc.getLength());
    }
    
    private void updateWait() {
        HTMLEditorKit kit = (HTMLEditorKit) htmlPane.getEditorKit();
        HTMLDocument doc = (HTMLDocument) htmlPane.getDocument();
        
        try {
            // Get the current document text.
            String fullText = doc.getText(0, doc.getLength());
            int containerStart = fullText.indexOf("<div class=\"console-panel\">" + System.lineSeparator());
            int containerEnd   = fullText.indexOf("</div>", containerStart);
            if (containerStart < 0 || containerEnd < 0) {
                return;
            }
            Pattern pattern = Pattern.compile(LOADING_REGEX);
            Matcher matcher = pattern.matcher(fullText);
            int lastMatchStart = -1;
            int lastMatchEnd = -1;
            while (matcher.find()) {
                // Save the position of the captured group for the last match.
                lastMatchStart = matcher.start(1);
                lastMatchEnd = matcher.end(1);
            }
            String loadingString = String.format(LOADING_STR, LocalTime.now().format(FORMATTER), getLoadingChar());
            if (lastMatchEnd != -1 && lastMatchEnd + 1 == containerEnd) {
                doc.remove(lastMatchStart, loadingString.length());
            }
            
            
            // Insert the new HTML snippet at the insertion point.
            kit.insertHTML(doc, containerEnd, loadingString, 0, 0, null);
        } catch (BadLocationException | IOException e) {
            e.printStackTrace();
        }
        
        // Scroll the pane to the end if desired.
        htmlPane.setCaretPosition(doc.getLength());
    }
    
    private String getLoadingChar() {
    	loadingCharInd = loadingCharInd % LOADING_CHAR.length;
    	String sel = LOADING_CHAR[loadingCharInd];
    	loadingCharInd ++;
    	return sel;
    }
    
    private static String formatInput(String text) {
        if (text.matches("^\\d{2}:\\d{2}:\\d{2}\\s--.*"))
        	return text;
        LocalTime now = LocalTime.now();
        return now.format(FORMATTER) + " -- " + text;
    }
}

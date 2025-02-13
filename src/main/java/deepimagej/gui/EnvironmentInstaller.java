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
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor;

public class EnvironmentInstaller extends JPanel {

    private static final long serialVersionUID = -459646711339061371L;
    private ModelDescriptor descriptor;
	private CountDownLatch latch;
    private Thread parentThread;
    private Consumer<String> consumer;
    private JEditorPane htmlPane;
    private JButton cancelButton;
    private Point initialClick;

    private EnvironmentInstaller(ModelDescriptor descriptor, CountDownLatch latch, Thread parentThread) {
    	this.descriptor = descriptor;
        this.latch = latch;
        this.parentThread = parentThread;
        
        consumer = (str) -> {
        	appendText(str, Color.BLACK);
        	System.out.println(str);
        };

        setLayout(new BorderLayout());

        // Set up the HTML pane
        htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setText("<html><body><h1>Installation in Progress</h1><p>Installing " + descriptor.getName() + "...</p></body></html>");
        htmlPane.setEditable(false); // Disable editing

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
    
    public static EnvironmentInstaller create(ModelDescriptor descriptor, CountDownLatch latch, Thread parentThread) {
        EnvironmentInstaller installer = new EnvironmentInstaller(descriptor, latch, parentThread);
        return installer;
    }
    
    public ModelDescriptor getDescriptor() {
    	return this.descriptor;
    }

    public CountDownLatch getCountDownLatch() {
    	return this.latch;
    }
    
    public Thread getReferenceThread() {
    	return this.parentThread;
    }
    
    public Consumer<String> getConsumer(){
    	return consumer;
    }
    
    public void cancelInstallation() {
        if (parentThread != null && parentThread.isAlive()) {
            parentThread.interrupt(); // Interrupt the installation thread
        }
        latch.countDown(); // Decrement the latch
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
    private void appendText(String text, Color color) {
        // Get the current HTML content
        String currentText = htmlPane.getText();

        // Convert the text to HTML with the specified color
        String colorCode = (color == Color.RED) ? "red" : "black";
        String newText = "<p style=\"color:" + colorCode + ";\">" + text + "</p>";

        // Append the new text to the current content
        String updatedText = currentText.substring(0, currentText.lastIndexOf("</body>")) + newText + "</body></html>";
        htmlPane.setText(updatedText);
        htmlPane.setCaretPosition(updatedText.length());
    }
    
    public void install() {
    	
    }
}

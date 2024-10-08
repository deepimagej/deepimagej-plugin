package deepimagej.gui

import ij.plugin.frame.PlugInFrame;
import java.awt.*;
import java.net.URL;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.util.Arrays;
import java.util.List;

public class DeepImageJPluginUI extends PlugInFrame {

    private int currentIndex = 1;
    private List<String> modelNames = Arrays.asList("Model A", "Model B", "Model C");
    private List<String> modelNicknames = Arrays.asList("Nickname A", "Nickname B", "Nickname C");
    private List<URL> modelImagePaths = Arrays.asList(getClass().getResource("deepimagej_icon.png"), 
											    		getClass().getResource("deepimagej_icon.png"), 
											    		getClass().getResource("deepimagej_icon.png"));

    private JPanel prevModelPanel;
    private JPanel selectedModelPanel;
    private JPanel modelSelectionPanel;
    private JPanel nextModelPanel;
    private JPanel modelCarouselPanel;
    private JPanel modelCardPanel;
    private JPanel titlePanel;
    private JPanel footerPanel;

    private JLabel exampleImageLabel;
    private JTextArea modelInfoArea;

    private final double CARR_VRATIO = 0.34;
    private final double SELECTION_PANE_VRATIO = 0.35;
    private final double ARROWS_VRATIO = 0.1;
    private final double TITLE_VRATIO = 0.15;
    private final double TITLE_LOGO_VRATIO = 0.1;
    private final double TITLE_LOGO_HRATIO = 1.0 / 7;
    private final double MODEL_VRATIO = 0.4;
    private final double FOOTER_VRATIO = 0.1;

    public DeepImageJPluginUI() {
        super("DeepImageJ Plugin");
        setSize(800, 900);
        setLayout(new BorderLayout());

        // Initialize UI components
        initTitlePanel();
        initMainContentPanel();
        initFooterPanel();

        setVisible(true);
    }

    private void initTitlePanel() {
        // Set up the title panel
        titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setBackground(Color.GRAY);
        titlePanel.setBorder(new LineBorder(Color.BLACK, 5, true));
        titlePanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * TITLE_VRATIO)));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (getHeight() * TITLE_LOGO_VRATIO);
        int logoWidth = (int) (getWidth() * TITLE_LOGO_HRATIO);

        // Create logo label with the specified size
        ImageIcon logoIcon = new ImageIcon(new ImageIcon(getClass().getResource("deepimagej_icon.png"))
                .getImage().getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH));
        JLabel logoLabel = new JLabel(logoIcon);

        // Title label
        JLabel titleLabel = new JLabel("deepImageJ");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Subtitle label
        JLabel subtitleLabel = new JLabel("The Fiji/ImageJ Plugin for AI");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Panel for title and subtitle
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        // Create a wrapper panel to hold logo and textPanel inline
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setOpaque(false);

        // GridBagConstraints for the logo
        GridBagConstraints logoGbc = new GridBagConstraints();
        logoGbc.gridx = 0;
        logoGbc.gridy = 0;
        logoGbc.anchor = GridBagConstraints.WEST;
        logoGbc.insets = new Insets(0, 0, 0, this.getWidth() / 80);
        wrapperPanel.add(logoLabel, logoGbc);

        // GridBagConstraints for the text panel
        GridBagConstraints textGbc = new GridBagConstraints();
        textGbc.gridx = 1;
        textGbc.gridy = 0;
        textGbc.anchor = GridBagConstraints.WEST;
        wrapperPanel.add(textPanel, textGbc);

        // Add the wrapperPanel to the titlePanel with custom constraints
        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.gridx = 0;
        wrapperGbc.gridy = 0;
        wrapperGbc.anchor = GridBagConstraints.CENTER;
        wrapperGbc.insets = new Insets(0, -logoWidth, 0, 0);
        titlePanel.add(wrapperPanel, wrapperGbc);

        // Add the title panel to the frame's NORTH section
        add(titlePanel, BorderLayout.NORTH);
    }

    private void initMainContentPanel() {
        // Create a main content panel with vertical BoxLayout
        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.setBackground(Color.WHITE);

        // Add the model selection panel and content panel to the main content panel
        mainContentPanel.add(initModelSelectionPanel());
        mainContentPanel.add(initContentPanel());

        // Add the main content panel to the frame's CENTER region
        add(mainContentPanel, BorderLayout.CENTER);
    }

    private JPanel initModelSelectionPanel() {
        modelSelectionPanel = new JPanel();
        modelSelectionPanel.setLayout(new BoxLayout(modelSelectionPanel, BoxLayout.Y_AXIS));
        modelSelectionPanel.setBackground(new Color(236, 240, 241));
        Border lineBorder = BorderFactory.createLineBorder(Color.gray, 2, true); // 2-pixel thick line border
        Border paddingBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5); // 10-pixel padding around the content
        modelSelectionPanel.setBorder(BorderFactory.createCompoundBorder(paddingBorder,lineBorder));
        modelSelectionPanel.setSize(new Dimension(getWidth(), (int) (getHeight() * SELECTION_PANE_VRATIO)));

        modelCarouselPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        modelCarouselPanel.setBackground(new Color(236, 240, 241));
        modelCarouselPanel.setSize(new Dimension(getWidth(), (int) (this.getHeight() * CARR_VRATIO)));

        prevModelPanel = createModelCard(modelNames.get(getWrappedIndex(currentIndex - 1)),
                                         modelImagePaths.get(getWrappedIndex(currentIndex - 1)),
                                         modelNicknames.get(getWrappedIndex(currentIndex - 1)), 0.8f);
        selectedModelPanel = createModelCard(modelNames.get(currentIndex),
                                             modelImagePaths.get(currentIndex),
                                             modelNicknames.get(currentIndex), 1.0f);
        nextModelPanel = createModelCard(modelNames.get(getWrappedIndex(currentIndex + 1)),
                                         modelImagePaths.get(getWrappedIndex(currentIndex + 1)),
                                         modelNicknames.get(getWrappedIndex(currentIndex + 1)), 0.8f);

        modelCarouselPanel.add(prevModelPanel);
        modelCarouselPanel.add(selectedModelPanel);
        modelCarouselPanel.add(nextModelPanel);

        JButton prevButton = new JButton("◀");
        prevButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        prevButton.addActionListener(e -> updateCarousel(-1));
        prevButton.setPreferredSize(new Dimension(this.getWidth() / 2, (int) (getHeight() * SELECTION_PANE_VRATIO * ARROWS_VRATIO)));

        JButton nextButton = new JButton("▶");
        nextButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        nextButton.addActionListener(e -> updateCarousel(1));
        nextButton.setPreferredSize(new Dimension(this.getWidth() / 2, (int) (getHeight() * SELECTION_PANE_VRATIO * ARROWS_VRATIO)));

        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.X_AXIS));
        navigationPanel.setPreferredSize(new Dimension(this.getWidth(), (int) (getHeight() * SELECTION_PANE_VRATIO * ARROWS_VRATIO)));
        navigationPanel.setBackground(new Color(236, 240, 241));
        navigationPanel.add(prevButton);
        navigationPanel.add(Box.createHorizontalGlue());
        navigationPanel.add(nextButton);

        modelSelectionPanel.add(modelCarouselPanel);
        modelSelectionPanel.add(navigationPanel);

        // Return the modelSelectionPanel
        return modelSelectionPanel;
    }

    private JPanel initContentPanel() {
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * MODEL_VRATIO)));

        // Example Image Panel
        JPanel exampleImagePanel = new JPanel(new BorderLayout());
        exampleImagePanel.setBackground(Color.WHITE);

        JLabel exampleTitleLabel = new JLabel("Example Image", JLabel.CENTER);
        exampleTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = new ImageIcon(new ImageIcon(getClass().getResource("deepimagej_icon.png"))
                .getImage().getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH));
        exampleImageLabel = new JLabel(logoIcon, JLabel.CENTER);
        exampleImagePanel.add(exampleTitleLabel, BorderLayout.NORTH);
        exampleImagePanel.add(exampleImageLabel, BorderLayout.CENTER);

        // Model Info Panel
        JPanel modelInfoPanel = new JPanel(new BorderLayout());
        modelInfoPanel.setBackground(Color.WHITE);

        JLabel infoTitleLabel = new JLabel("Model Information", JLabel.CENTER);
        infoTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        modelInfoArea = new JTextArea("Detailed model description...");
        modelInfoArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        modelInfoArea.setLineWrap(true);
        modelInfoArea.setWrapStyleWord(true);
        modelInfoArea.setEditable(false);
        JScrollPane infoScrollPane = new JScrollPane(modelInfoArea);

        modelInfoPanel.add(infoTitleLabel, BorderLayout.NORTH);
        modelInfoPanel.add(infoScrollPane, BorderLayout.CENTER);

        contentPanel.add(exampleImagePanel);
        contentPanel.add(modelInfoPanel);

        // Return the contentPanel
        return contentPanel;
    }

    private void initFooterPanel() {
        footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(45, 62, 80));
        footerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        footerPanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * FOOTER_VRATIO)));

        JPanel runButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        runButtonPanel.setBackground(new Color(45, 62, 80));

        JButton runOnTestButton = new JButton("Run on Test");
        JButton runButton = new JButton("Run");

        styleButton(runOnTestButton);
        styleButton(runButton);

        runButtonPanel.add(runOnTestButton);
        runButtonPanel.add(runButton);

        JLabel copyrightLabel = new JLabel("© 2024 deepImageJ - Version 1.0");
        copyrightLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        copyrightLabel.setForeground(Color.WHITE);

        footerPanel.add(runButtonPanel, BorderLayout.CENTER);
        footerPanel.add(copyrightLabel, BorderLayout.SOUTH);

        add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createModelCard(String modelName, URL imagePath, String modelNickname, float scale) {
        modelCardPanel = new JPanel(new BorderLayout());
        int cardHeight = (int) (getHeight() * CARR_VRATIO * 0.9);
        int cardWidth = getWidth() / 3;
        modelCardPanel.setPreferredSize(new Dimension((int) (cardWidth * scale), (int) (cardHeight * scale)));
        modelCardPanel.setBackground(Color.WHITE);
        modelCardPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Calculate dimensions for the logo based on the main interface size
        int logoHeight = (int) (getHeight() * CARR_VRATIO / 3);
        int logoWidth = getWidth() / 4;
        ImageIcon logoIcon = new ImageIcon(new ImageIcon(getClass().getResource("deepimagej_icon.png"))
                .getImage().getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH));
        JLabel modelImageLabel = new JLabel(logoIcon, JLabel.CENTER);

        JLabel modelNameLabel = new JLabel(modelName, JLabel.CENTER);
        modelNameLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (16 * scale)));
        JLabel modelNicknameLabel = new JLabel(modelNickname, JLabel.CENTER);
        modelNicknameLabel.setFont(new Font("SansSerif", Font.ITALIC, (int) (14 * scale)));

        modelCardPanel.add(modelImageLabel, BorderLayout.CENTER);
        modelCardPanel.add(modelNameLabel, BorderLayout.NORTH);
        modelCardPanel.add(modelNicknameLabel, BorderLayout.SOUTH);

        return modelCardPanel;
    }

    private void updateCarousel(int direction) {
        modelSelectionPanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * SELECTION_PANE_VRATIO)));
        modelCarouselPanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * CARR_VRATIO)));
        modelCardPanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * CARR_VRATIO)));
        titlePanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * TITLE_VRATIO)));
        footerPanel.setSize(new Dimension(this.getWidth(), (int) (this.getHeight() * FOOTER_VRATIO)));
        currentIndex = getWrappedIndex(currentIndex + direction);

        // Update model panels
        modelCarouselPanel.removeAll();

        prevModelPanel = createModelCard(modelNames.get(getWrappedIndex(currentIndex - 1)),
                modelImagePaths.get(getWrappedIndex(currentIndex - 1)),
                modelNicknames.get(getWrappedIndex(currentIndex - 1)), 0.8f);
        selectedModelPanel = createModelCard(modelNames.get(currentIndex),
                modelImagePaths.get(currentIndex),
                modelNicknames.get(currentIndex), 1.0f);
        nextModelPanel = createModelCard(modelNames.get(getWrappedIndex(currentIndex + 1)),
                modelImagePaths.get(getWrappedIndex(currentIndex + 1)),
                modelNicknames.get(getWrappedIndex(currentIndex + 1)), 0.8f);

        modelCarouselPanel.add(prevModelPanel);
        modelCarouselPanel.add(selectedModelPanel);
        modelCarouselPanel.add(nextModelPanel);


        modelCarouselPanel.revalidate();
        modelCarouselPanel.repaint();

        // Update example image and model info
        int logoHeight = (int) (getHeight() * 0.3);
        int logoWidth = getWidth() / 3;
        ImageIcon logoIcon = new ImageIcon(new ImageIcon(getClass().getResource("deepimagej_icon.png"))
                .getImage().getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH));
        exampleImageLabel.setIcon(logoIcon);
        modelInfoArea.setText("Detailed information for " + modelNames.get(currentIndex));
    }

    private int getWrappedIndex(int index) {
        int size = modelNames.size();
        return (index % size + size) % size;
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(new Color(52, 152, 219));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DeepImageJPluginUI());
    }
}

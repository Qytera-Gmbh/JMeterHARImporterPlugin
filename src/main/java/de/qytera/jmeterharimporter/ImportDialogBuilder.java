package de.qytera.jmeterharimporter;

import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarRequest;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * Builds and displays the dialog for importing a HAR file into JMeter.
 */
public class ImportDialogBuilder {
    private static final Logger LOGGER = Logger.getLogger(ImportDialogBuilder.class.getName());

    private static final int DIALOG_WIDTH = 500;
    private static final int DIALOG_HEIGHT = 400;
    private static final int ELEMENT_HEIGHT = 30;

    private final JDialog dialog;
    private final JTextField harInputField = new JTextField();
    private final JPanel ignoredHostsPanel = new JPanel();
    private final JButton importButton = new JButton("Import");
    private final JCheckBox addTimerCheckbox = new JCheckBox("Add Recorded Waiting Time");
    private final JCheckBox addHeaderCheckbox = new JCheckBox("Add Recorded Headers");
    private final JCheckBox addCookiesCheckbox = new JCheckBox("Add Recorded Cookies");

    private final Map<String, JCheckBox> hostCheckboxes = new HashMap<>();
    private Har har;

    public ImportDialogBuilder() {
        dialog = new JDialog();
        dialog.setTitle("Import HAR File");
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));

        JPanel form = buildForm();
        dialog.add(form);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        GridBagConstraints gbc = new GridBagConstraints();

        addFileSelector(form, gbc);
        addHostExclusionPanel(form, gbc);
        addOptionsPanel(form, gbc);
        addActionButtons(form, gbc);

        return form;
    }

    private void addFileSelector(JPanel form, GridBagConstraints gbc) {
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.setBorder(BorderFactory.createTitledBorder("HAR File"));

        harInputField.setPreferredSize(new Dimension(DIALOG_WIDTH * 4 / 5, ELEMENT_HEIGHT));
        filePanel.add(harInputField, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse...");
        browseButton.setPreferredSize(new Dimension(DIALOG_WIDTH / 5, ELEMENT_HEIGHT));
        browseButton.addActionListener(e -> chooseHarFile());
        filePanel.add(browseButton, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(filePanel, gbc);
    }

    // Use JFileChooser directly in the method for simplicity
    public File chooseHarFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".har");
            }

            public String getDescription() {
                return "HAR Files (*.har)";
            }
        });

        if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            harInputField.setText(selectedFile.getAbsolutePath());
            loadHarFile(selectedFile);
            return selectedFile;
        }
        return null;
    }

    // Simplify the file loading process
    public void loadHarFile(File file) {
        try {
            clearHostPanel();
            har = new de.sstoehr.harreader.HarReader().readFromFile(file);

            har.log().entries().stream()
                .map(HarEntry::request)
                .map(HarRequest::url)
                .filter(url -> !url.startsWith("data:"))
                .map(URI::create)
                .map(URI::getHost)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(this::addHostCheckbox);

            importButton.setEnabled(true);
            dialog.revalidate();
            dialog.repaint();
        } catch (HarReaderException e) {
            LOGGER.severe("Exception occured when loading har file.");
            importButton.setEnabled(false);
            JOptionPane.showMessageDialog(dialog, "Failed to load HAR file.", "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addHostCheckbox(String host) {
        JCheckBox checkbox = new JCheckBox(host);
        checkbox.setPreferredSize(new Dimension(DIALOG_WIDTH, ELEMENT_HEIGHT));
        hostCheckboxes.put(host, checkbox);
        ignoredHostsPanel.add(checkbox);
    }

    private void addHostExclusionPanel(JPanel form, GridBagConstraints gbc) {
        ignoredHostsPanel.setLayout(new BoxLayout(ignoredHostsPanel, BoxLayout.Y_AXIS));
        ignoredHostsPanel.setBorder(BorderFactory.createTitledBorder("Exclude Hosts"));

        JScrollPane scrollPane = new JScrollPane(ignoredHostsPanel);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(scrollPane, gbc);
    }

    private void addOptionsPanel(JPanel form, GridBagConstraints gbc) {
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(BorderFactory.createTitledBorder("Options"));

        options.add(addTimerCheckbox);
        options.add(addHeaderCheckbox);
        options.add(addCookiesCheckbox);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(options, gbc);
    }

    private void addActionButtons(JPanel form, GridBagConstraints gbc) {
        JPanel actionPanel = new JPanel(new GridBagLayout());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(DIALOG_WIDTH / 2, ELEMENT_HEIGHT));
        cancelButton.addActionListener(e -> dialog.dispose());

        importButton.setPreferredSize(new Dimension(DIALOG_WIDTH / 2, ELEMENT_HEIGHT));
        importButton.setEnabled(false);
        importButton.addActionListener(e -> performImport());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        actionPanel.add(cancelButton, c);

        c.gridx = 1;
        actionPanel.add(importButton, c);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(actionPanel, gbc);
    }

    private void performImport() {
        HARImporter importer = new HARImporter(har);
        hostCheckboxes.forEach((host, checkbox) -> {
            if (checkbox.isSelected()) {
                importer.ignoreHost(host);
            }
        });
        importer.addNewThreadGroupWithSamplers(
            addTimerCheckbox.isSelected(),
            addHeaderCheckbox.isSelected(),
            addCookiesCheckbox.isSelected()
        );
        dialog.dispose();
    }

    private void clearHostPanel() {
        ignoredHostsPanel.removeAll();
        ignoredHostsPanel.revalidate();
        ignoredHostsPanel.repaint();
        hostCheckboxes.clear();
    }

    /**
     * Displays the import dialog.
     */
    public void showDialog() {
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Returns the HAR input field.
     *
     * @return the HAR input field
     */
    public JTextField getHarInputField() {
        return harInputField;
    }
}

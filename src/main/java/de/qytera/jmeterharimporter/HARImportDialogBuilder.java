package de.qytera.jmeterharimporter;

import de.sstoehr.harreader.HarReader;
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
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

public class HARImportDialogBuilder {
    private static final int MINIMUM_DIALOG_WIDTH = 500;
    private static final int MINIMUM_DIALOG_HEIGHT = 400;
    private static final int MINIMUM_ELEMENT_HEIGHT = 30;

    private final JDialog dialog;
    private final JTextField harInputField = new JTextField();
    private final JPanel ignoredHostsPanel = new JPanel();
    private final JButton importButton = new JButton("Import");
    private final JCheckBox addTimerCheckbox = new JCheckBox("Add Recorded Waiting Time");
    private final JCheckBox addHeaderCheckbox = new JCheckBox("Add Recorded Headers");
    private final JCheckBox addCookiesCheckbox = new JCheckBox("Add Recorded Cookies");

    private final Map<String, JCheckBox> hostCheckboxes = new HashMap<>();
    private Har har;

    public HARImportDialogBuilder() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setPreferredSize(new Dimension(MINIMUM_DIALOG_WIDTH, MINIMUM_DIALOG_HEIGHT));
        GridBagConstraints gbc = new GridBagConstraints();

        buildFileSelector(form, gbc);
        buildHostExclusion(form, gbc);
        buildOptions(form, gbc);
        buildActions(form, gbc);

        dialog = new JDialog();
        dialog.setTitle("Import HAR File");
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setPreferredSize(new Dimension(MINIMUM_DIALOG_WIDTH, MINIMUM_DIALOG_HEIGHT));
        dialog.add(form);
    }

    private void buildFileSelector(JPanel form, GridBagConstraints gbc) {
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.setBorder(BorderFactory.createTitledBorder("HAR file"));
        harInputField.setPreferredSize(
            new Dimension(MINIMUM_DIALOG_WIDTH * 4 / 5, MINIMUM_ELEMENT_HEIGHT));
        filePanel.add(harInputField, BorderLayout.CENTER);

        JButton chooseButton = new JButton("Choose");
        chooseButton.setPreferredSize(
            new Dimension(MINIMUM_DIALOG_WIDTH / 5, MINIMUM_ELEMENT_HEIGHT));
        filePanel.add(chooseButton, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(filePanel, gbc);

        chooseButton.addActionListener(e -> chooseHarFile());
    }

    private void chooseHarFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.addChoosableFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".har") || f.isDirectory();
            }

            public String getDescription() {
                return "HTTP Archive Files (*.har)";
            }
        });
        if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            harInputField.setText(chooser.getSelectedFile().getAbsolutePath());
            try {
                clearPanel();
                har = new HarReader().readFromFile(new File(harInputField.getText()));
                har.log().entries().stream()
                    .map(HarEntry::request)
                    .map(HarRequest::url)
                    .filter(url -> !url.startsWith("data:"))
                    .map(URI::create)
                    .map(URI::getHost)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .forEach(host -> {
                        JCheckBox box = new JCheckBox(host);
                        box.setPreferredSize(
                            new Dimension(MINIMUM_DIALOG_WIDTH, MINIMUM_ELEMENT_HEIGHT));
                        hostCheckboxes.put(host, box);
                        ignoredHostsPanel.add(box);
                    });
                importButton.setEnabled(true);
                dialog.revalidate();
                dialog.repaint();
            } catch (HarReaderException ex) {
                ex.printStackTrace();
                importButton.setEnabled(false);
            }
        }
    }

    private void buildHostExclusion(JPanel form, GridBagConstraints gbc) {
        ignoredHostsPanel.setLayout(new BoxLayout(ignoredHostsPanel, BoxLayout.PAGE_AXIS));
        ignoredHostsPanel.setBorder(BorderFactory.createTitledBorder("Select hosts to exclude"));
        JScrollPane scrollPane = new JScrollPane(ignoredHostsPanel);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(scrollPane, gbc);
    }

    private void buildOptions(JPanel form, GridBagConstraints gbc) {
        JPanel options = new JPanel();
        options.setBorder(BorderFactory.createTitledBorder("Options"));
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
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

    private void buildActions(JPanel form, GridBagConstraints gbc) {
        JPanel actions = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(actions, gbc);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(
            new Dimension(MINIMUM_DIALOG_WIDTH / 2, MINIMUM_ELEMENT_HEIGHT));
        cancelButton.addActionListener(e -> dialog.dispose());

        importButton.setPreferredSize(
            new Dimension(MINIMUM_DIALOG_WIDTH / 2, MINIMUM_ELEMENT_HEIGHT));
        importButton.setEnabled(false);
        importButton.addActionListener(e -> {
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
        });

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        actions.add(cancelButton, c);

        c.gridx = 1;
        actions.add(importButton, c);
    }

    private void clearPanel() {
        ignoredHostsPanel.removeAll();
        ignoredHostsPanel.revalidate();
        ignoredHostsPanel.repaint();
    }

    public void showDialog() {
        dialog.pack();
        dialog.setVisible(true);
    }
}

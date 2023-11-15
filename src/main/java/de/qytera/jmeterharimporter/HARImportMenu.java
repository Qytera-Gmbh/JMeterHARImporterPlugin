/**
 * This class implements the MenuCreator and ActionListener interfaces to create a menu item in JMeter's Tools menu
 * that allows the user to import a HTTP Archive (HAR) file. When the user clicks on the menu item, a dialog box is
 * displayed that allows the user to browse for the HAR file and import it. The imported HAR file is then used to
 * create JMeter sampler elements.
 *
 * @author Matthias Eggert - Qytera
 */

package de.qytera.jmeterharimporter;

import org.apache.jmeter.gui.plugin.MenuCreator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;


public class HARImportMenu implements MenuCreator, ActionListener {

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if (location == MENU_LOCATION.TOOLS) {
            JMenuItem importItem = new JMenuItem("Import HAR File");
            importItem.addActionListener(this);
            return new JMenuItem[] { importItem };
        }

        return new JMenuItem[] {};
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        // no top level menus
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menu) {
        // nothing to be done, only english is supported
        return false;
    }

    @Override
    public void localeChanged() {
        // nothing to be done, only english is supported
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openImportWizard();
    }

    private void openImportWizard() {
        JDialog importWizard = new JDialog();
        importWizard.setTitle("Import HAR File");
        importWizard.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        importWizard.setSize(460, 150);
        importWizard.setLayout(null);

        // First row - Label
        JLabel label = new JLabel("Select HAR File:");
        label.setBounds(25, 5, 100, 25);
        importWizard.add(label);

        // Second row - TextField spanning 3 columns
        JTextField textField = new JTextField();
        textField.setBounds(25, 35, 300, 25);
        importWizard.add(textField);

        // Second row - Browse Button
        JButton browseButton = new JButton("Browse");
        browseButton.setBounds(330, 35, 100, 25);
        importWizard.add(browseButton);

        // Third row - Cancel Button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBounds(25, 65, 200, 25);
        importWizard.add(cancelButton);

        // Third row - Import Button
        JButton importButton = new JButton("Import");
        importButton.setBounds(230, 65, 200, 25);
        importWizard.add(importButton);

        // Display the dialog
        importWizard.setLocationRelativeTo(null); // Center the dialog
        importWizard.setVisible(true);

        // Add action listeners
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select HAR File");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(java.io.File f) {
                    return f.getName().toLowerCase().endsWith(".har") || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "HTTP Archive Files (*.har)";
                }
            });

            if (fileChooser.showOpenDialog(importWizard) == JFileChooser.APPROVE_OPTION) {
                textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        cancelButton.addActionListener(e -> importWizard.dispose());

        importButton.addActionListener(e -> {
            importWizard.dispose();
            HARImporter importer = new HARImporter(textField.getText());
            importer.addNewThreadGroupWithSamplers();
        });
    }
}

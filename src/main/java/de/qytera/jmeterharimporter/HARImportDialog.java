package de.qytera.jmeterharimporter;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import org.apache.jmeter.gui.plugin.MenuCreator;

public class HARImportDialog implements MenuCreator {

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION menuLocation) {
        if (menuLocation == MENU_LOCATION.TOOLS) {
            JMenuItem importHarItem = new JMenuItem("Import HAR File...");
            importHarItem.addActionListener(e -> new HARImportDialogBuilder().showDialog());
            return new JMenuItem[] {importHarItem};
        }
        return new JMenuItem[0];
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menuElement) {
        return false;
    }

    @Override
    public void localeChanged() {
        // No-op
    }
} 
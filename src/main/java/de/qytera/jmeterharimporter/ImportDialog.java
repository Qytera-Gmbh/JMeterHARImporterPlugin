package de.qytera.jmeterharimporter;

import org.apache.jmeter.gui.plugin.MenuCreator;

import javax.swing.*;

/**
 * JMeter plugin menu item for importing a HAR file.
 */
public final class ImportDialog implements MenuCreator {

    /**
     * Creates the menu items to appear under specific JMeter menu locations.
     *
     * @param menuLocation the location in the JMeter menu
     * @return an array of {@link JMenuItem}, or empty array if not applicable
     */
    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION menuLocation) {
        if (menuLocation == MENU_LOCATION.TOOLS) {
            JMenuItem importHarItem = new JMenuItem("Import HAR File...");
            importHarItem.addActionListener(e -> new ImportDialogBuilder().showDialog());
            return new JMenuItem[]{importHarItem};
        }
        return new JMenuItem[0];
    }

    /**
     * Returns top-level menus provided by this plugin. Not used here.
     *
     * @return empty array as no top-level menus are added
     */
    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    /**
     * Called when locale changes for a specific menu element.
     *
     * @param menuElement the affected menu element
     * @return false as no locale-sensitive elements exist
     */
    @Override
    public boolean localeChanged(MenuElement menuElement) {
        return false;
    }

    /**
     * Called when the application locale changes. No-op here.
     */
    @Override
    public void localeChanged() {
        // No-op
    }
}

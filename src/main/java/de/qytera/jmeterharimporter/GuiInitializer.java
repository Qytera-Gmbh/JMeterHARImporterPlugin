package de.qytera.jmeterharimporter;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;

import java.util.logging.Logger;

/**
 * Utility class to ensure that the GuiPackage singleton is properly initialized.
 */
public final class GuiInitializer {

    private static final Logger LOGGER = Logger.getLogger(GuiInitializer.class.getName());

    private GuiInitializer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Initializes the GuiPackage singleton if it hasn't been initialized yet.
     *
     * @return the initialized GuiPackage instance
     */
    public static GuiPackage initializeGuiPackage() {
        if (GuiPackage.getInstance() == null) {
            synchronized (GuiInitializer.class) {
                if (GuiPackage.getInstance() == null) {
                    LOGGER.fine("Initializing GuiPackage instance...");
                    GuiPackage.initInstance(null, new JMeterTreeModel());
                }
            }
        }
        return GuiPackage.getInstance();
    }
}

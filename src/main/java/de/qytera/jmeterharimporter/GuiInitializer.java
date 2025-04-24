package de.qytera.jmeterharimporter;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;

public class GuiInitializer {
    public static GuiPackage initializeGuiPackage() {
        if (GuiPackage.getInstance() == null) {
            GuiPackage.initInstance(null, new JMeterTreeModel());
        }

        return GuiPackage.getInstance();
    }
}

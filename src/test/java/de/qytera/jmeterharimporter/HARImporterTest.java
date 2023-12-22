package de.qytera.jmeterharimporter;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.apache.jmeter.gui.tree.JMeterTreeNode;

public class HARImporterTest {
    
    @Test
    public void testHARImporter() {
        HARImporter harImporter = new HARImporter("src/test/resources/www.randomnumberapi.com.har");
        JMeterTreeNode threadGroupNode = harImporter.addNewThreadGroupWithSamplers();
        assertEquals("HAR Imported", threadGroupNode.getName());
    }
}
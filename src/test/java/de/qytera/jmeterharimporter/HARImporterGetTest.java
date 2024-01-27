package de.qytera.jmeterharimporter;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.apache.jmeter.gui.tree.JMeterTreeNode;

public class HARImporterGetTest {
    
    JMeterTreeNode threadGroupNode = null;

    @Before
    public void setUp() {
        HARImporter harImporter = new HARImporter("src/test/resources/get-www.randomnumberapi.com.har");
        threadGroupNode = harImporter.addNewThreadGroupWithSamplers();
    }

    @Test
    public void testHARImporter_threadGroupName() {
        assertEquals("HAR Imported", threadGroupNode.getName());
    }

    @Test
    public void testHARImporter_controller() {
        assertEquals(2, threadGroupNode.getChildCount());
        assertEquals("TC.001 - www.randomnumberapi.com", ((JMeterTreeNode)threadGroupNode.getChildAt(0)).getName());
        assertEquals("TC.002 - www.randomnumberapi.com", ((JMeterTreeNode)threadGroupNode.getChildAt(1)).getName());
    }
}
package de.qytera.jmeterharimporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// tests POST, PUT, and PATCH
public class HARImporterPostTest {

    JMeterTreeNode threadGroupNode = null;

    @BeforeEach
    public void setUp() {
        HARImporter harImporter = new HARImporter("src/test/resources/post-example.har");
        threadGroupNode = harImporter.addNewThreadGroupWithSamplers();
    }

    @Test
    public void testHARImporter_threadGroup() {
        assertEquals("HAR Imported", threadGroupNode.getName());
    }

    @Test
    public void testHARImporter_controller() {
        assertEquals(1, threadGroupNode.getChildCount());
        assertEquals("TC.001 - example.com",
            ((JMeterTreeNode) threadGroupNode.getChildAt(0)).getName());
    }

    @Test
    public void testHARImporter_postData() {
        JMeterTreeNode controller = (JMeterTreeNode) threadGroupNode.getChildAt(0);
        JMeterTreeNode request = (JMeterTreeNode) controller.getChildAt(1);
        HTTPSamplerProxy requestObject = (HTTPSamplerProxy) request.getUserObject();
        assertTrue(requestObject.getPostBodyRaw());
        assertEquals("{\"key1\": \"value1\", \"key2\": \"value2\"}",
            requestObject.getArguments().getArgument(0).getValue());
    }
}
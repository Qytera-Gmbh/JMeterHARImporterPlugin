package de.qytera.jmeterharimporter;

import static org.junit.Assert.assertEquals;

import javax.swing.tree.TreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// tests ignored hosts
public class HARImporterIgnoreHostTest {

    HARImporter harImporter = null;

    @BeforeEach
    public void setUp() {
        harImporter = new HARImporter("src/test/resources/filter/filter-host.har");
    }

    @Test
    public void testHARImporter_ignoreHostsEmpty() {
        TreeNode threadGroup = harImporter.addNewThreadGroupWithSamplers();
        assertEquals(4, threadGroup.getChildCount());
        assertEquals("jsonplaceholder.typicode.com",
            NodeUtil.getChild(threadGroup.getChildAt(0), HTTPSamplerProxy.class).getDomain());
        assertEquals("jsonplaceholder.typicode.com",
            NodeUtil.getChild(threadGroup.getChildAt(1), HTTPSamplerProxy.class).getDomain());
        assertEquals("example.org",
            NodeUtil.getChild(threadGroup.getChildAt(2), HTTPSamplerProxy.class).getDomain());
        assertEquals("example.org",
            NodeUtil.getChild(threadGroup.getChildAt(3), HTTPSamplerProxy.class).getDomain());
    }

    @Test
    public void testHARImporter_ignoreSingleHost() {
        harImporter.ignoreHost("example.org");
        TreeNode threadGroup = harImporter.addNewThreadGroupWithSamplers();
        assertEquals(2, threadGroup.getChildCount());
        assertEquals("jsonplaceholder.typicode.com",
            NodeUtil.getChild(threadGroup.getChildAt(0), HTTPSamplerProxy.class).getDomain());
        assertEquals("jsonplaceholder.typicode.com",
            NodeUtil.getChild(threadGroup.getChildAt(1), HTTPSamplerProxy.class).getDomain());
    }

    @Test
    public void testHARImporter_ignoreAllHosts() {
        harImporter.ignoreHost("example.org");
        harImporter.ignoreHost("jsonplaceholder.typicode.com");
        TreeNode threadGroup = harImporter.addNewThreadGroupWithSamplers();
        assertEquals(0, threadGroup.getChildCount());
    }

}
package de.qytera.jmeterharimporter;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.sampler.TestAction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class HARImporterGetTest {

    JMeterTreeNode threadGroupNode = null;

    @BeforeEach
    public void setUp() {
        Configurator.setLevel("org.apache.jmeter", Level.OFF);
        HARImporter harImporter =
                new HARImporter("src/test/resources/get-www.randomnumberapi.com.har");

        try {
            threadGroupNode = harImporter.addNewThreadGroupWithSamplers();
        } catch (Exception e) {
            // nop
        }
    }

    @Test
    public void testHARImporter_threadGroup() {
        assertEquals("HAR Imported", threadGroupNode.getName());
    }

    @Test
    public void testHARImporter_controller() {
        assertEquals(2, threadGroupNode.getChildCount());
        assertEquals("TC.001 - www.randomnumberapi.com",
                ((JMeterTreeNode) threadGroupNode.getChildAt(0)).getName());
        assertEquals("TC.002 - www.randomnumberapi.com",
                ((JMeterTreeNode) threadGroupNode.getChildAt(1)).getName());
    }

    @Test
    public void testHARImporter_timer() {
        String[] timerDelays = {"0", "8293"};

        assertEquals(timerDelays.length, threadGroupNode.getChildCount());

        for (int i = 0; i < timerDelays.length; i++) {
            JMeterTreeNode controller = (JMeterTreeNode) threadGroupNode.getChildAt(i);
            JMeterTreeNode timer = (JMeterTreeNode) controller.getChildAt(0);
            assertEquals("Think Time", timer.getName());
            TestAction timerObject = (TestAction) timer.getUserObject();
            assertEquals(timerDelays[i], timerObject.getDurationAsString());
        }
    }

    @Test
    public void testHARImporter_timer_grouping() {
        HARImporter harImporter = new HARImporter("src/test/resources/www.qytera.de.har");
        threadGroupNode = harImporter.addNewThreadGroupWithSamplers(true, false, false);


        String[] timerDelays =
                {"0", "1", "78", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1", "1",
                        "20"};

        for (int i = 0; i < timerDelays.length; i++) {
            JMeterTreeNode controller = (JMeterTreeNode) threadGroupNode.getChildAt(i);
            JMeterTreeNode timer = (JMeterTreeNode) controller.getChildAt(0);
            assertEquals("Think Time", timer.getName());
            TestAction timerObject = (TestAction) timer.getUserObject();
            assertEquals(timerDelays[i], timerObject.getDurationAsString());
        }
    }

    @Test
    public void testHARImporter_no_timer() {
        HARImporter harImporter =
                new HARImporter("src/test/resources/get-www.randomnumberapi.com.har");
        threadGroupNode = harImporter.addNewThreadGroupWithSamplers(false, false, false);

        String[] timerDelays = {"0", "8293"};

        assertEquals(timerDelays.length, threadGroupNode.getChildCount());

        for (int i = 0; i < timerDelays.length; i++) {
            JMeterTreeNode controller = (JMeterTreeNode) threadGroupNode.getChildAt(i);
            JMeterTreeNode timer = (JMeterTreeNode) controller.getChildAt(0);
            assertNotEquals("Think Time", timer.getName());
        }
    }

    @Test
    public void testHARImporter_request() {
        List<Map<String, String>> listOfMaps = new ArrayList<>();

        Map<String, String> firstMap = new HashMap<>();
        firstMap.put("min", "100");
        firstMap.put("max", "1000");
        firstMap.put("count", "5");

        Map<String, String> secondMap = new HashMap<>();
        secondMap.put("min", "100");
        secondMap.put("max", "1000");
        secondMap.put("count", "1");

        listOfMaps.add(firstMap);
        listOfMaps.add(secondMap);

        for (int i = 0; i < threadGroupNode.getChildCount(); i++) {
            JMeterTreeNode controller = (JMeterTreeNode) threadGroupNode.getChildAt(i);
            JMeterTreeNode request = (JMeterTreeNode) controller.getChildAt(1);
            assertEquals("GET - /api/v1.0/random", request.getName());
            HTTPSamplerProxy requestObject = (HTTPSamplerProxy) request.getUserObject();
            assertEquals("https", requestObject.getProtocol());
            assertEquals("www.randomnumberapi.com", requestObject.getDomain());
            assertEquals(443, requestObject.getPort());
            assertEquals("GET", requestObject.getMethod());
            assertEquals("/api/v1.0/random", requestObject.getPath());
            assertEquals("/api/v1.0/random", requestObject.getPath());
            for (int j = 0; j < requestObject.getArguments().getArgumentCount(); j++) {
                Argument arg = requestObject.getArguments().getArgument(j);
                assertEquals(listOfMaps.get(i).get(arg.getName()), arg.getValue());
            }
        }
    }

    @Test
    public void testHARImporter_header() {
        JMeterTreeNode header =
                (JMeterTreeNode) threadGroupNode.getChildAt(1).getChildAt(1).getChildAt(0);
        HeaderManager headerOject = (HeaderManager) header.getUserObject();
        assertEquals(headerOject.getHeaders().size(), 16);
    }

    @Test
    public void testHARImporter_no_header() {
        HARImporter harImporter =
                new HARImporter("src/test/resources/get-www.randomnumberapi.com.har");
        threadGroupNode = harImporter.addNewThreadGroupWithSamplers(false, false, false);

        boolean isHeaderAvailable = true;

        try {
            threadGroupNode.getChildAt(1).getChildAt(1).getChildAt(0);
        } catch (Exception e) {
            isHeaderAvailable = false;
        }

        assertFalse(isHeaderAvailable);
    }

    @Test
    public void testHARImporter_cookie_no_expiry() {
        boolean exceptionThrown = false;

        try {
            HARImporter harImporter =
                    new HARImporter("src/test/resources/cookie-no-expiry-date.har");
            threadGroupNode = harImporter.addNewThreadGroupWithSamplers(false, false, true);
        } catch (Exception e) {
            exceptionThrown = true;
        }

        assertFalse(exceptionThrown);
    }
}
package de.qytera.jmeterharimporter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.Cookie;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.timers.ConstantTimer;
import org.apache.jmeter.timers.gui.ConstantTimerGui;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.gui.CookiePanel;
import de.sstoehr.harreader.model.HarCookie;

import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarHeader;
import de.sstoehr.harreader.model.HarQueryParam;
import de.sstoehr.harreader.model.HarRequest;

public class HARImporter {
    /**
     *
     */
    private static final String THINK_TIME = "Think Time";
    private static final String HAR_IMPORTED = "HAR Imported";
    private Har har;
    private GuiPackage guiPackage;

    /**
     * Constructor for the HARImporter class
     * 
     * @param filePath
     */
    public HARImporter(String filePath) {
        try {
            this.har = new HarReader().readFromFile(new File(filePath));
        } catch (HarReaderException e) {
            e.printStackTrace();
        }

        if (GuiPackage.getInstance() == null) {
            GuiPackage.initInstance(null, new JMeterTreeModel());
        }

        this.guiPackage = GuiPackage.getInstance();
    }

    /**
     * Adds a new Thread Group to the JMeter tree and adds a HTTP Sampler for each
     * HAR entry
     */
    public JMeterTreeNode addNewThreadGroupWithSamplers() {
        try {
            // Get the root node of the JMeter tree
            JMeterTreeNode root = (JMeterTreeNode) this.guiPackage.getTreeModel().getRoot();

            // Create a Thread Group to hold the requests
            JMeterTreeNode threadGroupNode = addComponent(createThreadGroup(), root);

            int i = 1;
            long lastTimestamp = -1;
            for (HarEntry harEntry : this.har.getLog().getEntries()) {
                HarRequest harRequest = harEntry.getRequest();
                // add a transaction controller for each entry to group the samplers
                JMeterTreeNode transactionControllerNodeSub = addComponent(createTransactionController(
                        String.format("TC.%03d - " + new URL(harRequest.getUrl()).getHost(), i++)),
                        threadGroupNode);

                // calculate think time
                if (lastTimestamp == -1) {
                    lastTimestamp = harEntry.getStartedDateTime().getTime(); // first entry should become 0
                }

                long currentEntryStartTime = harEntry.getStartedDateTime().getTime();
                long timeDifference = currentEntryStartTime - lastTimestamp;

                // add a constant timer to simulate the think time
                addComponent(createConstantTimer(timeDifference), transactionControllerNodeSub);

                // add the http sampler
                JMeterTreeNode httpSamplerNode = addComponent(createHttpSampler(harRequest),
                        transactionControllerNodeSub);

                // add the header manager
                addComponent(createHeaderManager(harRequest), httpSamplerNode);

                // add the cookie manager
                addComponent(createCookieManager(harRequest), httpSamplerNode);

                // add body
                if (harRequest.getPostData() != null && harRequest.getPostData().getText() != null) {
                    HTTPSamplerProxy httpSampler = (HTTPSamplerProxy) httpSamplerNode.getUserObject();
                    httpSampler.setPostBodyRaw(true);
                    httpSampler.addNonEncodedArgument("", harRequest.getPostData().getText(), "");
                }
            }

            // Refresh the JMeter GUI
            if (this.guiPackage.getMainFrame() != null) {
                this.guiPackage.getMainFrame().repaint();
            }

            return threadGroupNode;
        } catch (IllegalUserActionException | MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private JMeterTreeNode addComponent(AbstractTestElement component, JMeterTreeNode node)
            throws IllegalUserActionException {
        if (component == null || node == null) {
            return null;
        }

        return this.guiPackage.getTreeModel().addComponent(component, node);
    }

    private CookieManager createCookieManager(HarRequest harRequest) {
        CookieManager cookieManager = null;
        if (harRequest.getCookies().size() > 0) {
            cookieManager = new CookieManager();
            cookieManager.setName("browser-cookies");
            cookieManager.setProperty(TestElement.TEST_CLASS, CookieManager.class.getName());
            cookieManager.setProperty(TestElement.GUI_CLASS, CookiePanel.class.getName());
            for (HarCookie cookie : harRequest.getCookies()) {
                long expiration = cookie.getExpires().getTime() - (new Date()).getTime();
                cookieManager.add(new Cookie(cookie.getName(), cookie.getValue(), cookie.getDomain(),
                        cookie.getPath(), cookie.getSecure(), expiration, cookie.getPath().length() > 0,
                        cookie.getDomain().length() > 0));
            }

            cookieManager.setClearEachIteration(true);
        }

        return cookieManager;
    }

    private HeaderManager createHeaderManager(HarRequest harRequest) {
        HeaderManager headerManager = null;
        if (harRequest.getHeaders().size() > 0) {
            // Create Header Manager
            headerManager = new HeaderManager();
            headerManager.setName("browser-headers");
            headerManager.setProperty(TestElement.TEST_CLASS, HeaderManager.class.getName());
            headerManager.setProperty(TestElement.GUI_CLASS, HeaderPanel.class.getName());
            for (HarHeader header : harRequest.getHeaders()) {
                headerManager.add(new Header(header.getName(),
                        header.getValue()));
            }
        }

        return headerManager;
    }

    private HTTPSamplerProxy createHttpSampler(HarRequest harRequest) throws MalformedURLException {
        URL url = new URL(harRequest.getUrl());

        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setName(harRequest.getMethod().name() + " - " + url.getPath());
        httpSampler.setProtocol(url.getProtocol());
        httpSampler.setDomain(url.getHost());
        httpSampler.setPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
        httpSampler.setMethod(harRequest.getMethod().name());
        httpSampler.setPath(url.getPath());

        for (HarQueryParam queryParam : harRequest.getQueryString()) {
            httpSampler.addArgument(queryParam.getName(), queryParam.getValue());
        }

        httpSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());

        return httpSampler;
    }

    private ConstantTimer createConstantTimer(long time) {
        ConstantTimer constantTimer = new ConstantTimer();
        constantTimer.setName(THINK_TIME);
        constantTimer.setDelay(String.valueOf(time));
        constantTimer.setProperty(TestElement.TEST_CLASS, ConstantTimer.class.getName());
        constantTimer.setProperty(TestElement.GUI_CLASS, ConstantTimerGui.class.getName());

        return constantTimer;
    }

    private LoopController createLoopControler() {
        LoopController loopController = new LoopController();
        loopController.setLoops(1);
        loopController.setFirst(true);
        loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
        loopController.initialize();

        return loopController;
    }

    private ThreadGroup createThreadGroup() {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName(HAR_IMPORTED);
        threadGroup.setNumThreads(1);
        threadGroup.setRampUp(1);
        threadGroup.setSamplerController(createLoopControler());
        threadGroup.setEnabled(false);
        threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());

        return threadGroup;
    }

    private TransactionController createTransactionController(String name) {
        TransactionController transactionControllerSub = new TransactionController();
        transactionControllerSub.setName(name);
        transactionControllerSub.setProperty(TestElement.TEST_CLASS, TransactionController.class.getName());
        transactionControllerSub.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());

        return transactionControllerSub;
    }
}

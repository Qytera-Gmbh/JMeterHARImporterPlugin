package de.qytera.jmeterharimporter;

import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarCookie;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarHeader;
import de.sstoehr.harreader.model.HarQueryParam;
import de.sstoehr.harreader.model.HarRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.Cookie;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.gui.CookiePanel;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.sampler.TestAction;
import org.apache.jmeter.sampler.gui.TestActionGui;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;

public class HARImporter {
    private static final String THINK_TIME = "Think Time";
    private static final String HAR_IMPORTED = "HAR Imported";
    private final GuiPackage guiPackage;
    private final Set<String> hostsIgnored = new HashSet<>();
    private final Har har;

    /**
     * Constructs a new importer based on the provided file path.
     *
     * @param filePath the path to the HAR file
     */
    public HARImporter(String filePath) {
        this.har = HarFileReader.readHarFromFile(filePath);
        this.guiPackage = GuiInitializer.initializeGuiPackage();
    }

    /**
     * Constructs a new importer based on the provided HAR data.
     *
     * @param har the HAR data
     */
    public HARImporter(Har har) {
        this.har = har;
        this.guiPackage = GuiInitializer.initializeGuiPackage();
    }

    /**
     * Adds a host to the set of ignored hosts. If a HAR entry contains the host in its request URL, the entry will be
     * skipped during conversion.
     *
     * @param host the host to ignore
     */
    public void ignoreHost(String host) {
        this.hostsIgnored.add(host);
    }

    /**
     * Adds a new Thread Group to the JMeter tree and adds a HTTP Sampler for each
     * HAR entry
     *
     * @return the root node of the JMeter tree
     */
    public JMeterTreeNode addNewThreadGroupWithSamplers() {
        return addNewThreadGroupWithSamplers(true, true, true);
    }

    /**
     * Adds a new Thread Group to the JMeter tree and adds a HTTP Sampler for each
     * HAR entry
     *
     * @param shouldAddThinkTime whether to add think time between the requests
     * @param shouldAddHeader    whether to add the recorded headers to the requests
     * @param shouldAddCookies   whether to add the recorded cookies to the requests
     * @return the root node of the JMeter tree
     */
    public JMeterTreeNode addNewThreadGroupWithSamplers(Boolean shouldAddThinkTime,
                                                        Boolean shouldAddHeader,
                                                        Boolean shouldAddCookies) {
        try {
            // Get the root node of the JMeter tree
            JMeterTreeNode root = (JMeterTreeNode) this.guiPackage.getTreeModel().getRoot();

            // Create a Thread Group to hold the requests
            JMeterTreeNode threadGroupNode = addComponent(createThreadGroup(), root);

            Map<Long, JMeterTreeNode> transactionControllers = new HashMap<>();
            Map<Long, Boolean> transactionControllerHasTimer = new HashMap<>();

            int i = 1;
            long lastTimestamp = -1;
            for (HarEntry harEntry : this.har.log().entries()) {
                // calculate think time
                if (lastTimestamp == -1) {
                    lastTimestamp = harEntry.startedDateTime().toInstant()
                        .toEpochMilli(); // first entry should become 0
                }

                long currentEntryStartTime = harEntry.startedDateTime().toInstant().toEpochMilli();
                long timeDifference = currentEntryStartTime - lastTimestamp;

                if (transactionControllerHasTimer.get(currentEntryStartTime) == null) {
                    lastTimestamp = currentEntryStartTime;
                }

                HarRequest harRequest = harEntry.request();
                URI uri = URI.create(harRequest.url());
                if (this.hostsIgnored.contains(uri.getHost())) {
                    continue;
                }
                // add a transaction controller for each entry to group the samplers
                if (transactionControllers.get(currentEntryStartTime) == null) {
                    TransactionController transactionController = createTransactionController(
                        String.format("TC.%03d - %s", i++, uri.getHost()));
                    JMeterTreeNode transactionControllerNodeSub =
                        addComponent(transactionController, threadGroupNode);
                    transactionControllers.put(currentEntryStartTime, transactionControllerNodeSub);
                }

                JMeterTreeNode transactionControllerNodeSub =
                    transactionControllers.get(currentEntryStartTime);

                // add a constant timer to simulate the think time
                if (shouldAddThinkTime) {
                    if (transactionControllerHasTimer.get(currentEntryStartTime) == null) {
                        transactionControllerHasTimer.put(currentEntryStartTime, true);
                        addComponent(createFlowControlAction(timeDifference),
                            transactionControllerNodeSub);
                    }
                }

                // add the http sampler
                JMeterTreeNode httpSamplerNode = addComponent(createHttpSampler(harRequest),
                    transactionControllerNodeSub);

                // add the header manager
                if (shouldAddHeader) {
                    addComponent(createHeaderManager(harRequest), httpSamplerNode);
                }

                // add the cookie manager
                if (shouldAddCookies) {
                    addComponent(createCookieManager(harRequest), httpSamplerNode);
                }

                // add body
                if (harRequest.postData() != null && harRequest.postData().text() != null) {
                    HTTPSamplerProxy httpSampler =
                        (HTTPSamplerProxy) httpSamplerNode.getUserObject();
                    httpSampler.setPostBodyRaw(true);
                    httpSampler.addNonEncodedArgument("", harRequest.postData().text(), "");
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
        if (!harRequest.cookies().isEmpty()) {
            cookieManager = new CookieManager();
            cookieManager.setName("browser-cookies");
            cookieManager.setProperty(TestElement.TEST_CLASS, CookieManager.class.getName());
            cookieManager.setProperty(TestElement.GUI_CLASS, CookiePanel.class.getName());
            for (HarCookie cookie : harRequest.cookies()) {
                long expiration = Long.MAX_VALUE;
                if (cookie.expires() != null) {
                    expiration =
                        cookie.expires().toInstant().toEpochMilli() - (new Date()).getTime();
                }

                boolean isSecure = cookie.secure() != null ? cookie.secure() : false;
                String path = cookie.path() != null ? cookie.path() : "";
                String domain = cookie.domain() != null ? cookie.domain() : "";

                cookieManager.add(new Cookie(cookie.name(), cookie.value(), domain,
                    path, isSecure, expiration, path.length() > 0,
                    domain.length() > 0));
            }

            cookieManager.setClearEachIteration(true);
        }

        return cookieManager;
    }

    private HeaderManager createHeaderManager(HarRequest harRequest) {
        HeaderManager headerManager = null;
        if (!harRequest.headers().isEmpty()) {
            // Create Header Manager
            headerManager = new HeaderManager();
            headerManager.setName("browser-headers");
            headerManager.setProperty(TestElement.TEST_CLASS, HeaderManager.class.getName());
            headerManager.setProperty(TestElement.GUI_CLASS, HeaderPanel.class.getName());
            for (HarHeader header : harRequest.headers()) {
                headerManager.add(new Header(header.name(),
                    header.value()));
            }
        }

        return headerManager;
    }

    private HTTPSamplerProxy createHttpSampler(HarRequest harRequest) throws MalformedURLException {
        URL url = new URL(harRequest.url());

        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setName(harRequest.httpMethod().name() + " - " + url.getPath());
        httpSampler.setProtocol(url.getProtocol());
        httpSampler.setDomain(url.getHost());
        httpSampler.setPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
        httpSampler.setMethod(harRequest.httpMethod().name());
        httpSampler.setPath(url.getPath());

        for (HarQueryParam queryParam : harRequest.queryString()) {
            httpSampler.addArgument(queryParam.name(), queryParam.value());
        }

        httpSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());

        return httpSampler;
    }

    private TestAction createFlowControlAction(long time) {
        TestAction testAction = new TestAction();
        testAction.setName(THINK_TIME);
        testAction.setAction(1);
        testAction.setTarget(0);
        testAction.setDuration(String.valueOf(time));
        testAction.setProperty(TestElement.TEST_CLASS, TestAction.class.getName());
        testAction.setProperty(TestElement.GUI_CLASS, TestActionGui.class.getName());

        return testAction;
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
        transactionControllerSub.setProperty(TestElement.TEST_CLASS,
            TransactionController.class.getName());
        transactionControllerSub.setProperty(TestElement.GUI_CLASS,
            LoopControlPanel.class.getName());

        return transactionControllerSub;
    }
}

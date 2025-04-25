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
     * Adds a new Thread Group to the JMeter test plan and populates it with HTTP samplers
     * generated from the HAR file's entries.
     *
     * @param addThinkTime whether to include timers to simulate "think time"
     * @param addHeader    whether to include request headers
     * @param addCookies   whether to include cookies
     * @return the JMeterTreeNode representing the created Thread Group, or null on error
     */
    public JMeterTreeNode addNewThreadGroupWithSamplers(Boolean addThinkTime, Boolean addHeader,
                                                        Boolean addCookies) {
        try {
            JMeterTreeNode root = (JMeterTreeNode) guiPackage.getTreeModel().getRoot();
            JMeterTreeNode threadGroupNode = addComponent(createThreadGroup(), root);

            Map<Long, JMeterTreeNode> transactionNodes = new HashMap<>();
            Set<Long> timersAdded = new HashSet<>();

            long lastTimestamp = -1;
            int index = 1;

            for (HarEntry entry : har.log().entries()) {
                long entryTime = entry.startedDateTime().toInstant().toEpochMilli();

                if (lastTimestamp == -1) {
                    lastTimestamp = entryTime;
                }

                HarRequest request = entry.request();
                URI uri = URI.create(request.url());

                if (hostsIgnored.contains(uri.getHost())) {
                    continue;
                }

                JMeterTreeNode transactionNode = transactionNodes.get(entryTime);
                if (transactionNode == null) {
                    TransactionController tc = createTransactionController(
                        String.format("TC.%03d - %s", index++, uri.getHost()));
                    transactionNode = addComponent(tc, threadGroupNode);
                    transactionNodes.put(entryTime, transactionNode);
                }

                if (addThinkTime && timersAdded.add(entryTime)) {
                    long thinkTime = entryTime - lastTimestamp;
                    addComponent(createFlowControlAction(thinkTime), transactionNode);
                    lastTimestamp = entryTime;
                }

                addSamplerWithExtras(request, transactionNode, addHeader, addCookies);
            }

            if (guiPackage.getMainFrame() != null) {
                guiPackage.getMainFrame().repaint();
            }

            return threadGroupNode;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void addSamplerWithExtras(HarRequest request, JMeterTreeNode parent, boolean addHeaders,
                                      boolean addCookies)
        throws MalformedURLException, IllegalUserActionException {

        JMeterTreeNode samplerNode = addComponent(createHttpSampler(request), parent);

        if (addHeaders) {
            HeaderManager headers = createHeaderManager(request);
            if (headers != null) {
                addComponent(headers, samplerNode);
            }
        }

        if (addCookies) {
            CookieManager cookies = createCookieManager(request);
            if (cookies != null) {
                addComponent(cookies, samplerNode);
            }
        }

        if (request.postData() != null && request.postData().text() != null) {
            HTTPSamplerProxy sampler = (HTTPSamplerProxy) samplerNode.getUserObject();
            sampler.setPostBodyRaw(true);
            sampler.addNonEncodedArgument("", request.postData().text(), "");
        }
    }


    private JMeterTreeNode addComponent(AbstractTestElement component, JMeterTreeNode node)
        throws IllegalUserActionException {
        if (component == null || node == null) {
            return null;
        }

        return this.guiPackage.getTreeModel().addComponent(component, node);
    }

    private CookieManager createCookieManager(HarRequest harRequest) {
        if (harRequest.cookies().isEmpty()) {
            return null;
        }

        CookieManager cookieManager = new CookieManager();
        cookieManager.setName("browser-cookies");
        cookieManager.setProperty(TestElement.TEST_CLASS, CookieManager.class.getName());
        cookieManager.setProperty(TestElement.GUI_CLASS, CookiePanel.class.getName());

        for (HarCookie harCookie : harRequest.cookies()) {
            cookieManager.add(convertHarCookie(harCookie));
        }

        cookieManager.setClearEachIteration(true);
        return cookieManager;
    }

    private Cookie convertHarCookie(HarCookie harCookie) {
        long expiration = harCookie.expires() != null
            ? harCookie.expires().toInstant().toEpochMilli() - new Date().getTime()
            : Long.MAX_VALUE;

        boolean isSecure = Boolean.TRUE.equals(harCookie.secure());
        String path = defaultString(harCookie.path());
        String domain = defaultString(harCookie.domain());

        return new Cookie(
            harCookie.name(),
            harCookie.value(),
            domain,
            path,
            isSecure,
            expiration,
            !path.isEmpty(),
            !domain.isEmpty()
        );
    }

    private String defaultString(String value) {
        return value != null ? value : "";
    }

    private HeaderManager createHeaderManager(HarRequest harRequest) {
        HeaderManager headerManager = null;
        if (!harRequest.headers().isEmpty()) {
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

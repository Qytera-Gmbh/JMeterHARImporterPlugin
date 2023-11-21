package de.qytera.jmeterharimporter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.timers.ConstantTimer;
import org.apache.jmeter.timers.gui.ConstantTimerGui;
import de.sstoehr.harreader.model.HarHeader;

import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarQueryParam;

public class HARImporter {
    Har har;

    public HARImporter(String filePath) {
        try {
            this.har = new HarReader().readFromFile(new File(filePath));
        } catch (HarReaderException e) {
            e.printStackTrace();
        }
    }

    public void addNewThreadGroupWithSamplers() {
        try {
            GuiPackage guiPackage = GuiPackage.getInstance();
            JMeterTreeNode root = (JMeterTreeNode) guiPackage.getTreeModel().getRoot();

            TransactionController transactionController = new TransactionController();
            transactionController.setName("Imported HAR Requests");
            transactionController.setProperty(TestElement.TEST_CLASS, TransactionController.class.getName());
            transactionController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());

            JMeterTreeNode transactionControllerNode = guiPackage.getTreeModel().addComponent(transactionController,
                    root);

            int i = 1;
            long lastTimestamp = -1;
            for (HarEntry harEntry : this.har.getLog().getEntries()) {
                String urlString = harEntry.getRequest().getUrl();
                URL url = new URL(urlString);

                TransactionController transactionControllerSub = new TransactionController();
                transactionControllerSub.setName(String.format("TC.%03d - " + url.getHost(), i++));
                transactionControllerSub.setProperty(TestElement.TEST_CLASS, TransactionController.class.getName());
                transactionControllerSub.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());

                JMeterTreeNode transactionControllerNodeSub = guiPackage.getTreeModel()
                        .addComponent(transactionControllerSub, transactionControllerNode);

                if (lastTimestamp == -1) {
                    lastTimestamp = harEntry.getStartedDateTime().getTime();
                }

                long timeDifference = harEntry.getStartedDateTime().getTime() - lastTimestamp;
                lastTimestamp = harEntry.getStartedDateTime().getTime();

                // add a constant timer to simulate the think time
                ConstantTimer constantTimer = new ConstantTimer();
                constantTimer.setName("Think Time");
                constantTimer.setDelay(String.valueOf(timeDifference));
                constantTimer.setProperty(TestElement.TEST_CLASS, ConstantTimer.class.getName());
                constantTimer.setProperty(TestElement.GUI_CLASS, ConstantTimerGui.class.getName());
                guiPackage.getTreeModel().addComponent(constantTimer, transactionControllerNodeSub);

                HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
                httpSampler.setName(harEntry.getRequest().getMethod().name() + " - " + url.getPath());
                httpSampler.setProtocol(url.getProtocol());
                httpSampler.setDomain(url.getHost());
                httpSampler.setPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
                httpSampler.setMethod(harEntry.getRequest().getMethod().name());
                httpSampler.setPath(url.getPath());

                for (HarQueryParam queryParam : harEntry.getRequest().getQueryString()) {
                    httpSampler.addArgument(queryParam.getName(), queryParam.getValue());
                }

                httpSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
                httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());

                JMeterTreeNode httpSamplerNode = guiPackage.getTreeModel().addComponent(httpSampler, transactionControllerNodeSub);

                // add header manager to sampler
                if (harEntry.getRequest().getHeaders().size() > 0) {
                    // Create Header Manager
                    HeaderManager headerManager = new HeaderManager();
                    headerManager.setName("browser-headers");
                    headerManager.setProperty(TestElement.TEST_CLASS, HeaderManager.class.getName());
                    headerManager.setProperty(TestElement.GUI_CLASS, HeaderPanel.class.getName());
                    for (HarHeader header : harEntry.getRequest().getHeaders()) {
                        headerManager.add(new org.apache.jmeter.protocol.http.control.Header(header.getName(), header.getValue()));
                    }

                    guiPackage.getTreeModel().addComponent(headerManager, httpSamplerNode);
                }
            }

            // Refresh the JMeter GUI
            guiPackage.getMainFrame().repaint();
        } catch (IllegalUserActionException | MalformedURLException e) {
            e.printStackTrace();
        }
    }
}

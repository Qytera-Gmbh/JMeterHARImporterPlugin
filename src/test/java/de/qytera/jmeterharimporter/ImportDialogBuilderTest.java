package de.qytera.jmeterharimporter;

import static org.assertj.core.api.Fail.fail;

import java.io.File;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ImportDialogBuilderTest {

    private static final Logger LOGGER = Logger.getLogger(ImportDialogBuilderTest.class.getName());

    private static Robot robot;
    private DialogFixture dialogFixture;
    private ImportDialogBuilder importDialogBuilder;

    @BeforeAll
    static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
        robot = BasicRobot.robotWithNewAwtHierarchy();
    }

    @AfterAll
    static void tearDownOnce() {
        robot.cleanUp();
    }

    @BeforeEach
    void setUp() {
        Configurator.setLevel("org.apache.jmeter", Level.OFF);
        try {
            SwingUtilities.invokeAndWait(() -> {
                importDialogBuilder = new ImportDialogBuilder();
                importDialogBuilder.showDialog();
            });

            dialogFixture = WindowFinder.findDialog(new GenericTypeMatcher<>(JDialog.class) {
                @Override
                protected boolean isMatching(JDialog dialog) {
                    return "Import HAR File".equals(dialog.getTitle()) && dialog.isShowing();
                }
            }).using(robot);

            if (dialogFixture != null) {
                LOGGER.info("Dialog Fixture initialized successfully.");
            } else {
                LOGGER.warning("Failed to initialize Dialog Fixture.");
            }

            dialogFixture.requireVisible();
        } catch (Exception e) {
            LOGGER.severe("Exception occured during test setup.");
        }
    }

    @Test
    @Order(999)
    void testFileSelectionAndLoading() {
        dialogFixture.robot().waitForIdle();

        // opening dialog is a bit flaky
        boolean dialogVisible = false;
        int retryCount = 0;
        int maxRetries = 3;
        while (retryCount < maxRetries) {
            try {
                dialogFixture.requireVisible();
                dialogVisible = true;
                break;
            } catch (AssertionError e) {
                retryCount++;
                LOGGER.warning(
                    "Dialog not visible, retrying... (" + retryCount + "/" + maxRetries + ")");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!dialogVisible) {
            LOGGER.severe("Dialog did not appear after " + maxRetries + " retries.");
            fail("Dialog did not appear after retries");
        }

        String testFilePath =
            getClass().getClassLoader().getResource("www.qytera.de.har").getPath();

        JButtonFixture importButton = dialogFixture.button(JButtonMatcher.withText("Import"));
        importButton.requireDisabled();

        JButtonFixture cancelButton = dialogFixture.button(JButtonMatcher.withText("Cancel"));
        cancelButton.requireEnabled();

        JButtonFixture browseButton = dialogFixture.button(JButtonMatcher.withText("Browse..."));
        browseButton.click();

        JFileChooserFixture fileChooserFixture = new JFileChooserFixture(robot);
        fileChooserFixture.selectFile(new File(testFilePath));
        robot.pressAndReleaseKeys(java.awt.event.KeyEvent.VK_ENTER);

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // nop
        }

        importButton.requireEnabled();
    }

    @AfterEach
    void tearDown() {
        if (dialogFixture != null) {
            dialogFixture.cleanUp();
        }
    }
}

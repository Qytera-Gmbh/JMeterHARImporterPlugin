package de.qytera.jmeterharimporter;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JButtonFixture;
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

    private static Robot robot;
    private DialogFixture dialogFixture;

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
        SwingUtilities.invokeLater(() -> {
            HARImportDialogBuilder builder = new HARImportDialogBuilder();
            builder.showDialog();
        });

        dialogFixture = WindowFinder.findDialog(new GenericTypeMatcher<>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Import HAR File".equals(dialog.getTitle()) && dialog.isShowing();
            }
        }).using(robot);
    }

    @Test
    @Order(999)
    void testDialogLoadsAndButtonsExist() {
        dialogFixture.requireVisible();

        JButtonFixture importButton = dialogFixture.button(JButtonMatcher.withText("Import"));
        importButton.requireDisabled();

        JButtonFixture cancelButton = dialogFixture.button(JButtonMatcher.withText("Cancel"));
        cancelButton.requireEnabled();
    }

    @AfterEach
    void tearDown() {
        if (dialogFixture != null) {
            dialogFixture.cleanUp();
        }
    }
}

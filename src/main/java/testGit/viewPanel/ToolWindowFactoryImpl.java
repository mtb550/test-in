package testGit.viewPanel;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ToolWindowFactoryImpl implements ToolWindowFactory, DumbAware {
    @Getter
    private static TestCaseDetailsPanel detailsInstance;

    @Getter
    private static AddTestCasePanel addInstance;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        detailsInstance = new TestCaseDetailsPanel();
        addInstance = new AddTestCasePanel();

        ContentFactory contentFactory = ContentFactory.getInstance();

        Content detailsTab = contentFactory.createContent(detailsInstance.getDetailsTab(), "Details", false);
        Content historyTab = contentFactory.createContent(detailsInstance.getHistoryTab(), "History", false);
        Content bugsTab = contentFactory.createContent(detailsInstance.getBugTab(), "Open Bugs", false);
        Content addTestCaseTab = contentFactory.createContent(addInstance.getMainPanel(), "Create Test Case", false);

        toolWindow.getContentManager().addContent(detailsTab);
        toolWindow.getContentManager().addContent(historyTab);
        toolWindow.getContentManager().addContent(bugsTab);
        toolWindow.getContentManager().addContent(addTestCaseTab);

        setupKeyHandlers(detailsInstance);
    }

    private void setupKeyHandlers(TestCaseDetailsPanel details) {
        JComponent root = details.getPanel();
        if (root == null) return;

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F2) {
                details.toggleEditMode(true);
                return true;
            }
            return false;
        });

        root.getActionMap().put("editMode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                details.toggleEditMode(true);
            }
        });
    }
}
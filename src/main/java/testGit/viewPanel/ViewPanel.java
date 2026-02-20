package testGit.viewPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import testGit.pojo.Config;
import testGit.pojo.TestCase;

import java.util.function.Consumer;

public class ViewPanel {

    // Helper to get the ToolWindow safely and avoid static state issues
    private static ToolWindow getToolWindow() {
        Project project = Config.getProject();
        if (project == null) return null;
        return ToolWindowManager.getInstance(project).getToolWindow("Details");
    }

    public static void addTestCase(Consumer<TestCase> onSaveCallback) {
        ToolWindow tw = getToolWindow();
        if (tw != null) {
            if (!tw.isVisible()) tw.show();

            selectContent(tw, "Create Test Case");

            AddTestCasePanel add = ToolWindowFactory.getAddInstance();
            if (add != null) {
                add.setOnSaveCallback(onSaveCallback);
            }
        }
    }

    public static void show(TestCase testCase) {
        ToolWindow tw = getToolWindow();
        if (tw != null) {
            if (!tw.isVisible())
                tw.show();

            selectContent(tw, "Details");

            TestCaseDetailsPanel viewer = ToolWindowFactory.getDetailsInstance();
            if (viewer != null) {
                viewer.update(testCase);
            }
        }
    }

    public static void hide() {
        ToolWindow tw = getToolWindow();
        if (tw != null && tw.isVisible()) {
            tw.hide(null);
        }
    }

    public static void reset() {
        // Reset the Details Panel content
        TestCaseDetailsPanel viewer = ToolWindowFactory.getDetailsInstance();
        if (viewer != null) {
            viewer.update(null); // Passing null to clear fields
        }
    }

    // Extracted logic to keep code DRY
    private static void selectContent(ToolWindow tw, String displayName) {
        Content[] contents = tw.getContentManager().getContents();
        for (Content content : contents) {
            if (displayName.equals(content.getDisplayName())) {
                tw.getContentManager().setSelectedContent(content);
                break;
            }
        }
    }
}
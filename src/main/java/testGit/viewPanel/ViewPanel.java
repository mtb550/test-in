package testGit.viewPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import testGit.pojo.Config;
import testGit.pojo.TestCase;

import java.util.function.Consumer;

public class ViewPanel {

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

            AddTestCasePanel add = ToolWindowFactoryImpl.getAddInstance();
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

            TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
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
        TestCaseDetailsPanel viewer = ToolWindowFactoryImpl.getDetailsInstance();
        if (viewer != null) {
            viewer.update(null);
        }
    }

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
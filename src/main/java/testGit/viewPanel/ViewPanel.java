package testGit.viewPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import testGit.pojo.Config;
import testGit.pojo.TestCase;

import java.util.function.Consumer;

public class ViewPanel {

    public static void addTestCase(Consumer<TestCase> onSaveCallback) {
        System.out.println("TestCaseToolWindow.addTestCase()");

        Project project = Config.getProject();
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Details"); // in plugin.xml <toolWindow id="TestCaseDetails"

        if (toolWindow != null) {
            if (!toolWindow.isVisible()) {
                toolWindow.show();
            }

            // Switch to "Create Test Case" tab
            Content[] contents = toolWindow.getContentManager().getContents();
            for (Content content : contents) {
                if ("Create Test Case".equals(content.getDisplayName())) {
                    toolWindow.getContentManager().setSelectedContent(content);
                    break;
                }
            }

            AddTestCasePanel add = TestCaseDetailsToolWindowFactory.getAddInstance();
            if (add != null) {
                add.setOnSaveCallback(onSaveCallback);
            }
        }
    }

    public static void show(TestCase testCase) {
        //System.out.println("TestCaseToolWindow.show()");

        ToolWindow toolWindow = ToolWindowManager.getInstance(Config.getProject()).getToolWindow("Details"); // in plugin.xml <toolWindow id="TestCaseDetails"

        if (toolWindow != null) {
            if (!toolWindow.isVisible()) {
                toolWindow.show();
            }

            // Switch to "Details" tab
            Content[] contents = toolWindow.getContentManager().getContents();
            for (Content content : contents) {
                if ("Details".equals(content.getDisplayName())) {
                    toolWindow.getContentManager().setSelectedContent(content);
                    break;
                }
            }

            TestCaseDetailsPanel viewer = TestCaseDetailsToolWindowFactory.getDetailsInstance();
            if (viewer != null) {
                viewer.update(testCase);
            }
        }
    }
}
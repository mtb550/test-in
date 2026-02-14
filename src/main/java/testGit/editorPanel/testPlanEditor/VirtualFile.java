package testGit.editorPanel.testPlanEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;

import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Paths;

@Getter
public class VirtualFile extends LightVirtualFile {
    private final String planPath;
    private final DefaultTreeModel testCasesTreeModel;

    public VirtualFile(String planPath, DefaultTreeModel testCasesTreeModel) {
        // The name shown on the Editor Tab
        super("Test Plan: " + Paths.get(planPath).getFileName().toString());
        this.planPath = planPath;
        this.testCasesTreeModel = testCasesTreeModel;
    }
}
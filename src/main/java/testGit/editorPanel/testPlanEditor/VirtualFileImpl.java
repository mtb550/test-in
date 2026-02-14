package testGit.editorPanel.testPlanEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;

import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Paths;

@Getter
public class VirtualFileImpl extends LightVirtualFile {
    private final String planPath;
    private final DefaultTreeModel testCasesTreeModel;

    public VirtualFileImpl(String planPath, DefaultTreeModel testCasesTreeModel) {
        super("Test Plan: " + Paths.get(planPath).getFileName().toString());
        this.planPath = planPath;
        this.testCasesTreeModel = testCasesTreeModel;
    }
}
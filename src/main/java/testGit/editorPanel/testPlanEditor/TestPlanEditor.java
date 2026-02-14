package testGit.editorPanel.testPlanEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import testGit.pojo.Config;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Path;

public class TestPlanEditor {

    public static void open(final Path planPath, ProjectPanel projectPanel, DefaultMutableTreeNode selectedNode) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());

        for (com.intellij.openapi.vfs.VirtualFile openFile : editorManager.getOpenFiles()) {
            if (openFile instanceof VirtualFile existing &&
                    existing.getPlanPath().equals(planPath.toString())) {
                editorManager.openFile(existing, true);
                return;
            }
        }

        VirtualFile virtualFile = new VirtualFile(
                planPath.toString(),
                (DefaultTreeModel) projectPanel.getTestCaseTree().getModel()
        );

        editorManager.openFile(virtualFile, true);
    }
}
package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import testGit.pojo.Config;
import testGit.projectPanel.ProjectPanel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Path;
import java.util.Arrays;

public class TestRunEditor {

    public static void open(final Path runPath, ProjectPanel projectPanel, DefaultMutableTreeNode selectedNode) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        String targetPath = runPath.toAbsolutePath().toString();

        VirtualFile existingFile = Arrays.stream(editorManager.getOpenFiles())
                .filter(f -> f instanceof VirtualFileImpl && ((VirtualFileImpl) f).getRunPath().equals(targetPath))
                .findFirst()
                .orElse(null);

        if (existingFile != null) {
            editorManager.openFile(existingFile, true);
        } else {
            VirtualFileImpl virtualFile = new VirtualFileImpl(
                    targetPath,
                    (DefaultTreeModel) projectPanel.getTestCaseTree().getModel()
            );
            editorManager.openFile(virtualFile, true);
        }
    }
}
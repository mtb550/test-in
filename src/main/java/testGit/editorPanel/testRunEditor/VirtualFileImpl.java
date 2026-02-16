package testGit.editorPanel.testRunEditor;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Paths;

@Getter
public class VirtualFileImpl extends LightVirtualFile {
    private final String runPath;
    private final DefaultTreeModel testCasesTreeModel;

    public VirtualFileImpl(@NotNull String runPath, @NotNull DefaultTreeModel testCasesTreeModel) {
        super(String.format("Test Run: %s", Paths.get(runPath).getFileName()));
        this.runPath = runPath;
        this.testCasesTreeModel = testCasesTreeModel;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
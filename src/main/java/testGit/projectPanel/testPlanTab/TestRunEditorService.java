package testGit.projectPanel.testPlanTab;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class TestRunEditorService {
    private final Map<VirtualFile, JComponent> editorMap = new HashMap<>();

    public static TestRunEditorService getInstance(Project project) {
        return project.getService(TestRunEditorService.class);
    }

    public void registerEditorComponent(VirtualFile file, JComponent component) {
        editorMap.put(file, component);
    }

    public JComponent getEditorComponent(VirtualFile file) {
        return editorMap.get(file);
    }
}

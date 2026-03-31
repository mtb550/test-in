package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.List;

public class ViewDetails extends DumbAwareAction {
    private final JBList<TestCaseDto> list;
    private final Path path;

    public ViewDetails(final JBList<TestCaseDto> list, final Path path) {
        super("View Details", "", AllIcons.Actions.PreviewDetails);
        this.list = list;
        this.path = path;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<TestCaseDto> selected = list.getSelectedValuesList();

        if (selected != null && !selected.isEmpty())
            ViewToolWindowFactory.showPanel(e.getProject(), selected, path);
    }
}

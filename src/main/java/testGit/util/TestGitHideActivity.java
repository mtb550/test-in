package testGit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestGitHideActivity implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ChangeListManager manager = ChangeListManager.getInstance(project);

        LocalChangeList list = manager.findChangeList("TestGit Automation");
        if (list == null) {
            manager.addChangeList("TestGit Automation", "Files managed by automation - do not commit");
        }
        return Unit.INSTANCE;
    }
}
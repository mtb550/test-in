package testGit.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;

public class PluginPostStartupActivity implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        System.out.println("PluginPostStartupActivity.execute()");

        Config.setProject(project);
        Config.setProjectBasePath(project.getBasePath());
        Config.setRootFolder();
        return Unit.INSTANCE;
    }
}
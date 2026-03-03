package testGit.settings;

import com.intellij.openapi.application.ApplicationManager;
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
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;

            Config.setProject(project);
            Config.setProjectBasePath(project.getBasePath());

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (project.isDisposed()) return;
                Config.setRootFolder();
                System.out.println("TestGit: Initial configuration complete.");
            });

        });

        return Unit.INSTANCE;
    }
}
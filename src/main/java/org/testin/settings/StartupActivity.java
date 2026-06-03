package org.testin.settings;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.Config;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.runner.TestCaseExecutionTracker;

import java.nio.file.Path;
import java.util.Optional;

public class StartupActivity implements ProjectActivity {

    public static void execute(@NotNull Project project) {
        Log.setProject(project);
        Log.setLogLevel(Log.Level.DEBUG);

        Log.info("StartupActivity.execute()");

        Config.setProject(project); // todo, to be removed as we can get the project from toolWindowManager

        AppSettingsState settings = AppSettingsState.getInstance();

        Path testinPath = null;

        if (settings.rootTestinPath != null && !settings.rootTestinPath.trim().isEmpty())
            testinPath = Path.of(settings.rootTestinPath);
        else
            Notifier.getInstance().warnWithAction(
                    "Testin Setup Required",
                    "Please configure the Root Testin Folder to enable test management features.",
                    "Open Settings",
                    () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, AppSettingsConfigurable.class)
            );

        Config.setTestinPath(testinPath);

        Path automationPath = null;

        if (settings.rootAutomationPath != null && !settings.rootAutomationPath.trim().isEmpty()) {
            String folderFormat = settings.rootAutomationPath.replace(".", "/");

            automationPath = Optional.ofNullable(project.getBasePath())
                    .map(Path::of)
                    .map(p -> p.resolve(folderFormat))
                    .orElse(null);
        }

        Config.setAutomationPath(automationPath);

        Log.info("testin Path: " + testinPath);
        Log.info("automation Path: " + automationPath);

        TestCaseExecutionTracker.initGlobalListener(Config.getProject());
    }

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super kotlin.Unit> continuation) {

        execute(project);
        return kotlin.Unit.INSTANCE;
    }
}
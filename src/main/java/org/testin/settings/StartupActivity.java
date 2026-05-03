package org.testin.settings;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.util.notifications.Notifier;
import org.testin.util.runner.TestCaseExecutionTracker;

import java.nio.file.Path;
import java.util.Optional;

public class StartupActivity {
    public static void execute(@NotNull Project project) {
        System.out.println("StartupActivity.execute()");

        Config.setProject(project); // todo, to be removed as we can get the project from toolWindowManager

        AppSettingsState settings = AppSettingsState.getInstance();

        Path testinPath = null;

        if (settings.rootTestinPath != null && !settings.rootTestinPath.trim().isEmpty())
            testinPath = Path.of(settings.rootTestinPath);
        else
            Notifier.warnWithAction(
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

        System.out.println("testin Path: " + testinPath);
        System.out.println("automation Path: " + automationPath);

        TestCaseExecutionTracker.initGlobalListener(Config.getProject());
    }
}
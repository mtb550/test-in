package org.testin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.coroutines.Continuation;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.runner.TestCaseExecutionTracker;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.util.Optional;

public final class StartupActivity implements ProjectActivity {

    public static void execute(final @NotNull Project project) {
        Log.setProject(project);

        AppSettingsState settings = AppSettingsState.getInstance();

        if (settings.rootTestinPath == null || settings.rootTestinPath.isEmpty()) {
            Log.info("First run detected — saving default settings to testinSettings.xml");
            settings.logLevel = Log.Level.INFO.name();
        }

        Log.setLogLevel(Log.Level.valueOf(settings.logLevel));

        Log.info("StartupActivity.execute()");

        Path testinPath = null;

        if (settings.rootTestinPath != null && !settings.rootTestinPath.trim().isEmpty()) {
            testinPath = Path.of(settings.rootTestinPath);
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    Services.getInstance(project, Notifier.class).warnWithAction(project,
                            "Testin Setup Required",
                            "Please configure the Root Testin Folder to enable test management features.",
                            "Open Settings",
                            () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfigurable.class)
                    );
                }
            });
        }

        Services.getInstance(project, Setting.class).setTestinPath(testinPath);

        Path automationPath = null;

        if (settings.rootAutomationPath != null && !settings.rootAutomationPath.trim().isEmpty()) {
            String folderFormat = settings.rootAutomationPath.replace(".", "/");

            automationPath = Optional.ofNullable(project.getBasePath())
                    .map(Path::of)
                    .map(p -> p.resolve(folderFormat))
                    .orElse(null);
        }

        Services.getInstance(project, Setting.class).setAutomationPath(automationPath);

        Log.info("testin Path: " + testinPath);
        Log.info("automation Path: " + automationPath);

        TestCaseExecutionTracker.initGlobalListener(project);
    }

    @Override
    public @NonNull Object execute(@NotNull Project project, @NotNull Continuation<? super kotlin.Unit> continuation) {
        execute(project);
        return kotlin.Unit.INSTANCE;
    }
}
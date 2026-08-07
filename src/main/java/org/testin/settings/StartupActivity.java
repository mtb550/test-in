package org.testin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.coroutines.Continuation;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.runner.TestCaseExecutionTracker;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.util.Optional;

public final class StartupActivity implements ProjectActivity {

    public static void execute(final @NotNull Project p) {
        Logger.setProject(p);

        AppSettingsState settings = AppSettingsState.getInstance();

        if (settings.rootTestinPath == null || settings.rootTestinPath.isEmpty()) {
            Logger.info("First run detected — saving default settings to testinSettings.xml");
            settings.logLevel = Logger.Level.INFO.name();
        }

        Logger.setLogLevel(Logger.Level.valueOf(settings.logLevel));

        Logger.info("StartupActivity.execute()");

        Path testinPath = null;

        if (settings.rootTestinPath != null && !settings.rootTestinPath.trim().isEmpty()) {
            testinPath = Path.of(settings.rootTestinPath);
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!p.isDisposed()) {
                    Services.getInstance(p, Notifier.class).warnWithAction(p,
                            "Testin Setup Required",
                            "Please configure the Root Testin Folder to enable test management features.",
                            "Open Settings",
                            () -> ShowSettingsUtil.getInstance().showSettingsDialog(p, SettingsConfigurable.class)
                    );
                }
            });
        }

        Services.getInstance(p, Setting.class).setTestinPath(testinPath);

        Path automationPath = null;

        if (settings.rootAutomationPath != null && !settings.rootAutomationPath.trim().isEmpty()) {
            String folderFormat = settings.rootAutomationPath.replace(".", "/");

            automationPath = Optional.ofNullable(p.getBasePath())
                    .map(Path::of)
                    .map(path -> path.resolve(folderFormat))
                    .orElse(null);
        }

        Services.getInstance(p, Setting.class).setAutomationPath(automationPath);

        Logger.info("testin Path: " + testinPath);
        Logger.info("automation Path: " + automationPath);

        if (testinPath != null) {
            Services.getInstance(p, ProjectIndexer.class).indexWithProgress();
        }

        TestCaseExecutionTracker.initGlobalListener(p);
    }

    @Override
    public @NonNull Object execute(@NotNull Project project, @NotNull Continuation<? super kotlin.Unit> continuation) {
        execute(project);
        return kotlin.Unit.INSTANCE;
    }
}
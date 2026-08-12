package org.testin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Key;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.runner.TestCaseExecutionTracker;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.nio.file.Path;

public final class StartupActivity implements ProjectActivity {

    private static final Key<Boolean> SOURCE_ROOT_CHECKED = Key.create("testin.sourceRootChecked");

    public static void execute(final @NotNull Project p) {
        AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

        if (settings.rootTestinPath == null || settings.rootTestinPath.isEmpty()) {
            Logger.info("First run detected — saving default settings to testinSettings.xml");
            settings.logLevel = Level.INFO.name();
        }

        Logger.setLogLevel(Level.valueOf(settings.logLevel));

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
        Logger.info("testin Path: " + testinPath);

        checkTestSourceRootOnce(p);

        if (testinPath != null) {
            Services.getInstance(p, ProjectIndexer.class).indexWithProgress();
        }

        TestCaseExecutionTracker.initGlobalListener(p);
    }

    /**
     * The Java test source root is detected automatically (see Tools.getTestSourceRoot);
     * warn once per project when none exists so the user knows automation code
     * generation (packages, classes, test methods) will be skipped.
     */
    private static void checkTestSourceRootOnce(final @NotNull Project p) {
        if (p.getUserData(SOURCE_ROOT_CHECKED) != null) return;
        p.putUserData(SOURCE_ROOT_CHECKED, Boolean.TRUE);

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    if (p.isDisposed()) return;
                    if (Services.getInstance(p, Tools.class).getTestSourceRoot(p) == null) {
                        Services.getInstance(p, Notifier.class).softShow(p,
                                "Java Test Source Not Found",
                                "Unable to find a Java test source package in this project - "
                                        + "creation of automation packages, classes, and methods will be skipped.");
                    }
                }));
    }

    @Override
    public @NotNull Object execute(@NotNull Project p, @NotNull Continuation<? super kotlin.Unit> continuation) {
        execute(p);
        return kotlin.Unit.INSTANCE;
    }
}
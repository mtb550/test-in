package org.testin.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Key;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.JavaSourceRoot;
import org.testin.config.TestinConfigService;
import org.testin.config.TestinProjectConfig;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.runner.TestCaseExecutionTracker;
import org.testin.services.Services;
import org.testin.util.Once;

import java.nio.file.Path;
import java.util.Optional;

public final class StartupActivity implements ProjectActivity {

    private static final @NotNull Key<Boolean> SOURCE_ROOT_CHECKED = Key.create("testin.sourceRootChecked");

    public static void execute(final @NotNull Project p) {
        final AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

        if (settings.rootTestinPath.isEmpty()) {
            Logger.info("First run detected — saving default settings to testinSettings.xml");
        }

        // AppSettingsState.loadState defaults a missing or blank level, so
        // Level.valueOf always has something to parse.
        Logger.setLogLevel(Level.valueOf(settings.logLevel));

        Logger.info("StartupActivity.execute()");

        Path testinPath = null;

        if (!settings.rootTestinPath.isBlank()) {
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

        Services.getInstance(p, TestinRoot.class).setPath(testinPath);
        Logger.info("testin Path: " + testinPath);

        checkTestSourceRootOnce(p);

        // Before the first index, never after it: the config names the test
        // project this repository exercises, and an index that started without it
        // would have to be thrown away and run again (#6).
        final TestinProjectConfig config = Services.getInstance(p, TestinConfigService.class).get();
        Logger.info(config.isBound()
                ? "Bound to test project '" + config.testinProject() + "'"
                : "No test project bound to " + p.getName());

        Optional.ofNullable(testinPath).ifPresent(root ->
                Services.getInstance(p, ProjectIndexer.class).indexWithProgress());

        TestCaseExecutionTracker.initGlobalListener(p);
    }

    /**
     * The Java test source root is detected automatically by JavaSourceRoot;
     * warn once per project when none exists so the user knows automation code
     * generation (packages, classes, test methods) will be skipped.
     */
    private static void checkTestSourceRootOnce(final @NotNull Project p) {
        if (!Once.claim(p, SOURCE_ROOT_CHECKED)) return;

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    if (p.isDisposed()) return;
                    if (JavaSourceRoot.find(p).isEmpty()) {
                        Services.getInstance(p, Notifier.class).softShow(p,
                                "Java Test Source Not Found",
                                "Unable to find a Java test source package in this project - "
                                        + "creation of automation packages, classes, and methods will be skipped.");
                    }
                }));
    }

    @Override
    public @NotNull Object execute(final @NotNull Project p,
                                   final @NotNull Continuation<? super kotlin.Unit> continuation) {
        execute(p);
        return kotlin.Unit.INSTANCE;
    }
}
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
import org.testin.indexer.DeletedNodes;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Level;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.runner.TestCaseExecutionTracker;
import org.testin.services.Services;
import org.testin.util.Once;

import java.nio.file.Path;

public final class StartupActivity implements ProjectActivity {

    private static final @NotNull Key<Boolean> STARTED = Key.create("testin.started");

    /**
     * Everything Testin does when a project opens, once per project.
     * <p>
     * Three doors lead here - the platform's startup extension, the tree tool
     * window and the view tool window - because any of the three can be the
     * first thing a tester touches, and each has to work on its own. They are
     * not alternatives, though: opening a project ran all of them, so the log
     * said {@code StartupActivity.execute()} twice, the settings were read
     * twice, and a full scan of the Testin root was started a second time while
     * the first was still walking it.
     * <p>
     * Guarded here rather than at each door, so a fourth door costs nothing and
     * cannot forget.
     */
    public static void execute(final @NotNull Project p) {
        if (!Once.claim(p, STARTED)) return;

        final @NotNull AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);

        // An unconfigured root is the empty path, not the absence of one - and
        // not the empty string either. This asked the stored value whether it
        // was empty while the warning below asked the normalized path, so a root
        // of nothing but spaces produced the setup warning and a log line
        // announcing defaults were being saved, in the same startup.
        final @NotNull Path testinPath = TestinRoot.normalize(settings.rootTestinPath);
        final boolean rootConfigured = TestinRoot.isConfigured(testinPath);

        if (!rootConfigured) {
            Logger.info("First run detected — saving default settings to testinSettings.xml");
        }

        // AppSettingsState.loadState defaults a missing or blank level, so
        // Level.valueOf always has something to parse.
        Logger.setLogLevel(Level.valueOf(settings.logLevel));

        Logger.info("StartupActivity.execute()");

        // Whatever the last run kept for an undo nobody is coming back for. Here
        // rather than at shutdown: the copies that matter are the ones a
        // shutdown never reached.
        Services.getInstance(DeletedNodes.class).sweep();

        if (!rootConfigured) {
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

        checkTestSourceRoot(p);

        // Before the first index, never after it: the config names the test
        // project this repository exercises, and an index that started without it
        // would have to be thrown away and run again (#6).
        final @NotNull TestinProjectConfig config = Services.getInstance(p, TestinConfigService.class).get();

        if (config.isBound()) {
            Logger.info("Bound to test project '" + config.projectName() + "'");
        } else {
            // Said rather than only logged. The name is not optional: it is what
            // the tree shows, what the reports are headed with, what a cloned
            // folder is called and what the server path ends in - so a
            // repository that has not been given one cannot do any of it, and
            // the tester should hear that when the project opens rather than
            // discover it at the first report.
            Logger.warn("No test project named in testin.yml for " + p.getName());

            ApplicationManager.getApplication().invokeLater(() -> {
                if (p.isDisposed()) return;

                Services.getInstance(p, Notifier.class).warn(p, "No Test Project Named",
                        "testin.yml does not say which test project this repository is about. "
                                + "Pick one in the Testin tool window, or set testinProject in the file.");
            });
        }

        if (TestinRoot.isConfigured(testinPath)) {
            Services.getInstance(p, ProjectIndexer.class).indexWithProgress();
        }

        TestCaseExecutionTracker.initGlobalListener(p);

        // Before any surface exists. Recording a verdict used to be something
        // the open editors did on the side, so a run in a session where none
        // had been opened stored nothing.
        TestCaseExecutionSubscriber.initRecording(p);
    }

    /**
     * The Java test source root is detected automatically by JavaSourceRoot;
     * warn when none exists so the user knows automation code generation
     * (packages, classes, test methods) will be skipped.
     * <p>
     * It kept a claim of its own while this method could run twice. It cannot
     * now, and a second flag for the same question is a second answer waiting to
     * disagree with the first.
     */
    private static void checkTestSourceRoot(final @NotNull Project p) {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    if (p.isDisposed()) return;
                    if (JavaSourceRoot.find(p).isEmpty()) {
                        Services.getInstance(p, Notifier.class).softRefuse(p,
                                "Java Test Source Not Found",
                                "Unable to find a Java test source package in this project - "
                                        + "creation of automation packages, classes, and methods will be skipped.");
                    }
                }));
    }

    @Override
    public @NotNull Object execute(final @NotNull Project p, final @NotNull Continuation<? super kotlin.Unit> continuation) {
        execute(p);
        return kotlin.Unit.INSTANCE;
    }
}
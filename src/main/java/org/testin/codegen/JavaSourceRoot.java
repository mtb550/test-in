package org.testin.codegen;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.logger.Logger;
import org.testin.model.Config;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Where generated automation code goes: the project's Java test source root.
 * <p>
 * There is none in a PyCharm project, or in a Java project with no test source
 * folder marked. Every generator treats that as "skip", never as a failure,
 * because test management works perfectly well without code generation.
 * <p>
 * That skip is the reason this class hands out work runners rather than only the
 * root itself. Five generators used to write the same four lines - find it, check
 * it, run a write action, log an IO failure - and three of the five logged
 * nothing at all, so the same missing root explained itself differently depending
 * on which generator noticed it (#66, finding 19).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaSourceRoot {

    /**
     * Work to do against the source root, allowed to fail the way file work
     * does - which is why it is not a plain {@link java.util.function.Consumer}.
     */
    @FunctionalInterface
    public interface RootWork {
        void run(final @NotNull VirtualFile root) throws IOException;
    }

    /**
     * Detected once and cached; the modules are scanned again only if the cached
     * root stopped being valid, which is what happens when the folder is deleted.
     */
    public static @NotNull Optional<VirtualFile> find(final @NotNull Project p) {
        final VirtualFile cached = Config.getTestSourceRoot();
        if (cached != null && cached.isValid()) return Optional.of(cached);

        for (final Module module : ModuleManager.getInstance(p).getModules()) {
            final List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
                    .getSourceRoots(JavaSourceRootType.TEST_SOURCE);

            if (!sourceRoots.isEmpty()) {
                Logger.debug("Found test source root: " + sourceRoots.getFirst());
                Config.setTestSourceRoot(sourceRoots.getFirst());
                return Optional.of(sourceRoots.getFirst());
            }
        }

        Logger.warn("No Java test source root found in the project.");
        return Optional.empty();
    }

    /**
     * Like {@link #find} and also says so, for the generators that create
     * something - the tester is left wondering why no code appeared otherwise.
     */
    public static @NotNull Optional<VirtualFile> findOrWarn(final @NotNull Project p) {
        final Optional<VirtualFile> root = find(p);
        if (root.isEmpty()) {
            Services.getInstance(p, Notifier.class).softShow(p,
                    "Java Test Source Not Found",
                    "Unable to find a Java test source package - automation code was not generated.");
        }
        return root;
    }

    /**
     * Runs the work against the root, and does nothing at all when there is
     * none. For a caller already inside a write action of its own.
     */
    public static void inRoot(final @NotNull Project p, final @NotNull RootWork work) throws IOException {
        final Optional<VirtualFile> root = find(p);
        if (root.isPresent()) work.run(root.get());
    }

    /**
     * The same, and tells the tester when there is no root - see
     * {@link #findOrWarn}.
     */
    public static void inRootOrWarn(final @NotNull Project p, final @NotNull RootWork work) throws IOException {
        final Optional<VirtualFile> root = findOrWarn(p);
        if (root.isPresent()) work.run(root.get());
    }

    /**
     * Runs the work against the root inside a write action, silently skipping a
     * project that has no root, and logging an IO failure as "Error " plus what
     * was being done. For tidying up after something the tester removed: there
     * is nothing to report when there was no root to remove from.
     *
     * @param whatFailed named in the log if the work raises, e.g. "removing class"
     */
    public static void writeInRoot(final @NotNull Project p, final @NotNull String whatFailed,
                                   final @NotNull RootWork work) {
        WriteAction.run(() -> {
            try {
                inRoot(p, work);
            } catch (final IOException ex) {
                Logger.info("Error " + whatFailed + ": " + ex.getMessage());
            }
        });
    }

    /**
     * For work that edits PSI, which the platform refuses outside a command -
     * "Must not change PSI outside command or undo-transparent action". A write
     * action is not enough, and the difference does not show until the edit is
     * attempted: moving a class threw here on the first drag (#51).
     *
     * @param title      names the command, as the platform shows it
     * @param whatFailed named in the log if the work raises, e.g. "moving class"
     */
    public static void commandInRoot(final @NotNull Project p, final @NotNull String title,
                                     final @NotNull String whatFailed, final @NotNull RootWork work) {
        WriteCommandAction.runWriteCommandAction(p, title, null, () -> {
            try {
                inRoot(p, work);
            } catch (final IOException ex) {
                Logger.info("Error " + whatFailed + ": " + ex.getMessage());
            }
        });
    }

    /**
     * The same for work that creates something, so a missing root is said out
     * loud: the tester made a node and is expecting a file to appear.
     *
     * @param whatFailed named in the log if the work raises, e.g. "creating package"
     */
    public static void writeInRootOrWarn(final @NotNull Project p, final @NotNull String whatFailed,
                                         final @NotNull RootWork work) {
        WriteAction.run(() -> {
            try {
                inRootOrWarn(p, work);
            } catch (final IOException ex) {
                Logger.info("Error " + whatFailed + ": " + ex.getMessage());
            }
        });
    }
}

package org.testin.codegen;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.logger.Logger;
import org.testin.model.Config;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.List;

/**
 * Where generated automation code goes: the project's Java test source root.
 * <p>
 * Null when the open project has none - a PyCharm project, or a Java project
 * with no test source folder marked. Every generator treats that as "skip",
 * never as a failure, because test management works perfectly well without
 * code generation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaSourceRoot {

    /**
     * Detected once and cached; the modules are scanned again only if the cached
     * root stopped being valid, which is what happens when the folder is deleted.
     */
    public static @Nullable VirtualFile find(final @NotNull Project p) {
        final VirtualFile cached = Config.getTestSourceRoot();
        if (cached != null && cached.isValid()) return cached;

        for (final Module module : ModuleManager.getInstance(p).getModules()) {
            final List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
                    .getSourceRoots(JavaSourceRootType.TEST_SOURCE);

            if (!sourceRoots.isEmpty()) {
                Logger.debug("Found test source root: " + sourceRoots.getFirst());
                Config.setTestSourceRoot(sourceRoots.getFirst());
                return sourceRoots.getFirst();
            }
        }

        Logger.warn("No Java test source root found in the project.");
        return null;
    }

    /**
     * Like {@link #find} and also says so, for the generators that skip without
     * one - the tester is left wondering why no code appeared otherwise.
     */
    public static @Nullable VirtualFile findOrWarn(final @NotNull Project p) {
        final VirtualFile root = find(p);
        if (root == null) {
            Services.getInstance(p, Notifier.class).softShow(p,
                    "Java Test Source Not Found",
                    "Unable to find a Java test source package - automation code was not generated.");
        }
        return root;
    }
}

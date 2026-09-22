/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.codegen;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Once;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaSourceRoot {
    private static final @NotNull Key<Boolean> NO_ROOT_SAID = Key.create("testin.noJavaTestSourceRoot.said");

    // UC-CODEGEN-020, Rule-CODEGEN-064, Rule-CODEGEN-066
    public static @NotNull Optional<VirtualFile> find(final @NotNull Project p) {
        final @NotNull TestSourceRoot remembered = Services.getInstance(p, TestSourceRoot.class);

        final @NotNull Optional<VirtualFile> cached = remembered.get();
        if (cached.isPresent()) return cached;

        for (final Module module : ModuleManager.getInstance(p).getModules()) {
            final @NotNull List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
                    .getSourceRoots(JavaSourceRootType.TEST_SOURCE);

            if (!sourceRoots.isEmpty()) {
                Logger.debug("Found test source root: " + sourceRoots.getFirst());
                remembered.set(sourceRoots.getFirst());
                return Optional.of(sourceRoots.getFirst());
            }
        }

        Logger.warn("No Java test source root found in the project.");
        return Optional.empty();
    }

    // UC-CODEGEN-020, Rule-CODEGEN-065, Rule-CODEGEN-072
    public static @NotNull Optional<VirtualFile> findOrWarn(final @NotNull Project p, final @NotNull String className) {
        final @NotNull Optional<VirtualFile> root = find(p);

        if (root.isEmpty() && Once.claim(p, NO_ROOT_SAID)) {
            Services.getInstance(p, Notifier.class).warn(p, Bundle.message("codegen.no.source.root.title"),
                    Bundle.message("codegen.no.source.root.message", className));
        }

        return root;
    }

    public static @NotNull Optional<VirtualFile> under(final @NotNull VirtualFile root, final @NotNull String relativePath) {
        return Optional.ofNullable(root.findFileByRelativePath(relativePath));
    }

    // UC-CODEGEN-018, Rule-CODEGEN-060
    public static void deleteUnder(final @NotNull VirtualFile root, final @NotNull String relativePath, final @NotNull Object requestor) {
        final @NotNull Optional<VirtualFile> found = under(root, relativePath).filter(VirtualFile::exists);
        if (found.isEmpty()) return;

        final @NotNull VirtualFile target = found.orElseThrow();

        try {
            target.delete(requestor);
            Logger.info("Removed generated code at: " + target.getPath());
        } catch (final IOException ex) {
            Logger.error("Could not remove generated code at " + target.getPath() + ": " + ex.getMessage());
        }
    }

    // UC-CODEGEN-001, Rule-CODEGEN-008
    public static @NotNull Optional<VirtualFile> packageFolder(final @NotNull VirtualFile root, final @NotNull List<String> packageSegments) {
        final @NotNull String relative = String.join("/", packageSegments);

        final @NotNull Optional<VirtualFile> folder;
        try {
            folder = Optional.ofNullable(VfsUtil.createDirectoryIfMissing(root, relative));
        } catch (final IOException ex) {
            Logger.error("Could not create the package folder " + relative + ": " + ex.getMessage());
            return Optional.empty();
        }

        if (folder.isEmpty()) Logger.error("Could not create the package folder: " + relative);
        return folder;
    }

    // UC-CODEGEN-001, Rule-CODEGEN-009, Rule-CODEGEN-010
    public static @NotNull Optional<VirtualFile> classFile(final @NotNull VirtualFile root, final @NotNull List<String> packageSegments, final @NotNull String className) {
        final @NotNull Optional<VirtualFile> folder = packageFolder(root, packageSegments);
        if (folder.isEmpty()) return Optional.empty();

        final @NotNull String fileName = className + ".java";
        final @NotNull Optional<VirtualFile> existing = Optional.ofNullable(folder.get().findChild(fileName));

        if (existing.isPresent()) {
            Logger.info("Test class already exists: " + existing.get().getPath());
            return existing;
        }

        final @NotNull String packageName = String.join(".", packageSegments);
        final @NotNull String declaration = packageName.isEmpty() ? "" : "package " + packageName + ";\n\n";

        try {
            final @NotNull VirtualFile file = folder.get().createChildData(JavaSourceRoot.class, fileName);
            VfsUtil.saveText(file, declaration + "public class " + className + " {\n}\n");

            Logger.info("Test class created at: " + file.getPath());
            return Optional.of(file);
        } catch (final IOException ex) {
            Logger.error("Could not create the test class " + fileName + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public static void inRoot(final @NotNull Project p, final @NotNull String whatFailed, final @NotNull RootWork work) {
        run(find(p), whatFailed, work);
    }

    private static void inRootOrWarn(final @NotNull Project p, final @NotNull String className, final @NotNull String whatFailed, final @NotNull RootWork work) {
        run(findOrWarn(p, className), whatFailed, work);
    }

    // UC-CODEGEN-020, Rule-CODEGEN-064
    public static @NotNull Optional<VirtualFile> fileInRootOrWarn(final @NotNull Project p, final @NotNull String className, final @NotNull String whatFailed, final @NotNull RootFile work) {
        final @NotNull Optional<VirtualFile> root = findOrWarn(p, className);
        if (root.isEmpty()) return Optional.empty();

        try {
            return work.from(root.orElseThrow());
        } catch (final IOException ex) {
            Logger.info("Error " + whatFailed + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    private static void run(final @NotNull Optional<VirtualFile> root, final @NotNull String whatFailed, final @NotNull RootWork work) {
        if (root.isEmpty()) return;

        try {
            work.run(root.get());
        } catch (final IOException ex) {
            Logger.info("Error " + whatFailed + ": " + ex.getMessage());
        }
    }

    // UC-CODEGEN-020, Rule-CODEGEN-065
    public static void writeInRoot(final @NotNull Project p, final @NotNull String whatFailed, final @NotNull RootWork work) {
        WriteAction.run(() -> inRoot(p, whatFailed, work));
    }

    public static void commandInRoot(final @NotNull Project p, final @NotNull String title, final @NotNull String whatFailed, final @NotNull RootWork work) {
        WriteCommandAction.runWriteCommandAction(p, title, null, () -> inRoot(p, whatFailed, work));
    }

    // UC-CODEGEN-020, Rule-CODEGEN-065
    public static void writeInRootOrWarn(final @NotNull Project p, final @NotNull String className, final @NotNull String whatFailed, final @NotNull RootWork work) {
        WriteAction.run(() -> inRootOrWarn(p, className, whatFailed, work));
    }

    @FunctionalInterface
    public interface RootWork {
        void run(final @NotNull VirtualFile root) throws IOException;
    }

    @FunctionalInterface
    public interface RootFile {
        @NotNull Optional<VirtualFile> from(final @NotNull VirtualFile root) throws IOException;
    }
}

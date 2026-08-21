package org.testin.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.util.Objects;

/**
 * Makes every generated file under a directory declare the package it is
 * actually in.
 * <p>
 * Moving or renaming a directory moves the files; it does not touch what they
 * say about themselves, and a Java file whose {@code package} line disagrees
 * with its folder does not compile. So the two operations that relocate files
 * both end here.
 * <p>
 * One owner, because the answer is the same for both and it is easy to get
 * subtly wrong: the correct package for a file is simply where it sits relative
 * to the test source root. The rename used to compute it from the new name and
 * the old parent package instead, which is the same answer arrived at the long
 * way round.
 * <p>
 * Runs inside the caller's write <em>command</em> action. A plain write action
 * is not enough for a PSI edit and the platform says so by throwing, which is
 * how this was found.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PackageDeclarations {

    /**
     * Rewrites the package of every {@code .java} file under {@code moved}.
     *
     * @param sourceRoot the test source root every package is named from
     * @param moved      the directory that was just moved or renamed
     */
    public static void retarget(final @NotNull Project p, final @NotNull VirtualFile sourceRoot,
                                final @NotNull VirtualFile moved) {
        for (final VirtualFile child : moved.getChildren()) {
            if (child.isDirectory()) {
                retarget(p, sourceRoot, child);
                continue;
            }

            if ("java".equals(child.getExtension())) retarget(p, sourceRoot, child, moved);
        }
    }

    /**
     * Rewrites one file's package to match the directory holding it.
     */
    public static void retarget(final @NotNull Project p, final @NotNull VirtualFile sourceRoot,
                                final @NotNull VirtualFile file, final @NotNull VirtualFile holder) {
        final PsiFile psiFile = PsiManager.getInstance(p).findFile(file);
        if (!(psiFile instanceof PsiJavaFile javaFile)) return;

        final String declared = packageOf(sourceRoot, holder);
        if (declared.equals(javaFile.getPackageName())) return;

        javaFile.setPackageName(declared);
        Logger.info("Package of " + file.getName() + " is now: " + (declared.isEmpty() ? "<default>" : declared));
    }

    /**
     * The package a directory stands for: its path below the source root, and
     * the default package for the root itself.
     */
    public static @NotNull String packageOf(final @NotNull VirtualFile sourceRoot, final @NotNull VirtualFile dir) {
        final String relative = VfsUtil.getRelativePath(dir, sourceRoot, '/');

        // Null when the directory is not under the root at all, which is not a
        // package this plugin generated - the default package is the honest
        // answer and leaves the file compiling either way.
        return Objects.requireNonNullElse(relative, "").replace('/', '.');
    }
}

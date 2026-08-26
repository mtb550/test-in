package org.testin.java.codegen.pkg;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.codegen.Renamed;
import org.testin.logger.Logger;
import org.testin.util.NameSanitizer;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RenameJavaPackage implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof Renamed renamed)) return;

        final @NotNull List<String> fqcn = Fqcn.ofPackage(renamed.dir());
        final @NotNull String newName = renamed.newName();

        JavaSourceRoot.find(p).ifPresentOrElse(
                testSourceRoot -> renameUnder(p, testSourceRoot, fqcn, newName),
                () -> Logger.info("Could not find Test Source Root in the project modules."));
    }

    /**
     * The rename itself, once the source root is known.
     */
    private void renameUnder(final @NotNull Project p, final @NotNull VirtualFile testSourceRoot, final @NotNull List<String> fqcn, final @NotNull String newName) {
        final @NotNull Optional<VirtualFile> found = JavaSourceRoot.under(testSourceRoot, String.join("/", fqcn))
                .filter(VirtualFile::isDirectory);
        if (found.isEmpty()) {
            Logger.info("Package not found for rename: " + String.join(".", fqcn));
            return;
        }

        final @NotNull VirtualFile pkgDir = found.orElseThrow();
        final @NotNull String newTop = NameSanitizer.packageName(newName);
        // The package path of the directory that CONTAINS the renamed package, e.g. "muath"
        // for a package at "muath.pkgtu" -> "muath.pkg". Needed so the new package
        // declaration keeps the full prefix instead of just "pkg".
        final @NotNull String parentPackage = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        WriteCommandAction.runWriteCommandAction(p, "Rename Package", null, () -> {
            try {
                pkgDir.rename(this, newTop);
                updatePackageDeclarations(p, pkgDir, newTop, parentPackage);
                Logger.info("Package renamed to: " + newName);
            } catch (final IOException ex) {
                Logger.info("Error renaming package: " + ex.getMessage());
            }
        });
    }

    private void updatePackageDeclarations(final @NotNull Project p, final @NotNull VirtualFile root, final @NotNull String newTop, final @NotNull String parentPackage) {
        updatePackageDeclarationsRecursive(p, root, root, newTop, parentPackage);
    }

    private void updatePackageDeclarationsRecursive(final @NotNull Project p, final @NotNull VirtualFile root, final @NotNull VirtualFile dir, final @NotNull String newTop, final @NotNull String parentPackage) {
        for (final VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                updatePackageDeclarationsRecursive(p, root, child, newTop, parentPackage);
            } else if ("java".equals(child.getExtension())) {
                // Not loaded and not Java are one answer: nothing to retarget.
                if (PsiManager.getInstance(p).findFile(child) instanceof PsiJavaFile javaFile) {
                    final @NotNull String newPackage = buildNewPackage(root, child.getParent(), newTop, parentPackage);
                    if (!newPackage.equals(javaFile.getPackageName())) {
                        javaFile.setPackageName(newPackage);
                    }
                }
            }
        }
    }

    private @NotNull String buildNewPackage(final @NotNull VirtualFile root, final @NotNull VirtualFile parentDir, final @NotNull String newTop, final @NotNull String parentPackage) {
        // The root itself is no path below the root, which VfsUtil says with a
        // null and which means the same as saying it with an empty string.
        final @NotNull String rel = Objects.requireNonNullElse(VfsUtil.getRelativePath(parentDir, root, '/'), "");
        final @NotNull String base = parentPackage.isEmpty() ? newTop : parentPackage + "." + newTop;

        if (rel.isEmpty()) return base;
        return base + "." + rel.replace('/', '.');
    }
}

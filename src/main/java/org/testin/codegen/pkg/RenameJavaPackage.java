package org.testin.codegen.pkg;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorAction;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.io.IOException;
import java.util.List;

public class RenameJavaPackage implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;
        execute(p, dir, dir.getName());
    }

    public void execute(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull String newName) {
        final Tools tools = Services.getInstance(p, Tools.class);

        final List<String> fqcn = tools.buildFqcnPackage(dir);
        final VirtualFile testSourceRoot = tools.getTestSourceRoot(p);
        if (testSourceRoot == null) {
            Logger.info("Could not find Test Source Root in the project modules.");
            return;
        }

        final VirtualFile pkgDir = testSourceRoot.findFileByRelativePath(String.join("/", fqcn));
        if (pkgDir == null || !pkgDir.isDirectory()) {
            Logger.info("Package not found for rename: " + String.join(".", fqcn));
            return;
        }

        final String newTop = tools.sanitizePackageName(newName);
        // The package path of the directory that CONTAINS the renamed package, e.g. "muath"
        // for a package at "muath.pkgtu" -> "muath.pkg". Needed so the new package
        // declaration keeps the full prefix instead of just "pkg".
        final String parentPackage = String.join(".", fqcn.subList(0, fqcn.size() - 1));

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

    private void updatePackageDeclarations(final @NotNull Project p, final @NotNull VirtualFile root,
                                           final @NotNull String newTop, final @NotNull String parentPackage) {
        updatePackageDeclarationsRecursive(p, root, root, newTop, parentPackage);
    }

    private void updatePackageDeclarationsRecursive(final @NotNull Project p, final @NotNull VirtualFile root,
                                                    final @NotNull VirtualFile dir, final @NotNull String newTop,
                                                    final @NotNull String parentPackage) {
        for (final VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                updatePackageDeclarationsRecursive(p, root, child, newTop, parentPackage);
            } else if ("java".equals(child.getExtension())) {
                final PsiFile psiFile = PsiManager.getInstance(p).findFile(child);
                if (psiFile instanceof PsiJavaFile javaFile) {
                    final String newPackage = buildNewPackage(root, child.getParent(), newTop, parentPackage);
                    if (!newPackage.equals(javaFile.getPackageName())) {
                        javaFile.setPackageName(newPackage);
                    }
                }
            }
        }
    }

    private @NotNull String buildNewPackage(final @NotNull VirtualFile root, final @NotNull VirtualFile parentDir,
                                            final @NotNull String newTop, final @NotNull String parentPackage) {
        final String rel = VfsUtil.getRelativePath(parentDir, root, '/');
        final String base = parentPackage.isEmpty() ? newTop : parentPackage + "." + newTop;

        if (rel == null || rel.isEmpty()) return base;
        return base + "." + rel.replace('/', '.');
    }
}

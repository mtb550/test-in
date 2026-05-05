package org.testin.util.automationGenerator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Tools;

import java.util.List;

public class CreateTestProject implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @NotNull String targetName, final @NotNull List<String> fqcn) {
        if (targetName.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // todo, add new step to remove all dots and chars and slashes.

                String safePackageName = Tools.toCamelCase(targetName);

                VirtualFile sourceRoot = Tools.getMainSourceRoot(project);

                if (sourceRoot != null) {
                    VirtualFile newPackage = VfsUtil.createDirectoryIfMissing(sourceRoot, safePackageName);

                    if (newPackage != null) {
                        System.out.println("[TRACE] Successfully created project package inside Source Root: " + newPackage.getPath());
                    }
                } else {
                    System.err.println("[WARNING] No Source Root found in the project. Please mark a directory as 'Sources Root'.");
                }

            } catch (Exception ex) {
                System.err.println("[ERROR] Failed to create project package: " + ex.getMessage());
            }
        }));
    }
}
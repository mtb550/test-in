package org.testin.util.autoGenerator;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.Tools;

import javax.swing.tree.TreePath;
import java.io.IOException;
import java.util.List;

public class CreateTestSetPackage implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @NotNull String targetName, final @Nullable TreePath path) {
        if (path == null) return;
        List<String> fqcn = Tools.getInstance().extractFqcn(path);

        String basePackage = String.join(".", fqcn);
        String fullPackagePath = basePackage.isEmpty() ? targetName : basePackage + "." + targetName;

        System.out.println("Ready to generate code for package: " + fullPackagePath);

        WriteAction.run(() -> {
            try {
                VirtualFile sourceRoot = Tools.getInstance().getTestSourceRoot(project);

                if (sourceRoot != null) {
                    String relativePath = fullPackagePath.replace('.', '/');

                    VirtualFile packageDir = VfsUtil.createDirectoryIfMissing(sourceRoot, relativePath);
                    System.out.println("Package created physically at: " + packageDir.getPath());
                } else {
                    System.out.println("Could not find Main Source Root in the project modules.");
                }

            } catch (IOException ex) {
                System.out.println("Error creating package: " + ex.getMessage());
            }
        });
    }
}
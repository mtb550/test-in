package org.testin.codegen.pkg;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;

import java.io.IOException;
import java.util.List;

public class RemoveJavaPackage implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;

        final List<String> fqcn = Fqcn.ofPackage(dir);
        if (fqcn.isEmpty()) return;
        final String packagePath = String.join("/", fqcn);

        WriteAction.run(() -> {
            try {
                final VirtualFile testSourceRoot = JavaSourceRoot.find(p);
                if (testSourceRoot == null) return;

                final VirtualFile pkgDir = testSourceRoot.findFileByRelativePath(packagePath);
                if (pkgDir != null && pkgDir.exists()) {
                    pkgDir.delete(this);
                    Logger.info("Package removed physically at: " + pkgDir.getPath());
                }
            } catch (final IOException ex) {
                Logger.info("Error removing package: " + ex.getMessage());
            }
        });
    }

}

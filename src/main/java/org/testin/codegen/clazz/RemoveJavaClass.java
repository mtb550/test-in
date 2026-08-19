package org.testin.codegen.clazz;

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

public class RemoveJavaClass implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;

        final List<String> fqcn = Fqcn.ofClass(p, dir);
        if (fqcn.isEmpty()) return;

        final String packagePath = String.join("/", fqcn.subList(0, fqcn.size() - 1));
        final String className = fqcn.getLast();
        final String fileName = className + ".java";

        WriteAction.run(() -> {
            try {
                final VirtualFile testSourceRoot = JavaSourceRoot.find(p);
                if (testSourceRoot == null) return;

                final VirtualFile pkgDir = testSourceRoot.findFileByRelativePath(packagePath);
                final VirtualFile classFile = pkgDir != null ? pkgDir.findChild(fileName) : null;

                if (classFile != null && classFile.exists()) {
                    classFile.delete(this);
                    Logger.info("Class removed physically at: " + classFile.getPath());
                }
            } catch (final IOException ex) {
                Logger.info("Error removing class: " + ex.getMessage());
            }
        });
    }

}

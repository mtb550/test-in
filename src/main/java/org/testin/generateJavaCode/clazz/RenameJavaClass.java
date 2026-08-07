package org.testin.generateJavaCode.clazz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.util.logger.Logger;

import java.io.IOException;
import java.nio.file.Path;

public class RenameJavaClass implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;
        final Path javaPath = dir.getPath().resolveSibling(dir.getPath().getFileName() + ".java");
        final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(javaPath);

        if (vf != null)
            WriteAction.run(() -> {
                try {
                    if (vf.exists()) {
                        vf.rename(this, dir.getName());
                        Logger.info("Class renamed to: " + dir.getName());
                    }
                } catch (final IOException ex) {
                    Logger.info("Error renaming class: " + ex.getMessage());
                }
            });

    }

}

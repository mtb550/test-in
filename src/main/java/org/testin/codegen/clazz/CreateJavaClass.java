package org.testin.codegen.clazz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.io.IOException;
import java.util.List;

public class CreateJavaClass implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestSetDirectoryDto dir)) return;
        final List<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnClass(p, dir);
        if (fqcn.isEmpty()) return;

        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final String className = fqcn.getLast();
        final String fileName = className + ".java";

        Logger.info("Ready to generate Test Class: " + className + " in package: " + fqcn);

        WriteAction.run(() -> {
            try {
                final VirtualFile testSourceRoot = Services.getInstance(p, Tools.class).getTestSourceRootOrWarn(p);
                if (testSourceRoot == null) {
                    Logger.info("Could not find Main Source Root in the project modules.");
                    return;
                }

                final VirtualFile vf = VfsUtil.createDirectoryIfMissing(testSourceRoot, path.replace(".", "/"));
                if (vf == null) {
                    Logger.error("Could not create package directory: " + path.replace(".", "/"));
                    return;
                }

                final VirtualFile existingFile = vf.findChild(fileName);
                if (existingFile != null) {
                    Logger.info("File already exists: " + existingFile.getPath());
                    return;
                }

                final VirtualFile javaFile = vf.createChildData(this, fileName);
                final String fileContent = "package " + path + ";\n\n" +
                        "public class " + className + " {\n" +
                        "    \n" +
                        "}\n";

                VfsUtil.saveText(javaFile, fileContent);
                Logger.info("Test Class created physically at: " + javaFile.getPath());

            } catch (final IOException ex) {
                Logger.info("Error creating test class: " + ex.getMessage());
            }
        });
    }
}
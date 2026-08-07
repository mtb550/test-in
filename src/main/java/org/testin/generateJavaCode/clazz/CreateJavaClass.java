package org.testin.generateJavaCode.clazz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.io.IOException;
import java.util.List;

public class CreateJavaClass implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestSetDirectoryDto dir)) return;
        final List<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnClass(p, dir);

        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final String className = fqcn.getLast();
        final String fileName = className + ".java";

        Logger.info("Ready to generate Test Class: " + className + " in package: " + fqcn);

        WriteAction.run(() -> {
            try {
                VirtualFile testSourceRoot = Services.getInstance(p, Tools.class).getTestSourceRoot(p);

                if (testSourceRoot != null) {
                    VirtualFile vf = VfsUtil.createDirectoryIfMissing(testSourceRoot, path.replace(".", "/"));

                    if (vf != null) {
                        VirtualFile existingFile = vf.findChild(fileName);

                        if (existingFile == null) {
                            VirtualFile javaFile = vf.createChildData(this, fileName);

                            String fileContent = "package " + path + ";\n\n" +
                                    "public class " + className + " {\n" +
                                    "    \n" +
                                    "}\n";

                            VfsUtil.saveText(javaFile, fileContent);
                            Logger.info("Test Class created physically at: " + javaFile.getPath());
                        } else {
                            Logger.info("File already exists: " + existingFile.getPath());
                        }
                    }
                } else {
                    Logger.info("Could not find Main Source Root in the project modules.");
                }

            } catch (final IOException ex) {
                Logger.info("Error creating test class: " + ex.getMessage());
            }
        });
    }
}
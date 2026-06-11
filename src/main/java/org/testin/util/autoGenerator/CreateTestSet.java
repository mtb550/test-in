package org.testin.util.autoGenerator;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.io.IOException;
import java.util.List;

public class CreateTestSet implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {

        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final String className = fqcn.getLast();
        final String fileName = className + ".java";

        Log.info("Ready to generate Test Class: " + className + " in package: " + fqcn);

        WriteAction.run(() -> {
            try {
                VirtualFile testSourceRoot = Services.getInstance(project, Tools.class).getTestSourceRoot(project);

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
                            Log.info("Test Class created physically at: " + javaFile.getPath());
                        } else {
                            Log.info("File already exists: " + existingFile.getPath());
                        }
                    }
                } else {
                    Log.info("Could not find Main Source Root in the project modules.");
                }

            } catch (IOException ex) {
                Log.info("Error creating test class: " + ex.getMessage());
            }
        });
    }
}
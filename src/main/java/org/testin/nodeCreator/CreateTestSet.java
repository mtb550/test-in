package org.testin.nodeCreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.settings.AppSettingsState;
import org.testin.util.Tools;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.Arrays;

public class CreateTestSet implements NodeCreator {
    private final @NotNull Project p;

    public CreateTestSet(final @NotNull Project p) {
        this.p = p;
    }

    @Override
    public @NonNull DirectoryDto execute(final @NonNull SimpleTree tree, final @NonNull String name, final @NonNull DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final @NonNull Path newDirPath) {
        final TestSetDirectoryDto ts = Services.getInstance(p, DirectoryMapper.class).getTestSetNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSet(ts);
        Services.getInstance(p, ProjectIndexer.class).createNode(tree, parentNode, ts);

        ApplicationManager.getApplication().invokeLater(() ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        ProjectRootManager rootManager = ProjectRootManager.getInstance(p);

                        VirtualFile testRoot = Arrays.stream(rootManager.getContentSourceRoots())
                                .filter(root -> rootManager.getFileIndex().isInTestSourceContent(root))
                                .findFirst()
                                .orElse(null);

                        if (testRoot != null) {
                            String basePath = Services.getInstance(p, AppSettingsState.class).rootAutomationPath;

                            String safePackageName = !parentDir.getName().isEmpty() ? Services.getInstance(p, Tools.class).toCamelCase(parentDir.getName()) : "";

                            String safeCamelClass = Services.getInstance(p, Tools.class).toCamelCase(name);
                            String safeClassName = safeCamelClass.substring(0, 1).toUpperCase() + safeCamelClass.substring(1);
                            safeClassName += "Test";

                            String relativePackagePath = basePath != null && !basePath.trim().isEmpty() ? basePath.replace(".", "/") : "";
                            String fullPackageDeclaration = basePath != null && !basePath.trim().isEmpty() ? basePath : "";

                            if (!safePackageName.isEmpty()) {
                                relativePackagePath = relativePackagePath.isEmpty() ? safePackageName : relativePackagePath + "/" + safePackageName;
                                fullPackageDeclaration = fullPackageDeclaration.isEmpty() ? safePackageName : fullPackageDeclaration + "." + safePackageName;
                            }

                            VirtualFile targetDirectory = VfsUtil.createDirectoryIfMissing(testRoot, relativePackagePath);

                            if (targetDirectory != null) {
                                String fileName = safeClassName + ".java";
                                VirtualFile existingFile = targetDirectory.findChild(fileName);

                                if (existingFile == null) {
                                    VirtualFile newClassFile = targetDirectory.createChildData(Tools.class, fileName);

                                    String classContent = buildClassContent(fullPackageDeclaration, safeClassName);
                                    VfsUtil.saveText(newClassFile, classContent);

                                    Logger.debug("Successfully created Java class: " + newClassFile.getPath());

                                } else {
                                    Logger.warn("Java class already exists: " + fileName);
                                }
                            }
                        } else {
                            Logger.info("No Test Source Root found in the project.");
                        }
                    } catch (final Exception ex) {
                        Logger.error("Failed to create Java class: " + ex.getMessage());
                    }
                }));

        return ts;
    }

    private String buildClassContent(final @NotNull String fullPackageName, final @NotNull String className) {
        StringBuilder content = new StringBuilder();

        if (!fullPackageName.isEmpty())
            content.append("package ").append(fullPackageName).append(";\n\n");

        content.append("public class ").append(className).append(" {\n\n");
        content.append("    // TODO: Auto-generated test class\n\n");
        content.append("}\n");

        return content.toString();
    }

}


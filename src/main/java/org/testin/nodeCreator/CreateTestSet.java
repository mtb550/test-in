package org.testin.nodeCreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.settings.AppSettingsState;
import org.testin.util.EditorUtil;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class CreateTestSet implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTreeNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetDirectoryDto ts = Services.getInstance(project, DirectoryMapper.class).getTestSetNode(project, newDirPath, parentDir);
        Services.getInstance(project, ProjectIndexer.class).addTestSet(ts);

        TreeUtilImpl util = Services.getInstance(project, TreeUtilImpl.class);
        util.createVf(project, this, parentDir.getPath(), ts.getName());
        util.createDataVf(project, this, newDirPath, DirectoryType.TS.getMarker());
        util.createNode(action.getTree(), parentNode, ts);

        createJavaClassInTestRoot(project, parentDir.getName(), name);
        Services.getInstance(project, EditorUtil.class).open(project, ts);

        return ts;
    }

    public VirtualFile inBackground(final @NotNull Project project, final Object requestor, final VirtualFile targetDirectory, final DirectoryDto parentDirDto, final DefaultMutableTreeNode parentNode, final SimpleTree tree, final String name) {
        String safeDirName = name.replaceAll("[\\\\/:*?\"<>|]", "_");

        VirtualFile sheetDir = targetDirectory.findChild(safeDirName);
        boolean isNewDirCreated = false;

        if (sheetDir == null) {
            try {
                sheetDir = targetDirectory.createChildDirectory(requestor, safeDirName);
            } catch (final IOException ex) {
                Logger.error("Can't create directory: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
            isNewDirCreated = true;

            TestSetDirectoryDto newTsDto = Services.getInstance(project, DirectoryMapper.class).setTestSetNode(project, Path.of(sheetDir.getPath()), parentDirDto);
            Services.getInstance(project, ProjectIndexer.class).addTestSet(newTsDto);
            Services.getInstance(project, TreeUtilImpl.class).createNode(tree, parentNode, newTsDto);
            createJavaClassInTestRoot(project, parentDirDto.getName(), safeDirName);
        }

        if (sheetDir.findChild(DirectoryType.TS.getMarker()) == null) {
            try {
                sheetDir.createChildData(requestor, DirectoryType.TS.getMarker());
            } catch (final IOException ex) {
                Logger.error("Can't create directory: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        }

        if (isNewDirCreated && tree != null && tree.getModel() instanceof DefaultTreeModel treeModel) {
            treeModel.reload(parentNode);
            tree.updateUI();
            tree.revalidate();
        }

        return sheetDir;
    }

    public void createJavaClassInTestRoot(final @NotNull Project project, final @NotNull String packageName, final @NotNull String className) {

        ApplicationManager.getApplication().invokeLater(() ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        ProjectRootManager rootManager = ProjectRootManager.getInstance(project);

                        VirtualFile testRoot = Arrays.stream(rootManager.getContentSourceRoots())
                                .filter(root -> rootManager.getFileIndex().isInTestSourceContent(root))
                                .findFirst()
                                .orElse(null);

                        if (testRoot != null) {
                            String basePath = AppSettingsState.getInstance().rootAutomationPath;

                            String safePackageName = !packageName.isEmpty() ? Services.getInstance(project, Tools.class).toCamelCase(packageName) : "";

                            String safeCamelClass = Services.getInstance(project, Tools.class).toCamelCase(className);
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
    }

    private String buildClassContent(String fullPackageName, String className) {
        StringBuilder content = new StringBuilder();

        if (fullPackageName != null && !fullPackageName.isEmpty()) {
            content.append("package ").append(fullPackageName).append(";\n\n");
        }

        content.append("public class ").append(className).append(" {\n\n");
        content.append("    // TODO: Auto-generated test class\n\n");
        content.append("}\n");

        return content.toString();
    }

}
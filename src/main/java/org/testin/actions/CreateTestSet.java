package org.testin.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.NodeCreator;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.settings.AppSettingsState;
import org.testin.util.EditorUtil;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;
import org.testin.util.logger.Log;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class CreateTestSet implements NodeCreator {

    @Override
    public DirectoryDto execute(final CreateTestNode action, final Project project, final String name, final DefaultMutableTreeNode parentNode, final DirectoryDto parentDir, final Path newDirPath) {
        TestSetDirectoryDto ts = DirectoryMapper.getInstance().testSetNode(project, newDirPath, parentDir);

        TreeUtilImpl.createVf(project, this, parentDir.getPath(), ts.getName());
        TreeUtilImpl.createDataVf(project, this, newDirPath, DirectoryType.TS.getMarker());
        TreeUtilImpl.createNode(action.getTree(), parentNode, ts);

        createJavaClassInTestRoot(project, parentDir.getName(), name);
        EditorUtil.getInstance().openEditor(project, ts);

        return ts;
    }

    public VirtualFile inBackground(final @NotNull Project project, final Object requestor, final VirtualFile targetDirectory, final DirectoryDto parentDirDto, final DefaultMutableTreeNode parentNode, final SimpleTree tree, final String name) throws IOException {
        String safeDirName = name.replaceAll("[\\\\/:*?\"<>|]", "_");

        VirtualFile sheetDir = targetDirectory.findChild(safeDirName);
        boolean isNewDirCreated = false;

        if (sheetDir == null) {
            sheetDir = targetDirectory.createChildDirectory(requestor, safeDirName);
            isNewDirCreated = true;

            TestSetDirectoryDto newTsDto = TestSetDirectoryDto
                    .builder()
                    .name(safeDirName)
                    .path(parentDirDto.getPath().resolve(safeDirName))
                    .build();

            TreeUtilImpl.createNode(tree, parentNode, newTsDto);
            createJavaClassInTestRoot(project, parentDirDto.getName(), safeDirName);
        }

        if (sheetDir.findChild(DirectoryType.TS.getMarker()) == null) {
            sheetDir.createChildData(requestor, DirectoryType.TS.getMarker());
        }

        if (isNewDirCreated && tree != null && tree.getModel() instanceof DefaultTreeModel treeModel) {
            treeModel.reload(parentNode);
            tree.updateUI();
            tree.revalidate();
        }

        return sheetDir;
    }

    public void createJavaClassInTestRoot(@NotNull final Project project, @NotNull final String packageName, @NotNull final String className) {

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

                            String safePackageName = !packageName.isEmpty() ? Tools.getInstance().toCamelCase(packageName) : "";

                            String safeCamelClass = Tools.getInstance().toCamelCase(className);
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

                                    Log.debug("[TRACE] Successfully created Java class: " + newClassFile.getPath());

                                } else {
                                    Log.warn("[WARNING] Java class already exists: " + fileName);
                                }
                            }
                        } else {
                            Log.info("[WARNING] No Test Source Root found in the project.");
                        }
                    } catch (Exception ex) {
                        Log.error("[ERROR] Failed to create Java class: " + ex.getMessage());
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
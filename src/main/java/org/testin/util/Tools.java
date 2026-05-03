package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.pojo.Config;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.settings.AppSettingsState;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tools {
    @NotNull
    public static String format(@Nullable final String text) {
        if (StringUtil.isEmptyOrSpaces(text)) return "";
        String s = text.trim();
        return StringUtil.capitalize(s) + ".";
    }

    public static void printTestSourceRoots(final Project project) {
        System.out.println("printTestSourceRoots.printTestSourceRoots()");

        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        for (VirtualFile root : ProjectRootManager.getInstance(project).getContentSourceRoots()) {
            if (fileIndex.isInTestSourceContent(root)) {
                System.out.println("Test Source Root: " + root.getPath());
            }
        }
    }

    public static void refreshPath(final Path path) {
        System.out.println("Tools.refreshPath()");

        if (path == null) return;

        VirtualFile virtualFile = VfsUtil.findFileByIoFile(path.toFile(), true);

        if (virtualFile != null) {
            virtualFile.refresh(true, true);
        } else {
            File parentFile = path.toFile().getParentFile();
            if (parentFile != null) {
                VfsUtil.markDirtyAndRefresh(true, true, true, parentFile);
            }
        }
    }

    public static int generateUniqueId() {
        return (int) (System.currentTimeMillis() % 100000);
    }

    public static void refreshFileSystem(final File ioFile) {
        System.out.println("Tools.refreshFileSystem()");
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile);

    }

    public static String toCamelCase(final String text) {
        if (text == null || text.isEmpty()) return text;
        String[] words = text.split("[\\W_]+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;
            if (i == 0) {
                result.append(word.substring(0, 1).toLowerCase()).append(word.substring(1).toLowerCase());
            } else {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    @Deprecated
    public static String fileToFqcn(final File file) {
        if (file == null) return "";

        String path = file.getAbsolutePath().replace("\\", "/");

        String marker = "/org/testin/";
        int index = path.indexOf(marker);

        if (index != -1) {
            path = path.substring(index + marker.length());
        } else {
            if (path.startsWith("org/testin/")) {
                path = path.substring("org/testin/".length());
            }
        }

        int dotIndex = path.lastIndexOf('.');
        if (dotIndex != -1) {
            path = path.substring(0, dotIndex);
        }

        StringBuilder fqcn = new StringBuilder();
        String basePath = AppSettingsState.getInstance().rootAutomationPath;

        if (basePath != null && !basePath.trim().isEmpty()) {
            fqcn.append(basePath.trim());
        }

        String[] segments = path.split("/");

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            if (segment.equals("testCases")) continue;

            String[] parts = segment.split("_", 3); // todo, no need. to be removed _
            String rawName = (parts.length >= 2) ? parts[1] : segment;

            String javaName = toCamelCase(rawName);

            if (i == segments.length - 1 && !javaName.isEmpty()) {
                javaName = javaName.substring(0, 1).toUpperCase() + javaName.substring(1) + "Test";
            }

            if (!javaName.isEmpty()) {
                if (!fqcn.isEmpty()) fqcn.append(".");
                fqcn.append(javaName);
            }
        }

        return fqcn.toString();
    }

    public static @NotNull List<String> extractLogicalPath(final @NotNull Path path) {
        if (path.toString().isEmpty()) return new ArrayList<>();

        String pathStr = path.toAbsolutePath().toString().replace("\\", "/");

        int markerIndex = pathStr.indexOf("/org/testin/");
        if (markerIndex != -1) {
            pathStr = pathStr.substring(markerIndex + "/org/testin/".length());
        } else if (pathStr.startsWith("org/testin/")) {
            pathStr = pathStr.substring("org/testin/".length());
        }

        String[] segments = pathStr.split("/");
        List<String> logicalPath = new ArrayList<>();

        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals("testCases")) continue;

            logicalPath.add(segment);
        }

        return logicalPath;
    }

    public static @NotNull List<String> generateFqcn(final @NotNull List<String> storedPath) {
        List<String> fqcnParts = new ArrayList<>();

        String basePath = AppSettingsState.getInstance().rootAutomationPath;
        if (basePath != null && !basePath.trim().isEmpty()) {
            fqcnParts.add(basePath.trim());
        }

        for (int i = 0; i < storedPath.size(); i++) {
            String segment = storedPath.get(i);

            if (segment == null || segment.isEmpty() || segment.equals("testCases")) continue;

            String formattedName = toCamelCase(segment);

            if (i == storedPath.size() - 1) {

                if (!formattedName.isEmpty()) {
                    formattedName = formattedName.substring(0, 1).toUpperCase() + formattedName.substring(1);
                }

                if (!formattedName.endsWith("Test")) {
                    formattedName += "Test";
                }
            }

            fqcnParts.add(formattedName);
        }

        return fqcnParts;
    }

    private static @NotNull String extractRelativePath(@NotNull Path path) {
        String pathStr = path.toAbsolutePath().toString().replace("\\", "/");

        int markerIndex = pathStr.indexOf("/org/testin/");
        if (markerIndex != -1) {
            pathStr = pathStr.substring(markerIndex + "/org/testin/".length());
        } else if (pathStr.startsWith("org/testin/")) {
            pathStr = pathStr.substring("org/testin/".length());
        }

        int dotIndex = pathStr.lastIndexOf('.');
        if (dotIndex != -1) {
            pathStr = pathStr.substring(0, dotIndex);
        }
        return pathStr;
    }

    public static boolean isEditorOpen(final String editorName) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        VirtualFile[] openFiles = editorManager.getOpenFiles();

        for (VirtualFile file : openFiles) {
            if (editorName.equals(file.getName())) {
                editorManager.openFile(file, true);
                return true;
            }
        }

        return false;
    }

    public static void closeEditor(final String editorName) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        VirtualFile[] openFiles = editorManager.getOpenFiles();

        for (VirtualFile file : openFiles) {
            if (editorName.equals(file.getName())) {
                editorManager.closeFile(file);
                break;
            }
        }
    }

    public static void updateChildrenPathsRecursive(final DefaultMutableTreeNode parentNode, final Path oldParentPath, final Path newParentPath) {
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);

            if (childNode.getUserObject() instanceof DirectoryDto childDir) {

                Path relativePath = oldParentPath.relativize(childDir.getPath());
                Path newChildPath = newParentPath.resolve(relativePath);

                childDir.setPath(newChildPath);
                updateChildrenPathsRecursive(childNode, oldParentPath, newParentPath);
            }
        }
    }

    public static String getFormattedDuration(final Duration duration) {
        if (duration == null) return null;
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    public static void createJavaPackageInTestRoot(@NotNull final Project project, @NotNull final String packageName) {
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

                            String safePackageName = toCamelCase(packageName);

                            String relativePackagePath = (basePath != null && !basePath.trim().isEmpty())
                                    ? basePath.replace(".", "/") + "/" + safePackageName
                                    : safePackageName;

                            VfsUtil.createDirectoryIfMissing(testRoot, relativePackagePath);

                        } else {
                            System.out.println("[WARNING] No Test Source Root found in the project.");
                        }
                    } catch (Exception ex) {
                        System.err.println("[ERROR] Failed to create Java package: " + ex.getMessage());
                    }
                }));
    }

    public static void createJavaClassInTestRoot(@NotNull final Project project, @NotNull final String packageName, @NotNull final String className) {

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

                            String safePackageName = !packageName.isEmpty() ? toCamelCase(packageName) : "";

                            String safeCamelClass = toCamelCase(className);
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

                                    System.out.println("[TRACE] Successfully created Java class: " + newClassFile.getPath());

                                } else {
                                    System.out.println("[WARNING] Java class already exists: " + fileName);
                                }
                            }
                        } else {
                            System.out.println("[WARNING] No Test Source Root found in the project.");
                        }
                    } catch (Exception ex) {
                        System.err.println("[ERROR] Failed to create Java class: " + ex.getMessage());
                    }
                }));
    }

    private static String buildClassContent(String fullPackageName, String className) {
        StringBuilder content = new StringBuilder();

        if (fullPackageName != null && !fullPackageName.isEmpty()) {
            content.append("package ").append(fullPackageName).append(";\n\n");
        }

        content.append("public class ").append(className).append(" {\n\n");
        content.append("    // TODO: Auto-generated test class\n\n");
        content.append("}\n");

        return content.toString();
    }

    public static void createJavaMethodInClass(@NotNull final Project project, @NotNull final List<String> fqcn, @NotNull final String testCaseName) {
        if (fqcn.isEmpty() || testCaseName.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, "Create Test Method", null, () -> {
                try {
                    String fqcnString = String.join(".", fqcn);

                    if (!fqcnString.endsWith("Test")) {
                        fqcnString += "Test";
                    }

                    String methodName = toCamelCase(testCaseName);

                    PsiClass targetClass = JavaPsiFacade.getInstance(project)
                            .findClass(fqcnString, GlobalSearchScope.projectScope(project));

                    if (targetClass != null) {
                        PsiMethod[] existingMethods = targetClass.findMethodsByName(methodName, false);

                        if (existingMethods.length == 0) {
                            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

                            String methodText = "@Test\n" +
                                    "public void " + methodName + "() {\n" +
                                    "    // TODO: Auto-generated test steps for " + testCaseName + "\n" +
                                    "}";

                            PsiMethod newMethod = factory.createMethodFromText(methodText, targetClass);
                            PsiElement addedElement = targetClass.add(newMethod);
                            CodeStyleManager.getInstance(project).reformat(addedElement);

                            System.out.println("[TRACE] Successfully injected method: " + methodName + " into " + fqcnString);
                        } else {
                            System.out.println("[WARNING] Method already exists: " + methodName);
                        }
                    } else {
                        System.out.println("[WARNING] Could not find class for FQCN: " + fqcnString);
                    }
                } catch (Exception ex) {
                    System.err.println("[ERROR] Failed to inject Java method: " + ex.getMessage());
                }
            });
        });
    }

    public static @Nullable VirtualFile getMainSourceRoot(final @NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();

        for (Module module : modules) {
            List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
                    .getSourceRoots(JavaSourceRootType.SOURCE);

            if (!sourceRoots.isEmpty()) {
                return sourceRoots.getFirst();
            }
        }

        return null;
    }


}

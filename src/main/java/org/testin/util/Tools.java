package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.editorPanel.testCaseEditor.TestEditor;
import org.testin.pojo.Config;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.settings.AppSettingsState;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Tools {

    private static final Tools INSTANCE = new Tools();

    public static Tools getInstance() {
        return INSTANCE;
    }

    @NotNull
    public String format(@Nullable final String text) {
        if (StringUtil.isEmptyOrSpaces(text)) return "";
        String s = text.trim();
        return StringUtil.capitalize(s) + ".";
    }

    public void printTestSourceRoots(final Project project) {
        System.out.println("printTestSourceRoots.printTestSourceRoots()");

        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        for (VirtualFile root : ProjectRootManager.getInstance(project).getContentSourceRoots()) {
            if (fileIndex.isInTestSourceContent(root)) {
                System.out.println("Test Source Root: " + root.getPath());
            }
        }
    }

    public void refreshPath(final Path path) {
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

    public int generateUniqueId() {
        return (int) (System.currentTimeMillis() % 100000);
    }

    public void refreshFileSystem(final File ioFile) {
        System.out.println("Tools.refreshFileSystem()");
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile);
    }

    public String toCamelCase(final String text) {
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
    public String fileToFqcn(final File file) {
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

    public @NotNull List<String> extractLogicalPath(final @NotNull Path path) {
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

    public @NotNull List<String> generateFqcn(final @NotNull List<String> storedPath) {
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

    private @NotNull String extractRelativePath(@NotNull Path path) {
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

    public boolean isEditorOpen(final String editorName) {
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

    public void closeEditor(final String editorName) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        VirtualFile[] openFiles = editorManager.getOpenFiles();

        for (VirtualFile file : openFiles) {
            if (editorName.equals(file.getName())) {
                editorManager.closeFile(file);
                break;
            }
        }
    }

    public void updateChildrenPathsRecursive(final DefaultMutableTreeNode parentNode, final Path oldParentPath, final Path newParentPath) {
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

    public String getFormattedDuration(final Duration duration) {
        if (duration == null) return null;
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    public void createJavaPackageInTestRoot(@NotNull final Project project, @NotNull final String packageName) {
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

    public @Nullable VirtualFile getMainSourceRoot(final @NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();

        for (Module module : modules) {
            List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
                    .getSourceRoots(JavaSourceRootType.TEST_SOURCE);

            if (!sourceRoots.isEmpty()) {
                return sourceRoots.getFirst();
            }
        }

        return null;
    }

    public List<String> extractFqcn(TreePath path) {
        List<String> fqcn = new ArrayList<>();

        System.out.println("--- Start Extracting FQCN ---");

        DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(lastNode.getUserObject() instanceof DirectoryDto dir)) return fqcn;

        Path dirPath = dir.getPath();
        System.out.println("dir path: " + dirPath);

        assert Config.getTestinPath() != null;

        try {
            Path relativePath = Config.getTestinPath().relativize(dirPath);
            System.out.println("Relative Path: " + relativePath);

            int testCasesIndex = -1;
            String tcdName = DirectoryType.TCD.getPathName();

            for (int i = 0; i < relativePath.getNameCount(); i++) {
                String nodeName = relativePath.getName(i).toString();
                if (nodeName.equals(tcdName) || nodeName.equalsIgnoreCase("testcases")) {
                    testCasesIndex = i;
                    break;
                }
            }

            if (testCasesIndex > 0) {
                String projectName = relativePath.getName(testCasesIndex - 1).toString();
                fqcn.add(projectName.replace(" ", "").toLowerCase());
                System.out.println("  -> Added Project Name: " + projectName);

                for (int i = testCasesIndex + 1; i < relativePath.getNameCount(); i++) {
                    String pkgName = relativePath.getName(i).toString();
                    fqcn.add(pkgName.replace(" ", "").toLowerCase());
                    System.out.println("  -> Added Package: " + pkgName);
                }
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Error calculating relative path: " + e.getMessage());
        }

        System.out.println("Final FQCN Result: " + fqcn);
        System.out.println("-----------------------------");
        return fqcn;
    }

    public String toPascalCase(String text) {
        if (text == null || text.trim().isEmpty()) return "";

        String[] words = text.split("[\\s_\\-]+");
        StringBuilder pascalCase = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                pascalCase.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }
        return pascalCase.toString();
    }

    public void openWithAssociatedProgram(VirtualFile virtualFile) {
        if (virtualFile == null || !virtualFile.exists()) {
            Notifier.error("Open Error", "The file does not exist.");
            return;
        }

        File file = new File(virtualFile.getPath());

        if (!Desktop.isDesktopSupported()) {
            Notifier.error("System Error", "Desktop operations are not supported on this system.");
            return;
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            Notifier.error("System Error", "The 'Open' action is not supported on this system.");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                desktop.open(file);
            } catch (IOException e) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Notifier.error("Execution Error", "Failed to open the file: " + e.getMessage())
                );
            }
        });
    }

    public void closeThenOpenTestEditor(final VirtualFile targetDirectory, final TestSetDirectoryDto ts) {
        if (targetDirectory == null || ts == null) return;

        final Project project = Config.getProject();
        final FileEditorManager editorManager = FileEditorManager.getInstance(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile fileToOpen = null;

            for (VirtualFile openFile : editorManager.getOpenFiles()) {
                if (openFile.getName().equals(targetDirectory.getName())) {
                    fileToOpen = openFile;
                    editorManager.closeFile(openFile);
                    break;
                }
            }

            if (fileToOpen == null) {
                TestEditor.open(ts);
                return;
            }

            editorManager.openFile(fileToOpen, true);
        });
    }
}
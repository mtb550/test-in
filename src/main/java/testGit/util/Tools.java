package testGit.util;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.dto.dirs.DirectoryDto;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.nio.file.Path;

public class Tools {
    @NotNull
    public static String format(@Nullable final String text) {
        if (StringUtil.isEmptyOrSpaces(text)) return "";
        String s = text.trim();
        return StringUtil.capitalize(s) + ".";
    }

    public static void printTestSourceRoots(Project project) {
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

    public static String toCamelCase(String text) {
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

    /**
     * Converts a physical file path into a Java Fully Qualified Class Name.
     * Example: ".../testGit/PR_IBRAM_AC/testCases/PA_ibram pkg_AC/TS_ibram 2_AC"
     * -> "src.test.ibram.ibramPkg.Ibram2"
     */
    public static String fileToFqcn(File file) {
        if (file == null) return "";

        String path = file.getAbsolutePath().replace("\\", "/");

        String marker = "/testGit/";
        int index = path.indexOf(marker);

        if (index != -1) {
            path = path.substring(index + marker.length());
        } else {
            if (path.startsWith("testGit/")) {
                path = path.substring("testGit/".length());
            }
        }

        int dotIndex = path.lastIndexOf('.');
        if (dotIndex != -1) {
            path = path.substring(0, dotIndex);
        }

        StringBuilder fqcn = new StringBuilder();
        String basePath = testGit.settings.AppSettingsState.getInstance().rootAutomationPath;

        if (basePath != null && !basePath.trim().isEmpty()) {
            fqcn.append(basePath.trim());
        }

        String[] segments = path.split("/");

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            if (segment.equals("testCases")) continue;

            String[] parts = segment.split("_", 3);
            String rawName = (parts.length >= 2) ? parts[1] : segment;

            String javaName = toCamelCase(rawName);

            if (i == segments.length - 1 && !javaName.isEmpty()) {
                javaName = javaName.substring(0, 1).toUpperCase() + javaName.substring(1) + "Test";
            }

            if (!javaName.isEmpty()) {
                if (fqcn.length() > 0) fqcn.append(".");
                fqcn.append(javaName);
            }
        }

        return fqcn.toString();
    }

    public static boolean isEditorOpen(String editorName) {
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

    public static void closeEditor(String editorName) {
        FileEditorManager editorManager = FileEditorManager.getInstance(Config.getProject());
        VirtualFile[] openFiles = editorManager.getOpenFiles();

        for (VirtualFile file : openFiles) {
            if (editorName.equals(file.getName())) {
                editorManager.closeFile(file);
                break;
            }
        }
    }

    public static void updateChildrenPathsRecursive(DefaultMutableTreeNode parentNode, Path oldParentPath, Path newParentPath) {
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

}

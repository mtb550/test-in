package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.testin.pojo.Config;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.Group;
import org.testin.pojo.Priority;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.settings.Setting;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Tools {

    private final Pattern SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9 _]");
    private final Pattern STEP_MUSH_PATTERN = Pattern.compile(".*\\s\\d+[-.].*");
    private final Pattern STEP_LINE_PATTERN = Pattern.compile("(\\s)(?=\\d+[-.])");
    private final Pattern STEP_CLEAN_PATTERN = Pattern.compile("^\\d+[-.]\\s*");

    public String sanitizePackageName(final @NotNull String name) {
        String removeKeyword = name.replace("-test-cases", "");
        String cleanName = SANITIZE_PATTERN.matcher(removeKeyword).replaceAll("").trim();
        String[] split = cleanName.split("[\\s_]+");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            String word = split[i];
            if (word.isEmpty()) continue;
            if (i == 0) sb.append(word.toLowerCase());
            else sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }

        String result = sb.toString();

        if (result.isEmpty()) return "generated" + System.currentTimeMillis();

        if (Character.isDigit(result.charAt(0))) result = "_" + result;
        return result;
    }

    public String sanitizeClassName(final @NotNull String name) {
        if (name.trim().isEmpty()) {
            return "DefaultTest";
        }

        String cleanName = SANITIZE_PATTERN.matcher(name).replaceAll("").trim();
        String[] split = cleanName.split("[\\s_]+");

        StringBuilder sb = new StringBuilder();
        for (String word : split) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        String result = sb.toString();

        if (result.isEmpty()) return "DefaultTest";

        if (Character.isDigit(result.charAt(0))) result = "_" + result;

        if (!result.toLowerCase().endsWith("test")) result += "Test";
        else if (result.equalsIgnoreCase("test")) result = "Test";

        return result;
    }

    public Path getProjectPath(final SimpleTree tree) {
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root != null && root.getUserObject() instanceof TestProjectDirectoryDto dir)
            return dir.getPath();
        return null;
    }

    public DirectoryDto getCurrentSelectedDirectory(final SimpleTree tree) {
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (parentNode.getUserObject() instanceof DirectoryDto parentDir) {
            return parentDir;
        }

        return null;
    }

    @NotNull
    public String format(final @Nullable String text) {
        if (StringUtil.isEmptyOrSpaces(text)) return "";
        String s = text.trim();
        return StringUtil.capitalize(s) + ".";
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

    public @Nullable VirtualFile getTestSourceRoot(final @NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();

        for (Module module : modules) {
            List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
                    .getSourceRoots(JavaSourceRootType.TEST_SOURCE);

            if (!sourceRoots.isEmpty()) {
                Log.debug("[TRACE] Found test source root: " + sourceRoots.getFirst());
                return sourceRoots.getFirst();
            }
        }

        Log.warn("[WARNING] No Test Source Root found in the project.");
        return null;
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

    public void openWithAssociatedProgram(final @NotNull Project project, final VirtualFile virtualFile) {
        if (virtualFile == null || !virtualFile.exists()) {
            Services.getInstance(project, Notifier.class).error(project, "Open Error", "The file does not exist.");
            return;
        }

        File file = new File(virtualFile.getPath());

        if (!Desktop.isDesktopSupported()) {
            Services.getInstance(project, Notifier.class).error(project, "System Error", "Desktop operations are not supported on this system.");
            return;
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            Services.getInstance(project, Notifier.class).error(project, "System Error", "The 'Open' action is not supported on this system.");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                desktop.open(file);
            } catch (IOException e) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(project, Notifier.class).error(project, "Execution Error", "Failed to open the file: " + e.getMessage())
                );
            }
        });
    }

    public String sanitizeDescription(final String rawDesc) {
        if (rawDesc == null || rawDesc.isBlank()) return "EMPTY_DESCRIPTION";
        String cleaned = SANITIZE_PATTERN.matcher(rawDesc).replaceAll("").trim();
        return cleaned.isEmpty() ? "EMPTY_DESCRIPTION" : cleaned;
    }

    public List<String> parseStepsSafe(final String stepsRaw) {
        if (stepsRaw == null || stepsRaw.isBlank()) {
            return new ArrayList<>();
        }

        String text = stepsRaw;

        if (!text.contains("\n") && STEP_MUSH_PATTERN.matcher(text).matches()) {
            text = STEP_LINE_PATTERN.matcher(text).replaceAll("\n");
        }

        return Arrays.stream(text.split("\n"))
                .map(line -> STEP_CLEAN_PATTERN.matcher(line).replaceFirst("").trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    public Priority parsePrioritySafe(final String priorityStr) {
        if (priorityStr == null || priorityStr.isBlank()) {
            return Priority.LOW;
        }
        try {
            return Priority.valueOf(priorityStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Priority.LOW;
        }
    }

    public ZonedDateTime parseDateSafe(final String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
        try {
            return LocalDateTime.parse(dateStr, Config.EXCEL_DATE_FORMATTER).atZone(ZoneId.systemDefault());
        } catch (Exception e) {
            return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
    }

    public List<Group> parseGroupsSafe(final String rawGroups) {
        if (rawGroups == null || rawGroups.isBlank()) {
            return new ArrayList<>();
        }

        return Arrays.stream(rawGroups.split(","))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .map(String::toUpperCase)
                .map(groupName -> {
                    try {
                        return Group.valueOf(groupName);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String sanitizeMethodName(final String description) {
        if (description == null || description.isEmpty()) return "testMethod";

        String[] words = description.split("[^a-zA-Z0-9]+");
        StringBuilder methodName = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            if (methodName.isEmpty()) {
                methodName.append(word.toLowerCase());
            } else {
                methodName.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    methodName.append(word.substring(1).toLowerCase());
                }
            }
        }
        return methodName.toString();
    }

    public ArrayList<String> buildPath2(final @Nullable List<String> parentPath, final @NotNull String newName) {
        ArrayList<String> newPath = new ArrayList<>();

        if (parentPath != null) newPath.addAll(parentPath);
        newPath.add(newName);

        return newPath;
    }

    public Path buildLocalPathFromList(final @NotNull Project project, final List<String> pathSegments) {
        Path currentPath = Path.of(Services.getInstance(project, Setting.class).getTestinPath().toString());

        if (pathSegments != null && !pathSegments.isEmpty()) {
            for (String segment : pathSegments) {
                currentPath = currentPath.resolve(segment);
            }
        }
        return currentPath;
    }

    public ArrayList<String> buildFqcnMethod(final TestCaseDto tc) {
        ArrayList<String> generatedFqcn = new ArrayList<>(tc.getParent().getPath2());

        int lastIdx = generatedFqcn.size() - 1;
        String className = sanitizeClassName(generatedFqcn.get(lastIdx));
        generatedFqcn.set(lastIdx, className);


        String methodName = sanitizeMethodName(tc.getDescription());
        generatedFqcn.add(methodName);

        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());

        for (int i = 0; i < lastIdx; i++) {
            String pkg = sanitizePackageName(generatedFqcn.get(i));
            generatedFqcn.set(i, pkg);
        }
        return generatedFqcn;
    }

    public @NotNull List<String> buildFqcnClass(DirectoryDto dir) {
        ArrayList<String> generatedFqcn = new ArrayList<>(dir.getPath2());

        int lastIdx = generatedFqcn.size() - 1;
        String className = sanitizeClassName(generatedFqcn.get(lastIdx));
        generatedFqcn.set(lastIdx, className);

        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());

        for (int i = 0; i < lastIdx; i++) {
            String pkg = sanitizePackageName(generatedFqcn.get(i));
            generatedFqcn.set(i, pkg);
        }
        return generatedFqcn;
    }

    public @NotNull List<String> buildFqcnPackage(DirectoryDto dir) {
        ArrayList<String> generatedFqcn = new ArrayList<>(dir.getPath2());
        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());
        generatedFqcn.replaceAll(this::sanitizePackageName);
        return generatedFqcn;
    }
}
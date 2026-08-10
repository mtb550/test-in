package org.testin.util;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
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
import org.testin.enums.DirectoryType;
import org.testin.enums.Group;
import org.testin.enums.Priority;
import org.testin.logger.Logger;
import org.testin.mappers.Config;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.services.Services;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Tools {

    private final NameSanitizer nameSanitizer = new NameSanitizer();
    private final TestDataParser testDataParser = new TestDataParser();

    public String sanitizePackageName(final @NotNull String s) {
        return nameSanitizer.packageName(s);
    }

    public String sanitizeClassName(final @NotNull String name) {
        return nameSanitizer.className(name);
    }

    public String removeSpecialChars(final @NotNull String s) {
        return nameSanitizer.removeSpecialChars(s);
    }

    public Path getProjectPath(final SimpleTree tree) {
        final Object root = TreeValueUtil.valueOf(tree.getModel().getRoot());
        if (root instanceof TestProjectDirectoryDto dir)
            return dir.getPath();
        return null;
    }

    public DirectoryDto getCurrentSelectedDirectory(final SimpleTree tree) {
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;

        final Object value = TreeValueUtil.valueOf(path.getLastPathComponent());
        if (value instanceof DirectoryDto parentDir) {
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

    public String getFormattedDuration(final Duration duration) {
        if (duration == null) return null;
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    public @Nullable VirtualFile getTestSourceRoot(final @NotNull Project p) {
        // Detected once at startup and cached; module scanning repeats only if the
        // cached root became invalid (e.g. the folder was deleted).
        final VirtualFile cached = Config.getTestSourceRoot();
        if (cached != null && cached.isValid()) return cached;

        Module[] modules = ModuleManager.getInstance(p).getModules();

        for (Module module : modules) {
            List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
                    .getSourceRoots(JavaSourceRootType.TEST_SOURCE);

            if (!sourceRoots.isEmpty()) {
                Logger.debug("[TRACE] Found test source root: " + sourceRoots.getFirst());
                Config.setTestSourceRoot(sourceRoots.getFirst());
                return sourceRoots.getFirst();
            }
        }

        Logger.warn("[WARNING] No Test Source Root found in the project.");
        return null;
    }

    /**
     * Like {@link #getTestSourceRoot} but also tells the user when no test source
     * root exists — used by the creation generators, which skip in that case.
     */
    public @Nullable VirtualFile getTestSourceRootOrWarn(final @NotNull Project p) {
        final VirtualFile root = getTestSourceRoot(p);
        if (root == null) {
            Services.getInstance(p, Notifier.class).softShow(p,
                    "Java Test Source Not Found",
                    "Unable to find a Java test source package - automation code was not generated.");
        }
        return root;
    }

    public void openWithAssociatedProgram(final @NotNull Project p, final VirtualFile virtualFile) {
        if (virtualFile == null || !virtualFile.exists()) {
            Services.getInstance(p, Notifier.class).error(p, "Open Error", "The file does not exist.");
            return;
        }

        File file = new File(virtualFile.getPath());

        if (!Desktop.isDesktopSupported()) {
            Services.getInstance(p, Notifier.class).error(p, "System Error", "Desktop operations are not supported on this system.");
            return;
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            Services.getInstance(p, Notifier.class).error(p, "System Error", "The 'Open' action is not supported on this system.");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                desktop.open(file);
            } catch (final IOException ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Services.getInstance(p, Notifier.class).error(p, "Execution Error", "Failed to open the file: " + ex.getMessage())
                );
            }
        });
    }

    public String sanitizeDescription(final String rawDesc) {
        return nameSanitizer.description(rawDesc);
    }

    public List<String> parseStepsSafe(final String stepsRaw) {
        return testDataParser.steps(stepsRaw);
    }

    public Priority parsePrioritySafe(final String priorityStr) {
        return testDataParser.priority(priorityStr);
    }

    public ZonedDateTime parseDateSafe(final String dateStr) {
        return testDataParser.date(dateStr);
    }

    public List<Group> parseGroupsSafe(final String rawGroups) {
        return testDataParser.groups(rawGroups);
    }

    public String sanitizeMethodName(final String description) {
        return nameSanitizer.methodName(description);
    }

    public ArrayList<String> buildPath2(final @Nullable List<String> parentPath, final @NotNull String newName) {
        ArrayList<String> newPath = new ArrayList<>();

        if (parentPath != null) newPath.addAll(parentPath);
        newPath.add(newName);

        return newPath;
    }

    public ArrayList<String> buildFqcnMethod(final @NotNull TestCaseDto tc) {
        ArrayList<String> generatedFqcn = new ArrayList<>(tc.getParent().getPath2());

        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());

        if (generatedFqcn.isEmpty()) {
            generatedFqcn.add("DefaultTest");
        }

        int lastIdx = generatedFqcn.size() - 1;
        String className = sanitizeClassName(generatedFqcn.get(lastIdx));
        generatedFqcn.set(lastIdx, className);

        String methodName = sanitizeMethodName(tc.getDescription());
        generatedFqcn.add(methodName);

        for (int i = 0; i < lastIdx; i++) {
            String pkg = sanitizePackageName(generatedFqcn.get(i));
            generatedFqcn.set(i, pkg);
        }
        return generatedFqcn;
    }

    public @NotNull List<String> buildFqcnClass(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        ArrayList<String> generatedFqcn = new ArrayList<>(dir.getPath2());

        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());

        if (generatedFqcn.isEmpty()) {
            Services.getInstance(p, Notifier.class).softShow(p, "empty fqcn , directory: " + dir.getPath());
        }

        int lastIdx = generatedFqcn.size() - 1;
        String className = sanitizeClassName(generatedFqcn.get(lastIdx));
        generatedFqcn.set(lastIdx, className);

        for (int i = 0; i < lastIdx; i++) {
            String pkg = sanitizePackageName(generatedFqcn.get(i));
            generatedFqcn.set(i, pkg);
        }
        return generatedFqcn;
    }

    public @NotNull List<String> buildFqcnPackage(final @NotNull DirectoryDto dir) {
        ArrayList<String> generatedFqcn = new ArrayList<>(dir.getPath2());
        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());
        if (generatedFqcn.isEmpty()) {
            generatedFqcn.add("generated");
        }
        generatedFqcn.replaceAll(this::sanitizePackageName);
        return generatedFqcn;
    }

    // ------------------------------------------------------------------
    // Keyboard shortcut helpers (see util.Shortcuts for the shared keys;
    // single-use keystrokes live as constants in their owning classes).
    // ------------------------------------------------------------------

    public static CustomShortcutSet customShortcut(final @NotNull KeyStroke key) {
        return new CustomShortcutSet(key);
    }

    public static Shortcut keyboardShortcut(final @NotNull KeyStroke key) {
        return new KeyboardShortcut(key, null);
    }

    public static String shortcutText(final @NotNull KeyStroke key) {
        return KeymapUtil.getKeystrokeText(key);
    }

    public static boolean matches(final @NotNull KeyEvent e, final @NotNull KeyStroke key) {
        return e.getKeyCode() == key.getKeyCode() && e.getModifiersEx() == key.getModifiers();
    }

    public DefaultActionGroup createSubGroup(final @NotNull String title, final @NotNull Icon icon, final @NotNull List<? extends DumbAwareAction> actions) {
        DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.getTemplatePresentation().setIcon(icon);
        for (AnAction action : actions)
            group.add(action);
        return group;
    }

    public String extractProjectNameFromUrl(final @NotNull String gitUrl) {
        return nameSanitizer.projectNameFromUrl(gitUrl);
    }
}

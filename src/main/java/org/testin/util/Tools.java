package org.testin.util;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.lang.annotations.MagicConstant;
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
import java.awt.event.InputEvent;
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

    private final @NotNull NameSanitizer nameSanitizer = new NameSanitizer();
    private final @NotNull TestDataParser testDataParser = new TestDataParser();

    public static @NotNull CustomShortcutSet customShortcut(final @NotNull KeyStroke key) {
        return new CustomShortcutSet(key);
    }

    public static @NotNull Shortcut keyboardShortcut(final @NotNull KeyStroke key) {
        return new KeyboardShortcut(key, null);
    }

    public static @NotNull String shortcutText(final @NotNull KeyStroke key) {
        return KeymapUtil.getKeystrokeText(key);
    }

    public static boolean matches(final @NotNull KeyEvent e, final @NotNull KeyStroke key) {
        // KeyStroke holds the same InputEvent mask getModifiersEx returns; saying
        // so lets the two be compared without the constant being called magic.
        @MagicConstant(flagsFromClass = InputEvent.class)
        final int required = key.getModifiers();

        return e.getKeyCode() == key.getKeyCode() && e.getModifiersEx() == required;
    }

    public @NotNull String sanitizePackageName(final @NotNull String s) {
        return nameSanitizer.packageName(s);
    }

    public @NotNull String sanitizeClassName(final @NotNull String name) {
        return nameSanitizer.className(name);
    }

    public @NotNull String removeSpecialChars(final @NotNull String s) {
        return nameSanitizer.removeSpecialChars(s);
    }

    public @Nullable Path getProjectPath(final @NotNull SimpleTree tree) {
        final Object root = TreeValueUtil.valueOf(tree.getModel().getRoot());
        if (root instanceof TestProjectDirectoryDto dir)
            return dir.getPath();
        return null;
    }

    public @Nullable DirectoryDto getCurrentSelectedDirectory(final @NotNull SimpleTree tree) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return null;

        final Object value = TreeValueUtil.valueOf(path.getLastPathComponent());
        if (value instanceof DirectoryDto parentDir) {
            return parentDir;
        }

        return null;
    }

    /**
     * A value as the details panel shows it: capitalised, and ended with a full
     * stop unless it already ends in something that closes it (#22).
     * <p>
     * Display only. The stored JSON is always what the tester typed - the
     * editable surfaces load the raw value, never this.
     */
    @NotNull
    public static String format(final @Nullable String text) {
        if (StringUtil.isEmptyOrSpaces(text)) return "";

        final String s = text.trim();
        return StringUtil.capitalize(s) + (endsClosed(s) ? "" : ".");
    }

    /**
     * True when a full stop would be wrong: the text already ends in
     * punctuation, or in a character that means it is not a sentence - a URL or
     * a path ending in '/', a parenthesised note, a code snippet.
     */
    private static boolean endsClosed(final @NotNull String s) {
        return ".!?:;/)".indexOf(s.charAt(s.length() - 1)) >= 0;
    }

    public @Nullable String getFormattedDuration(final @Nullable Duration duration) {
        if (duration == null) return null;
        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    public @Nullable VirtualFile getTestSourceRoot(final @NotNull Project p) {
        // Detected once at startup and cached; module scanning repeats only if the
        // cached root became invalid (e.g. the folder was deleted).
        final VirtualFile cached = Config.getTestSourceRoot();
        if (cached != null && cached.isValid()) return cached;

        final Module[] modules = ModuleManager.getInstance(p).getModules();

        for (final Module module : modules) {
            final List<VirtualFile> sourceRoots = ModuleRootManager.getInstance(module)
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

    public void openWithAssociatedProgram(final @NotNull Project p, final @Nullable VirtualFile virtualFile) {
        if (virtualFile == null || !virtualFile.exists()) {
            Services.getInstance(p, Notifier.class).error(p, "Open Error", "The file does not exist.");
            return;
        }

        final File file = new File(virtualFile.getPath());

        if (!Desktop.isDesktopSupported()) {
            Services.getInstance(p, Notifier.class).error(p, "System Error", "Desktop operations are not supported on this system.");
            return;
        }

        final Desktop desktop = Desktop.getDesktop();

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

    public @NotNull String sanitizeDescription(final @Nullable String rawDesc) {
        return nameSanitizer.description(rawDesc);
    }

    public @NotNull List<String> parseStepsSafe(final @Nullable String stepsRaw) {
        return testDataParser.steps(stepsRaw);
    }

    public @NotNull Priority parsePrioritySafe(final @Nullable String priorityStr) {
        return testDataParser.priority(priorityStr);
    }

    public @NotNull ZonedDateTime parseDateSafe(final @Nullable String dateStr) {
        return testDataParser.date(dateStr);
    }

    public @NotNull List<Group> parseGroupsSafe(final @Nullable String rawGroups) {
        return testDataParser.groups(rawGroups);
    }

    public @NotNull String sanitizeMethodName(final @Nullable String description) {
        return nameSanitizer.methodName(description);
    }

    // ------------------------------------------------------------------
    // Keyboard shortcut helpers (see util.Shortcuts for the shared keys;
    // single-use keystrokes live as constants in their owning classes).
    // ------------------------------------------------------------------

    public @NotNull ArrayList<String> buildPath2(final @Nullable List<String> parentPath, final @NotNull String newName) {
        final ArrayList<String> newPath = new ArrayList<>();

        if (parentPath != null) newPath.addAll(parentPath);
        newPath.add(newName);

        return newPath;
    }

    public @NotNull ArrayList<String> buildFqcnMethod(final @NotNull TestCaseDto tc) {
        final ArrayList<String> generatedFqcn = new ArrayList<>(tc.getParent().getPath2());

        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());

        if (generatedFqcn.isEmpty()) {
            generatedFqcn.add("DefaultTest");
        }

        final int lastIdx = generatedFqcn.size() - 1;
        final String className = sanitizeClassName(generatedFqcn.get(lastIdx));
        generatedFqcn.set(lastIdx, className);

        final String methodName = sanitizeMethodName(tc.getDescription());
        generatedFqcn.add(methodName);

        for (int i = 0; i < lastIdx; i++) {
            final String pkg = sanitizePackageName(generatedFqcn.get(i));
            generatedFqcn.set(i, pkg);
        }
        return generatedFqcn;
    }

    public @NotNull List<String> buildFqcnClass(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final ArrayList<String> generatedFqcn = new ArrayList<>(dir.getPath2());

        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());

        if (generatedFqcn.isEmpty()) {
            Services.getInstance(p, Notifier.class).softShow(p, "empty fqcn , directory: " + dir.getPath());
        }

        final int lastIdx = generatedFqcn.size() - 1;
        final String className = sanitizeClassName(generatedFqcn.get(lastIdx));
        generatedFqcn.set(lastIdx, className);

        for (int i = 0; i < lastIdx; i++) {
            final String pkg = sanitizePackageName(generatedFqcn.get(i));
            generatedFqcn.set(i, pkg);
        }
        return generatedFqcn;
    }

    public @NotNull List<String> buildFqcnPackage(final @NotNull DirectoryDto dir) {
        final ArrayList<String> generatedFqcn = new ArrayList<>(dir.getPath2());
        generatedFqcn.remove(DirectoryType.TCD.getDisplayedName());
        if (generatedFqcn.isEmpty()) {
            generatedFqcn.add("generated");
        }
        generatedFqcn.replaceAll(this::sanitizePackageName);
        return generatedFqcn;
    }

    public @NotNull DefaultActionGroup createSubGroup(final @NotNull String title, final @NotNull Icon icon, final @NotNull List<? extends DumbAwareAction> actions) {
        final DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.getTemplatePresentation().setIcon(icon);
        for (final AnAction action : actions)
            group.add(action);
        return group;
    }

    public @NotNull String extractProjectNameFromUrl(final @NotNull String gitUrl) {
        return nameSanitizer.projectNameFromUrl(gitUrl);
    }
}

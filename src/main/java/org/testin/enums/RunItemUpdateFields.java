package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.testRun.updateDialog.RunItemEditSection;
import org.testin.testRun.updateDialog.UpdateRunItemDialog;
import org.testin.util.KeyboardSet;
import org.testin.util.statusBar.IStatusBarItem;

import javax.swing.*;

@Getter
public enum RunItemUpdateFields implements IStatusBarItem {

    SAVE(
            "Save",
            KeyboardSet.Enter,
            new IStatusBarItem[]{},
            false,
            null
    ),

    ACTUAL_RESULT(
            "Actual Result",
            KeyboardSet.SetActualResult,
            AllIcons.Actions.Copy,
            new IStatusBarItem[]{SAVE},
            true,
            UpdateRunItemDialog::getActualResultSection
    ),

    BUG_SEVERITY(
            "Bug Severity",
            KeyboardSet.BugSeverity,
            AllIcons.Actions.Highlighting,
            new IStatusBarItem[]{SAVE},
            true,
            UpdateRunItemDialog::getBugSeveritySection
    ),

    BUG_PRIORITY(
            "Bug Priority",
            KeyboardSet.BugPriority,
            AllIcons.Actions.Report,
            new IStatusBarItem[]{SAVE},
            true,
            UpdateRunItemDialog::getBugPrioritySection
    ),

    ERROR_CAPTURE(
            "Error Capture",
            null,
            AllIcons.FileTypes.Text,
            new IStatusBarItem[]{SAVE},
            true,
            UpdateRunItemDialog::getErrorCaptureSection
    );

    private final @NotNull String name;
    private final @Nullable KeyboardSet shortcut;
    private final @Nullable Icon icon;
    private final @NotNull IStatusBarItem[] statusBarItems;
    private final boolean updateMenuItem;
    private final @Nullable SectionExtractor sectionExtractor;

    RunItemUpdateFields(final @NotNull String name, final @Nullable KeyboardSet shortcut, final @NotNull Icon icon, final @NotNull IStatusBarItem[] statusBarItems, final boolean updateMenuItem, final @Nullable SectionExtractor sectionExtractor) {
        this.name = name;
        this.shortcut = shortcut;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.updateMenuItem = updateMenuItem;
        this.sectionExtractor = sectionExtractor;
    }

    RunItemUpdateFields(final @NotNull String name, final @NotNull KeyboardSet shortcut, final @NotNull IStatusBarItem[] statusBarItems, final boolean updateMenuItem, final @Nullable SectionExtractor sectionExtractor) {
        this.name = name;
        this.shortcut = shortcut;
        this.icon = null;
        this.statusBarItems = statusBarItems;
        this.updateMenuItem = updateMenuItem;
        this.sectionExtractor = sectionExtractor;
    }

    @Override
    public String getShortcutText() {
        return shortcut != null ? shortcut.getShortcutText() : "";
    }

    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        if (this.shortcut != null) {
            new DumbAwareAction() {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    onTrigger.run();
                }
            }.registerCustomShortcutSet(this.shortcut.getCustomShortcut(), component);
        }
    }

    public interface SectionExtractor {
        RunItemEditSection create(final UpdateRunItemDialog ui);
    }
}

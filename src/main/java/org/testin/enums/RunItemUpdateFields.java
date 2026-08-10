package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.statusBar.IStatusBarItem;
import org.testin.testRun.updateDialog.UpdateRunItemDialog;
import org.testin.util.Shortcuts;
import org.testin.util.Shortcuts;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum RunItemUpdateFields implements IStatusBarItem {

    SAVE(
            "Save",
            Shortcuts.Enter,
            null,
            new IStatusBarItem[]{},
            false,
            null
    ),

    ACTUAL_RESULT(
            "Actual Result",
            Shortcuts.SetActualResult,
            AllIcons.Actions.Copy,
            new IStatusBarItem[]{SAVE},
            true,
            UpdateRunItemDialog::getActualResultSection
    ),

    BUG_SEVERITY(
            "Bug Severity",
            Shortcuts.BugSeverity,
            AllIcons.Actions.Highlighting,
            new IStatusBarItem[]{SAVE},
            true,
            UpdateRunItemDialog::getBugSeveritySection
    ),

    BUG_PRIORITY(
            "Bug Priority",
            Shortcuts.BugPriority,
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
    private final @Nullable Shortcuts shortcut;
    private final @Nullable Icon icon;
    private final @NotNull IStatusBarItem[] statusBarItems;
    private final boolean updateMenuItem;
    private final @Nullable SectionExtractor sectionExtractor;


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

}

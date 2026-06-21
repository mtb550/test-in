package org.testin.ui.testRun.update;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
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
            RunItemUpdateUI::getActualResultSection
    ),

    STATUS(
            "Status",
            KeyboardSet.SetStatus,
            AllIcons.General.Filter,
            new IStatusBarItem[]{SAVE},
            true,
            RunItemUpdateUI::getStatusSection
    ),

    ATTACHMENTS(
            "Attachments",
            null,
            AllIcons.FileTypes.Text,
            new IStatusBarItem[]{SAVE},
            true,
            RunItemUpdateUI::getAttachmentsSection
    );

    private final String name;
    private final KeyboardSet shortcut;
    private final Icon icon;
    private final IStatusBarItem[] statusBarItems;
    private final boolean updateMenuItem;
    private final SectionExtractor sectionExtractor;

    RunItemUpdateFields(final String name, final KeyboardSet shortcut, final Icon icon,
                        final IStatusBarItem[] statusBarItems, final boolean updateMenuItem,
                        final SectionExtractor sectionExtractor) {
        this.name = name;
        this.shortcut = shortcut;
        this.icon = icon;
        this.statusBarItems = statusBarItems;
        this.updateMenuItem = updateMenuItem;
        this.sectionExtractor = sectionExtractor;
    }

    RunItemUpdateFields(final String name, final KeyboardSet shortcut,
                        final IStatusBarItem[] statusBarItems, final boolean updateMenuItem,
                        final SectionExtractor sectionExtractor) {
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

    public void bindShortcut(final JComponent component, final Runnable onTrigger) {
        if (this.shortcut != null) {
            new DumbAwareAction() {
                @Override
                public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                    onTrigger.run();
                }
            }.registerCustomShortcutSet(this.shortcut.getCustomShortcut(), component);
        }
    }

    public interface SectionExtractor {
        RunItemEditSection create(final RunItemUpdateUI ui);
    }
}

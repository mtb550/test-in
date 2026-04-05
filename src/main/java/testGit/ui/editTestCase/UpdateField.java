package testGit.ui.editTestCase;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import testGit.util.KeyboardSet;
import testGit.util.statusBar.StatusBarItem;

import javax.swing.*;

@Getter
public enum UpdateField implements StatusBarItem {
    SAVE("Save", KeyboardSet.Enter, null),
    TITLE("Title", KeyboardSet.UpdateTestCaseTitle, AllIcons.Actions.Edit),
    EXPECTED("Expected Results", KeyboardSet.UpdateTestCaseExpected, AllIcons.General.InspectionsOK),
    STEPS("Steps", KeyboardSet.UpdateTestCaseSteps, AllIcons.Actions.ListFiles),
    PRIORITY("Priority", KeyboardSet.UpdateTestCasePriority, AllIcons.Nodes.Favorite),
    GROUPS("Groups", KeyboardSet.UpdateTestCaseGroups, AllIcons.Nodes.Tag);

    private final String label;
    private final KeyboardSet shortcut;
    private final Icon icon;

    UpdateField(final String label, final KeyboardSet shortcut, final Icon icon) {
        this.label = label;
        this.shortcut = shortcut;
        this.icon = icon;
    }

    @Override
    public String getShortcutText() {
        return shortcut.getShortcutText();
    }
}
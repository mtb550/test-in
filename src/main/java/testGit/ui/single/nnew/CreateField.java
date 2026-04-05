package testGit.ui.single.nnew;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import testGit.util.KeyboardSet;

import javax.swing.*;

@Getter
public enum CreateField {
    TITLE("Title", KeyboardSet.CreateTestCaseTitle, AllIcons.Actions.Edit),
    EXPECTED("Expected Results", KeyboardSet.CreateTestCaseExpected, AllIcons.General.InspectionsOK),
    STEPS("Steps", KeyboardSet.CreateTestCaseAddStep, AllIcons.Actions.ListFiles),
    PRIORITY("Priority", KeyboardSet.CreateTestCasePriority, AllIcons.Nodes.Favorite),
    GROUPS("Groups", KeyboardSet.CreateTestCaseGroups, AllIcons.Nodes.Tag);

    private final String label;
    private final KeyboardSet shortcut;
    private final Icon icon;

    CreateField(final String label, final KeyboardSet shortcut, Icon icon) {
        this.label = label;
        this.shortcut = shortcut;
        this.icon = icon;
    }
}
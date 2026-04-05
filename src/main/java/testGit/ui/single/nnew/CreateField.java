package testGit.ui.single.nnew;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import testGit.util.KeyboardSet;

import javax.swing.*;

@Getter
public enum CreateField {
    TITLE("Title", "Set title", KeyboardSet.CreateTestCaseTitle, AllIcons.Actions.Edit),
    EXPECTED("Expected Results", "Set expected result", KeyboardSet.CreateTestCaseExpected, AllIcons.General.InspectionsOK),
    STEPS("Steps", "Set step", KeyboardSet.CreateTestCaseStep, AllIcons.Actions.ListFiles),
    PRIORITY("Priority", "Select priority", KeyboardSet.CreateTestCasePriority, AllIcons.Nodes.Favorite),
    GROUPS("Groups", "Select group", KeyboardSet.CreateTestCaseGroups, AllIcons.Nodes.Tag);

    private final String label;
    private final String placeHolder;
    private final KeyboardSet shortcut;
    private final Icon icon;

    CreateField(final String label, final String placeHolder, final KeyboardSet shortcut, Icon icon) {
        this.label = label;
        this.placeHolder = placeHolder;
        this.shortcut = shortcut;
        this.icon = icon;
    }
}
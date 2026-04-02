package testGit.ui.bulk;

import com.intellij.icons.AllIcons;
import lombok.Getter;

import javax.swing.*;

@Getter
public enum UpdateField {
    TITLE("Title", 'T', AllIcons.Actions.Edit),
    EXPECTED("Expected Results", 'E', AllIcons.General.InspectionsOK),
    STEPS("Steps", 'S', AllIcons.Actions.ListFiles),
    PRIORITY("Priority", 'P', AllIcons.Nodes.Favorite),
    GROUPS("Groups", 'G', AllIcons.Nodes.Tag);

    private final String label;
    private final char shortcut;
    private final Icon icon;

    UpdateField(final String label, final char shortcut, Icon icon) {
        this.label = label;
        this.shortcut = shortcut;
        this.icon = icon;
    }
}
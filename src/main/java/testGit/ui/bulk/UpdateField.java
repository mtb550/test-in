package testGit.ui.bulk;

import lombok.Getter;

@Getter
public enum UpdateField {
    TITLE("Title", 'T'),
    EXPECTED("Expected Results", 'E'),
    STEPS("Steps", 'S'),
    PRIORITY("Priority", 'P');

    private final String label;
    private final char shortcut;

    UpdateField(String label, char shortcut) {
        this.label = label;
        this.shortcut = shortcut;
    }
}
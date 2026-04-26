package testGit.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.Messages;

public class CreateTestProjectDialog {

    public static String show() {
        String name = Messages.showInputDialog("Set the name:", "New Test Project", AllIcons.General.Add);

        if (name == null || name.isBlank()) {
            return null;
        }

        return name.replace("_", " "); // todo, add lass special chars to separate method and use it.
    }
}
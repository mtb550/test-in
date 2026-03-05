package testGit.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.Messages;

public class AddNewTestPackageDialog {

    public static String show() {
        String name = Messages.showInputDialog("Enter package name:", "New Test Package", AllIcons.Nodes.Package);

        if (name == null || name.isBlank())
            return null;

        return name.replace("_", " ");
    }
}
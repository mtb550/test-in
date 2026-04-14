package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class GenerateReportBtn extends ToolbarActionButton {

    public GenerateReportBtn(Runnable onClickAction) {
        super("Export Results", AllIcons.ToolbarDecorator.Export);
        addActionListener(e -> onClickAction.run());
    }
}
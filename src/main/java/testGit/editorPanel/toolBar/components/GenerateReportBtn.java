package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

// TODO: cange it to pop up and list the implemented actions, excel, pdf and html
public class GenerateReportBtn extends AbstractButton implements IToolbarItem {

    public GenerateReportBtn() {
        super("Export Results", AllIcons.ToolbarDecorator.Export);

        addActionListener(e -> {
            // TODO: JSON/Report export logic
            System.out.println("Exporting results...");
        });
    }
}
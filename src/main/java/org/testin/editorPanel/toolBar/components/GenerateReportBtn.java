package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import org.testin.util.logger.Log;

// TODO: cange it to pop up and list the implemented actions, excel, pdf and html
public class GenerateReportBtn extends AbstractButton implements IToolbarItem {

    public GenerateReportBtn() {
        super("Export Results", AllIcons.ToolbarDecorator.Export);

        addActionListener(e -> {
            // TODO: JSON/Report export logic, use same action in action package.
            Log.info("Exporting results...");
        });
    }
}
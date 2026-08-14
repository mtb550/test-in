package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class GridViewBtn extends AbstractButton implements IToolbarItem {

    public GridViewBtn(final @NotNull Runnable onSwitchToGrid) {
        // The icon the IDE paints on gradlew.bat: four equal squares, which reads
        // as a grid far more plainly than the table outline of Nodes.DataTables.
        // Its name is about the file type, not about Windows - it is a plain
        // platform icon, so nothing extra is depended on to use it here.
//        super("Grid View", AllIcons.FileTypes.MicrosoftWindows);
        super("Grid View", AllIcons.General.Groups);

        addActionListener(e -> onSwitchToGrid.run());
    }
}

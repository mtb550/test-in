package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.model.RunEditorAttributes;

import java.util.List;

public class RunDetailsPopupBtn extends AbstractDetailsPopupBtn<RunEditorAttributes> {

    public RunDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        // v5: Executed By and Executed At joined the attributes (#27). A saved
        // selection under the old key predates them and would never show them.
        super("testin.selectedDetails.run.v7",
                List.of(RunEditorAttributes.values()),
                RunEditorAttributes::getName,
                RunEditorAttributes::isDefaultToolBarSelected,
                RunEditorAttributes::valueOf,
                onToolBarDetailsSelectedChanged);
    }
}

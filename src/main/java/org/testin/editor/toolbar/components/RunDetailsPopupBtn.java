package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.model.RunEditorAttributes;

import java.util.Arrays;

public class RunDetailsPopupBtn extends AbstractDetailsPopupBtn<RunEditorAttributes> {

    public RunDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        // v5: Executed By and Executed At joined the attributes (#27). A saved
        // selection under the old key predates them and would never show them.
        super("testin.selectedDetails.run.v5",
                Arrays.stream(RunEditorAttributes.values())
                        .filter(RunEditorAttributes::isStandardToolBarOption)
                        .toList(),
                RunEditorAttributes::getName,
                RunEditorAttributes::valueOf,
                onToolBarDetailsSelectedChanged);
    }
}

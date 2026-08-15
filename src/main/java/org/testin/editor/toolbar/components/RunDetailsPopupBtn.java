package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.model.RunEditorAttributes;

import java.util.Arrays;

public class RunDetailsPopupBtn extends AbstractDetailsPopupBtn<RunEditorAttributes> {

    public RunDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        super("testin.selectedDetails.run.v4",
                Arrays.stream(RunEditorAttributes.values())
                        .filter(RunEditorAttributes::isStandardToolBarOption)
                        .toList(),
                RunEditorAttributes::getName,
                RunEditorAttributes::valueOf,
                onToolBarDetailsSelectedChanged);
    }
}

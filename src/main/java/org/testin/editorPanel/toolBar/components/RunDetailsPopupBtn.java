package org.testin.editorPanel.toolBar.components;

import org.testin.enums.RunEditorAttributes;

import java.util.Arrays;

public class RunDetailsPopupBtn extends AbstractDetailsPopupBtn<RunEditorAttributes> {

    public RunDetailsPopupBtn(final Runnable onToolBarDetailsSelectedChanged) {
        super("testin.selectedDetails.run.v4",
                Arrays.stream(RunEditorAttributes.values())
                        .filter(RunEditorAttributes::isStandardToolBarOption)
                        .toList(),
                RunEditorAttributes::getName,
                RunEditorAttributes::valueOf,
                onToolBarDetailsSelectedChanged);
    }
}

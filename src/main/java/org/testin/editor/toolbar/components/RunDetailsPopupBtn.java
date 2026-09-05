package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.model.RunEditorAttributes;

public class RunDetailsPopupBtn extends AbstractDetailsPopupBtn<RunEditorAttributes> {

    public RunDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        // The key is bumped only when a stored selection would be answering an
        // older question, because bumping discards what every tester ticked: v5
        // was Executed By and Executed At joining (#27), v7 the curated defaults
        // (#80). Order needed none - it is LOCKED_CHECKED, and a locked attribute
        // is forced into whatever was stored when the popup loads it.
        super("Details",
                "testin.selectedDetails.run.v7",
                RunEditorAttributes.class,
                onToolBarDetailsSelectedChanged);
    }
}

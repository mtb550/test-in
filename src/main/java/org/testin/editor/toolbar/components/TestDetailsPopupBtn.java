package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestEditorAttributes;

public class TestDetailsPopupBtn extends AbstractDetailsPopupBtn<TestEditorAttributes> {

    public TestDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        // v4 is the curated default set (#80). See RunDetailsPopupBtn for when
        // this is bumped and what it costs.
        super("Details",
                "testin.selectedDetails.test.v4",
                TestEditorAttributes.class,
                onToolBarDetailsSelectedChanged);
    }
}

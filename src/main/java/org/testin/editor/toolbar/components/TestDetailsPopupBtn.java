package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;

public class TestDetailsPopupBtn extends AbstractDetailsPopupBtn<TestEditorAttributes> {

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-022
    public TestDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        // v4 is the curated default set (#80). See RunDetailsPopupBtn for when
        // this is bumped and what it costs.
        super(FIELDS,
                "testin.selectedDetails.test.v4",
                TestEditorAttributes.class,
                onToolBarDetailsSelectedChanged);
    }
}

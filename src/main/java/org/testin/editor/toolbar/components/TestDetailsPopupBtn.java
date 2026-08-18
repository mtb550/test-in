package org.testin.editor.toolbar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestEditorAttributes;

import java.util.List;

public class TestDetailsPopupBtn extends AbstractDetailsPopupBtn<TestEditorAttributes> {

    public TestDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        super("testin.selectedDetails.test.v4",
                List.of(TestEditorAttributes.values()),
                TestEditorAttributes::getName,
                TestEditorAttributes::isDefaultToolBarSelected,
                TestEditorAttributes::valueOf,
                onToolBarDetailsSelectedChanged);
    }
}

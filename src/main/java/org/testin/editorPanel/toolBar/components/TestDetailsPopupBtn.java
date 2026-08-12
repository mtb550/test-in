package org.testin.editorPanel.toolBar.components;

import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestEditorAttributes;

import java.util.Arrays;

public class TestDetailsPopupBtn extends AbstractDetailsPopupBtn<TestEditorAttributes> {

    public TestDetailsPopupBtn(final @NotNull Runnable onToolBarDetailsSelectedChanged) {
        super("testin.selectedDetails.test.v2",
                Arrays.stream(TestEditorAttributes.values())
                        .filter(TestEditorAttributes::isStandardToolBarOption)
                        .toList(),
                TestEditorAttributes::getName,
                TestEditorAttributes::valueOf,
                onToolBarDetailsSelectedChanged);
    }
}

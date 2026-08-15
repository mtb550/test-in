package org.testin.editor.list;

import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

/**
 * The constructed list view: model, list, and the scroll pane wrapping it.
 * Built by {@link ListPanelBuilder}.
 */
public record ListView(@NotNull CollectionListModel<TestCaseDto> model,
                       @NotNull JBList<TestCaseDto> list,
                       @NotNull JBScrollPane scrollPane) {
}

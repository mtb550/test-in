/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
public record ListView(@NotNull CollectionListModel<TestCaseDto> model, @NotNull JBList<TestCaseDto> list, @NotNull JBScrollPane scrollPane) {
}

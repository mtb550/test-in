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

package org.testin.testcase.create;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import javax.swing.JList;
import java.util.Objects;
import java.util.Optional;

public class StatusSection implements CreateTestCaseSection {
    private final @NotNull ComboBox<TestCaseStatus> status;
    private final @NotNull JBPanel<?> wrapper;

    public StatusSection() {
        this.status = new ComboBox<>(TestCaseStatus.values());
        this.status.setSelectedItem(TestCaseStatus.PENDING);
        DialogStyle.asChoice(this.status);

        this.status.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(final @NotNull JList<? extends TestCaseStatus> list, final TestCaseStatus value, final int index, final boolean selected, final boolean hasFocus) {
                Optional.ofNullable(value).ifPresent(current -> {
                    append(Bundle.message("section.status.caption"));
                    append(current.getLabel());
                });
            }
        });

        this.wrapper = createWrapper(UpdateTestCaseFields.STATUS.getIcon(), this.status);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public boolean isPopupOpen() {
        return status.isPopupVisible();
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setStatus((TestCaseStatus) Objects.requireNonNull(status.getSelectedItem()));
    }

    // Rule-EDITOR-PANEL-194, Rule-PRODUCT-017
    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull Runnable repackAction) {
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return status;
    }

    @Override
    public void setEditable(final boolean editable) {
        status.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto) {
        status.setSelectedItem(dto.getStatus());
    }
}

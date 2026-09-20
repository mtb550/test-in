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

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;

import javax.swing.*;
import java.awt.*;

public interface CreateTestCaseSection {
    @NotNull JBPanel<?> getWrapper();

    default boolean isShown() {
        return getWrapper().getParent() != null;
    }

    default void showSection(final @NotNull JBPanel<?> contentPanel) {
        if (!isShown()) contentPanel.add(getWrapper());
        focusOnShow();
    }

    default void focusOnShow() {
        getFocusComponent().requestFocus();
    }

    default boolean isPopupOpen() {
        return false;
    }

    default boolean accepts() {
        return true;
    }

    void applyTo(final @NotNull TestCaseDto dto);

    void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull Runnable repackAction);

    @NotNull JComponent getFocusComponent();

    void setEditable(final boolean editable);

    void fillData(final @NotNull TestCaseDto dto, final @NotNull Runnable repackAction);

    default @NotNull Font fieldFont() {
        return JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    }

    default void styleField(final @NotNull EditorTextField field, final @NotNull CreateTestCaseFields describes) {
        field.setFont(fieldFont());
        field.setPlaceholder(describes.getPlaceholder());
        field.setShowPlaceholderWhenFocused(true);
        field.setBorder(JBUI.Borders.empty(10));
    }

    default @NotNull JBPanel<?> createWrapper(final @NotNull Icon icon, final @NotNull JComponent field) {
        return createWrapper(new JBLabel(icon), field);
    }

    default @NotNull JBPanel<?> createWrapper(final @NotNull JBLabel iconLabel, final @NotNull JComponent field) {
        final @NotNull JBPanel<?> iconPanel = new JBPanel<>(new GridBagLayout());
        iconPanel.setOpaque(false);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);

        final @NotNull JBPanel<?> wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(iconPanel, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.setBorder(JBUI.Borders.emptyTop(8));
        return wrapper;
    }
}
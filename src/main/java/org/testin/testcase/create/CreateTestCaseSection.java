package org.testin.testcase.create;

import com.intellij.ui.components.JBLabel;


import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.UIAction;

import javax.swing.*;
import java.awt.*;

public interface CreateTestCaseSection {
    @NotNull JBPanel<?> getWrapper();

    /**
     * Whether the tester has opened this section. An unopened section has not
     * been added to the dialog and so has no parent, and a missing parent
     * means nothing else here - which is why it is read in this one place.
     */
    default boolean isShown() {
        return getWrapper().getParent() != null;
    }

    default void showSection(final @NotNull JBPanel<?> contentPanel) {
        if (!isShown()) contentPanel.add(getWrapper());
        focusOnShow();
    }

    /**
     * What takes focus when the section opens: the component it already
     * advertises, unless a section wants something else or nothing at all.
     */
    default void focusOnShow() {
        getFocusComponent().requestFocus();
    }

    void applyTo(final @NotNull TestCaseDto dto);

    void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction);

    @NotNull JComponent getFocusComponent();

    void setEditable(final boolean editable);

    void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction);

    default @NotNull JBPanel<?> createIconPanel(final @NotNull Icon icon) {
        final JBPanel<?> iconPanel = new JBPanel<>(new GridBagLayout());
        iconPanel.setOpaque(false);
        final JBLabel iconLabel = new JBLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}
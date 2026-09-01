package org.testin.testcase.create;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.CreateTestCaseFields;
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

    /**
     * Whether what this section is holding can be written to the test case.
     * <p>
     * Asked before anything is applied, so a section that refuses stops the save
     * whole: a refused edit that still reached the file would stamp the case as
     * modified for a change nobody made.
     * <p>
     * A section that refuses says why itself - a balloon, a field marked in red
     * - because only it knows what is wrong with what it holds. Yes by default:
     * a section with nothing to check has nothing to refuse.
     */
    default boolean accepts() {
        return true;
    }

    void applyTo(final @NotNull TestCaseDto dto);

    void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction);

    @NotNull JComponent getFocusComponent();

    void setEditable(final boolean editable);

    void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction);

    /**
     * The font every field in the dialog is drawn in.
     * <p>
     * Derived when a section is built rather than held as a constant: the size
     * follows the IDE's own label font, so a value captured once at class load
     * would keep whatever size the IDE had the first time a dialog was opened.
     */
    default @NotNull Font fieldFont() {
        return JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    }

    /**
     * The look every text field in the dialog shares: the font, the placeholder
     * the field describes itself with, and the padding inside its frame.
     * <p>
     * Whether the field is one line is deliberately not set here. That is the
     * one thing the sections genuinely differ on - it is what makes Test Data
     * and Expected Result what they are - so each says it for itself.
     */
    default void styleField(final @NotNull EditorTextField field, final @NotNull CreateTestCaseFields describes) {
        field.setFont(fieldFont());
        field.setPlaceholder(describes.getPlaceholder());
        field.setShowPlaceholderWhenFocused(true);
        field.setBorder(JBUI.Borders.empty(10));
    }

    /**
     * A section's row: its icon on the left, and whatever the section lets the
     * tester edit filling the rest.
     * <p>
     * Every section is laid out this way, so it is laid out once. A section that
     * composed its own row is the one whose icon sits a few pixels off the
     * others the first time this spacing changes.
     */
    default @NotNull JBPanel<?> createWrapper(final @NotNull Icon icon, final @NotNull JComponent field) {
        final @NotNull JBPanel<?> wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(createIconPanel(icon), BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.setBorder(JBUI.Borders.emptyTop(8));
        return wrapper;
    }

    private @NotNull JBPanel<?> createIconPanel(final @NotNull Icon icon) {
        final @NotNull JBPanel<?> iconPanel = new JBPanel<>(new GridBagLayout());
        iconPanel.setOpaque(false);
        final @NotNull JBLabel iconLabel = new JBLabel(icon);
        iconLabel.setBorder(JBUI.Borders.empty(0, 10, 0, 8));
        iconPanel.add(iconLabel);
        return iconPanel;
    }
}
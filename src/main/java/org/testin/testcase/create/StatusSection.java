package org.testin.testcase.create;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestCaseStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.UIAction;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.util.Bundle;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

/**
 * Where a test case is in its own life: Reviewed, Pending, Disabled, To Be
 * Updated.
 * <p>
 * Built like the priority section, because it is the same thing to a tester -
 * one value chosen from a short fixed list. What it does not carry is a colour
 * or a key: the status enum has no colour of its own, and the letters that would
 * name these four are taken by fields a tester reaches far more often.
 * <p>
 * Offered by the update menu and not by the create dialog. A case being written
 * has not been reviewed yet and is not disabled, so the value it starts with is
 * the only one it could have - the same reason Order is on one dialog and not
 * the other (#162).
 */
public class StatusSection implements CreateTestCaseSection {

    private final @NotNull ComboBox<TestCaseStatus> status;
    private final @NotNull JBPanel<?> wrapper;

    public StatusSection() {
        this.status = new ComboBox<>(TestCaseStatus.values());
        this.status.setSelectedItem(TestCaseStatus.PENDING);
        this.status.setFont(fieldFont());

        this.status.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(final @NotNull JList<? extends TestCaseStatus> list, final TestCaseStatus value, final int index, final boolean selected, final boolean hasFocus) {
                // Swing renders the empty selection with no value at all, and
                // there is nothing to draw for it.
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

    /**
     * No key opens this section.
     * <p>
     * Every letter that would name it is already a field a tester reaches far
     * more often - `s` is Steps - and a status is not worth taking one from
     * them. The section is opened from the menu, which is where a row with no
     * key belongs (Rule-EDITOR-PANEL-194, and Rule-PRODUCT-017 - a capability
     * with no key says so rather than being silently unreachable).
     */
    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        // Nothing to bind.
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
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        status.setSelectedItem(dto.getStatus());
    }
}

package org.testin.testcase.create;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.Rank;
import org.testin.testcase.TestCaseOrder;
import org.testin.testcase.UIAction;
import org.testin.testcase.UpdateTestCaseFields;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Where the case runs in its test set, as the number the tester reads on its
 * card: "Position: 3 of 17".
 * <p>
 * The one section the create dialog never offers. A case being created has no
 * position to choose - it goes to the end of its set, which is where a tester
 * looks for something that has just arrived - so this is reachable only from
 * the update menu, and {@link #setupShortcut} deliberately binds nothing.
 * <p>
 * What it writes is a rank, not the number. Ranks are strings with room between
 * any two of them, so moving a case writes that one case and leaves the rest of
 * the set alone - the same one-file move a drag makes. The number is only how
 * the tester says which two cases to land between.
 */
public class OrderSection implements CreateTestCaseSection {

    final @NotNull Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);

    private final @NotNull Project p;

    @Getter
    private final @NotNull IntegerField position;
    private final @NotNull JBLabel outOf;
    private final @NotNull JBPanel<?> wrapper;

    public OrderSection(final @NotNull Project p) {
        this.p = p;

        // The platform's own numeric field: a plain text field that already
        // knows a range, refuses what is outside it and says why. Nothing to
        // write here for any of that - no filter on the document, no pattern,
        // no clamp on the way out - and the range is set when the set is known.
        this.position = new IntegerField("Position", 1, 1);
        this.position.setFont(fieldFont);
        this.position.setColumns(4);

        this.outOf = new JBLabel("of 1");
        this.outOf.setFont(fieldFont);
        this.outOf.setBorder(JBUI.Borders.emptyLeft(10));

        final @NotNull JBPanel<?> field = new JBPanel<>(new BorderLayout());
        field.setOpaque(false);
        field.add(this.position, BorderLayout.WEST);
        field.add(this.outOf, BorderLayout.CENTER);

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(UpdateTestCaseFields.ORDER.getIcon()), BorderLayout.WEST);
        this.wrapper.add(field, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    /**
     * The case's place in its set when the dialog opened, and how many places
     * there are - both read from the set rather than from the case, because a
     * case carries a rank and not a number.
     */
    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        final @NotNull List<TestCaseDto> inSet = ExecutionPosition.setOf(p, dto);
        final int size = Math.max(1, inSet.size());
        final int current = Math.min(TestCaseOrder.positionOf(inSet, dto), size);

        position.setMaxValue(size);

        // Where the case already is, as the value and as the fallback: text the
        // field cannot read as a position in range means leave it where it is,
        // which is the only harmless answer to give.
        position.setDefaultValue(current);
        position.setValue(current);

        outOf.setText("of " + size);
    }

    /**
     * Ranks the case between the two it was asked to land between, as they sit
     * in the set with this case taken out of it.
     * <p>
     * Taken out first because the tester reads positions on the list they are
     * looking at: moving the third case of five to fourth means landing between
     * the cases now at three and four, and counting this case among them would
     * put it back where it was.
     */
    /**
     * Refuses a position the field cannot read as one, in the platform's own
     * words - "Value must be between 1 and 17" - rather than quietly leaving the
     * case where it is and reporting the edit as saved.
     */
    @Override
    public boolean accepts() {
        try {
            position.validateContent();
            return true;

        } catch (final ConfigurationException invalid) {
            // getMessageHtml rather than getMessage, which is deprecated. A
            // balloon renders HTML, so the chunk goes in as it is.
            Services.getInstance(p, Notifier.class).softRefuse(p, invalid.getMessageHtml().toString());
            return false;
        }
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        final @NotNull List<TestCaseDto> inSet = ExecutionPosition.setOf(p, dto);
        final int target = position.getValue();

        // Where it already is: a rank is a new string every time it is written,
        // so re-ranking a case that has not moved would rewrite its file and
        // change the set's order in the commit for a move nobody made.
        if (target == TestCaseOrder.positionOf(inSet, dto)) return;

        final @NotNull List<TestCaseDto> others = inSet.stream()
                .filter(tc -> !tc.getId().equals(dto.getId()))
                .toList();

        // An empty rank on either side is "nothing on that side", which is what
        // Rank.between already means by it - so the first and last places need
        // no branch of their own.
        final @NotNull String before = target > 1 ? others.get(target - 2).getOrder() : "";
        final @NotNull String after = target <= others.size() ? others.get(target - 1).getOrder() : "";

        dto.setOrder(Rank.between(before, after));
    }

    /**
     * Nothing: the create dialog is the only caller that offers a section by a
     * shortcut of its own, and it does not offer this one.
     */
    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        // See the class comment: a case being created has no position to choose.
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return position;
    }

    @Override
    public void setEditable(final boolean editable) {
        position.setEnabled(editable);
    }
}

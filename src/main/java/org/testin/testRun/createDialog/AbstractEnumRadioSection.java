package org.testin.testRun.createDialog;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.TestRunItems;
import org.testin.testRun.updateDialog.RunItemEditSection;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Radio-button section over an enum field of {@link TestRunItems}; the concrete
 * sections only supply the enum values and the accessors.
 */
public abstract class AbstractEnumRadioSection<E extends Enum<E>> implements RunItemEditSection {

    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 4f);

    @Getter
    private final ButtonGroup buttonGroup;
    private final JBPanel<?> wrapper;
    private final @NotNull Function<TestRunItems, E> getter;
    private final @NotNull BiConsumer<TestRunItems, E> setter;
    private E selected;

    protected AbstractEnumRadioSection(final E @NotNull [] values,
                                       final @NotNull Function<E, String> displayName,
                                       final @NotNull Function<String, E> parser,
                                       final @NotNull Function<TestRunItems, E> getter,
                                       final @NotNull BiConsumer<TestRunItems, E> setter) {
        this.getter = getter;
        this.setter = setter;
        this.buttonGroup = new ButtonGroup();

        JBPanel<?> radioPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radioPanel.setOpaque(false);

        for (final E value : values) {
            JRadioButton rb = new JRadioButton(displayName.apply(value));
            rb.setFont(fieldFont);
            rb.setOpaque(false);
            rb.setActionCommand(value.name());
            rb.addActionListener(e -> selected = parser.apply(e.getActionCommand()));
            buttonGroup.add(rb);
            radioPanel.add(rb);
        }

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(AllIcons.General.Filter), BorderLayout.WEST);
        this.wrapper.add(radioPanel, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JBPanel<?> contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
    }

    @Override
    public void fillData(final @NotNull TestRunItems runItem) {
        final E value = getter.apply(runItem);
        selected = value;

        // A hand-edited or legacy run JSON can deserialize the field to null.
        if (value == null) {
            buttonGroup.clearSelection();
            return;
        }

        Enumeration<AbstractButton> e = buttonGroup.getElements();
        while (e.hasMoreElements()) {
            AbstractButton b = e.nextElement();
            if (b.getActionCommand().equals(value.name())) {
                b.setSelected(true);
                break;
            }
        }
    }

    @Override
    public void applyTo(final @NotNull TestRunItems runItem) {
        if (wrapper.getParent() != null && selected != null) {
            setter.accept(runItem, selected);
        }
    }

    @Override
    public JComponent getFocusComponent() {
        return wrapper;
    }
}

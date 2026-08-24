package org.testin.testrun;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunConfiguration;
import org.testin.ui.framework.DialogComponent;

import java.util.Optional;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * The run-configuration part of the create-run dialog: the (fixed) run name,
 * change log, commit id, and one editable combo per configured field —
 * collapsible, as before. A framework dialog component; the selection tree is
 * a separate component.
 */
@Getter
public class RunConfigurationForm implements DialogComponent {

    /**
     * The configuration is open when the dialog opens, rather than folded away
     * behind its header.
     * <p>
     * Named because {@code true} at the call site says nothing about what is
     * true. It is open because it is what the dialog is for after the test cases
     * themselves, and because the first field in it takes the focus - folded
     * away, the dialog opened with the keyboard on a field nobody could see and
     * Tab moving between fields nobody could reach.
     */
    private static final boolean EXPANDED = true;

    private final @NotNull JBPanel<?> wrapper;
    private final @NotNull JBTextArea changeLog;
    private final @NotNull JBTextField commitIdField;
    private final @NotNull Map<TestRunConfiguration, JComponent> fieldMap = new EnumMap<>(TestRunConfiguration.class);

    /**
     * The label beside each field, so a field that does not apply can take its
     * name off the form with it. A hidden box under a visible "Browser:" is
     * worse than either.
     */
    @Getter(AccessLevel.NONE)
    private final @NotNull Map<TestRunConfiguration, JBLabel> labelMap = new EnumMap<>(TestRunConfiguration.class);

    public RunConfigurationForm(final @NotNull String runName) {
        changeLog = new JBTextArea();
        commitIdField = new JBTextField();

        wrapper = new JBPanel<>(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(CollapsiblePanel.build("Configuration details", buildConfigurationPanel(runName), EXPANDED), BorderLayout.CENTER);

        // After the wrapper exists, because this asks it to lay itself out
        // again. Nothing is chosen yet, so the fields that wait on an answer
        // start off the form rather than appearing to be unanswered.
        applyVisibility();
    }

    private @NotNull JBPanel<?> buildConfigurationPanel(final @NotNull String runName) {
        final @NotNull JBPanel<?> configurationPanel = new JBPanel<>(new GridBagLayout());

        final @NotNull GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.anchor = GridBagConstraints.NORTHWEST;
        labelGbc.insets = JBUI.insets(4, 4, 4, 10);

        final @NotNull GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.gridx = 1;
        fieldGbc.weightx = 1.0;
        fieldGbc.anchor = GridBagConstraints.NORTHWEST;
        fieldGbc.insets = JBUI.insets(4, 0, 4, 4);

        final @NotNull JBTextField runNameField = new JBTextField(runName);
        runNameField.setEditable(false);
        runNameField.setEnabled(false);
        runNameField.setColumns(50);
        addLabeledRow(configurationPanel, labelGbc, fieldGbc, 0, "Test Run name:", runNameField);

        // Room for more than one story, because a run usually covers several.
        changeLog.setColumns(50);
        changeLog.setRows(3);
        changeLog.setLineWrap(true);
        changeLog.setWrapStyleWord(true);
        changeLog.getEmptyText().setText("Story-002 (register new user), Story-003 (forget password)");
        keepTabForNavigation(changeLog);

        commitIdField.setColumns(50);
        commitIdField.getEmptyText().setText("Commit hash, like 9f3c1ab...");

        // Registered like the dropdowns, so every answer is read through one
        // method. These two used to be reached by name from the creator, which
        // is why "all the configuration" meant six fields plus two exceptions.
        // Held in a local and used twice. Reading it back out of fieldMap inside
        // this call did not work: the arguments are evaluated before register
        // runs, so the map was still empty and the row was handed a null.
        final @NotNull JBScrollPane changeLogScroller = new JBScrollPane(changeLog);
        register(TestRunConfiguration.CHANGE_LOG, changeLogScroller,
                addLabeledRow(configurationPanel, labelGbc, fieldGbc, 1, TestRunConfiguration.CHANGE_LOG.getDisplayName(), changeLogScroller));
        register(TestRunConfiguration.COMMIT_ID, commitIdField,
                addLabeledRow(configurationPanel, labelGbc, fieldGbc, 2, TestRunConfiguration.COMMIT_ID.getDisplayName(), commitIdField));

        int row = 3;
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            if (!field.isChoice()) continue;

            final @NotNull ComboBox<String> comboBox = new ComboBox<>(field.getOptions());
            comboBox.setEditable(true);

            // Every choice tells the form to look again, rather than each one
            // naming the fields that wait on it. Which answers matter is the
            // fields' own business, and they already say so.
            comboBox.addActionListener(event -> applyVisibility());

            register(field, comboBox,
                    addLabeledRow(configurationPanel, labelGbc, fieldGbc, row, field.getDisplayName(), comboBox));
            row++;
        }

        configurationPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(10)
        ));

        return configurationPanel;
    }

    /**
     * One label-and-field row, and the label back - so a caller that may have to
     * hide the row later has both halves of it.
     */
    private @NotNull JBLabel addLabeledRow(final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints labelGbc, final @NotNull GridBagConstraints fieldGbc, final int row, final @NotNull String label, final @NotNull JComponent component) {
        final @NotNull GridBagConstraints lc = (GridBagConstraints) labelGbc.clone();
        lc.gridy = row;
        final @NotNull JBLabel labelComp = new JBLabel(label);
        labelComp.setVerticalAlignment(SwingConstants.TOP);
        panel.add(labelComp, lc);

        final @NotNull GridBagConstraints fc = (GridBagConstraints) fieldGbc.clone();
        fc.gridy = row;
        panel.add(component, fc);

        return labelComp;
    }

    /**
     * Puts on screen the fields that apply to what has been answered so far, and
     * takes away the ones that do not.
     * <p>
     * The rule is not here. Each field says when it applies and this only
     * carries that out, so a browser is asked about for a web frontend and a
     * handset for a mobile one because {@link TestRunConfiguration} says so -
     * and a field added later appears at the right moment by declaring it,
     * rather than by this method growing another branch.
     */
    private void applyVisibility() {
        fieldMap.forEach((field, component) -> {
            final boolean applies = field.isShownFor(this::chosenIn);

            component.setVisible(applies);
            labelMap.get(field).setVisible(applies);
        });

        wrapper.revalidate();
        wrapper.repaint();
    }

    /**
     * Gives a text area back the Tab key.
     * <p>
     * A {@code JTextArea} takes Tab as a character, which is right in an editor
     * and wrong in a form: the tester reaches this field, presses Tab expecting
     * the next one, and puts a tab stop in their change log instead. Handing it
     * the traversal keys every other component already has makes it behave like
     * the fields above and below it, and the dialog says so on its status bar.
     * <p>
     * Shift+Tab too, or the field would be a place the keyboard could enter and
     * not leave backwards.
     */
    private static void keepTabForNavigation(final @NotNull JComponent field) {
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Set.of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0)));
        field.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Set.of(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
    }

    /**
     * Remembers a field and its label together, because whatever hides one has
     * to hide the other.
     */
    private void register(final @NotNull TestRunConfiguration field, final @NotNull JComponent component, final @NotNull JBLabel label) {
        fieldMap.put(field, component);
        labelMap.put(field, label);
    }

    /**
     * What is sitting in a field, even when it is not on screen.
     * <p>
     * Separate from {@link #getFieldValue} on purpose: this is what the
     * visibility rules read, and reading them through that method would ask a
     * field whether it applies in order to work out whether it applies.
     */
    private @NotNull String chosenIn(final @NotNull TestRunConfiguration field) {
        // Nothing under that field means no value, the same as a field nobody
        // filled in.
        return Optional.ofNullable(fieldMap.get(field)).map(RunConfigurationForm::textIn).orElse("");
    }

    /**
     * What a field holds, whichever kind of field it is: an option that was
     * picked, or a line that was typed.
     * <p>
     * The one place that asks. Two kinds of component answer the same question,
     * and every caller above this reads an answer rather than a component.
     */
    private static @NotNull String textIn(final @NotNull JComponent component) {
        if (component instanceof JTextComponent typed) return typed.getText().trim();

        // A field too tall to sit in the row on its own is in a scroll pane, and
        // what was typed is inside that. Unwrapped here so no caller has to know
        // which fields are big enough to scroll.
        if (component instanceof JScrollPane scroller && scroller.getViewport().getView() instanceof JComponent inner) {
            return textIn(inner);
        }

        if (component instanceof ComboBox<?> picked) {
            return Optional.ofNullable(picked.getSelectedItem()).map(Object::toString).map(String::trim).orElse("");
        }

        return "";
    }

    /**
     * Everything the tester answered, under the field that asked - and nothing
     * under a field the run does not apply to, because {@link #getFieldValue}
     * already says so.
     * <p>
     * Read while the dialog is still on screen, and handed on as plain values:
     * what happens to it afterwards runs on a background thread, where there are
     * no components to ask (#87).
     */
    public @NotNull Map<TestRunConfiguration, String> configuration() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);

        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            answers.put(field, getFieldValue(field));
        }

        return answers;
    }

    public @NotNull String getFieldValue(final @NotNull TestRunConfiguration field) {
        // A field that does not apply has no value, whatever is still sitting in
        // the box behind it. A run moved from web to mobile would otherwise be
        // saved carrying the browser picked before the move, which no report
        // could then explain.
        if (!field.isShownFor(this::chosenIn)) return "";

        return chosenIn(field);
    }

    @Override
    public @NotNull JComponent getPanel() {
        return wrapper;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return changeLog;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Form fields have no submit gesture of their own; the declared keys confirm.
    }
}

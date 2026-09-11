package org.testin.testcase.create;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * UC-EDITOR-PANEL-005.
 * <p>
 * A field a test case holds several of: one completion box per value, stacked,
 * and a key that adds another.
 * <p>
 * Steps have worked this way for as long as they have been a list. Groups were
 * tick boxes over an enum until a group became a word (#296), and the moment
 * they stopped being a fixed set they became the same thing: a few values a
 * tester types, completed from what the project already uses. So the rows, the
 * key that adds one, the focus and what is saved are here, and a section says
 * only which field it is and how the test case holds it.
 * <p>
 * <b>There is no remove.</b> A row left empty is not saved - {@link #applyTo}
 * drops it - so clearing a box is removing the value, and the X button and its
 * key were a second way to say the same thing. They are gone.
 */
public abstract class AbstractMultiValueSection implements CreateTestCaseSection {

    protected final @NotNull Project p;

    @Getter
    private final @NotNull List<EditorTextField> fields = new ArrayList<>();

    private final @NotNull JBPanel<?> container;
    private final @NotNull JBPanel<?> wrapper;

    protected AbstractMultiValueSection(final @NotNull Project p) {
        this.p = p;

        this.container = new JBPanel<>();
        this.container.setLayout(new BoxLayout(this.container, BoxLayout.Y_AXIS));
        this.container.setOpaque(false);

        this.wrapper = createWrapper(field().getIcon(), this.container);
    }

    /**
     * Which field this section edits. Everything drawn - the icon, the
     * placeholder, the key that opens it - is that field's own.
     */
    protected abstract @NotNull CreateTestCaseFields field();

    /**
     * What the completion offers: every value the project has already used.
     */
    protected abstract @NotNull Set<String> completions(final @NotNull TestCaseValues cache);

    /**
     * The values as the test case holds them.
     */
    protected abstract @NotNull List<String> valuesOf(final @NotNull TestCaseDto dto);

    /**
     * Writes them back, blanks already dropped.
     */
    protected abstract void write(final @NotNull TestCaseDto dto, final @NotNull List<String> values);

    /**
     * The key that opens the section and adds a row.
     */
    protected abstract @NotNull Shortcuts addKey();

    /**
     * What a row says before anything is typed in it. Numbered for steps, where
     * the order is the point; the same words for every row otherwise.
     */
    protected @NotNull String placeholderFor(final int index) {
        return field().getPlaceholder();
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    /**
     * Nothing: the tester opens the section to type into a row the overload
     * below has not added yet, and it focuses that row.
     */
    @Override
    public void focusOnShow() {
    }

    public void showSection(final @NotNull JBPanel<?> contentPanel, final @NotNull UIAction repackAction) {
        showSection(contentPanel);
        wrapper.setVisible(true);
        addField("", repackAction);

        ApplicationManager.getApplication().invokeLater(() -> {
            repackAction.execute();
            if (!fields.isEmpty()) fields.getLast().requestFocus();
        });
    }

    public void addField(final @NotNull String text, final @NotNull UIAction repackAction) {
        final @NotNull EditorTextField box = SpellChecker.createCompletionField(p,
                new TextFieldWithAutoCompletion.StringsCompletionProvider(completions(Services.getInstance(p, TestCaseValues.class)), field().getIcon()), text);

        box.setOneLineMode(true);
        box.setFont(fieldFont());
        box.setPlaceholder(placeholderFor(fields.size()));
        box.setShowPlaceholderWhenFocused(true);
        box.setBorder(JBUI.Borders.empty(6, 10));

        final @NotNull JBPanel<?> row = new JBPanel<>(new java.awt.BorderLayout());
        row.setOpaque(false);
        row.setBorder(JBUI.Borders.emptyBottom(6));
        row.add(box, java.awt.BorderLayout.CENTER);

        fields.add(box);
        container.add(row);
    }

    /**
     * UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-033.
     * <p>
     * Every row that has something in it, in the order they are drawn. A row
     * left empty is not a value, which is why nothing needs removing.
     */
    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        final @NotNull List<String> values = new ArrayList<>();

        for (final EditorTextField box : fields) {
            final @NotNull String typed = box.getText().trim();
            if (!typed.isEmpty()) values.add(typed);
        }

        write(dto, values);
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, addKey().getCustomShortcut(), () -> showSection(slot, repackAction));
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return fields.isEmpty() ? container : fields.getLast();
    }

    @Override
    public void setEditable(final boolean editable) {
        fields.forEach(box -> box.setEnabled(editable));
    }

    public void setData(final @NotNull List<String> values, final @NotNull UIAction repack) {
        container.removeAll();
        fields.clear();

        values.forEach(value -> addField(value, repack));
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        setData(valuesOf(dto), repackAction);
    }
}

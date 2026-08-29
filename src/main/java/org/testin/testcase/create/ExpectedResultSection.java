package org.testin.testcase.create;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ExpectedResultSection implements CreateTestCaseSection {
    final @NotNull Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    private final @NotNull Project p;
    @Getter
    private final @NotNull EditorTextField expectedResultField;
    private final @NotNull JBPanel<?> wrapper;

    public ExpectedResultSection(final @NotNull Project p) {
        this.p = p;
        this.expectedResultField = SpellChecker.createCompletionField(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseCacheService.class).getExpectedResults(), CreateTestCaseFields.EXPECTED_RESULT.getIcon()), "");
        this.expectedResultField.setFont(fieldFont);
        this.expectedResultField.setPlaceholder(CreateTestCaseFields.EXPECTED_RESULT.getPlaceholder());
        this.expectedResultField.setShowPlaceholderWhenFocused(true);
        this.expectedResultField.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.EXPECTED_RESULT.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.expectedResultField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    /**
     * Turns the field into a multi-line text area and hands its keys to the ones
     * a tester expects there. A multi-line editor otherwise takes Enter and Tab
     * for itself, so each is rebound:
     * <ul>
     *   <li>Enter saves the dialog,</li>
     *   <li>Ctrl+Enter ({@link Shortcuts#InsertNewLine}) inserts a line break,</li>
     *   <li>Tab / Shift+Tab move to the next / previous field,</li>
     *   <li>Alt+Enter is left to the platform - it opens the spelling corrections.</li>
     * </ul>
     * Bound through the dialog's own registrar, which already stands these keys
     * down while an autocomplete popup is open, and so through the action system -
     * the only place an IntelliJ editor reads its keys from (a Swing binding on it
     * is never reached).
     */
    public void enableMultiLine(final @NotNull TestCaseBaseDialog base, final @NotNull Runnable onSave) {
        expectedResultField.setOneLineMode(false);

        // Tab leaves the field instead of indenting inside it.
        //
        // A one-line EditorTextField gets this from the platform, which turns
        // focus traversal back on for the editor it wraps; a multi-line one does
        // not, so the editor keeps Tab for itself. Registering VK_TAB through
        // the action system did nothing, because the editor has its own Tab
        // action and the editor is where the key stops.
        //
        // Traversal keys are read by AWT before any of that, which is why this
        // is the thing that works - and why the two Tab registrations that used
        // to be here are gone rather than kept alongside it.
        expectedResultField.addSettingsProvider(editor ->
                editor.getContentComponent().setFocusTraversalKeysEnabled(true));

        base.registerShortcut(expectedResultField, Shortcuts.Enter.getCustomShortcut(), onSave::run);
        base.registerShortcut(expectedResultField, Shortcuts.InsertNewLine.getCustomShortcut(), this::insertNewLine);
    }

    /**
     * Inserts a line break at the caret. The editor exists only while the field
     * is on screen and focused, which is the only time this key can fire.
     */
    private void insertNewLine() {
        final @Nullable Editor editor = expectedResultField.getEditor();
        if (editor == null) return;

        final int caret = editor.getCaretModel().getOffset();
        WriteCommandAction.runWriteCommandAction(p, () -> {
            editor.getDocument().insertString(caret, "\n");
            editor.getCaretModel().moveToOffset(caret + 1);
        });
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setExpectedResult(expectedResultField.getText().trim());
    }

    @Override
    public void setupShortcut(final @NotNull JComponent mainPanel, final @NotNull JBPanel<?> slot, final @NotNull TestCaseBaseDialog base, final @NotNull UIAction repackAction) {
        base.registerShortcut(mainPanel, Shortcuts.CreateTestCaseExpectedResult.getCustomShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return expectedResultField;
    }

    @Override
    public void setEditable(final boolean editable) {
        expectedResultField.setEnabled(editable);
    }

    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull UIAction repackAction) {
        expectedResultField.setText(dto.getExpectedResult());
    }
}
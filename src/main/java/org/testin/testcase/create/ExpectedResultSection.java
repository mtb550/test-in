package org.testin.testcase.create;


import com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
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

    /**
     * The field height the popup was last measured around - see the document
     * listener in {@link #enableMultiLine}.
     */
    private int packedHeight;

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

        // Three things EditorTextField.initOneLineMode does for a one-line field
        // and not for a multi-line one, so the line above is what took each of
        // them away. Settings providers run after initOneLineMode, which is why
        // putting them back here is enough.
        expectedResultField.addSettingsProvider(editor -> {

            // Tab leaves the field instead of indenting inside it. Registering
            // VK_TAB through the action system did nothing, because the editor
            // has its own Tab action and the editor is where the key stops.
            // Traversal keys are read by AWT before any of that, which is why
            // this is the thing that works - and why the two Tab registrations
            // that used to be here are gone rather than kept alongside it.
            editor.getContentComponent().setFocusTraversalKeysEnabled(true);

            // The frame and its blue focus ring - the platform's own border, so
            // it follows the theme and repaints on focus by itself.
            editor.setBorder(new DarculaEditorTextFieldBorder(expectedResultField, editor));

            // The colors a dialog is read in rather than the ones a source file
            // is: a one-line field is bound to the scheme of the current UI
            // theme, a multi-line one to whatever scheme the editor is set to.
            // A dark theme over a light editor scheme - the ordinary pairing -
            // therefore drew this field's text black while every other field in
            // the dialog was white.
            //
            // The font rides on the scheme, so the new one is given the dialog's
            // font too; without that the field would come back at the editor's
            // size while its neighbors keep the size every field is set in.
            final @NotNull EditorColorsScheme themed = editor.createBoundColorSchemeDelegate(EditorColorsManager.getInstance().getSchemeForCurrentUITheme());
            themed.setEditorFontName(fieldFont.getFontName());
            themed.setEditorFontSize(fieldFont.getSize());
            editor.setColorsScheme(themed);
        });

        // The dialog grows and shrinks with the text.
        //
        // The field already reports the editor's height as its preferred size,
        // but the editor sits in a scroll pane, and a scroll pane is a Swing
        // validation root: the revalidation a new line causes stops there and
        // never reaches the popup. So the popup is re-measured here instead.
        //
        // Only when the height actually changed, because this fires on every
        // keystroke and repacking also scrolls the focused component back into
        // view - doing that per character would drag the caret around while a
        // tester types.
        expectedResultField.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(final @NotNull DocumentEvent event) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    final int height = expectedResultField.getPreferredSize().height;
                    if (height == packedHeight) return;

                    packedHeight = height;
                    base.repack();
                });
            }
        });

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
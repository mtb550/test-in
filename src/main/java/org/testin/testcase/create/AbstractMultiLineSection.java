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
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;

/**
 * A section whose value is typed over as many lines as the tester needs -
 * Expected Result and Test Data - and everything the two have in common.
 * <p>
 * One owner for it rather than a copy per section. What is shared is not a style
 * but four separate compensations for things {@link EditorTextField} stops doing
 * the moment one-line mode is switched off, each of which was found the hard way
 * and none of which is guessable from the field's own API. A second copy would be
 * a second thing to keep right, and the first to fall behind.
 */
public abstract class AbstractMultiLineSection implements CreateTestCaseSection {

    /**
     * The size every field in the dialog is set in - held here rather than at
     * the field, because a multi-line editor takes its font from its color
     * scheme and the scheme is built in {@link #enableMultiLine}.
     */
    protected final @NotNull Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);

    protected final @NotNull Project p;

    protected final @NotNull EditorTextField field;

    private final @NotNull JBPanel<?> wrapper;

    /**
     * The field height the popup was last measured around - see the document
     * listener in {@link #enableMultiLine}.
     */
    private int packedHeight;

    protected AbstractMultiLineSection(final @NotNull Project p, final @NotNull EditorTextField field, final @NotNull CreateTestCaseFields describes) {
        this.p = p;
        this.field = field;
        this.field.setFont(fieldFont);
        this.field.setPlaceholder(describes.getPlaceholder());
        this.field.setShowPlaceholderWhenFocused(true);
        this.field.setBorder(JBUI.Borders.empty(10));

        this.wrapper = new JBPanel<>(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(describes.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.field, BorderLayout.CENTER);
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
        field.setOneLineMode(false);

        // Three things EditorTextField.initOneLineMode does for a one-line field
        // and not for a multi-line one, so the line above is what took each of
        // them away. Settings providers run after initOneLineMode, which is why
        // putting them back here is enough.
        field.addSettingsProvider(editor -> {

            // Tab leaves the field instead of indenting inside it. Registering
            // VK_TAB through the action system did nothing, because the editor
            // has its own Tab action and the editor is where the key stops.
            // Traversal keys are read by AWT before any of that, which is why
            // this is the thing that works - and why the two Tab registrations
            // that used to be here are gone rather than kept alongside it.
            editor.getContentComponent().setFocusTraversalKeysEnabled(true);

            // The frame and its blue focus ring - the platform's own border, so
            // it follows the theme and repaints on focus by itself.
            editor.setBorder(new DarculaEditorTextFieldBorder(field, editor));

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
        field.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(final @NotNull DocumentEvent event) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    final int height = field.getPreferredSize().height;
                    if (height == packedHeight) return;

                    packedHeight = height;
                    base.repack();
                });
            }
        });

        base.registerShortcut(field, Shortcuts.Enter.getCustomShortcut(), onSave::run);
        base.registerShortcut(field, Shortcuts.InsertNewLine.getCustomShortcut(), this::insertNewLine);
    }

    /**
     * Inserts a line break at the caret. The editor exists only while the field
     * is on screen and focused, which is the only time this key can fire.
     */
    private void insertNewLine() {
        final @Nullable Editor editor = field.getEditor();
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
    public @NotNull JComponent getFocusComponent() {
        return field;
    }

    @Override
    public void setEditable(final boolean editable) {
        field.setEnabled(editable);
    }
}

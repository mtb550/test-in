package org.testin.util;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.textCompletion.TextCompletionProvider;
import com.intellij.util.textCompletion.TextCompletionUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the IDE's own spell checker on for a text field (issue #1).
 * <p>
 * Nothing is reimplemented here: the platform already highlights misspellings
 * and offers corrections, dictionaries and "save to dictionary" through the
 * usual intention popup. It is simply off by default on the small editors that
 * dialogs embed, and this switches it on.
 * <p>
 * The checker runs as an inspection, so it only works on an editor that has
 * both a project and a PSI file behind its document. The platform's own
 * customization returns silently when either is missing, which makes a field
 * look supported when it is not - so the fields are built here rather than
 * configured after the fact, and switching the checker on over an existing
 * {@code TextFieldWithAutoCompletion} is deliberately not offered, because it
 * does not work.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpellChecker {

    /**
     * A plain-text field that spell checks, for the places that need no
     * completion.
     * <p>
     * Built through {@link EditorTextFieldProvider} rather than with
     * {@code new EditorTextField(...)}: the plain constructors hand the field a
     * bare {@code EditorFactory} document with no PSI file behind it. The
     * checker is an inspection, so no PSI file means no checking, however the
     * field is configured afterward.
     * <p>
     * This is the same construction the platform uses for the commit message
     * field.
     */
    public static @NotNull EditorTextField createField(final @NotNull Project p) {
        final List<EditorCustomization> customizations = new ArrayList<>();
        ContainerUtil.addIfNotNull(customizations,
                SpellCheckingEditorCustomizationProvider.getInstance().getEnabledCustomization());

        return EditorTextFieldProvider.getInstance()
                .getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), p, customizations);
    }

    /**
     * The same field, plus completion from the given provider.
     * <p>
     * Built this way round - a spell-checked field that completion is installed
     * onto - rather than by switching the checker on over a
     * {@code TextFieldWithAutoCompletion}, which does not take. This is the same
     * pairing that class makes internally: it also ends at
     * {@link TextCompletionUtil#installProvider}.
     */
    public static @NotNull EditorTextField createCompletionField(final @NotNull Project p,
                                                                 final @NotNull TextCompletionProvider provider,
                                                                 final @NotNull String text) {
        final EditorTextField field = createField(p);

        // Blocking rather than cancellable: this runs while the dialog is being
        // built and the field cannot be returned half-made.
        final PsiFile psiFile = ReadAction.computeBlocking(
                () -> PsiDocumentManager.getInstance(p).getPsiFile(field.getDocument()));

        if (psiFile == null) {
            Logger.warn("Completion not installed: the field has no PSI file");
        } else {
            TextCompletionUtil.installProvider(psiFile, provider, true);
        }

        field.setText(text);
        return field;
    }
}

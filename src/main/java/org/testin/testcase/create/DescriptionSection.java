/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.testcase.create;

import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Bundle;
import org.testin.util.Icons;
import org.testin.util.NameSanitizer;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DescriptionSection extends AbstractOneLineSection {

    /**
     * The description's letter in red, for a description the dialog refused.
     */
    private static final @NotNull Icon REFUSED = Icons.fieldLetter("D", Icons.RED);

    private final @NotNull Project p;

    /**
     * How the hint in the empty box is drawn, handed to the editor when it is
     * made: no color of its own, so the editor's gray, until the description is
     * refused.
     */
    private final @NotNull TextAttributes hint = new TextAttributes();

    /**
     * The other test cases in this test set, as they are when asked.
     * <p>
     * None until a dialog says what to compare against, and none is a dialog
     * with nothing to compare - it refuses nothing, which is what the update
     * dialogs want.
     */
    private @NotNull Supplier<List<TestCaseDto>> siblings = List::of;

    public DescriptionSection(final @NotNull Project p) {
        super(SpellChecker.createCompletionField(p, new TextFieldWithAutoCompletion.StringsCompletionProvider(Services.getInstance(p, TestCaseValues.class).getDescription(), CreateTestCaseFields.DESCRIPTION.getIcon()), ""),
                CreateTestCaseFields.DESCRIPTION, Shortcuts.CreateTestCaseDescription);

        this.p = p;
        field.addSettingsProvider(editor -> editor.setPlaceholderAttributes(hint));
    }

    /**
     * UC-EDITOR-PANEL-005, Rule-CODEGEN-001.
     * <p>
     * The test cases this description must not name the same method as, asked
     * for again at each Enter. They were read once, when the dialog opened, on
     * the belief that the set could not change while it was in front of it; but
     * the popup stays open when the tester clicks away, and the editors and the
     * tree behind it stay live, so a case written meanwhile was not compared
     * against (#66, finding 219).
     */
    public void compareAgainst(final @NotNull Supplier<List<TestCaseDto>> siblings) {
        this.siblings = siblings;
    }

    /**
     * The test methods the other test cases in this test set name now.
     */
    private @NotNull Set<String> takenMethodKeys() {
        return siblings.get().stream()
                .map(TestCaseDto::getDescription)
                .map(NameSanitizer::methodName)
                .filter(name -> !name.isEmpty())
                .map(NameSanitizer::methodKey)
                .collect(Collectors.toSet());
    }

    /**
     * UC-EDITOR-PANEL-005.
     * <p>
     * Turns the description red, or back: the text typed, the hint in the empty
     * box, and the icon. Turning only the text red left an empty description
     * looking untouched, since there was no text to turn red - and an empty one
     * is what Enter refuses most (#328).
     * <p>
     * The foreground rather than the background, which it once was: a field
     * refused once then stayed red however it was corrected.
     */
    public void setError(final boolean error) {
        field.setForeground(error ? Icons.RED : UIUtil.getTextFieldForeground());
        hint.setForegroundColor(error ? Icons.RED : null);
        icon.setIcon(error ? REFUSED : CreateTestCaseFields.DESCRIPTION.getIcon());
        if (error) field.requestFocus();
        field.repaint();
    }

    /**
     * UC-EDITOR-PANEL-005, Rule-CODEGEN-001.
     * <p>
     * Refuses a description the generated code could not be named after.
     * <p>
     * Here rather than in the generator, which is where it used to be found: by
     * then the description is stored, the method is being renamed or written,
     * and the answer is either an exception the tester did not cause or a method
     * declaration Java will not compile. A description is typed once and read
     * from for the life of the case, so this is the moment to say no (#66).
     * <p>
     * A blank one is not this section's refusal to make - the save has always
     * owned that, and it says so differently.
     */
    @Override
    public boolean accepts() {
        final @NotNull String description = field.getText().trim();
        if (description.isEmpty()) {
            setError(false);
            return true;
        }

        final @NotNull String methodName = NameSanitizer.methodName(description);

        if (!NameSanitizer.canMakeMethodName(description)) {
            setError(true);
            Services.getInstance(p, Notifier.class).softRefuse(p,
                    Bundle.message("description.not.a.method.title"),
                    Bundle.message("description.not.a.method.message", methodName));

            return false;
        }

        // Punctuation and capitals are dropped on the way to a method name, so
        // "Log in" and "Log-in!" are one method - and only one was ever written.
        // The second test case ended up with no method of its own: it could not
        // be run and could not be jumped to, and nothing said so until the first
        // F5 (#244).
        if (takenMethodKeys().contains(NameSanitizer.methodKey(methodName))) {
            setError(true);
            Services.getInstance(p, Notifier.class).softRefuse(p,
                    Bundle.message("description.taken.title"),
                    Bundle.message("description.taken.message", methodName));

            return false;
        }

        setError(false);
        return true;
    }


    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-032
    @Override
    public void applyTo(final @NotNull TestCaseDto dto) {
        dto.setDescription(typed());
    }

    /**
     * The description as {@link #applyTo} would save it, so the save can refuse
     * a blank one before anything is written onto the case.
     */
    public @NotNull String typed() {
        return field.getText().trim();
    }




    @Override
    public void fillData(final @NotNull TestCaseDto dto, final @NotNull Runnable repackAction) {
        field.setText(dto.getDescription());
    }
}
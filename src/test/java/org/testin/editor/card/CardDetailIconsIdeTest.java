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
package org.testin.editor.card;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testcase.TestCaseCard;
import org.testin.editor.testrun.TestRunCard;
import org.testin.model.TestCaseDto;
import org.testin.model.result.TestRunItems;
import org.testin.model.status.RunItemStatus;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.TestCaseEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.view.Drawn;

import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CardDetailIconsIdeTest extends BasePlatformTestCase {
    private static final @NotNull String EXPECTED = "The dashboard opens.";
    private static final @NotNull Dimension CELL = new Dimension(900, 120);

    private static @NotNull JBLabel theLineSaying(final @NotNull BaseCard card, final @NotNull String words) {
        return Drawn.first(card, JBLabel.class, label -> label.isVisible() && Objects.requireNonNullElse(label.getText(), "").contains(words));
    }

    private @NotNull TestCaseCard aCardWithItsExpectedResult() {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").expectedResult(EXPECTED).build();
        final @NotNull TestCaseCard card = new TestCaseCard(getProject());
        card.updateData(0, tc, Set.of(TestCaseEditorAttributes.EXPECTED_RESULT), BaseCard.titleText(1, true, tc.getDescription()));
        card.layOutAs(CELL);
        return card;
    }

    // Rule-EDITOR-PANEL-269
    public void testADetailLineLeadsWithItsFieldsIconAndTheScreenReaderStillHearsTheName() {
        final @NotNull TestCaseCard card = aCardWithItsExpectedResult();
        final @NotNull JBLabel line = theLineSaying(card, EXPECTED);

        assertSame("the line does not lead with the field's icon", CreateTestCaseFields.EXPECTED_RESULT.getIcon(), line.getIcon());
        assertEquals("the line still names its field", EXPECTED, line.getText());
        assertTrue("a screen reader no longer hears the field's name: " + card.getAccessibleContext().getAccessibleDescription(),
                Objects.requireNonNullElse(card.getAccessibleContext().getAccessibleDescription(), "").contains(TestCaseEditorAttributes.EXPECTED_RESULT.getName() + ": " + EXPECTED));
    }

    // Rule-EDITOR-PANEL-269
    public void testHoveringTheIconNamesTheField() {
        final @NotNull TestCaseCard card = aCardWithItsExpectedResult();
        final @NotNull Rectangle line = SwingUtilities.convertRectangle(theLineSaying(card, EXPECTED).getParent(), theLineSaying(card, EXPECTED).getBounds(), card);

        assertEquals(TestCaseEditorAttributes.EXPECTED_RESULT.getName(), card.tooltipAt(new Point(line.x + 2, line.y + line.height / 2), CELL));
    }

    // Rule-EDITOR-PANEL-269
    public void testAFieldWithNoIconKeepsItsName() {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
        final @NotNull TestRunItems runItem = new TestRunItems().setId(tc.getId()).setStatus(RunItemStatus.FAILED).showing(Optional.of(tc), Optional.empty(), Optional.empty(), false);
        final @NotNull TestRunCard card = new TestRunCard(getProject());
        card.updateData(0, Set.of(TestRunEditorAttributes.RUN_STATUS), runItem, BaseCard.titleText(1, true, tc.getDescription()));

        final @NotNull JBLabel line = theLineSaying(card, RunItemStatus.FAILED.getLabel());
        assertTrue("a field with no icon lost its name: " + line.getText(), line.getText().startsWith(TestRunEditorAttributes.RUN_STATUS.getName() + ": "));
    }
}

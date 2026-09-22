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

package org.testin.editor;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.StandIn;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CardTitleWrapTest {

    private static final String LONG = "Log in with a valid user and check that the dashboard opens with every widget it is supposed to show and nothing else at all";

    private static Card laidOut(final String title) {
        final JBList<String> list = new JBList<>("a");
        list.setSize(900, 400);

        final Card card = new Card();
        card.feed(title);
        card.applyListLayout(list);
        return card;
    }

    private static List<CardHoverAction.Offered> everyButton() {
        return offered(CardHoverAction.NAVIGATE_TO_TEST_METHOD, CardHoverAction.RUN_TEST_METHOD, CardHoverAction.NAVIGATE_TO_TEST_CASE);
    }

    private static List<CardHoverAction.Offered> offered(final CardHoverAction... actions) {
        return Arrays.stream(actions).map(action -> new CardHoverAction.Offered(action, Optional.empty())).toList();
    }

    @Test
    public void wrappingMakesTheCardTaller() {
        final int fits = laidOut("Log in").getPreferredSize().height;
        final int wraps = laidOut(LONG).getPreferredSize().height;

        assertTrue(wraps > fits, "a wrapped title has to make the row taller, got " + wraps + " against " + fits);
    }

    @Test
    public void theTitleIsAlwaysTheTestersOwnWords() {
        assertEquals(laidOut(LONG).shown(), LONG);
        assertEquals(laidOut("Log in <script>").shown(), "Log in <script>");
    }

    @Test
    public void theIconsStillFitAfterATitleThatFillsTheColumn() {
        final @NotNull List<CardHoverAction.Offered> buttons = everyButton();
        final int column = CardTitle.titleColumnWidth(900, buttons.size());
        final @NotNull List<CardTitle.Slot> slots = CardTitle.descriptionActionIcons(column, buttons).slots();

        assertTrue(column > 0 && column < 900, "a 900px list must give a bounded column, got " + column);
        assertEquals(slots.size(), buttons.size(), "every button gets a slot");
        assertTrue(slots.getLast().at().getMaxX() <= 900, "the icons after a full-width title run off the card");
    }

    @Test
    public void theNewButtonGoesLastAndMovesNeitherOfTheOthers() {
        final List<CardTitle.Slot> before = CardTitle.descriptionActionIcons(200, offered(CardHoverAction.NAVIGATE_TO_TEST_METHOD, CardHoverAction.RUN_TEST_METHOD)).slots();
        final List<CardTitle.Slot> now = CardTitle.descriptionActionIcons(200, everyButton()).slots();

        assertEquals(now.get(0).at(), before.get(0).at(), "the method button moved");
        assertEquals(now.get(1).at(), before.get(1).at(), "the run button moved");
        assertEquals(now.getLast().button().action(), CardHoverAction.NAVIGATE_TO_TEST_CASE);
        assertEquals(now.getLast().at().getSize(), now.getFirst().at().getSize(), "the new button's slot is not the size of the others");
        assertTrue(now.getLast().at().getMinX() > now.get(1).at().getMaxX(), "the new button overlaps Run");
    }

    @Test
    public void aClickLandsOnTheButtonDrawnThere() {
        final CardTitle.ActionIcons icons = CardTitle.descriptionActionIcons(200, everyButton());

        for (final CardTitle.Slot slot : icons.slots()) {
            assertEquals(icons.at((int) slot.at().getCenterX(), (int) slot.at().getCenterY()).orElseThrow(), slot.button());
        }
        assertTrue(icons.at(0, 0).isEmpty(), "a click on the title reached a button");
    }

    @Test
    public void aListWithNoWidthYetLetsTheTitleRun() {
        assertEquals(CardTitle.titleColumnWidth(0, 3), Integer.MAX_VALUE);
    }

    private static final class Card extends BaseCard {
        Card() {
            super(StandIn.of(Project.class));
        }

        void feed(final String title) {
            updateUI(0, title, List.of(), Map.of());
        }

        String shown() {
            return titleArea.getText();
        }
    }
}

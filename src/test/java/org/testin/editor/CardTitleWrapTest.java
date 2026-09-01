package org.testin.editor;

import com.intellij.ui.components.JBList;
import org.testng.annotations.Test;

import javax.swing.*;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A card title too long for its card wraps, and the card grows to hold it.
 * <p>
 * The height is the half worth testing, and the reason this file exists. The
 * title was a label given {@code <html><body style='width:818px'>} - the form
 * everybody writes - and it measured 1063x22: the whole sentence on one line,
 * the CSS width silently ignored by the stylesheet a JLabel renders through.
 * The decision to wrap was right, the width was right, the markup looked right,
 * and nothing wrapped. A test asking whether the title had been handed the
 * wrapping instruction would have passed throughout.
 * <p>
 * So this asks how tall the card ended up, which is the only thing the tester
 * can see.
 */
public class CardTitleWrapTest {

    private static final String LONG = "Log in with a valid user and check that the dashboard opens with every widget it is supposed to show and nothing else at all";

    /**
     * A stand-in for the two real cards: {@code BaseCard} is abstract only so
     * each editor can bring its own data binding, and none of that is involved
     * in laying out a title.
     */
    private static final class Card extends BaseCard {
        void feed(final String title) {
            updateUI(0, title, List.of(), Map.of());
        }

        String shown() {
            return titleArea.getText();
        }
    }

    private static Card laidOut(final String title) {
        final JBList<String> list = new JBList<>("a");
        list.setSize(900, 400);

        final Card card = new Card();
        card.feed(title);
        card.applyListLayout(list);
        return card;
    }

    /**
     * The one that catches a wrap the renderer does not honor: a title that was
     * told to wrap but did not reports the height of a single line, so the row
     * clips it and the tester sees no wrapping at all.
     */
    @Test
    public void wrappingMakesTheCardTaller() {
        final int fits = laidOut("Log in").getPreferredSize().height;
        final int wraps = laidOut(LONG).getPreferredSize().height;

        assertTrue(wraps > fits, "a wrapped title has to make the row taller, got " + wraps + " against " + fits);
    }

    /**
     * Whatever the length, the title stays the words the tester typed. It was
     * markup for the long case once, which meant a description holding a
     * {@code <} swallowed the rest of its own line.
     */
    @Test
    public void theTitleIsAlwaysTheTestersOwnWords() {
        assertEquals(laidOut(LONG).shown(), LONG);
        assertEquals(laidOut("Log in <script>").shown(), "Log in <script>");
    }

    /**
     * The title stops where the hover icons still fit on the card - the width
     * the card wraps at, and the one the mouse listener hit-tests against.
     */
    @Test
    public void theIconsStillFitAfterATitleThatFillsTheColumn() {
        final int column = Shared.titleColumnWidth(900);

        assertTrue(column > 0 && column < 900, "a 900px list must give a bounded column, got " + column);
        assertTrue(Shared.descriptionActionIcons(column).run().getMaxX() <= 900, "the icons after a full-width title run off the card");
    }

    /**
     * A card measured before its list has been laid out reads as the one-line
     * card it used to be, rather than wrapping inside a width of zero and
     * drawing one character per line.
     */
    @Test
    public void aListWithNoWidthYetLetsTheTitleRun() {
        assertEquals(Shared.titleColumnWidth(0), Integer.MAX_VALUE);
    }
}

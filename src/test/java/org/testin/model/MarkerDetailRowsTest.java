package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.DetailRow;
import org.testin.model.markers.Marker;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.markers.TestSetMarker;
import org.testng.annotations.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * One Details button serves both editors, and it never asks which kind of node
 * it is looking at.
 * <p>
 * It asks the node's marker what it has to say, and every marker answers -
 * because the question is on the {@link Marker} contract they all implement.
 * A run lists the configuration it was created with; a test set has nothing
 * extra and says so with an empty list rather than by the caller knowing not to
 * ask.
 */
public class MarkerDetailRowsTest {

    @Test
    public void aMarkerWithNothingExtraSaysNothing() {
        assertTrue(new TestSetMarker().getDetailRows().isEmpty(),
                "a test set adds no rows of its own, and must answer that rather than leave the caller to know it");
    }

    @Test
    public void aRunListsWhatItWasCreatedWith() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        answers.put(TestRunConfiguration.PLATFORM, "Web");
        answers.put(TestRunConfiguration.BROWSER, "Firefox");

        final @NotNull List<DetailRow> rows = new TestRunMarker().setConfiguration(answers).getDetailRows();

        assertEquals(rows.size(), TestRunConfiguration.values().length,
                "every question is offered; a blank answer is dropped when the row is drawn, not here");
        assertTrue(rows.contains(new DetailRow("Platform", "Web")));
        assertTrue(rows.contains(new DetailRow("Browser", "Firefox")));
    }

    /**
     * The captions are the form's own labels, so a question renamed there is
     * renamed in the popup and in the reports without anything being kept in
     * step by hand.
     */
    @Test
    public void theCaptionsAreTheNamesTheFormUsed() {
        final @NotNull List<DetailRow> rows = new TestRunMarker().getDetailRows();

        for (int at = 0; at < TestRunConfiguration.values().length; at++) {
            assertEquals(rows.get(at).caption(), TestRunConfiguration.values()[at].getDisplayName());
        }
    }

    /**
     * A run created before the configuration was stored has none, and still
     * answers - the popup shows its audit and nothing else.
     */
    @Test
    public void anOlderRunWithNoConfigurationStillAnswers() {
        final @NotNull List<DetailRow> rows = new TestRunMarker().getDetailRows();

        assertEquals(rows.size(), TestRunConfiguration.values().length);
        assertTrue(rows.stream().allMatch(row -> row.value().isEmpty()),
                "nothing was answered, so every row is blank and every one is dropped when drawn");
    }
}

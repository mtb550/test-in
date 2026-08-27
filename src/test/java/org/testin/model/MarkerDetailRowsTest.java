package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestRunDto;
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
 * <p>
 * A run's configuration used to be among those answers, held on the marker.
 * It is not any more: the run's own file owns it, and the popup looks the run
 * up the same way it already looks up the execution timestamps beside it. The
 * marker held a copy, so the reports read one store and the popup read the
 * other, and two owners for one fact is one of them going stale.
 * <p>
 * What that guarantee was worth is asserted here still, against the owner it
 * moved to.
 */
public class MarkerDetailRowsTest {

    @Test
    public void aMarkerWithNothingExtraSaysNothing() {
        assertTrue(new TestSetMarker().getDetailRows().isEmpty(),
                "a test set adds no rows of its own, and must answer that rather than leave the caller to know it");
    }

    @Test
    public void aRunMarkerNoLongerAnswersForTheRun() {
        assertTrue(new TestRunMarker().getDetailRows().isEmpty(),
                "the configuration is the run file's; a marker that still offered it would be the second copy again");
    }

    @Test
    public void aRunListsWhatItWasCreatedWith() {
        final @NotNull Map<TestRunConfiguration, String> answers = new EnumMap<>(TestRunConfiguration.class);
        answers.put(TestRunConfiguration.PLATFORM, "Web");
        answers.put(TestRunConfiguration.BROWSER, "Firefox");

        final @NotNull List<DetailRow> rows = TestRunConfiguration.rowsOf(new TestRunDto().setConfiguration(answers));

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
        final @NotNull List<DetailRow> rows = TestRunConfiguration.rowsOf(new TestRunDto());

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
        final @NotNull List<DetailRow> rows = TestRunConfiguration.rowsOf(new TestRunDto());

        assertEquals(rows.size(), TestRunConfiguration.values().length);
        assertTrue(rows.stream().allMatch(row -> row.value().isEmpty()),
                "nothing was answered, so every row is blank and every one is dropped when drawn");
    }
}

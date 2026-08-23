package org.testin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.testin.model.markers.TestCasesMainDirectoryMarker;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.markers.Marker;
import org.testin.model.markers.TestSetMarker;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.testng.Assert.*;

/**
 * The four rules that keep a marker file readable (#66).
 * <p>
 * A marker is tester data - it is the only place a node's audit info lives - and
 * every one of these rules is one annotation away from being broken silently: a
 * Lombok change, a dropped {@code @JsonIgnore}, a field renamed without its
 * alias. The plugin keeps running either way and the file quietly loses a value,
 * which is why they are pinned here rather than left to a sandbox pass to notice.
 * <p>
 * The mapper is built the way {@code Mapper} builds its own - Jackson plus the
 * time module - because {@code Mapper} is a project service and these rules are
 * about the marker classes, not about the service that hands them over.
 */
public class MarkerJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ZonedDateTime when = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusYears(3);
    private final String onDisk = DateTimeFormatter.ofPattern(Config.DATE_FORMAT_PATTERN, Locale.US).format(when);

    /**
     * The audit block and the status are written; the status label is not. The
     * label is derived from the status for the Details popup, so writing it would
     * put a second copy of the same fact in the file - and a stale one the first
     * time a label is reworded.
     */
    @Test
    public void writesTheAuditBlockAndTheStatusAndNothingElse() {
        try {
            final String json = mapper.writeValueAsString(new TestSetMarker().setCreatedBy("mtb"));

            assertTrue(json.contains("\"createdBy\":\"mtb\""), json);
            assertTrue(json.contains("\"modifiedBy\""), json);
            assertTrue(json.contains("\"status\":\"ACTIVE\""), json);
            assertFalse(json.contains("statusLabel"), json);
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * A node nobody ordered says nothing about order.
     * <p>
     * "No number" is the largest number there is, so that an unordered node
     * sorts after every ordered one without anything having to test for it - but
     * these files are committed and read by people, and a marker carrying
     * 2147483647 would be a number no human wrote and none can explain. It is
     * left out instead, and a file without the key reads back as unordered.
     */
    @Test
    public void anUnorderedMarkerCarriesNoOrderAtAll() {
        try {
            assertFalse(mapper.writeValueAsString(new TestSetMarker()).contains("order"),
                    mapper.writeValueAsString(new TestSetMarker()));

            assertEquals(mapper.readValue("{}", TestSetMarker.class).getOrder(), Marker.NOT_ORDERED);
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * A node the tester did order carries the number they typed, and only that.
     */
    @Test
    public void anOrderedMarkerCarriesTheNumberTyped() {
        try {
            final String json = mapper.writeValueAsString(new TestSetMarker().setOrder(3));

            assertTrue(json.contains("\"order\":3"), json);
            assertEquals(mapper.readValue(json, TestSetMarker.class).getOrder(), 3);
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Markers written before the rename carry updatedBy/updatedAt. The alias used
     * to sit on the test project marker alone, because it was the only marker
     * that existed when the fields were renamed; the audit block has one owner
     * now, so every marker reads an old file.
     */
    @Test
    public void readsThePreRenameKeysOnEveryMarker() {
        try {
            final String old = "{\"createdBy\":\"a\",\"updatedBy\":\"b\",\"updatedAt\":\"" + onDisk + "\",\"status\":\"ACTIVE\"}";

            assertEquals(mapper.readValue(old, TestProjectMarker.class).getModifiedBy(), "b");
            assertEquals(mapper.readValue(old, TestProjectMarker.class).getModifiedAt().toInstant(), when.toInstant());
            assertEquals(mapper.readValue(old, TestSetMarker.class).getModifiedBy(), "b");
            assertEquals(mapper.readValue(old, TestSetMarker.class).getModifiedAt().toInstant(), when.toInstant());
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * A key the marker does not have is ignored rather than fatal: the same
     * directory can be read by an older build, and a status belongs to five of
     * the seven markers but not to the two directory ones.
     */
    @Test
    public void ignoresAKeyTheMarkerDoesNotHave() {
        try {
            final String withStatus = "{\"createdBy\":\"a\",\"status\":\"ACTIVE\",\"whatever\":1}";

            assertEquals(mapper.readValue(withStatus, TestCasesMainDirectoryMarker.class).getCreatedBy(), "a");
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * A marker file written before the modified pair existed has only the
     * creation pair, and says so: the node was last touched when it was made.
     * The default used to be now(), so those nodes reported themselves as
     * modified at the moment they were read.
     */
    @Test
    public void aMarkerNeverModifiedReportsItsCreation() {
        try {
            final String onlyCreated = "{\"createdBy\":\"mtb\",\"createdAt\":\"" + onDisk + "\"}";

            final TestCasesMainDirectoryMarker marker = mapper.readValue(onlyCreated, TestCasesMainDirectoryMarker.class);

            assertEquals(marker.getModifiedBy(), "mtb", "nobody has modified it, so it stands as its creator made it");
            assertEquals(marker.getModifiedAt().toInstant(), when.toInstant(), "and at the time they made it");
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * The date format is the one every marker file on disk is written in, so it
     * has to survive both directions - a change to the pattern would make every
     * existing marker unreadable rather than merely differently formatted.
     */
    @Test
    public void roundTripsTheDateFormatOnDisk() {
        try {
            final String json = mapper.writeValueAsString(new TestSetMarker().setCreatedAt(when).setModifiedAt(when));
            assertTrue(json.contains(onDisk), json);

            final TestSetMarker read = mapper.readValue(json, TestSetMarker.class);
            assertEquals(read.getCreatedAt().toInstant(), when.toInstant());
            assertEquals(read.getModifiedAt().toInstant(), when.toInstant());
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }
}

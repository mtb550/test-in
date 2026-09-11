package org.testin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.components.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.File;
import java.io.InputStream;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Mapper {

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-070.
     * <p>
     * <b>A timestamp is stored in the zone it happened in, on every machine.</b>
     * <p>
     * This used to call {@code setTimeZone(TimeZone.getDefault())}, and Jackson
     * reads that as an instruction to convert a {@code ZonedDateTime} into the
     * machine's zone before formatting it. So one test case saved the same
     * instant two ways:
     * <pre>
     * in Riyadh   "createdAt" : "Wednesday 02-09-2026 At 23:29:28 [Asia/Riyadh]"
     * in London   "createdAt" : "Wednesday 02-09-2026 At 20:29:28 [UTC]"
     * </pre>
     * Three things follow, and the first is the one that matters. <b>The stored
     * bytes were not byte-identical across machines</b>, which is the rule this
     * project states hardest. A colleague in another zone who opened a test set
     * and saved rewrote every timestamp in it - a commit full of changes that
     * mean nothing, over test data two people are meant to share. And the zone
     * the work actually happened in was destroyed on the way through, replaced
     * by the zone of whoever last wrote the file.
     * <p>
     * Disabled rather than pinned to UTC: the instant is not the only fact a
     * timestamp carries. A run executed at nine in the morning in Riyadh reads
     * as six in the morning in UTC, and the first is what the tester wants to
     * see about their own run. The value already knows its zone; nothing else
     * has to.
     * <p>
     * The read side is disabled for the same reason - a file that says
     * {@code [Asia/Riyadh]} parses back as Riyadh rather than as the reader's
     * zone, so reading and writing are inverses.
     * <p>
     * Nothing is pinned in place of it. {@code setTimeZone} governed no other
     * type here: there is not one {@code java.util.Date} or {@code Calendar} in
     * anything this serializes.
     */
    private final @NotNull ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    public @NotNull <T> T readValue(final @NotNull File src, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(src, valueType);

        } catch (final Exception ex) {
            Logger.error("Mapper.readValue() failed for file '" + src.getAbsolutePath() + "' to class " + valueType.getSimpleName() + ": " + ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }

    public @NotNull <T> T readValue(final @NotNull File src, final @NotNull TypeReference<T> valueTypeRef) {
        try {
            return mapper.readValue(src, valueTypeRef);

        } catch (final Exception ex) {
            Logger.error("Failed to read file path " + src + " to TypeReference");
            Logger.error("Exception: " + ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }

    public @NotNull <T> T readValue(final @NotNull String content, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(content, valueType);

        } catch (final Exception ex) {
            Logger.error("Failed to parse JSON string to class " + valueType.getSimpleName());
            throw new RuntimeException(ex.getMessage());
        }
    }

    public @NotNull <T> T readValue(final @NotNull InputStream src, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(src, valueType);

        } catch (final Exception ex) {
            Logger.error("Failed to read InputStream to class " + valueType.getSimpleName());
            Logger.error("Exception: " + ex.getMessage());
            throw new RuntimeException(ex.getMessage());
        }
    }

    public @NotNull <T> T readValue(final @NotNull String content, final @NotNull TypeReference<T> valueTypeRef) {
        try {
            return mapper.readValue(content, valueTypeRef);

        } catch (final Exception ex) {
            Logger.error("Failed to parse JSON string to TypeReference.");
            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Raises rather than answering with nothing.
     * <p>
     * This used to return an empty array when serialization failed, and the
     * empty array was then written over the file: a failure was logged and
     * committed to disk as a zero-byte marker or test case, destroying what was
     * there. Both callers in the indexer already catch a failure here and refuse
     * to write - they simply never got one.
     */
    public byte @NotNull [] writeValueAsBytes(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);

        } catch (final Exception ex) {
            Logger.error("Failed to serialize object to bytes: " + value.getClass().getSimpleName());
            Logger.error("Exception: " + ex.getMessage());
            throw new IllegalStateException("Could not serialize " + value.getClass().getSimpleName(), ex);
        }
    }

    /**
     * Raises rather than answering with an empty string - see
     * {@link #writeValueAsBytes}. An empty string put on the clipboard is a
     * silent copy of nothing.
     */
    public @NotNull String writeValueAsString(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);

        } catch (final Exception ex) {
            Logger.error("Failed to serialize object to string: " + value.getClass().getSimpleName());
            throw new IllegalStateException("Could not serialize " + value.getClass().getSimpleName(), ex);
        }
    }

    /**
     * JSON as a tree rather than as an object, for the one job that is about the
     * file and not about what it holds: merging two versions of a test case
     * field by field, where a side that will not parse into a DTO still has
     * fields worth keeping (#90).
     * <p>
     * An empty object for anything unreadable. Every caller here treats an
     * unknown side as one that says nothing, which is what an empty object is.
     */
    public @NotNull ObjectNode readTree(final @NotNull String content) {
        try {
            final @NotNull JsonNode node = mapper.readTree(content);
            return node instanceof ObjectNode object ? object : mapper.createObjectNode();

        } catch (final Exception ex) {
            Logger.debug("Mapper.readTree() could not parse the content: " + ex.getMessage());
            return mapper.createObjectNode();
        }
    }

    public @NotNull <T> T convertValue(final @NotNull Object fromValue, final @NotNull Class<T> toValueType) {
        try {
            return mapper.convertValue(fromValue, toValueType);

        } catch (final Exception ex) {
            Logger.error("Failed to convert value to class " + toValueType.getSimpleName());
            throw new RuntimeException(ex.getMessage());
        }
    }
}

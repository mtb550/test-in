package org.testin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.components.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.TimeZone;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Mapper {
    private final @NotNull ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setTimeZone(TimeZone.getDefault());

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
     * An empty object for anything unreadable. Every caller of this treats an
     * unknown side as one that says nothing, which is what an empty object is.
     */
    public @NotNull ObjectNode readTree(final @NotNull String content) {
        try {
            final JsonNode node = mapper.readTree(content);
            return node instanceof ObjectNode object ? object : mapper.createObjectNode();

        } catch (final Exception ex) {
            Logger.debug("Mapper.readTree() could not parse the content: " + ex.getMessage());
            return mapper.createObjectNode();
        }
    }

    public @NotNull ObjectNode createObjectNode() {
        return mapper.createObjectNode();
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

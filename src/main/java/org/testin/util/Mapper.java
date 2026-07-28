package org.testin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.components.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.logger.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.TimeZone;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Mapper {
    private final ObjectMapper mapper = new ObjectMapper()
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

    public byte[] writeValueAsBytes(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);

        } catch (final Exception ex) {
            Logger.error("Failed to serialize object to bytes: " + value.getClass().getSimpleName());
            Logger.error("Exception: " + ex.getMessage());
            return new byte[0];
        }
    }

    public @NotNull String writeValueAsString(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);

        } catch (final Exception ex) {
            Logger.error("Failed to serialize object to string: " + value.getClass().getSimpleName());
            return "";
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

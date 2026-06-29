package org.testin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.components.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.logger.Log;

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

    public <T> T readValue(final @NotNull File src, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(src, valueType);

        } catch (Exception e) {
            Log.error("Mapper.readValue() failed for file '" + src.getAbsolutePath() + "' to class " + valueType.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    public <T> T readValue(final @NotNull File src, final @NotNull TypeReference<T> valueTypeRef) {
        try {
            return mapper.readValue(src, valueTypeRef);

        } catch (Exception e) {
            Log.error("Failed to read file path " + src + " to TypeReference");
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public <T> T readValue(final @NotNull String content, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(content, valueType);

        } catch (Exception e) {
            Log.error("Failed to parse JSON string to class " + valueType.getSimpleName());
            return null;
        }
    }

    public <T> T readValue(final @NotNull InputStream src, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(src, valueType);

        } catch (Exception e) {
            Log.error("Failed to read InputStream to class " + valueType.getSimpleName());
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public <T> T readValue(final @NotNull String content, final @NotNull TypeReference<T> valueTypeRef) {
        try {
            return mapper.readValue(content, valueTypeRef);

        } catch (Exception e) {
            Log.error("Failed to parse JSON string to TypeReference.");
            return null;
        }
    }

    public byte[] writeValueAsBytes(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);

        } catch (Exception e) {
            Log.error("Failed to serialize object to bytes: " + value.getClass().getSimpleName());
            Log.error("Exception: " + e.getMessage());
            return new byte[0];
        }
    }

    public String writeValueAsString(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);

        } catch (Exception e) {
            Log.error("Failed to serialize object to string: " + value.getClass().getSimpleName());
            return "";
        }
    }

    public <T> T convertValue(final @NotNull Object fromValue, final @NotNull Class<T> toValueType) {
        try {
            return mapper.convertValue(fromValue, toValueType);

        } catch (Exception e) {
            Log.error("Failed to convert value to class " + toValueType.getSimpleName());
            return null;
        }
    }
}

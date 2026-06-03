package org.testin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.util.TimeZone;

public class Mapper {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setTimeZone(TimeZone.getDefault());

    public static <T> T readValue(final @NotNull File src, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(src, valueType);

        } catch (Exception e) {
            System.err.println("Failed to read file path " + src + ". to class " + valueType.getSimpleName());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static <T> T readValue(final @NotNull File src, final @NotNull TypeReference<T> valueTypeRef) {
        try {
            return mapper.readValue(src, valueTypeRef);

        } catch (Exception e) {
            System.err.println("Failed to read file path " + src + " to TypeReference");
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static <T> T readValue(final @NotNull String content, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(content, valueType);

        } catch (Exception e) {
            System.err.println("Failed to parse JSON string to class " + valueType.getSimpleName());
            return null;
        }
    }

    public static <T> T readValue(final @NotNull InputStream src, final @NotNull Class<T> valueType) {
        try {
            return mapper.readValue(src, valueType);

        } catch (Exception e) {
            System.err.println("Failed to read InputStream to class " + valueType.getSimpleName());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static <T> T readValue(final @NotNull String content, final @NotNull TypeReference<T> valueTypeRef) {
        try {
            return mapper.readValue(content, valueTypeRef);

        } catch (Exception e) {
            System.err.println("Failed to parse JSON string to TypeReference.");
            return null;
        }
    }

    public static byte[] writeValueAsBytes(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);

        } catch (Exception e) {
            System.err.println("Failed to serialize object to bytes: " + value.getClass().getSimpleName());
            e.printStackTrace(System.err);
            return new byte[0];
        }
    }

    public static String writeValueAsString(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);

        } catch (Exception e) {
            System.err.println("Failed to serialize object to string: " + value.getClass().getSimpleName());
            return "";
        }
    }

    public static <T> T convertValue(final @NotNull Object fromValue, final @NotNull Class<T> toValueType) {
        try {
            return mapper.convertValue(fromValue, toValueType);

        } catch (Exception e) {
            System.err.println("Failed to convert value to class " + toValueType.getSimpleName());
            return null;
        }
    }
}

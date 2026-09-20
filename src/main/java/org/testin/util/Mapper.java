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
    // UC-INTERNAL-002, Rule-INTERNAL-070
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

    public byte @NotNull [] writeValueAsBytes(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(value);

        } catch (final Exception ex) {
            Logger.error("Failed to serialize object to bytes: " + value.getClass().getSimpleName());
            Logger.error("Exception: " + ex.getMessage());
            throw new IllegalStateException("Could not serialize " + value.getClass().getSimpleName(), ex);
        }
    }

    public @NotNull String writeValueAsString(final @NotNull Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);

        } catch (final Exception ex) {
            Logger.error("Failed to serialize object to string: " + value.getClass().getSimpleName());
            throw new IllegalStateException("Could not serialize " + value.getClass().getSimpleName(), ex);
        }
    }

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

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

package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Setter
@Getter
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public abstract class AbstractMarker implements Marker {
    // Rule-INTERNAL-090, Rule-TREE-PANEL-051, Rule-INTERNAL-083
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @NonNull
    private String id = "";

    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = AbstractMarker.Unordered.class)
    private int order = Marker.NOT_ORDERED;

    static final class Unordered {
        @Override
        public boolean equals(final Object value) {
            return value instanceof Integer order && order == Marker.NOT_ORDERED;
        }

        @Override
        public int hashCode() {
            return Marker.NOT_ORDERED;
        }
    }

    @NonNull
    private String createdBy = "";

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @JsonAlias("updatedBy")
    @NonNull
    private String modifiedBy = "";

    @JsonAlias("updatedAt")
    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime modifiedAt = Config.NOT_EXECUTED;

    public @NotNull String getModifiedBy() {
        return modifiedBy.isBlank() ? createdBy : modifiedBy;
    }

    public @NotNull ZonedDateTime getModifiedAt() {
        return Config.isNotExecuted(modifiedAt) ? createdAt : modifiedAt;
    }
}

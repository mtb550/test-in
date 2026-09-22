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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeStatus;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface Marker {
    int NOT_ORDERED = Integer.MAX_VALUE;

    @NotNull String getId();

    void setId(@NotNull String id);

    int getOrder();

    void setOrder(int order);

    @NotNull String getCreatedBy();

    void setCreatedBy(@NotNull String createdBy);

    @NotNull ZonedDateTime getCreatedAt();

    void setCreatedAt(@NotNull ZonedDateTime createdAt);

    @NotNull String getModifiedBy();

    void setModifiedBy(@NotNull String modifiedBy);

    @NotNull ZonedDateTime getModifiedAt();

    void setModifiedAt(@NotNull ZonedDateTime modifiedAt);

    default void stampCreated(final @NotNull String tester) {
        final @NotNull ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        setCreatedBy(tester);
        setCreatedAt(now);
        setModifiedBy(tester);
        setModifiedAt(now);
    }

    default void touch(final @NotNull String tester) {
        setModifiedBy(tester);
        setModifiedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    default @NotNull NodeStatus status() {
        return NodeStatus.NONE;
    }

    default @NotNull List<NodeStatus> statuses() {
        return List.of();
    }

    default void applyStatus(final @NotNull NodeStatus status) {
    }

    @JsonIgnore
    default @NotNull String getStatusLabel() {
        return status().getLabel();
    }

    @JsonIgnore
    default @NotNull List<DetailRow> getDetailRows() {
        return List.of();
    }
}

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

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeStatus;
import org.testin.model.TestSetStatus;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestSetMarker extends AbstractMarker {
    /**
     * Deprecated test sets keep their cases and their run history; they stop
     * being offered when a new run is configured (#68).
     */
    @NonNull
    private TestSetStatus status = TestSetStatus.ACTIVE;

    @Override
    public @NotNull NodeStatus status() {
        return status;
    }

    @Override
    public @NotNull List<NodeStatus> statuses() {
        return List.of(TestSetStatus.values());
    }

    @Override
    public void applyStatus(final @NotNull NodeStatus status) {
        setStatus((TestSetStatus) status);
    }
}

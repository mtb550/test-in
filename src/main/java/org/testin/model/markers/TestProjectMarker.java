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
import org.testin.model.ProjectStatus;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestProjectMarker extends AbstractMarker {
    @NonNull
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Override
    public @NotNull NodeStatus status() {
        return status;
    }

    @Override
    public @NotNull List<NodeStatus> statuses() {
        return List.of(ProjectStatus.values());
    }

    @Override
    public void applyStatus(final @NotNull NodeStatus status) {
        setStatus((ProjectStatus) status);
    }
}

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
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeStatus;
import org.testin.model.ProjectStatus;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestProjectMarker extends AbstractMarker {

    /**
     * Rule-INTERNAL-091.
     * <p>
     * The format this build writes, and the number a converted project carries.
     * It is on the marker that holds the field rather than on the converter,
     * because 2.14.0-alpha deletes the converter and still has to refuse a
     * project nobody converted (#305, D4).
     */
    public static final int FORMAT = 2;

    @NonNull
    private ProjectStatus status = ProjectStatus.ACTIVE;

    /**
     * Rule-INTERNAL-091.
     * <p>
     * Which format this project's files are in: test cases as {@code .tc}, a
     * result per case as {@code .ri}, and an id in every marker is format 2. The
     * default, 0, means a file written before the number existed - a project
     * 2.13.0-alpha converts once, and 2.14.0-alpha refuses (#305, D4).
     */
    private int format;

    /**
     * Rule-INTERNAL-091.
     * <p>
     * Why this build cannot read the project, and nothing when it can: written by
     * an older Testin and not converted yet, or by a newer one whose format this
     * build does not know. A newer format is refused rather than guessed at -
     * reading a format 3 project as a format 2 one is how a build deletes what it
     * does not understand (#305, S5).
     */
    @JsonIgnore
    public @NotNull Optional<String> whyNotReadable() {
        if (format == FORMAT) return Optional.empty();

        return Optional.of(format > FORMAT
                ? Bundle.message("scan.newer.format")
                : Bundle.message("scan.older.format", "2.13.0-alpha"));
    }

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

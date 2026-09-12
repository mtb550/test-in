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
import org.testin.model.TestRunStatus;


@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestRunMarker extends AbstractMarker {
    @NonNull
    private TestRunStatus status = TestRunStatus.CREATED;

    /**
     * The one marker that shows a status the tester cannot set.
     * <p>
     * A run's status is the run's own account of itself - created, executed -
     * moved by running it, not by a menu entry. So {@link TestRunStatus} is not
     * a {@link org.testin.model.NodeStatus} and this answers the label directly
     * rather than through {@code status()} (#110).
     */
    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }

}

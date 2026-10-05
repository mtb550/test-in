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


package org.testin.help;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class Hints {
    private final @NotNull Project p;

    private final @NotNull Map<Guide, Hint> waiting = new EnumMap<>(Guide.class);

    public Hints(final @NotNull Project p) {
        this.p = p;
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    public synchronized void fire(final @NotNull Hint hint) {
        waiting.put(hint.topic(), hint);
        HelpMark.redraw(p);
    }

    // UC-INTERNAL-009, Rule-INTERNAL-127
    public synchronized void clear(final @NotNull Guide topic) {
        waiting.remove(topic);
        HelpMark.redraw(p);
    }

    synchronized @NotNull List<Hint> waiting() {
        return List.copyOf(waiting.values());
    }
}

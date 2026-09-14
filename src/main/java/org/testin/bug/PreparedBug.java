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

package org.testin.bug;

import org.jetbrains.annotations.NotNull;
import org.testin.config.BugRepository;

import java.util.Optional;

/**
 * A bug report ready to open (#28): what it says, where it would be filed, and
 * whether it can be sent.
 *
 * @param body        the template, filled
 * @param repository  where it would be filed, and empty when {@code bugRepoUrl}
 *                    names no repository
 * @param whyNotReady why it cannot be sent, and empty when it can
 */
record PreparedBug(@NotNull BugFacts facts, @NotNull String body, @NotNull Optional<BugRepository> repository, @NotNull Optional<String> whyNotReady) {
}

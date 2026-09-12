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

package org.testin.ui.framework;

import lombok.Builder;
import lombok.NonNull;

import java.util.List;

/**
 * Everything a framework dialog declares, as one object. Framework-internal:
 * {@code AbstractFrameworkDialog} packages its declaration fields here on
 * first show. All parts are {@code @NonNull}, so a field the dialog forgot to
 * assign fails immediately with a clear message.
 */
// The canonical constructor is reported as never used, and kept: Lombok's
// generated builder is what calls it, and the inspection does not see generated
// code (#61).
@Builder
record DialogDto(@NonNull String title, @NonNull List<? extends ComponentDialogBase<?>> components, @NonNull List<StatusBarShortcut> shortcuts) {
}

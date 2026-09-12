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

package org.testin.editor.listeners;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * What one edit of one grid cell came to.
 * <p>
 * It was a boolean, and a boolean cannot say the third thing: that the grid
 * would not take the value and has already told the tester why. The parent
 * listener then followed a red <i>Could not read "Urgnt" as a Priority</i> with
 * a blue <i>Adjusted - Testin stored 'High' rather than 'Urgnt'</i>, because the
 * cell really did end up holding something other than what was typed - two
 * balloons for one keystroke, the second of them explaining the first as though
 * it were a different event (#66, finding 81).
 */
@Getter
@AllArgsConstructor
public enum GridEdit {

    /**
     * The value reached the test case or the run item, so the edit is confirmed,
     * saved and generated.
     */
    WROTE(true, false),

    /**
     * Nothing changed: a column that cannot be edited, a row the run does not
     * cover, or a value that came back equal to the one already there. Nothing
     * to save and nothing to say.
     */
    UNCHANGED(false, false),

    /**
     * The grid would not take the value, and has said so in its own words. There
     * is nothing left to tell the tester.
     */
    REFUSED(false, true);

    private final boolean written;
    private final boolean said;
}

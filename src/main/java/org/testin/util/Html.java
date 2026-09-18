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

package org.testin.util;

import com.intellij.openapi.util.text.StringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Plain text, for a surface that renders HTML.
 * <p>
 * Balloons, notifications and a few labels render HTML, and what they carry is
 * often what the tester typed or what a tool printed. Handed over raw, anything
 * that looks like a tag is eaten: a refused grid value of "{@code <none>}" was
 * quoted back as "Could not read  as Priority", with the value the message
 * existed to show gone (#66, finding 197).
 * <p>
 * One owner, because it was written out by hand in two places and not at all in
 * the two that needed it most. Callers hand over text, with {@code \n} for a
 * line break; the markup is the template around it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Html {

    /**
     * The text with everything HTML would read as markup escaped, and each line
     * break a {@code <br>}.
     */
    public static @NotNull String ofText(final @NotNull String text) {
        return StringUtil.escapeXmlEntities(text).replace("\n", "<br>");
    }
}

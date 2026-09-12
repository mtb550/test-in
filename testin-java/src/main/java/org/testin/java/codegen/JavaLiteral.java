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

package org.testin.java.codegen;

import com.intellij.openapi.util.text.StringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A tester's text, as a Java string literal.
 * <p>
 * Two places wrote a test case description into generated code and both escaped
 * only the double quote. A description is free text a tester types, so it
 * carries whatever they typed: a Windows path like {@code clear C:\Users\temp}
 * became an illegal escape and the generated class stopped compiling, and
 * {@code C:\temp} silently became a tab. Either way every case in that test set
 * stopped running and nothing said why.
 * <p>
 * The platform already knows how to do this. {@code StringUtil} escapes the
 * quote, the backslash, and the control characters that cannot appear in a
 * literal at all - which is the list nobody remembers in full, and the reason
 * this is one call rather than a rule each generator applies.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaLiteral {

    /**
     * The text as a quoted literal, ready to be written into source.
     */
    public static @NotNull String of(final @NotNull String text) {
        return '"' + StringUtil.escapeStringCharacters(text) + '"';
    }
}

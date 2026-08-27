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

package org.testin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.SourceVersion;
import java.util.regex.Pattern;

/**
 * Naming rules used when generating Java packages, classes, and methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NameSanitizer {

    private static final @NotNull Pattern INVALID_NAME = Pattern.compile("[^a-zA-Z0-9 _]");

    public static @NotNull String packageName(final @NotNull String value) {
        final @NotNull String cleanName = INVALID_NAME.matcher(value.replace("-test-cases", ""))
                .replaceAll("").trim();
        final @NotNull StringBuilder result = new StringBuilder();
        for (final String word : cleanName.split("[\\s_]+")) {
            if (word.isEmpty()) continue;
            if (result.isEmpty()) {
                result.append(word.toLowerCase());
            } else {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }

        if (result.isEmpty()) return "generated" + System.currentTimeMillis();
        if (Character.isDigit(result.charAt(0))) result.insert(0, '_');
        return result.toString();
    }

    public static @NotNull String className(final @NotNull String value) {
        if (value.trim().isEmpty()) return "DefaultTest";
        final @NotNull String cleanName = INVALID_NAME.matcher(value).replaceAll("").trim();
        final @NotNull StringBuilder result = new StringBuilder();
        for (final String word : cleanName.split("[\\s_]+")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        if (result.isEmpty()) return "DefaultTest";
        if (Character.isDigit(result.charAt(0))) result.insert(0, '_');
        return result.append("Test").toString();
    }

    public static @NotNull String description(final @NotNull String rawDescription) {
        if (rawDescription.isBlank()) return "EMPTY_DESCRIPTION";
        final @NotNull String cleaned = INVALID_NAME.matcher(rawDescription).replaceAll("").trim();
        return cleaned.isEmpty() ? "EMPTY_DESCRIPTION" : cleaned;
    }

    public static @NotNull String methodName(final @NotNull String description) {
        if (description.isEmpty()) return "testMethod";
        final @NotNull StringBuilder result = new StringBuilder();
        for (final String word : description.split("[^a-zA-Z0-9]+")) {
            if (word.isEmpty()) continue;
            if (result.isEmpty()) {
                result.append(word.toLowerCase());
            } else {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) result.append(word.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    /**
     * The form of a method name used to decide whether two names are the same
     * method: lowercase, underscores dropped.
     * <p>
     * {@link #methodName} flattens a description's inner capitals and
     * underscores, so a hand-written {@code verifyNEXTDbValue} or
     * {@code verifyOTP_Retry_Count} and the generated {@code verifyNextDbValue}
     * or {@code verifyOtpRetryCount} are one method to a reader - and must be
     * one to the generator, or an import writes an empty stub beside the real
     * test (#66, finding 41).
     */
    public static @NotNull String methodKey(final @NotNull String methodName) {
        return methodName.replace("_", "").toLowerCase();
    }

    /**
     * Whether a description can name a generated test method at all.
     * <p>
     * Asked by the dialogs before a description is stored, because the answer is
     * no more often than it looks: {@code 4redxk,jfsdf} names
     * {@code 4redxkJfsdf}, which cannot start with a digit; a description of
     * only punctuation names nothing; and {@code New} names {@code new}, which
     * Java keeps for itself. All three reached the generator, where renaming a
     * method threw and writing one put uncompilable Java in the file (#66).
     * <p>
     * Asked of the JDK rather than answered here: {@code SourceVersion.isName}
     * is exactly this question - a valid identifier that is not a keyword or a
     * literal - and it is one call instead of a list of fifty words to keep in
     * step with the language.
     */
    public static boolean canMakeMethodName(final @NotNull String description) {
        return SourceVersion.isName(methodName(description));
    }

    public static @NotNull String removeSpecialChars(final @NotNull String value) {
        if (value.isEmpty()) return "";
        return value.chars()
                .mapToObj(c -> isSpecial((char) c) ? "_" : String.valueOf((char) c))
                .reduce(new StringBuilder(value.length()), StringBuilder::append, StringBuilder::append)
                .toString();
    }

    private static boolean isSpecial(final char value) {
        return "!\"#$%&'()*+,./:;<=>?@[\\]^_`{|}~".indexOf(value) >= 0;
    }
}

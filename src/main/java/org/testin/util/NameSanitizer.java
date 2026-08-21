package org.testin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Naming rules used when generating Java packages, classes, and methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NameSanitizer {

    private static final @NotNull Pattern INVALID_NAME = Pattern.compile("[^a-zA-Z0-9 _]");

    public static @NotNull String packageName(final @NotNull String value) {
        final String cleanName = INVALID_NAME.matcher(value.replace("-test-cases", ""))
                .replaceAll("").trim();
        final StringBuilder result = new StringBuilder();
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
        final String cleanName = INVALID_NAME.matcher(value).replaceAll("").trim();
        final StringBuilder result = new StringBuilder();
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
        final String cleaned = INVALID_NAME.matcher(rawDescription).replaceAll("").trim();
        return cleaned.isEmpty() ? "EMPTY_DESCRIPTION" : cleaned;
    }

    public static @NotNull String methodName(final @NotNull String description) {
        if (description.isEmpty()) return "testMethod";
        final StringBuilder result = new StringBuilder();
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

    public static @NotNull String removeSpecialChars(final @NotNull String value) {
        if (value.isEmpty()) return "";
        return value.chars()
                .mapToObj(c -> isSpecial((char) c) ? "_" : String.valueOf((char) c))
                .reduce(new StringBuilder(value.length()), StringBuilder::append, StringBuilder::append)
                .toString();
    }

    public static @NotNull String projectNameFromUrl(final @NotNull String gitUrl) {
        String name = gitUrl;
        if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        if (name.endsWith(".git")) name = name.substring(0, name.length() - 4);
        final int splitIndex = Math.max(name.lastIndexOf('/'), name.lastIndexOf(':'));
        return splitIndex >= 0 && splitIndex < name.length() - 1
                ? name.substring(splitIndex + 1)
                : "ImportedTestProject";
    }

    private static boolean isSpecial(final char value) {
        return "!\"#$%&'()*+,./:;<=>?@[\\]^_`{|}~".indexOf(value) >= 0;
    }
}

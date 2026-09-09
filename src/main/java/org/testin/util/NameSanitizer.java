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

        if (result.isEmpty()) return "generated" + tag(value);
        if (Character.isDigit(result.charAt(0))) result.insert(0, '_');
        return result.toString();
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-011.
     * <p>
     * What tells two names apart when neither has a letter or a digit left in
     * it, derived from the name itself and from nothing else.
     * <p>
     * The package fallback used to be {@code "generated" + currentTimeMillis()}.
     * That is not a name, it is a new name every time it is asked for: the same
     * node produced a different package on the call that generated its code and
     * on the call that came looking for it, so a rename, a move or a remove
     * found nothing and did nothing, in silence. The class fallback had the
     * opposite fault - every such node answered {@code DefaultTest}, so two of
     * them wrote into one file and the second set's methods landed in the
     * first's class (#250).
     * <p>
     * A hash of the raw name fixes both at once, because it is the two things
     * neither fallback was: the same answer every time for one name, and a
     * different answer for a different name. {@code String.hashCode} is
     * specified by the language rather than left to the JVM, so a name generated
     * on one machine is found again on another.
     */
    private static @NotNull String tag(final @NotNull String value) {
        return Integer.toHexString(value.hashCode());
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-011.
     * <p>
     * The class a test set's name gives. The same name always gives the same
     * class, and two different names never give one class - which is the whole
     * of what a generated name has to promise.
     * <p>
     * A name with nothing left in it after the illegal characters go takes a
     * name built from what it was, rather than the one word every such name used
     * to take. See {@link #tag}.
     */
    public static @NotNull String className(final @NotNull String value) {
        // A node with no name at all is one node, not a family of them, so one
        // word answers for it. The tree refuses an empty name, so this is data
        // that arrived some other way.
        if (value.trim().isEmpty()) return "DefaultTest";

        final @NotNull String cleanName = INVALID_NAME.matcher(value).replaceAll("").trim();
        final @NotNull StringBuilder result = new StringBuilder();
        for (final String word : cleanName.split("[\\s_]+")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        if (result.isEmpty()) return "Generated" + tag(value) + "Test";
        if (Character.isDigit(result.charAt(0))) result.insert(0, '_');
        return result.append("Test").toString();
    }

    /**
     * A description, cleaned of the characters a description may not carry - and
     * nothing else.
     * <p>
     * It used to answer the literal {@code EMPTY_DESCRIPTION} for a blank one,
     * and the DESCRIPTION setter runs every save through here, so the
     * placeholder was <b>stored</b>. The tester then read it as the case's name
     * on the card, in the grid, in the details panel, in every report and in the
     * execution log, and the generated method was called {@code emptyDescription}
     * - which is the rule in CLAUDE.md broken as plainly as it can be: the
     * tester typed nothing and the file said something (#155).
     * <p>
     * A description nobody has written is empty, it is stored empty, and every
     * surface draws it blank. {@link #className} keeps its fallback: a class
     * name is derived from the tree and is never stored.
     */
    public static @NotNull String description(final @NotNull String rawDescription) {
        return INVALID_NAME.matcher(rawDescription).replaceAll("").trim();
    }

    /**
     * The method name a description gives, and none when it gives none.
     * <p>
     * It used to answer {@code testMethod} for a case with no description, so a
     * case nobody had finished writing got a method with a name nobody chose -
     * and the second such case in a set collided with the first. Nothing is
     * generated for a case that cannot name a method; the method is written the
     * moment somebody gives it one (#155).
     */
    public static @NotNull String methodName(final @NotNull String description) {
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

package org.testin.model;

import com.intellij.icons.AllIcons;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import org.testin.model.dto.TestRunDto;

import javax.swing.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Getter
@AllArgsConstructor
public enum TestRunConfiguration {

    TEST_TYPE(
            "Test Type",
            AllIcons.Nodes.Type,
            new String[]{"", "Functional Test", "Performance Test"},
            ShownWhen.ALWAYS
    ),

    CHANGE_LOG(
            "Change Log",
            AllIcons.Nodes.Type,
            Free.OPTIONS,
            ShownWhen.ALWAYS
    ),

    COMMIT_ID(
            "Commit ID",
            AllIcons.Nodes.Type,
            Free.OPTIONS,
            ShownWhen.ALWAYS
    ),

    PLATFORM(
            "Platform",
            AllIcons.Nodes.PpLib,
            new String[]{"", Answer.WEB, Answer.MOBILE},
            ShownWhen.ALWAYS
    ),

    COMPONENT(
            "Component",
            AllIcons.Nodes.PpLib,
            new String[]{"", Answer.FRONTEND, "Backend"},
            ShownWhen.ALWAYS
    ),

    LANGUAGE(
            "Language",
            AllIcons.Nodes.Lambda,
            new String[]{"", "English", "Arabic", "French"},
            ShownWhen.ALWAYS
    ),

    /**
     * A browser is what a web frontend runs in, so the question is only worth
     * asking of a run that says it is one. A backend run has no browser, and a
     * mobile one has a handset instead.
     */
    BROWSER(
            "Browser",
            AllIcons.Nodes.WebFolder,
            new String[]{"", "Chrome", "Firefox", "Safari", "Edge"},
            chosen -> chosen.is(PLATFORM, Answer.WEB) && chosen.is(COMPONENT, Answer.FRONTEND)
    ),

    /**
     * A handset is what a mobile frontend runs on - the same question as the
     * browser, on the other platform.
     */
    DEVICE_TYPE(
            "Device Type",
            AllIcons.Nodes.Include,
            new String[]{"", "iPhone", "Samsung", "Huawei"},
            chosen -> chosen.is(PLATFORM, Answer.MOBILE) && chosen.is(COMPONENT, Answer.FRONTEND)
    );

    private final @NotNull String displayName;
    private final @NotNull Icon icon;

    /**
     * A field with nothing to pick from: a line to type in.
     * <p>
     * In a holder because an enum constant cannot name a static field of its own
     * enum, and the constants are declared first.
     */
    private static final class Free {
        private static final String @NotNull[] OPTIONS = new String[0];
    }

    /**
     * The answers one field offers that another field's rule reads.
     * <p>
     * Here rather than written twice because they have to be the same word: a
     * list offering "Web" and a rule looking for "Web" that drift apart do not
     * fail, they quietly stop showing a field nobody can then fill in. Only the
     * answers a rule actually asks about are named - the rest are read by
     * nobody and are plain text in their list.
     * <p>
     * In a holder for the reason {@link Free} is.
     */
    private static final class Answer {
        private static final @NotNull String WEB = "Web";
        private static final @NotNull String MOBILE = "Mobile";
        private static final @NotNull String FRONTEND = "Frontend";
    }

    /**
     * What the field offers to pick from, and nothing at all for a field that is
     * free text - which is what {@link #isChoice()} answers.
     */
    private final @NotNull String[] options;

    /**
     * When this field applies at all. Not exposed: the question to ask is
     * {@link #isShownFor}, so there is one way to put it rather than a rule
     * callers can fetch and apply themselves.
     */
    @Getter(AccessLevel.NONE)
    private final @NotNull ShownWhen shownWhen;

    /**
     * This field of that run, and blank when the tester did not answer it.
     * <p>
     * Blank rather than absent, so no reader has to hold a maybe-answer. A
     * question that did not apply and a question left empty are the same thing
     * to everything that shows one.
     */
    public @NotNull String valueIn(final @NotNull TestRunDto run) {
        return run.getConfiguration().getOrDefault(this, "");
    }

    /**
     * The answers worth storing: the ones the tester actually gave.
     * <p>
     * Here rather than at the one caller, because what counts as an answer is
     * this enum's business. A run left with the defaults then stores no
     * configuration at all, and a question that did not apply - a browser on a
     * mobile run - is absent from the file rather than present and blank.
     */
    public static @NotNull Map<TestRunConfiguration, String> answered(final @NotNull Map<TestRunConfiguration, String> chosen) {
        final @NotNull Map<TestRunConfiguration, String> stored = new EnumMap<>(TestRunConfiguration.class);

        for (final TestRunConfiguration field : values()) {
            final @NotNull String value = Objects.toString(chosen.get(field), "");
            if (!value.isEmpty()) stored.put(field, value);
        }

        return stored;
    }

    /**
     * True when this field is a dropdown rather than a line to type in.
     */
    public boolean isChoice() {
        return options.length > 0;
    }

    /**
     * Whether this field belongs on a run at all, given what has been chosen in
     * the others.
     * <p>
     * One question behind both halves of what "does not apply" means: the form
     * hides the field, and the run is saved without a value for it. A run
     * switched from web to mobile must not keep the browser that was picked
     * before the switch, and it will not, because the same rule answers here.
     */
    public boolean isShownFor(final @NotNull Chosen chosen) {
        return shownWhen.holds(chosen);
    }

    /**
     * Whether a field applies, asked of what the tester has chosen so far.
     */
    @FunctionalInterface
    public interface ShownWhen {

        /**
         * A field that is on every run, whatever else was answered. Most are.
         * <p>
         * On the interface rather than beside the constants because an enum
         * constant cannot name a static field of its own enum.
         */
        @NotNull ShownWhen ALWAYS = chosen -> true;

        boolean holds(final @NotNull Chosen chosen);
    }

    /**
     * What is currently answered in another field, however the caller keeps it.
     */
    @FunctionalInterface
    public interface Chosen {

        @NotNull String in(final @NotNull TestRunConfiguration field);

        default boolean is(final @NotNull TestRunConfiguration field, final @NotNull String answer) {
            return answer.equals(in(field));
        }
    }
}

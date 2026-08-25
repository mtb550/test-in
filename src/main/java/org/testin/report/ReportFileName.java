package org.testin.report;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.NameSanitizer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What a report is called when the save dialog opens:
 * {@code TestRun_<project>_<run>_<date>_<time>}.
 * <p>
 * The name says what the file is without being opened, which matters because
 * reports leave the plugin - they are attached to a ticket, mailed, dropped in a
 * shared folder - and a folder of files called "Sprint 7 Cycle 3_Report" from
 * three projects is a folder nobody can sort out afterwards. The stamp makes a
 * second report of the same run a second file rather than a question about
 * whether to overwrite the first.
 * <p>
 * The project is the one testin.yml names, the same name the tree and the report
 * body show.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportFileName {

    /**
     * The date and the time, in the plugin's day-first order.
     * <p>
     * The time is separated by dashes rather than the colons a clock uses.
     * Windows forbids a colon in a file name outright, and macOS shows one as a
     * slash - so a name built with them is either refused or silently mangled at
     * the moment the tester presses Save.
     * <p>
     * Twelve-hour with AM or PM, so a report generated at 21:40 reads as the
     * tester's own working day.
     */
    private static final @NotNull DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd-MM-yyyy_hh-mm-ssa", Locale.US);

    public static @NotNull String suggestedFor(final @NotNull Project p, final @NotNull TestRunDirectoryDto run, final @NotNull ZonedDateTime at) {
        return of(Services.getInstance(p, BoundTestProject.class).name(), run.getName(), at);
    }

    /**
     * The name from its parts. Separate from the lookup above so the rule can be
     * checked without a project to look anything up in.
     * <p>
     * A part that is empty is left out rather than leaving a gap: a repository
     * that names no project should produce {@code TestRun_Sprint 7_...}, not
     * {@code TestRun__Sprint 7_...}.
     */
    static @NotNull String of(final @NotNull String projectName, final @NotNull String runName, final @NotNull ZonedDateTime at) {
        return Stream.of("TestRun", safe(projectName), safe(runName), STAMP.format(at))
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining("_"));
    }

    /**
     * A name a file system will accept, through the same rule everything else in
     * the plugin uses for that.
     */
    private static @NotNull String safe(final @NotNull String name) {
        // Spaces come out too. "Sprint 7 Cycle 3" is a name a tester reads; a
        // file called that is one they have to quote on a command line, and one
        // that arrives with %20 through half the tools it is sent through.
        return NameSanitizer.removeSpecialChars(name).replace(" ", "").trim();
    }
}

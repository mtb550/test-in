package org.testin.importexport.imports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * How one attribute writes what a tester typed onto a test case - in a grid
 * cell, in the import preview, or in a row of an imported sheet.
 * <p>
 * It answers whether the value took. Four attributes have to read their text
 * before they can store it, and each of them used to answer an unreadable value
 * differently and in silence: a priority became the lowest, a group was dropped
 * from the list, a date became blank, and a status kept whatever the row had.
 * Two of those change data the tester did not ask to change, and none of them
 * said so - which is how 200 imported cases whose priority column read High,
 * Medium and Low all arrived at the lowest priority (#204, #264).
 * <p>
 * One answer now: <b>a value Testin cannot read is refused, and what was there
 * stays</b>. The boolean is what lets the caller say so - once for a cell, and
 * once with a count for an import of two hundred rows.
 */
@FunctionalInterface
public interface ImportSetter {

    /**
     * @return true when the value was written, false when Testin could not read
     * it and the test case was left as it was
     */
    boolean execute(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String value);

    /**
     * For an attribute that can always read what it is handed - anything stored
     * as the tester typed it, and the three that store nothing at all.
     */
    static boolean always(final @NotNull Runnable write) {
        write.run();
        return true;
    }

    /**
     * For an attribute whose text has to be read first: written when it was
     * read, and refused when it was not.
     */
    static <T> boolean took(final @NotNull Optional<T> read, final @NotNull Consumer<T> onto) {
        read.ifPresent(onto);

        return read.isPresent();
    }
}

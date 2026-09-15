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

package org.testin.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * What a test framework said when a case did not pass: the message, and the
 * stacktrace behind it.
 * <p>
 * The two travel as one value because they describe one failure and mean little
 * apart - a stacktrace with no message is a dump nobody reads, and a message
 * with someone else's stacktrace is worse than none. It is the same argument as
 * {@link RunStatus.Badge}, which holds a label and the color to draw it in.
 * <p>
 * A report that is not a failure carries {@link #NONE} rather than a null, so
 * every broadcaster passes a value and no listener asks whether there is one.
 */
public record Failure(@NotNull String message, @NotNull String stacktrace) {

    /**
     * What a case that has just started, or passed, or that a tester stopped
     * reports: nothing went wrong, said with a value of its own type.
     */
    public static final @NotNull Failure NONE = new Failure("", "");

    /**
     * UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-220.
     * <p>
     * Writes what happened onto a run row, into the two fields a tester would
     * otherwise fill in by hand through {@code FailedResultDialog} - the actual
     * result and the error capture. An automated failure describes itself in
     * exactly the terms a manual one does, so it fills the same two fields
     * rather than growing two more beside them, and every report already built
     * on those fields shows it without being told.
     * <p>
     * What the last failure said happened goes first - the actual result, the
     * stacktrace and the screenshots - so the row describes this run and not the
     * last one: a failure whose framework gave no stacktrace would otherwise keep
     * an earlier attempt's, and a screenshot of the last failure would read as a
     * picture of this one. The bug is kept (#50).
     * <p>
     * A run that reported nothing wrong leaves the row untouched, which is why
     * this lives here. Every verdict asks it, including the ones a tester gives
     * by hand, and a {@link #NONE} that wrote itself in would erase what they
     * had typed. One check, in the one place that knows it is empty.
     */
    public void recordOn(final @NotNull TestRunItems item) {
        if (isNothing()) return;

        FailureDetail.clear(item, FailureDetail.WHAT_HAPPENED);
        item.setActualResult(message);
        item.setStacktrace(stacktrace);
    }

    /**
     * UC-EDITOR-PANEL-043, Rule-EDITOR-PANEL-220.
     * <p>
     * What {@link #recordOn} would clear on this row, in the tester's words, and
     * empty when it would clear nothing - which is always, for {@link #NONE}.
     */
    public @NotNull List<String> wouldClear(final @NotNull TestRunItems item) {
        return isNothing() ? List.of() : FailureDetail.filledIn(item, FailureDetail.WHAT_HAPPENED);
    }

    private boolean isNothing() {
        return message.isBlank() && stacktrace.isBlank();
    }
}

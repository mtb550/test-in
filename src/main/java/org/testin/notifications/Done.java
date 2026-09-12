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

package org.testin.notifications;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

/**
 * What a state-changing action says when it worked.
 * <p>
 * CLAUDE.md has stated the rule for a long time: the message is the outcome in
 * the past tense and nothing else, one word wherever one will do, no trailing
 * dot and no noun - the tester pressed the key on the thing in front of them,
 * so naming it back is a word they read every time and needed once.
 * <p>
 * The rule had no owner, so every action spelled its own word: eighteen of them
 * across two methods, with nothing to compare a new one against. A word here is
 * a word the whole plugin can use, and a nineteenth outcome is a constant rather
 * than a decision taken alone at a call site.
 * <p>
 * It also makes the rule enforceable rather than remembered. A present-tense
 * word, a sentence, or a noun cannot be passed to
 * {@code Notifier.softShow(Project, Done)} at all.
 */
@Getter
@AllArgsConstructor
public enum Done {

    COPIED(Bundle.message("done.copied")),
    CUT(Bundle.message("done.cut")),
    PASTED(Bundle.message("done.pasted")),
    MOVED(Bundle.message("done.moved")),

    CREATED(Bundle.message("done.created")),
    BOUND(Bundle.message("done.bound")),
    CLONED(Bundle.message("done.cloned")),
    RENAMED(Bundle.message("done.renamed")),
    REMOVED(Bundle.message("done.removed")),
    UPDATED(Bundle.message("done.updated")),
    SAVED(Bundle.message("done.saved")),

    IMPORTED(Bundle.message("done.imported")),
    EXPORTED(Bundle.message("done.exported")),

    ORDERED(Bundle.message("done.ordered")),
    RE_SORTED(Bundle.message("done.re.sorted")),
    REFRESHED(Bundle.message("done.refreshed")),

    /**
     * Two words because two things happened. Refresh reads the run again from
     * disk, which throws away the one being counted, so a walk in progress ends
     * with it - and "Refreshed" alone left the tester with a stopped clock, a
     * Start button back and no word about why (#218).
     */
    REFRESHED_EXECUTION_STOPPED(Bundle.message("done.refreshed.execution.stopped")),

    UNDONE(Bundle.message("done.undone")),
    REDONE(Bundle.message("done.redone")),
    REVERTED(Bundle.message("done.reverted")),
    KEPT(Bundle.message("done.kept")),

    STOPPED(Bundle.message("done.stopped")),
    CLEARED(Bundle.message("done.cleared"));

    /**
     * The word itself. Past tense, one word where one will do - which is the
     * whole of the rule, and the reason a constant here is worth more than the
     * string it holds.
     */
    private final @NotNull String outcome;

    /**
     * Rule-TREE-PANEL-007, Rule-EDITOR-PANEL-008.
     * <p>
     * One outcome for a gesture that reached several things: <i>Removed</i> for
     * one and <i>Removed 4</i> for four.
     * <p>
     * Here rather than in {@code Notifier}, which delivers messages and does not
     * write them, and as a sentence from the bundle rather than the word with a
     * space and a number glued after it. That glue was the last counted sentence
     * in the plugin still assembled in code: it reads correctly in English,
     * French and Hindi by luck, and a language that puts the count first or
     * inflects the past participle by it had nowhere to say so (#66, finding 92).
     * <p>
     * A {@link String} rather than a {@code Done}, because a count is put on
     * words this enum does not own as well - a test status label after a walk, a
     * badge label when an automated run starts.
     */
    public static @NotNull String counted(final @NotNull String outcome, final int count) {
        return count == 1 ? outcome : Bundle.message("done.counted", outcome, String.valueOf(count));
    }
}

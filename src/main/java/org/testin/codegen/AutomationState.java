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

package org.testin.codegen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.Automated;
import org.testin.model.dto.TestCaseDto;
import org.testin.navigate.CodeNavigation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Whether each test case has automation behind it, for the cards on screen.
 * <p>
 * Derived and never stored. Nothing about this goes into a test case's JSON:
 * the answer is a generated method carrying the case's id, so the code is where
 * it lives and asking again is one pass over one class.
 * <p>
 * A project service because the answer is a project's. Two projects open in one
 * IDE generate into two different source trees, and a static map would let the
 * second one answer for the first - which is the bug {@code TestSourceRoot}
 * exists to record.
 * <p>
 * Reading is asked for a whole test set, never for a card. The cost is
 * resolving the class and walking its methods, and a test set is one class
 * whether fifty cases are asked about or a thousand - so the page was the wrong
 * unit. It cost exactly the same as the set and left the filter with nothing to
 * filter on beyond the page in front of it. Per card it would be one resolve
 * each, and {@code CreateTestMethod} has already measured what per-element PSI
 * work costs: 2,035ms against 590ms at 550 methods.
 */
@Service(Service.Level.PROJECT)
public final class AutomationState {

    /**
     * What the last read found, by case id. A case that is not in here is
     * {@link Automated#UNKNOWN} - the empty value of the type rather than an
     * absent one, so no caller tests for a missing answer before drawing.
     * <p>
     * Nothing invalidates this, because nothing has to: every redraw of a page
     * reads that page again and writes over what it holds. A method generated,
     * renamed or deleted shows at the next redraw, and the design's plan to
     * have six generators each tell this service turned out to be a cost with
     * nothing to buy.
     */
    private final @NotNull Map<UUID, Automated> known = new ConcurrentHashMap<>();

    /**
     * UC-CODEGEN-005, Rule-CODEGEN-025.
     * <p>
     * The cases a generated method exists for, whatever is written in it.
     * <p>
     * A second fact out of the same read, because {@link Automated} deliberately
     * cannot answer it: an empty stub and a case that can have no method at all
     * are both {@link Automated#NONE}, and to a tester reading a card that is
     * right - neither has automation. To Automate Test Case it is the whole
     * question, and merging them is what let the entry stay live over a case
     * whose method was already there: a second press wrote nothing and said
     * <i>Automated 1</i>.
     * <p>
     * Rebuilt for the cases each read covers rather than added to, so a method
     * the tester deletes leaves this at the next redraw the way it leaves
     * {@link #known}.
     */
    private final @NotNull Set<UUID> withAMethod = ConcurrentHashMap.newKeySet();

    /**
     * UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-197.
     * <p>
     * What is known about this case right now, which is what a card draws.
     * Never waits: a card painting is on the EDT, and the answer arrives when
     * {@link #read} has it.
     */
    public @NotNull Automated of(final @NotNull UUID id) {
        return known.getOrDefault(id, Automated.UNKNOWN);
    }

    /**
     * UC-CODEGEN-005, Rule-CODEGEN-025.
     * <p>
     * Whether a generated method exists for this case, which is not the same
     * question as whether it does anything - see {@link #withAMethod}. False for
     * a case no read has covered yet, which is the safe way round: Automate Test
     * Case offers to write one, the generator finds it already there and writes
     * nothing.
     */
    public boolean hasMethod(final @NotNull UUID id) {
        return withAMethod.contains(id);
    }

    /**
     * UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-196.
     * <p>
     * Answers for every case on a page, off the EDT, and calls back when the
     * answers are in so the list can repaint.
     * <p>
     * The test set opens at the speed it opens today: nothing here is waited
     * for, the cards draw {@link Automated#UNKNOWN} until this lands, and
     * UNKNOWN draws what the button has always drawn. A card never claims a
     * case is un-automated because nobody has looked yet.
     * <p>
     * In an IDE with no Java plugin nothing is read and nothing is claimed.
     * That is not the same as answering NONE for everything: a tester on
     * PyCharm has not failed to automate anything, the IDE simply cannot say.
     * <p>
     * Deferred while the IDE is indexing, because resolving a class then either
     * throws or answers wrongly - the same guard {@code CodeNavigator.toCode}
     * makes before it navigates.
     */
    public void read(final @NotNull Project p, final @NotNull List<TestCaseDto> cases, final @NotNull Runnable onAnswered) {
        if (cases.isEmpty()) return;

        // Rule-CODEGEN-082. Code is off - no Java plugin, or no testin.yml naming
        // this test project: nothing is read, and what was read while
        // it was on is forgotten, so no case shows a mark it may not have. Said
        // once; the next read finds nothing to forget.
        if (!CodeOn.isOn(p)) {
            if (known.isEmpty() && withAMethod.isEmpty()) return;

            known.clear();
            withAMethod.clear();
            ApplicationManager.getApplication().invokeLater(onAnswered);
            return;
        }

        if (DumbService.isDumb(p)) {
            DumbService.getInstance(p).runWhenSmart(() -> read(p, cases, onAnswered));
            return;
        }

        // What the caller is drawing, taken as it asks. The answer below is
        // compared against this rather than against what is known when it lands,
        // which is a different question: a second surface asking about a case
        // another one is already reading gets its answer put into `known` by that
        // first read, finds nothing left to change, and is never told - so it goes
        // on drawing "not read yet" over a case whose state arrived while its row
        // was being built (#312, N16, A64's remaining half).
        final @NotNull Map<UUID, Automated> asking = cases.stream()
                .collect(Collectors.toMap(TestCaseDto::getId, tc -> of(tc.getId()), (first, second) -> first));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<UUID, Automated> answers = new LinkedHashMap<>();
            final @NotNull Set<UUID> found = new LinkedHashSet<>();

            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    // A key here is a method that exists; its value is whether
                    // that method does anything. Both halves are kept - the value
                    // becomes the card's state and the key answers Automate Test
                    // Case, which used to be told the value and act on it.
                    final @NotNull Map<UUID, Boolean> methods = CodeNavigation.available().methodsFor(p, cases);

                    for (final TestCaseDto tc : cases) {
                        answers.put(tc.getId(), stateOf(tc, methods));
                        if (methods.containsKey(tc.getId())) found.add(tc.getId());
                    }
                } catch (final Exception ex) {
                    // The cards keep whatever they had, which is UNKNOWN on a
                    // first read - the icon the button has always drawn. A page
                    // that could not be read says nothing rather than reporting
                    // every case as un-automated.
                    Logger.warn("Could not read the automation state of " + cases.size() + " test case(s): " + ex.getMessage());
                }
            });

            if (answers.isEmpty()) return;

            ApplicationManager.getApplication().invokeLater(() -> {
                known.putAll(answers);

                // Rebuilt for what this read covered, so a method that has gone
                // leaves. Before the early return below, which is about telling
                // the caller rather than about what is known.
                for (final TestCaseDto tc : cases) {
                    if (found.contains(tc.getId())) withAMethod.add(tc.getId());
                    else withAMethod.remove(tc.getId());
                }

                // Only when something changed for this caller. Telling it every
                // time is a loop that never settles: it rebuilds its list, which
                // reads again, which answers again - and that still stops here,
                // because the rebuild asks while already holding the answer, so
                // the second read has nothing to report.
                if (answers.equals(asking)) return;

                // What was found, so a sandbox pass can confirm the count on the
                // status bar from the log rather than from looking at it. This
                // method used to log only when the read failed, which made
                // "it worked" and "it never ran" the same silence.
                Logger.debug("Automation state read: " + answers.size() + " case(s), "
                        + answers.values().stream().filter(state -> state == Automated.WRITTEN).count() + " automated");

                onAnswered.run();
            });
        });
    }

    /**
     * UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-210.
     * <p>
     * How many of these have automation behind them.
     * <p>
     * Counted here rather than by the status bar, for the reason
     * {@link #matching} is here: automation is not a field of a test case, it is
     * what this service last read, so a caller that counted for itself would be
     * counting a copy of these answers.
     */
    public int writtenIn(final @NotNull List<TestCaseDto> cases) {
        return (int) cases.stream().filter(tc -> of(tc.getId()) == Automated.WRITTEN).count();
    }

    /**
     * UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-211.
     * <p>
     * How many of these this service can speak for at all - which is none until
     * a read lands, and none in an IDE with no Java plugin.
     * <p>
     * The denominator of the count above, and the reason it is asked separately:
     * "0 of 15 automated" and "nobody has looked yet" are the same arithmetic and
     * opposite statements, and {@link Automated#UNKNOWN} exists precisely so the
     * second is never said as the first.
     */
    public int knownIn(final @NotNull List<TestCaseDto> cases) {
        return (int) cases.stream().filter(tc -> of(tc.getId()) != Automated.UNKNOWN).count();
    }

    /**
     * UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-198.
     * <p>
     * Only the cases in one of these states, and all of them when nothing is
     * chosen - which is how every other filter answers an empty selection.
     * <p>
     * Here rather than in {@code TestCaseFilter} beside the others: automation
     * is not a field of a test case, it is what this service last read, and a
     * filter that had to be handed the answers would be handed a copy of them.
     */
    public @NotNull List<TestCaseDto> matching(final @NotNull List<TestCaseDto> cases, final @NotNull Set<Automated> wanted) {
        if (wanted.isEmpty()) return cases;

        return cases.stream().filter(tc -> wanted.contains(of(tc.getId()))).toList();
    }

    /**
     * UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-195.
     * <p>
     * The three states, from what the code says and what the case carries
     * itself.
     * <p>
     * A method whose body does nothing is not automation. Testin writes every
     * method as a stub - an annotation, a name and a TODO comment - so a case
     * that merely has a method is one somebody created and has not written yet,
     * which is exactly the state a tester is looking for. Counting a stub as
     * automated made every test case with a description look automated.
     * <p>
     * A case with no description names no method at all (Rule-CODEGEN-002), so
     * there is nothing missing there either. Only a case that names a method and
     * has none is MISSING: the automation was written and is gone.
     */
    private static @NotNull Automated stateOf(final @NotNull TestCaseDto tc, final @NotNull Map<UUID, Boolean> methods) {
        final @NotNull Optional<Boolean> method = Optional.ofNullable(methods.get(tc.getId()));

        if (method.isPresent()) return method.orElseThrow() ? Automated.WRITTEN : Automated.NONE;

        return Fqcn.methodNameOf(tc).isEmpty() ? Automated.NONE : Automated.MISSING;
    }
}

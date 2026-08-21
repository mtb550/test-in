package org.testin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;


@Service(Service.Level.PROJECT)
public final class TestCaseCacheService implements Disposable {

    private final @NotNull Set<String> descriptions = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> expectedResults = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> modules = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> steps = ConcurrentHashMap.newKeySet();
    private final @NotNull AtomicBoolean reloadScheduled = new AtomicBoolean();

    private static void addTo(final @NotNull Set<String> target, final @NotNull String value) {
        if (!value.isBlank()) target.add(value.trim());
    }

    /**
     * Swaps a rebuilt set into a live one in as few operations as possible:
     * everything new first, then only what is genuinely gone.
     */
    private static void replace(final @NotNull Set<String> live, final @NotNull Set<String> rebuilt) {
        live.addAll(rebuilt);
        live.retainAll(rebuilt);
    }

    public @NotNull Set<String> getDescription() {
        return Collections.unmodifiableSet(descriptions);
    }

    public @NotNull Set<String> getExpectedResults() {
        return Collections.unmodifiableSet(expectedResults);
    }

    public @NotNull Set<String> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public @NotNull Set<String> getSteps() {
        return Collections.unmodifiableSet(steps);
    }

    public void addDescription(final @NotNull String t) {
        addTo(descriptions, t);
    }

    public void addExpectedResult(final @NotNull String e) {
        addTo(expectedResults, e);
    }

    public void addModule(final @NotNull String e) {
        addTo(modules, e);
    }

    public void addStep(final @NotNull String s) {
        addTo(steps, s);
    }

    public void load(final @NotNull List<TestCaseDto> testCases) {
        cacheAsync(testCases);
    }

    public void addNewItems(final @NotNull List<TestCaseDto> tcs) {
        cacheAsync(tcs);
    }

    /**
     * Rebuilds the cache from the given test cases, dropping anything they no
     * longer mention.
     * <p>
     * Deleting a test case cannot simply remove its values: another test case may
     * use the same module or the same step, and dropping those would empty the
     * completion for cases that still exist. Rebuilding from what remains is the
     * only answer that is right in both directions.
     */
    public void reload(final @NotNull Supplier<@NotNull List<TestCaseDto>> source) {
        // Bursts collapse into one rebuild: deleting fifty cases asks fifty times
        // and the answer is the same each time. The flag is cleared before the
        // source is read, so a removal landing after that still gets a pass of
        // its own. The source is a supplier for the same reason - the rebuild
        // must read what remains when it runs, not when it was asked.
        if (!reloadScheduled.compareAndSet(false, true)) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            reloadScheduled.set(false);

            final @NotNull List<TestCaseDto> testCases = source.get();

            // Built beside the live sets and swapped in, not cleared and refilled.
            // A dialog reading them while a deletion rebuilds would otherwise see
            // the completion briefly empty.
            final @NotNull Set<String> newDescriptions = ConcurrentHashMap.newKeySet();
            final @NotNull Set<String> newExpectedResults = ConcurrentHashMap.newKeySet();
            final @NotNull Set<String> newModules = ConcurrentHashMap.newKeySet();
            final @NotNull Set<String> newSteps = ConcurrentHashMap.newKeySet();

            for (final TestCaseDto tc : testCases) {
                addTo(newDescriptions, tc.getDescription());
                addTo(newExpectedResults, tc.getExpectedResult());
                addTo(newModules, tc.getModule());
                // Jackson can leave steps null on hand-edited JSON despite the field default.
                Optional.of(tc.getSteps()).ifPresent(stepList -> stepList.forEach(s -> addTo(newSteps, s)));
            }

            replace(descriptions, newDescriptions);
            replace(expectedResults, newExpectedResults);
            replace(modules, newModules);
            replace(steps, newSteps);
        });
    }

    private void cacheAsync(final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() -> testCases.forEach(this::cache));
    }

    private void cache(final @NotNull TestCaseDto tc) {
        addDescription(tc.getDescription());
        addExpectedResult(tc.getExpectedResult());
        addModule(tc.getModule());
        tc.getSteps().forEach(this::addStep);
    }

    @Override
    public void dispose() {
        descriptions.clear();
        expectedResults.clear();
        modules.clear();
        steps.clear();
    }
}
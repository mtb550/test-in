package org.testin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.TestCaseDto;

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

    public void addDescription(final @Nullable String t) {
        if (t != null && !t.trim().isEmpty()) descriptions.add(t.trim());
    }

    public void addExpectedResult(final @Nullable String e) {
        if (e != null && !e.trim().isEmpty()) expectedResults.add(e.trim());
    }

    public void addModule(final @Nullable String e) {
        if (e != null && !e.trim().isEmpty()) modules.add(e.trim());
    }

    public void addStep(final @Nullable String s) {
        if (s != null && !s.trim().isEmpty()) steps.add(s.trim());
    }

    public void load(final @Nullable List<TestCaseDto> testCases) {
        cacheAsync(testCases);
    }

    public void addNewItems(final @Nullable List<TestCaseDto> tcs) {
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
    public void reload(final @NotNull Supplier<@Nullable List<TestCaseDto>> source) {
        // Bursts collapse into one rebuild: deleting fifty cases asks fifty times
        // and the answer is the same each time. The flag is cleared before the
        // source is read, so a removal landing after that still gets a pass of
        // its own. The source is a supplier for the same reason - the rebuild
        // must read what remains when it runs, not when it was asked.
        if (!reloadScheduled.compareAndSet(false, true)) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            reloadScheduled.set(false);

            final List<TestCaseDto> testCases = source.get();

            descriptions.clear();
            expectedResults.clear();
            modules.clear();
            steps.clear();

            if (testCases != null) testCases.forEach(this::cache);
        });
    }

    private void cacheAsync(final @Nullable List<TestCaseDto> testCases) {
        if (testCases == null || testCases.isEmpty()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() -> testCases.forEach(this::cache));
    }

    private void cache(final @NotNull TestCaseDto tc) {
        addDescription(tc.getDescription());
        addExpectedResult(tc.getExpectedResult());
        addModule(tc.getModule());
        // Jackson can leave steps null on hand-edited JSON despite the field default.
        Optional.ofNullable(tc.getSteps()).ifPresent(stepList -> stepList.forEach(this::addStep));
    }

    @Override
    public void dispose() {
        descriptions.clear();
        expectedResults.clear();
        modules.clear();
        steps.clear();
    }
}
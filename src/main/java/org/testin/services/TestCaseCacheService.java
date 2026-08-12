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


@Service(Service.Level.PROJECT)
public final class TestCaseCacheService implements Disposable {

    private final @NotNull Set<String> descriptions = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> expectedResults = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> modules = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> steps = ConcurrentHashMap.newKeySet();

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

    private void cacheAsync(final @Nullable List<TestCaseDto> testCases) {
        if (testCases == null || testCases.isEmpty()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                testCases.forEach(tc -> {
                    addDescription(tc.getDescription());
                    addExpectedResult(tc.getExpectedResult());
                    addModule(tc.getModule());
                    // Jackson can leave steps null on hand-edited JSON despite the field default.
                    Optional.ofNullable(tc.getSteps()).ifPresent(stepList -> stepList.forEach(this::addStep));
                }));
    }

    @Override
    public void dispose() {
        descriptions.clear();
        expectedResults.clear();
        modules.clear();
        steps.clear();
    }
}
package org.testin.util.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.testin.pojo.dto.TestCaseDto;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Service(Service.Level.PROJECT)
public final class TestCaseCacheService implements Disposable {

    private final Set<String> descriptions = ConcurrentHashMap.newKeySet();
    private final Set<String> expectedResults = ConcurrentHashMap.newKeySet();
    private final Set<String> modules = ConcurrentHashMap.newKeySet();
    private final Set<String> steps = ConcurrentHashMap.newKeySet();

    public Set<String> getDescription() {
        return Collections.unmodifiableSet(descriptions);
    }

    public Set<String> getExpectedResults() {
        return Collections.unmodifiableSet(expectedResults);
    }

    public Set<String> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public Set<String> getSteps() {
        return Collections.unmodifiableSet(steps);
    }

    public void addDescription(final String t) {
        if (t != null && !t.trim().isEmpty()) descriptions.add(t.trim());
    }

    public void addExpectedResult(final String e) {
        if (e != null && !e.trim().isEmpty()) expectedResults.add(e.trim());
    }

    public void addModule(final String e) {
        if (e != null && !e.trim().isEmpty()) modules.add(e.trim());
    }

    public void addStep(final String s) {
        if (s != null && !s.trim().isEmpty()) steps.add(s.trim());
    }

    public void load(final List<TestCaseDto> testCases) {
        if (testCases == null || testCases.isEmpty()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                testCases.forEach(tc -> {
                    addDescription(tc.getDescription());
                    addExpectedResult(tc.getExpectedResult());
                    addModule(tc.getModule());
                    Optional.of(tc.getSteps()).ifPresent(stepList -> stepList.forEach(this::addStep));
                }));
    }

    public void addNewItems(final List<TestCaseDto> tcs) {
        if (tcs == null || tcs.isEmpty()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                tcs.forEach(tc -> {
                    this.addDescription(tc.getDescription());
                    this.addExpectedResult(tc.getExpectedResult());
                    this.addModule(tc.getModule());
                    tc.getSteps().forEach(this::addStep);
                })
        );
    }

    @Override
    public void dispose() {
        descriptions.clear();
        expectedResults.clear();
        modules.clear();
        steps.clear();
    }
}
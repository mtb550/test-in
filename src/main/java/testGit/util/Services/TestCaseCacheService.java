package testGit.util.Services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.Service.Level;
import com.intellij.openapi.project.Project;
import testGit.pojo.dto.TestCaseDto;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service(Level.PROJECT)
public final class TestCaseCacheService implements Disposable {
    private final Set<String> titles = ConcurrentHashMap.newKeySet();
    private final Set<String> expectedResults = ConcurrentHashMap.newKeySet();
    private final Set<String> steps = ConcurrentHashMap.newKeySet();

    public static TestCaseCacheService getInstance(final Project project) {
        return project.getService(TestCaseCacheService.class);
    }

    public Set<String> getTitles() {
        return Collections.unmodifiableSet(titles);
    }

    public Set<String> getExpectedResults() {
        return Collections.unmodifiableSet(expectedResults);
    }

    public Set<String> getSteps() {
        return Collections.unmodifiableSet(steps);
    }

    public void addTitle(final String t) {
        if (t != null && !t.trim().isEmpty()) titles.add(t.trim());
    }

    public void addExpected(final String e) {
        if (e != null && !e.trim().isEmpty()) expectedResults.add(e.trim());
    }

    public void addStep(final String s) {
        if (s != null && !s.trim().isEmpty()) steps.add(s.trim());
    }

    public void load(final List<TestCaseDto> testCases) {
        if (testCases == null || testCases.isEmpty()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                testCases.forEach(tc -> {
                    addTitle(tc.getTitle());
                    addExpected(tc.getExpected());
                    Optional.ofNullable(tc.getSteps()).ifPresent(stepList -> stepList.forEach(this::addStep));
                }));
    }

    public void addNewItems(final List<TestCaseDto> tcs) {
        if (tcs == null || tcs.isEmpty()) return;
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                tcs.forEach(tc -> {
                    this.addTitle(tc.getTitle());
                    this.addExpected(tc.getExpected());
                    if (tc.getSteps() != null) tc.getSteps().forEach(this::addStep);
                })
        );
    }

    @Override
    public void dispose() {
        titles.clear();
        expectedResults.clear();
        steps.clear();
    }
}
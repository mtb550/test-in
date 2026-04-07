package testGit.repository;

import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

public class PersistenceManager {

    public static void updateTitles(List<TestCaseDto> items, String[] newTitles, Runnable onUpdate) {
        int limit = Math.min(newTitles.length, items.size());
        for (int i = 0; i < limit; i++) {
            if (!newTitles[i].trim().isEmpty()) {
                items.get(i).setTitle(newTitles[i].trim());
            }
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public static void updateExpected(List<TestCaseDto> items, String[] newExpected, Runnable onUpdate) {
        int limit = Math.min(newExpected.length, items.size());
        for (int i = 0; i < limit; i++) {
            items.get(i).setExpected(newExpected[i].trim());
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public static void updatePriority(List<TestCaseDto> items, Priority priority, Runnable onUpdate) {
        for (TestCaseDto tc : items) {
            tc.setPriority(priority);
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public static void updateSteps(List<TestCaseDto> items, List<List<String>> newSteps, Runnable onUpdate) {
        int limit = Math.min(newSteps.size(), items.size());
        for (int i = 0; i < limit; i++) {
            List<String> cleanSteps = newSteps.get(i).stream()
                    .filter(step -> !step.trim().isEmpty())
                    .toList();

            items.get(i).setSteps(cleanSteps);
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public static void updateGroups(List<TestCaseDto> items, List<Groups> newGroups, Runnable onUpdate) {
        for (TestCaseDto tc : items) {
            tc.setGroups(newGroups.isEmpty() ? null : new ArrayList<>(newGroups));
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }
}
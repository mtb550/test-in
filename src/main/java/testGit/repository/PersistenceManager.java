package testGit.repository;

import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;

import java.util.List;

public class PersistenceManager {

    public static void updateTitles(List<TestCaseDto> items, String[] newTitles, Runnable onUpdate) {
        int limit = Math.min(newTitles.length, items.size());
        for (int i = 0; i < limit; i++) {
            if (!newTitles[i].trim().isEmpty()) {
                items.get(i).setTitle(newTitles[i].trim());
            }
        }

        // TODO: مستقبلاً، أضف هنا كود حفظ البيانات في الملفات أو قاعدة البيانات

        if (onUpdate != null) onUpdate.run();
    }

    public static void updatePriority(List<TestCaseDto> items, Priority priority, Runnable onUpdate) {
        for (TestCaseDto tc : items) {
            tc.setPriority(priority);
        }

        // TODO: مستقبلاً، أضف هنا كود حفظ البيانات في الملفات أو قاعدة البيانات

        if (onUpdate != null) onUpdate.run();
    }
}
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
        // TODO: حفظ فعلي في الداتابيز
        if (onUpdate != null) onUpdate.run();
    }

    public static void updateExpected(List<TestCaseDto> items, String[] newExpected, Runnable onUpdate) {
        int limit = Math.min(newExpected.length, items.size());
        for (int i = 0; i < limit; i++) {
            // نقوم بتحديث النتيجة المتوقعة (حتى لو كانت فارغة لأن بعض المستخدمين قد يرغب بمسحها)
            items.get(i).setExpected(newExpected[i].trim());
        }
        // TODO: حفظ فعلي في الداتابيز
        if (onUpdate != null) onUpdate.run();
    }

    public static void updatePriority(List<TestCaseDto> items, Priority priority, Runnable onUpdate) {
        for (TestCaseDto tc : items) {
            tc.setPriority(priority);
        }
        // TODO: حفظ فعلي في الداتابيز
        if (onUpdate != null) onUpdate.run();
    }
}
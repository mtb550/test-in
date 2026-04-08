package testGit.ui.TestCase.edit.bulk;

import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.persist.PersistenceManager;

import java.util.ArrayList;
import java.util.List;

public class StepsBulkEditor extends JsonArraySplitBulkEditor {

    @Override
    protected String getPopupTitle() {
        return "Bulk Edit Steps";
    }

    @Override
    protected String getArrayFieldName() {
        return "steps";
    }

    @Override
    protected List<List<String>> extractOriginalValues(List<TestCaseDto> items) {
        List<List<String>> originalSteps = new ArrayList<>();
        for (TestCaseDto tc : items) {
            if (tc.getSteps() != null) {
                originalSteps.add(new ArrayList<>(tc.getSteps()));
            } else {
                originalSteps.add(new ArrayList<>());
            }
        }
        return originalSteps;
    }

    @Override
    protected void saveValues(List<TestCaseDto> items, List<List<String>> activeValues, Runnable onUpdate) {
        PersistenceManager.getInstance(Config.getProject()).updateSteps(items, activeValues, onUpdate);
    }
}
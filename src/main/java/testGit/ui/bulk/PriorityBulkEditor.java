package testGit.ui.bulk;

import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.repository.PersistenceManager;
import testGit.ui.GenericSelectionPopup;

import java.util.List;

public class PriorityBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        GenericSelectionPopup.show(
                "Select Priority",
                Priority.values(),
                Priority::name,
                p -> p.name().charAt(0),
                Priority::getIcon,
                selectedPriority -> {
                    PersistenceManager.updatePriority(selectedItems, selectedPriority, onUpdate);
                    System.out.println("Priority updated to " + selectedPriority + " for " + selectedItems.size() + " test cases.");
                }
        );
    }
}
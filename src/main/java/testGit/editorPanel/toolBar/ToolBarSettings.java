package testGit.editorPanel.toolBar;

import com.intellij.ide.util.PropertiesComponent;
import lombok.Getter;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.TestCaseAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class ToolBarSettings {
    private static final String KEY_DETAILS = "testGit.selectedDetails.v2";

    ///  to be implemented, use dynamic
    private static final String DEFAULT_DETAILS = String.join(",",
            TestCaseAttributes.ID.name(),
            TestCaseAttributes.MODULE.name(),
            TestCaseAttributes.EXPECTED_RESULT.name(),
            TestCaseAttributes.STEPS.name(),
            TestCaseAttributes.AUTO_REF.name(),
            TestCaseAttributes.BUSI_REF.name(),
            TestCaseAttributes.PRIORITY.name(),
            TestCaseAttributes.GROUPS.name()
    );

    private final Set<Groups> selectedGroups = new HashSet<>();
    private final Set<String> selectedDetails = new HashSet<>();

    public ToolBarSettings() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        String saved = props.getValue(KEY_DETAILS, DEFAULT_DETAILS);
        if (!saved.isEmpty()) {
            this.selectedDetails.addAll(List.of(saved.split(",")));
        }
    }

    public boolean isShowPriorityBadge() {
        return selectedDetails.contains(TestCaseAttributes.PRIORITY.name());
    }

    public boolean isShowGroupsBadge() {
        return selectedDetails.contains(TestCaseAttributes.GROUPS.name());
    }

    public Set<String> getSelectedPriorityFilters() {
        Set<String> validPriorityKeys = Arrays.stream(Priority.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        return selectedDetails.stream()
                .filter(validPriorityKeys::contains)
                .collect(Collectors.toSet());
    }

    public void save() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(KEY_DETAILS, String.join(",", selectedDetails));
    }

    public void resetFilters() {
        selectedGroups.clear();
        selectedDetails.clear();
    }
}
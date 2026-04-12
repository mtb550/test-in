package testGit.editorPanel.toolBar;

import com.intellij.ide.util.PropertiesComponent;
import lombok.Getter;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.TestCaseAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class ToolBarSettings {
    private static final String KEY_DETAILS = "testGit.selectedDetails.v2";
    /// TODO: remove default selected as will depend on preferrences, otherwise show all.
    private static final String DEFAULT_DETAILS = Arrays.stream(TestCaseAttributes.values())
            .filter(TestCaseAttributes::isDefaultSelected)
            .map(Enum::name)
            .collect(Collectors.joining(","));

    private final Set<Groups> selectedGroups = new HashSet<>();
    private final Set<String> selectedDetails = new HashSet<>();

    public ToolBarSettings() {
        final PropertiesComponent props = PropertiesComponent.getInstance();
        final String saved = props.getValue(KEY_DETAILS, DEFAULT_DETAILS);

        Arrays.stream(saved.split(","))
                .filter(s -> !s.isEmpty())
                .forEach(selectedDetails::add);
    }

    public Set<String> getSelectedPriorityFilters() {
        final Set<String> validPriorityKeys = Arrays.stream(Priority.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        return selectedDetails.stream()
                .filter(validPriorityKeys::contains)
                .collect(Collectors.toSet());
    }

    public void save() {
        final PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(KEY_DETAILS, String.join(",", selectedDetails));
    }

    public void resetFilters() {
        selectedGroups.clear();
        selectedDetails.clear();
    }
}
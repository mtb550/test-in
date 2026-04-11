package testGit.editorPanel.toolBar;

import com.intellij.ide.util.PropertiesComponent;
import lombok.Getter;
import lombok.Setter;
import testGit.pojo.Groups;
import testGit.pojo.Priority;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class ToolBarSettings {
    private static final String KEY_SHOW_GROUPS = "testGit.showGroups";
    private static final String KEY_DETAILS = "testGit.selectedDetails";
    private static final String DEFAULT_DETAILS = "ID,Module,Expected Result,Steps,Automation Ref,Business Ref,Priority";

    private final Set<Groups> selectedGroups = new HashSet<>();
    private final Set<String> selectedDetails = new HashSet<>();

    @Setter
    private boolean showGroups;

    public ToolBarSettings() {
        PropertiesComponent props = PropertiesComponent.getInstance();
        this.showGroups = props.getBoolean(KEY_SHOW_GROUPS, true);

        String saved = props.getValue(KEY_DETAILS, DEFAULT_DETAILS);
        if (!saved.isEmpty()) {
            this.selectedDetails.addAll(List.of(saved.split(",")));
        }
    }

    public boolean isShowPriorityBadge() {
        return selectedDetails.contains("Priority");
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
        props.setValue(KEY_SHOW_GROUPS, showGroups, true);
        props.setValue(KEY_DETAILS, String.join(",", selectedDetails));
    }

    public void resetFilters() {
        selectedGroups.clear();
        selectedDetails.clear();
    }
}
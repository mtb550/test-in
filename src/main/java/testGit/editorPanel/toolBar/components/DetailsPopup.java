package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import lombok.Getter;
import testGit.pojo.TestEditorAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DetailsPopup extends AbstractButton implements IToolbarItem {

    private static final String KEY_DETAILS = "testGit.selectedDetails.v3";

    private static final String DEFAULT_DETAILS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isStandardToolBarOption)
            .map(Enum::name)
            .collect(Collectors.joining(","));

    @Getter
    private final Set<String> selectedDetails = new HashSet<>();

    public DetailsPopup(final Runnable onToolBarDetailsSelectedChanged) {
        super("Details", AllIcons.Actions.PreviewDetailsVertically);

        final PropertiesComponent props = PropertiesComponent.getInstance();
        final String saved = props.getValue(KEY_DETAILS, DEFAULT_DETAILS);

        Arrays.stream(saved.split(",")).filter(s -> !s.isEmpty()).forEach(selectedDetails::add);

        addActionListener(e -> showDetailsPopup(onToolBarDetailsSelectedChanged));
    }

    private void saveProps() {
        final PropertiesComponent props = PropertiesComponent.getInstance();
        props.setValue(KEY_DETAILS, String.join(",", selectedDetails));
    }

    private void showDetailsPopup(final Runnable onToolBarDetailsSelectedChanged) {
        CheckBoxList<TestEditorAttributes> detailsList = new CheckBoxList<>();

        Arrays.stream(TestEditorAttributes.values())
                .filter(TestEditorAttributes::isStandardToolBarOption)
                .forEach(attr -> detailsList.addItem(attr, attr.getName(), selectedDetails.contains(attr.name())));

        detailsList.setCheckBoxListListener((index, state) -> {
            TestEditorAttributes item = detailsList.getItemAt(index);
            if (item != null) {
                if (state) {
                    selectedDetails.add(item.name());
                } else {
                    selectedDetails.remove(item.name());
                }
            }

            saveProps();

            if (onToolBarDetailsSelectedChanged != null) {
                onToolBarDetailsSelectedChanged.run();
            }
        });

        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(detailsList, detailsList)
                .setRequestFocus(true)
                .createPopup()
                .showUnderneathOf(this);
    }
}
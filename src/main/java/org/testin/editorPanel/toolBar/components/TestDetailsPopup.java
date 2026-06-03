package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import lombok.Getter;
import org.testin.pojo.TestEditorAttributes;
import org.testin.util.logger.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TestDetailsPopup extends AbstractButton implements IToolbarItem {

    private static final String KEY_DETAILS = "testin.selectedDetails.test.v1";

    private static final String DEFAULT_DETAILS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isStandardToolBarOption)
            .map(Enum::name)
            .collect(Collectors.joining(","));

    @Getter
    private final Set<TestEditorAttributes> selectedDetails = new HashSet<>();

    public TestDetailsPopup(final Runnable onToolBarDetailsSelectedChanged) {
        super("Details", AllIcons.Actions.PreviewDetailsVertically);

        final PropertiesComponent props = PropertiesComponent.getInstance();
        final String saved = props.getValue(KEY_DETAILS, DEFAULT_DETAILS);

        Arrays.stream(saved.split(","))
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        selectedDetails.add(TestEditorAttributes.valueOf(s));
                    } catch (IllegalArgumentException ignored) {
                        Log.error("Invalid test editor attributes: " + s);
                    }
                });

        addActionListener(e -> showDetailsPopup(onToolBarDetailsSelectedChanged));
    }

    private void saveProps() {
        final PropertiesComponent props = PropertiesComponent.getInstance();

        String joinedNames = selectedDetails.stream()
                .map(TestEditorAttributes::name)
                .collect(Collectors.joining(","));

        props.setValue(KEY_DETAILS, joinedNames);
    }

    private void showDetailsPopup(final Runnable onToolBarDetailsSelectedChanged) {
        CheckBoxList<TestEditorAttributes> detailsList = new CheckBoxList<>();

        /// todo: no need to retrieve again, you retrive above.
        Arrays.stream(TestEditorAttributes.values())
                .filter(TestEditorAttributes::isStandardToolBarOption)
                .forEach(attr -> detailsList.addItem(attr, attr.getName(), selectedDetails.contains(attr)));

        detailsList.setCheckBoxListListener((index, state) -> {
            TestEditorAttributes item = detailsList.getItemAt(index);
            if (item != null) {
                if (state)
                    selectedDetails.add(item);
                else
                    selectedDetails.remove(item);
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
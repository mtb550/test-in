package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import lombok.Getter;
import org.testin.pojo.RunEditorAttributes;
import org.testin.util.logger.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RunDetailsPopup extends AbstractButton implements IToolbarItem {

    private static final String KEY_DETAILS = "testin.selectedDetails.run.v3";

    private static final String DEFAULT_DETAILS = Arrays.stream(RunEditorAttributes.values())
            .filter(RunEditorAttributes::isStandardToolBarOption)
            .map(Enum::name)
            .collect(Collectors.joining(","));

    @Getter
    private final Set<RunEditorAttributes> selectedDetails = new HashSet<>();

    public RunDetailsPopup(final Runnable onToolBarDetailsSelectedChanged) {
        super("Details", AllIcons.Actions.PreviewDetailsVertically);

        final PropertiesComponent props = PropertiesComponent.getInstance();
        final String saved = props.getValue(KEY_DETAILS, DEFAULT_DETAILS);

        Arrays.stream(saved.split(","))
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        selectedDetails.add(RunEditorAttributes.valueOf(s));
                    } catch (IllegalArgumentException ignored) {
                        Log.error("Invalid run editor attributes: " + s);
                    }
                });

        addActionListener(e -> showDetailsPopup(onToolBarDetailsSelectedChanged));
    }

    private void saveProps() {
        final PropertiesComponent props = PropertiesComponent.getInstance();

        String joinedNames = selectedDetails.stream()
                .map(RunEditorAttributes::name)
                .collect(Collectors.joining(","));

        props.setValue(KEY_DETAILS, joinedNames);
    }

    private void showDetailsPopup(final Runnable onToolBarDetailsSelectedChanged) {
        CheckBoxList<RunEditorAttributes> detailsList = new CheckBoxList<>();

        Arrays.stream(RunEditorAttributes.values())
                .filter(RunEditorAttributes::isStandardToolBarOption)
                .forEach(attr -> detailsList.addItem(attr, attr.getName(), selectedDetails.contains(attr)));

        detailsList.setCheckBoxListListener((index, state) -> {
            RunEditorAttributes item = detailsList.getItemAt(index);
            if (item != null) {
                if (state) {
                    selectedDetails.add(item);
                } else {
                    selectedDetails.remove(item);
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
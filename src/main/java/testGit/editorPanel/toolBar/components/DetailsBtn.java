package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import testGit.editorPanel.toolBar.ToolBarSettings;
import testGit.pojo.TestCaseAttributes;

import java.util.Arrays;
import java.util.Set;

public class DetailsBtn extends ToolbarActionButton {

    public DetailsBtn(ToolBarSettings settings, Runnable onDetailsChanged) {
        super("Details", AllIcons.Actions.PreviewDetailsVertically);
        addActionListener(e -> showDetailsPopup(settings, onDetailsChanged));
    }

    private void showDetailsPopup(ToolBarSettings settings, Runnable onDetailsChanged) {
        Set<String> selectedDetails = settings.getSelectedDetails();
        CheckBoxList<TestCaseAttributes> detailsList = new CheckBoxList<>();

        Arrays.stream(TestCaseAttributes.values())
                .filter(TestCaseAttributes::isStandardToolBarOption)
                .forEach(attr -> detailsList.addItem(attr, attr.getName(), selectedDetails.contains(attr.name())));

        detailsList.setCheckBoxListListener((index, state) -> {
            TestCaseAttributes item = detailsList.getItemAt(index);
            if (item != null) {
                if (state) {
                    selectedDetails.add(item.name());
                } else {
                    selectedDetails.remove(item.name());
                }
            }
            settings.save();
            if (onDetailsChanged != null) {
                onDetailsChanged.run();
            }
        });

        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(detailsList, detailsList)
                .setRequestFocus(true)
                .createPopup()
                .showUnderneathOf(this);
    }
}
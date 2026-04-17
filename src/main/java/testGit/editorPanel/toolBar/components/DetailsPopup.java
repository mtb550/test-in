package testGit.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import testGit.editorPanel.toolBar.ToolBarSettings;
import testGit.pojo.TestCaseAttributes;

import java.util.Arrays;
import java.util.Set;

public class DetailsPopup extends AbstractButton implements IToolbarItem {

    public DetailsPopup(final ToolBarSettings settings, final Runnable onToolBarDetailsSelectedChanged) {
        super("Details", AllIcons.Actions.PreviewDetailsVertically);
        addActionListener(e -> showDetailsPopup(settings, onToolBarDetailsSelectedChanged));
    }

    private void showDetailsPopup(final ToolBarSettings settings, final Runnable onToolBarDetailsSelectedChanged) {
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
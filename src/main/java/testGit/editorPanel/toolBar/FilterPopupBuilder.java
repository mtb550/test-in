package testGit.editorPanel.toolBar;

import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Groups;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;

public class FilterPopupBuilder {

    public static void showGroupPopup(JButton anchor, Set<Groups> selectedGroups, Runnable onChange) {
        JBList<Groups> groupList = new JBList<>(Groups.values());
        setupListUI(groupList);

        groupList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
                createCheckBoxRenderer(value.name(), selectedGroups.contains(value), list, isSelected));

        groupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = groupList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    Groups group = groupList.getModel().getElementAt(index);
                    if (selectedGroups.contains(group)) selectedGroups.remove(group);
                    else selectedGroups.add(group);

                    groupList.repaint();
                    if (onChange != null) onChange.run();
                }
            }
        });

        showPopup(anchor, groupList);
    }

    public static void showDetailsPopup(JButton anchor, Set<String> selectedDetails, Runnable onChange) {
        JBList<String> detailsList = new JBList<>(
                List.of("ID",
                        "Module",
                        "Expected Result",
                        "Steps",
                        "Automation Ref",
                        "Business Ref",
                        "Priority"
                )
        );
        setupListUI(detailsList);

        detailsList.setCellRenderer((list, value, index, isSelected, cellHasFocus) ->
                createCheckBoxRenderer(value, selectedDetails.contains(value), list, isSelected));

        detailsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = detailsList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    String detail = detailsList.getModel().getElementAt(index);
                    if (selectedDetails.contains(detail)) selectedDetails.remove(detail);
                    else selectedDetails.add(detail);

                    detailsList.repaint();
                    if (onChange != null) onChange.run();
                }
            }
        });

        showPopup(anchor, detailsList);
    }

    // --- Private UI Helpers ---

    private static void setupListUI(JBList<?> list) {
        list.setBackground(JBColor.namedColor("Popup.background", new JBColor(0xffffff, 0x3c3f41)));
    }

    private static JCheckBox createCheckBoxRenderer(String text, boolean isChecked, JList<?> list, boolean isSelected) {
        JCheckBox cb = new JCheckBox(text, isChecked);
        cb.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        cb.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        cb.setBorder(JBUI.Borders.empty(2, 8));
        cb.setOpaque(true);
        return cb;
    }

    private static void showPopup(JButton anchor, JComponent content) {
        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new JBScrollPane(content), null)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .createPopup()
                .showUnderneathOf(anchor);
    }
}
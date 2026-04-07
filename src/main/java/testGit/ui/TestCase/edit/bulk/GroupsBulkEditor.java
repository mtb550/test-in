package testGit.ui.TestCase.edit.bulk;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Config;
import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;
import testGit.repository.PersistenceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupsBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(8, 12));

        Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 1f);
        List<JBCheckBox> checkBoxes = new ArrayList<>();

        Arrays.stream(Groups.values())
                .filter(Groups::isActive)
                .forEach(group -> {
                    JBCheckBox checkBox = new JBCheckBox(group.name());
                    checkBox.setFont(fieldFont);
                    checkBoxes.add(checkBox);
                    panel.add(checkBox);
                });

        JLabel hintLabel = new JLabel("Press 'Enter' to save");
        hintLabel.setFont(JBUI.Fonts.smallFont());
        hintLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
        hintLabel.setBorder(JBUI.Borders.emptyTop(8));
        panel.add(hintLabel);

        JBScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setBorder(JBUI.Borders.empty());

        final JBPopup[] popupWrapper = new JBPopup[1];

        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    List<Groups> selectedGroups = new ArrayList<>();
                    for (JBCheckBox cb : checkBoxes) {
                        if (cb.isSelected()) {
                            selectedGroups.add(Groups.valueOf(cb.getText()));
                        }
                    }

                    PersistenceManager.updateGroups(selectedItems, selectedGroups, onUpdate);
                    System.out.println("Groups updated to " + selectedGroups + " for " + selectedItems.size() + " test cases.");

                    if (popupWrapper[0] != null) popupWrapper[0].closeOk(null);
                    e.consume();

                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (popupWrapper[0] != null) popupWrapper[0].cancel();
                    e.consume();
                }
            }
        };

        panel.addKeyListener(keyAdapter);
        for (JBCheckBox cb : checkBoxes) {
            cb.addKeyListener(keyAdapter);
        }

        popupWrapper[0] = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, checkBoxes.isEmpty() ? panel : checkBoxes.getFirst())
                .setTitle("Select Groups")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .createPopup();

        popupWrapper[0].showCenteredInCurrentWindow(Config.getProject());
    }
}
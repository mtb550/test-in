package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.JBUI;
import testGit.editorPanel.testCaseEditor.TestCaseCard;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TestRunCard extends JPanel {
    private final JBCheckBox selectionBox = new JBCheckBox();
    private final TestCaseCard detailCard = new TestCaseCard();

    public TestRunCard(int index, TestCase tc) {
        setLayout(new BorderLayout());
        setOpaque(false);

        // CRITICAL FIX: Give the card a height so it doesn't collapse to 0px
        setPreferredSize(new Dimension(-1, JBUI.scale(140)));

        JPanel checkWrapper = new JPanel(new GridBagLayout());
        checkWrapper.setOpaque(false);
        checkWrapper.setBorder(JBUI.Borders.empty(0, 10));
        checkWrapper.add(selectionBox);

        detailCard.updateData(index, tc);

        add(checkWrapper, BorderLayout.WEST);
        add(detailCard, BorderLayout.CENTER);

        // Border to visually separate cards
        setBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.EditorTabs.borderColor(), 0, 0, 1, 0));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }
        });
    }

    private void showMenu(MouseEvent e) {
        TestRunContextMenu menu = new TestRunContextMenu();
        ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu("TestRunContext", menu);
        popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());
    }
}
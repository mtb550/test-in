package testGit.viewPanel.history;

import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.DB;
import testGit.pojo.dto.TestCaseHistoryDto;

import javax.swing.*;
import java.awt.*;

public class HistoryTab {
    public static void load(@NotNull JBPanel<?> historyTab) {
        historyTab.removeAll();
        DefaultListModel<String> model = new DefaultListModel<>();

        for (TestCaseHistoryDto h : DB.loadTestCaseHistory()) {
            model.addElement(h.getTimestamp() + " - " + h.getChangeSummary());
        }

        JBList<String> list = new JBList<>(model);
        historyTab.add(new JBScrollPane(list), BorderLayout.CENTER);
    }
}
package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import testGit.pojo.TestCase;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

@Getter
public class TestRunOpeningUI implements Disposable {
    private final List<TestCase> initialTestCases;
    private final DefaultTreeModel testCasesTreeModel;

    public TestRunOpeningUI(VirtualFileImpl vf) {
        this.initialTestCases = vf.getTestCases();
        this.testCasesTreeModel = vf.getTestCasesTreeModel();
    }

    public JComponent createEditorPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());

        JPanel cardList = new JPanel();
        cardList.setLayout(new BoxLayout(cardList, BoxLayout.Y_AXIS));
        cardList.setBackground(UIUtil.getTreeBackground());

        for (int i = 0; i < initialTestCases.size(); i++) {
            TestCase tc = initialTestCases.get(i);

            TestRunCard card = new TestRunCard(i, tc);

            card.updateData(i, tc);

            cardList.add(card);
        }

        cardList.add(Box.createVerticalGlue());

        JBScrollPane scrollPane = new JBScrollPane(cardList);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    @Override
    public void dispose() {
    }
}
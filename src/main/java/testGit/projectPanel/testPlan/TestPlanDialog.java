package testGit.projectPanel.testPlan;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.treeStructure.SimpleTree;
import testGit.pojo.TestPlan;

import javax.swing.*;
import java.awt.*;

public class TestPlanDialog extends DialogWrapper {
    private final TestPlan plan;
    private final SimpleTree parent;
    private final TestPlanUIComponents uiComponents;

    public TestPlanDialog(TestPlan plan, SimpleTree parent) {
        super(true);
        this.plan = plan;
        this.parent = parent;
        this.uiComponents = new TestPlanUIComponents(plan, parent);

        init();
        setTitle("Add Test Cases to Plan");
    }

    @Override
    protected JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(550, 600));

        panel.add(uiComponents.createTopPanel(), BorderLayout.NORTH);
        panel.add(uiComponents.createTreePanel(), BorderLayout.CENTER);
        panel.add(uiComponents.createConfigPanel(), BorderLayout.SOUTH);

        return panel;
    }

    @Override
    protected void doOKAction() {
        uiComponents.handleOkAction();
        super.doOKAction();
    }
}
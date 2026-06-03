package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Log;

public class GenerateTestCase extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public GenerateTestCase(final JBList<TestCaseDto> list) {
        super("Generate Test", "", AllIcons.Actions.IntentionBulb);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.GenerateTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCaseDto tc = list.getSelectedValue();

        /// TODO: to be implemented
        Log.info(tc.getDescription());
    }
}

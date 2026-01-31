package com.example.actions.editorPanel;

import com.example.pojo.TestCase;
import com.example.viewPanel.TestCaseToolWindow;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ViewDetailsAction extends AnAction {
    TestCase tc;

    public ViewDetailsAction(TestCase tc) {
        super("🔍 View Details");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCaseToolWindow.show(tc);
    }
}

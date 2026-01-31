package com.example.editorPanel;

import com.example.actions.editorPanel.*;
import com.example.pojo.TestCase;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ContextMenu extends DefaultActionGroup {

    public ContextMenu(final String featurePath, final @NotNull VirtualFile file, final JBList<TestCase> list, DefaultListModel<TestCase> model, TestCase tc) {
        super("Test Case Actions", false);

        add(new CopyTestCaseAction(tc));
        add(new RunTestCaseAction(tc));
        add(new ViewDetailsAction(tc));

        addSeparator();

        add(new DeleteTestCaseAction(tc, model));
        add(new AddTestCaseAction(featurePath, file, list, model, tc));

    }

}

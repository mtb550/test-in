package com.example.actions.editorPanel;

import com.example.pojo.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class AddTestCaseAction extends AnAction {
    TestCase tc;
    JList<TestCase> list;
    String featurePath;
    VirtualFile file;
    DefaultListModel<TestCase> model;

    public AddTestCaseAction(String featurePath, @NotNull VirtualFile file, JList<TestCase> list, DefaultListModel<TestCase> model, TestCase tc) {
        super("➕ Add Test Case");
        this.tc = tc;
        this.list = list;
        this.file = file;
        this.featurePath = featurePath;
        this.model = model;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String title = JOptionPane.showInputDialog(list,
                "Enter title for new test case:",
                "New Test Case",
                JOptionPane.PLAIN_MESSAGE);

        if (title != null && !title.trim().isEmpty()) {
            TestCase newCase = new TestCase();
            newCase.setTitle(title.trim());
            //newCase.setSteps("Step 1: ...");
            //newCase.setExpectedResult("Expected result...");
            newCase.setPriority("LOW");
            //newCase.setAutomationRef("");
            newCase.setSort(model.getSize() + 1);
            newCase.setId(UUID.randomUUID().toString());

            /// here create json file
            ObjectMapper mapper = new ObjectMapper();
            try {
                // issue in path
                System.out.println(file.getCanonicalPath());
                System.out.println(file.getCanonicalFile());
                System.out.println(featurePath);
                mapper.registerModule(new JavaTimeModule());
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                File targetFile = new File(featurePath, newCase.getId() + ".json");
                mapper.writeValue(targetFile, newCase);
                // Refresh the IDE so the file appears in the project tree
                com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.out.println("JSON file created successfully!");

            model.addElement(newCase);
            list.ensureIndexIsVisible(model.getSize() - 1);
            list.setSelectedIndex(model.getSize() - 1);
        }
    }
}

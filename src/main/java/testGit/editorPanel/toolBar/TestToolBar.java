package testGit.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import testGit.editorPanel.toolBar.components.CreateTestCaseBtn;

import javax.swing.*;
import java.util.List;

public class TestToolBar extends ActionToolbarPanel {

    public TestToolBar(final Disposable pDisposable, final IToolBar callbacks) {
        super(pDisposable, callbacks);
    }

    @Override
    public List<JComponent> getCustomComponents() {
        CreateTestCaseBtn addTestBtn = new CreateTestCaseBtn(() -> {
            // TODO: "Create Test Case" call action here
            System.out.println("Add Test Case clicked");
        });

        return List.of(addTestBtn);
    }
}
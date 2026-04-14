package testGit.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import testGit.editorPanel.toolBar.components.ExecuteTestCaseBtn;
import testGit.editorPanel.toolBar.components.GenerateReportBtn;

import javax.swing.*;
import java.util.List;

public class RunToolBar extends ActionToolbarPanel {

    public RunToolBar(final Disposable pDisposable, final IToolBar callbacks) {
        super(pDisposable,callbacks);
    }

    @Override
    public List<JComponent> getCustomComponents() {
        ExecuteTestCaseBtn executeBtn = new ExecuteTestCaseBtn(() -> {
            // TODO: Gatling/TestNG trigger
            System.out.println("Executing tests...");
        });

        GenerateReportBtn exportBtn = new GenerateReportBtn(() -> {
            // TODO: JSON/Report export logic
            System.out.println("Exporting results...");
        });

        return List.of(executeBtn, exportBtn);
    }
}
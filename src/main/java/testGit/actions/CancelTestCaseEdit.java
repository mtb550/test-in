//package testGit.actions;
//
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.project.DumbAwareAction;
//import org.jetbrains.annotations.NotNull;
//import testGit.util.KeyboardSet;
//import testGit.viewPanel.details.TestCaseDetailsPanel;
//import testGit.viewPanel.ToolWindowFactoryImpl;
//
//import javax.swing.*;
//
//public class CancelTestCaseEdit extends DumbAwareAction {
//
//    public CancelTestCaseEdit(final JComponent targetComponent) {
//        super("Cancel Edit");
//        this.registerCustomShortcutSet(KeyboardSet.Escape.getShortcut(), targetComponent);
//    }
//
//    @Override
//    public void actionPerformed(@NotNull final AnActionEvent e) {
//        TestCaseDetailsPanel detailsPanel = ToolWindowFactoryImpl.getDetailsInstance();
//        if (detailsPanel != null && detailsPanel.isEditing()) {
//            detailsPanel.toggleEditMode(false);
//        }
//    }
//}
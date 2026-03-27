package testGit.editorPanel.listeners;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;

public class TestFocusListener {

    private final JBList<TestCaseDto> list;
    private final UnifiedVirtualFile vf;
    private final ToolWindow viewPanel = ViewPanel.getToolWindow();

    public TestFocusListener(final JBList<TestCaseDto> list, final UnifiedVirtualFile vf) {
        this.list = list;
        this.vf = vf;
    }

    public void register(final Disposable parentDisposable) {
        Config.getProject().getMessageBus().connect(parentDisposable).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        execute(event);
                    }
                }
        );
    }

    private void execute(final @NotNull FileEditorManagerEvent event) {
        if (vf.equals(event.getNewFile())) {
            TestCaseDto selected = list.getSelectedValue();

            // 🌟 دمج الشروط في سطر واحد لتبسيط القراءة
            if (viewPanel != null && viewPanel.isVisible() && selected != null) {
                SwingUtilities.invokeLater(() -> ViewPanel.show(selected));
            }
        }
    }
}
//package testGit.editorPanel.listeners;
//
//import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
//import com.intellij.openapi.fileEditor.FileEditorManagerListener;
//import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.ui.components.JBList;
//import com.intellij.util.messages.MessageBusConnection;
//import org.jetbrains.annotations.NotNull;
//import testGit.editorPanel.UnifiedVirtualFile;
//import testGit.pojo.Config;
//import testGit.pojo.dto.TestCaseDto;
//import testGit.viewPanel.ViewPanel;
//
//import javax.swing.*;
//
//public class TestFocusListener {
//
//    private final JBList<TestCaseDto> list;
//    private final UnifiedVirtualFile vf;
//    private final ToolWindow viewPanel = ViewPanel.getToolWindow();
//    private MessageBusConnection connection; // 🌟 حفظ الاتصال للتحكم به
//
//    public TestFocusListener(final JBList<TestCaseDto> list, final UnifiedVirtualFile vf) {
//        this.list = list;
//        this.vf = vf;
//    }
//
//    public void register() {
//        // 🌟 فتح الاتصال بدون الاعتماد على الـ Disposer المعقد
//        connection = Config.getProject().getMessageBus().connect();
//        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
//                new FileEditorManagerListener() {
//                    @Override
//                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
//                        execute(event);
//                    }
//                }
//        );
//    }
//
//    public void disconnect() {
//        // 🌟 إغلاق الاتصال بنظافة عند تدمير التبويب
//        if (connection != null) {
//            connection.disconnect();
//            connection = null;
//        }
//    }
//
//    private void execute(final @NotNull FileEditorManagerEvent event) {
//        if (vf.equals(event.getNewFile())) {
//            TestCaseDto selected = list.getSelectedValue();
//
//            if (viewPanel != null && viewPanel.isVisible() && selected != null) {
//                SwingUtilities.invokeLater(() -> ViewPanel.show(selected));
//            }
//        }
//    }
//}
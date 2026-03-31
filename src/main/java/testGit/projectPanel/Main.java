package testGit.projectPanel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import testGit.settings.StartupActivity;
import testGit.settings.service.ProjectPanelService;

public class Main implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        System.out.println("ToolWindowFactory.createToolWindowContent()");


        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                StartupActivity.execute(project);
            }

            ProjectPanel projectPanel = new ProjectPanel(project);

            toolWindow.setTitleActions(ProjectPanelActions.create(projectPanel));

            ProjectPanelService.getInstance(project).setPanel(projectPanel);

            Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);
            Disposer.register(content, projectPanel);
            toolWindow.getContentManager().addContent(content);
        });
    }
}
//package testGit.projectPanel;
//
//import com.intellij.ide.util.PropertiesComponent;
//import com.intellij.ide.util.treeView.TreeState;
//import com.intellij.openapi.application.ApplicationManager;
//import com.intellij.openapi.fileEditor.FileEditorManager;
//import com.intellij.openapi.fileEditor.FileEditorManagerListener;
//import com.intellij.openapi.project.*;
//import com.intellij.openapi.util.Disposer;
//import com.intellij.openapi.util.JDOMUtil;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.openapi.wm.ToolWindow;
//import com.intellij.openapi.wm.ToolWindowFactory;
//import com.intellij.ui.content.Content;
//import com.intellij.ui.content.ContentFactory;
//import org.jdom.Element;
//import org.jetbrains.annotations.NotNull;
//import testGit.settings.StartupActivity;
//import testGit.settings.service.ProjectPanelService;
//import testGit.editorPanel.UnifiedVirtualFile;
//import testGit.editorPanel.testCaseEditor.TestEditor;
//import testGit.pojo.DirectoryMapper;
//import testGit.pojo.dto.dirs.TestSetDirectoryDto;
//import testGit.pojo.dto.dirs.TestRunDirectoryDto;
//
//import javax.swing.*;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.StringJoiner;
//
//public class Main implements ToolWindowFactory, DumbAware {
//
//    private static final String KEY_OPEN_EDITORS = "testGit.savedOpenEditors";
//    private static final String KEY_TREE_STATE = "testGit.savedTreeState";
//
//    @Override
//    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
//        // استخدام invokeLater لضمان استقرار الواجهة قبل البدء
//        ApplicationManager.getApplication().invokeLater(() -> {
//            if (project.isDisposed()) return;
//
//            StartupActivity.execute(project);
//            ProjectPanel projectPanel = new ProjectPanel(project);
//
//            toolWindow.setTitleActions(TitleActions.create(projectPanel));
//            ProjectPanelService.getInstance(project).setPanel(projectPanel);
//
//            Content content = ContentFactory.getInstance().createContent(projectPanel.getPanel(), null, false);
//            // ربط دورة حياة الـ Panel بالمشروع لمنع Memory Leaks
//            Disposer.register(project, projectPanel);
//            toolWindow.getContentManager().addContent(content);
//
//            setupPersistenceListeners(project, projectPanel);
//
//            // استرجاع الحالة فقط عندما يكون IntelliJ مستعداً (Smart Mode)
//            DumbService.getInstance(project).runWhenSmart(() -> restoreStateSafely(project, projectPanel));
//        }, project.getDisposed());
//    }
//
//    private void setupPersistenceListeners(Project project, ProjectPanel projectPanel) {
//        var connection = project.getMessageBus().connect(projectPanel);
//
//        // 🌟 حفظ مستمر للتبويبات
//        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
//            @Override
//            public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
//                if (file instanceof UnifiedVirtualFile) saveOpenEditors(project, source);
//            }
//            @Override
//            public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
//                if (file instanceof UnifiedVirtualFile) saveOpenEditors(project, source);
//            }
//        });
//
//        // 🌟 حفظ الشجرة عند إغلاق المشروع
//        connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
//            @Override
//            public void projectClosing(@NotNull Project p) {
//                if (p.equals(project)) saveTreeState(project, projectPanel);
//            }
//        });
//    }
//
//    private void saveOpenEditors(Project project, FileEditorManager editorManager) {
//        StringJoiner sj = new StringJoiner(";");
//        boolean hasAny = false;
//
//        for (VirtualFile vf : editorManager.getOpenFiles()) {
//            if (vf instanceof UnifiedVirtualFile uvf) {
//                if (uvf.getTestSet() != null) {
//                    sj.add("TS|" + uvf.getTestSet().getPath());
//                    hasAny = true;
//                } else if (uvf.getTestRunPkg() != null) {
//                    sj.add("TR|" + uvf.getTestRunPkg().getPath());
//                    hasAny = true;
//                }
//            }
//        }
//
//        if (hasAny) {
//            PropertiesComponent.getInstance(project).setValue(KEY_OPEN_EDITORS, sj.toString());
//        } else {
//            PropertiesComponent.getInstance(project).unsetValue(KEY_OPEN_EDITORS);
//        }
//    }
//
//    private void saveTreeState(Project project, ProjectPanel panel) {
//        try {
//            JTree tree = panel.getProjectTree().getMainTree();
//            if (tree != null) {
//                Element el = new Element("TreeState");
//                TreeState.createOn(tree).writeExternal(el);
//                PropertiesComponent.getInstance(project).setValue(KEY_TREE_STATE, JDOMUtil.writeElement(el));
//            }
//        } catch (Exception ignored) {}
//    }
//
//    private void restoreStateSafely(Project project, ProjectPanel panel) {
//        PropertiesComponent props = PropertiesComponent.getInstance(project);
//
//        // 1. استرجاع الشجرة (عملية خفيفة)
//        String treeXml = props.getValue(KEY_TREE_STATE);
//        if (treeXml != null && panel.getProjectTree().getMainTree() != null) {
//            try {
//                TreeState.createFrom(JDOMUtil.load(treeXml)).applyTo(panel.getProjectTree().getMainTree());
//            } catch (Exception ignored) {}
//        }
//
//        // 2. استرجاع التبويبات (عملية ثقيلة - تنفذ في الخلفية)
//        String editors = props.getValue(KEY_OPEN_EDITORS);
//        if (editors == null || editors.isEmpty()) return;
//
//        // تنفيذ فحص الملفات في Background Thread لمنع تعليق الواجهة (Faster Opening)
//        ApplicationManager.getApplication().executeOnPooledThread(() -> {
//            String[] paths = editors.split(";");
//            for (String entry : paths) {
//                if (!entry.contains("|")) continue;
//
//                String[] parts = entry.split("\\|");
//                Path path = Paths.get(parts[1]);
//
//                if (Files.exists(path) && Files.isDirectory(path)) {
//                    // العودة للـ EDT فقط لفتح التبويب
//                    ApplicationManager.getApplication().invokeLater(() -> {
//                        if ("TS".equals(parts[0])) {
//                            var node = DirectoryMapper.testSetNode(path);
//                            if (node instanceof TestSetDirectoryDto ts) TestEditor.open(ts);
//                        }
//                    }, project.getDisposed());
//                }
//            }
//        });
//    }
//}
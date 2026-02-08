package testGit.projectPanel;

import com.intellij.openapi.ui.ComboBox;
import testGit.pojo.Config;
import testGit.pojo.Directory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

public class ComboBoxProjectSelector {
    public static ComboBox<Directory> comboBox;
    private final DefaultComboBoxModel<Directory> model;
    public ProjectPanel projectPanel;

    public ComboBoxProjectSelector(final ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.model = new DefaultComboBoxModel<>();
        comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);

        // ✅ 1. الإعدادات الثابتة توضع هنا لمرة واحدة فقط
        setupRenderer();
        setupSelectionListener();

        // 2. تحميل البيانات
        loadProjects();
    }

    public static Directory getSelectedProject() {
        return (Directory) comboBox.getSelectedItem();
    }

    private void setupRenderer() {
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Directory dir) {
                    setText(dir.getName());
                } else if (model.getSize() == 0) {
                    setText("No projects found");
                }
                return this;
            }
        });
    }

    private void setupSelectionListener() {
        // إضافة الـ Listener مرة واحدة فقط في الـ Constructor
        comboBox.addActionListener(e -> {
            Directory selected = (Directory) comboBox.getSelectedItem();
            if (selected != null) {
                System.out.println("Selection changed to: " + selected.getName());
                projectPanel.filterByProject(selected);
            }
        });
    }

    public void loadProjects() {
        System.out.println("ComboBoxProjectSelector.loadProjects()");

        // تنظيف البيانات دون حذف الـ Listener أو الـ Renderer
        model.removeAllElements();

        File root = Config.getRootFolderFile();
        File[] dirs = root.listFiles(File::isDirectory);

        // إضافة خيار "All Projects" دائماً في البداية
        Directory allProjects = new Directory().setName("All Projects");
        model.addElement(allProjects);

        if (dirs != null) {
            Arrays.stream(dirs)
                    .filter(dir -> !dir.getName().equals(".git") && dir.getName().contains("_"))
                    .map(DirectoryMapper::map)
                    .filter(p -> p != null && p.getActive() == 1)
                    .forEach(model::addElement);
        }

        // ✅ تأكد من تفعيل الكومبو بوكس دائماً طالما يوجد عنصر واحد على الأقل
        comboBox.setEnabled(model.getSize() > 0);
        comboBox.setSelectedIndex(0);
    }

    public void reloadProjects() {
        // التحديث الآن يعني إعادة تحميل البيانات فقط، والـ Listener سيتكفل بالباقي
        loadProjects();
    }

    public JComboBox<Directory> selected() {
        return comboBox;
    }

    public void addAndSelectProject(Directory project) {
        System.out.println("ComboBoxProjectSelector.addAndSelectProject()");

        // 1. التأكد من تفعيل الكومبو بوكس
        if (!comboBox.isEnabled()) {
            comboBox.setEnabled(true);
        }

        // 2. إضافة المشروع للموديل
        model.addElement(project);

        // 3. اختياره في الواجهة (سيقوم الـ Listener تلقائياً باستدعاء filterByProject)
        comboBox.setSelectedItem(project);
    }
}
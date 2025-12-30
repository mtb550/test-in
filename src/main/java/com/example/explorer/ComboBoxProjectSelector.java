package com.example.explorer;

import com.example.pojo.Directory;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;

public class ComboBoxProjectSelector {
    private static ComboBox<Directory> comboBox = null;
    public ExplorerPanel panel;

    public ComboBoxProjectSelector(ExplorerPanel panel) {
        this.panel = panel;
        DefaultComboBoxModel<Directory> model = new DefaultComboBoxModel<>();
        comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);

        File testCasesFolder = new File("/home/mtb/IdeaProjects/untitled/TestGit");
        File[] dirs = testCasesFolder.listFiles(File::isDirectory);

        // التغيير هنا: استخدم الواجهة Directory في مصفوفة البداية والنهاية
        Directory[] projects = (dirs == null) ? new Directory[0] : Arrays.stream(dirs)
                .map(dir -> {
                    String fullName = dir.getName();
                    String[] parts = fullName.split("_", 3);

                    // استخدام كائن Project وتعبئته
                    Directory p = new Directory();
                    p.setFile(dir);
                    p.setName(fullName); // اسم افتراضي
                    p.setActive(1);      // نشط افتراضياً

                    try {
                        if (parts.length >= 2) {
                            p.setId(Integer.parseInt(parts[0]));
                            p.setName(parts[1]);
                            if (parts.length > 2) {
                                p.setActive(Integer.parseInt(parts[2]));
                            }
                        }
                    } catch (NumberFormatException e) {
                        // في حال فشل الأرقام، سيبقى الاسم هو fullName والنشاط 1
                    }

                    return p; // تحويل صريح للواجهة لضمان توافق الـ Stream
                })
                .filter(p -> {
                    // بما أن الفلتر يحتاج الوصول لـ active، نحتاج تحويله لـ Project مؤقتاً
                    return p.getActive() == 1;
                })
                .toArray(Directory[]::new); // التخزين في مصفوفة Directory

        // Sort alphabetically
        //java.util.Arrays.sort(projects);

        // Now you have: ["ibram", "nafath", ...]
        System.out.println("Found projects: " + Arrays.toString(projects));

        if (projects.length > 0) {
            for (Directory project : projects) {
                model.addElement(project);
            }
            comboBox.addActionListener(this::onSelection);
            comboBox.setSelectedIndex(0);
        } else {
            //comboBox.addItem("No projects found");
            comboBox.setEnabled(false);
        }

        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((Directory) value).getName()); // هنا نخبره أن يعرض حقل الـ Name فقط
                return this;
            }
        });
    }

    public static Directory getSelectedProject() {
        return (Directory) comboBox.getSelectedItem();
    }

    private void onSelection(ActionEvent e) {
        Directory selected = (Directory) comboBox.getSelectedItem();
        panel.filterByProject(selected);

    }

    public JComponent getComponent() {
        return comboBox;
    }


}

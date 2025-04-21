package com.example.explorer;

import com.example.explorer.actions.TestPlanInfoPopup;
import com.example.pojo.TestPlan;
import com.example.util.sql;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class TestPlanContextMenu {

    public static JPopupMenu create(JTree tree, TestPlan plan, DefaultMutableTreeNode node) {
        JPopupMenu menu = new JPopupMenu();

        // 🧪 New Test Run
        JMenuItem newRun = new JMenuItem("🧪 New Test Run");
        newRun.addActionListener(e -> TestPlanPopup.showFolderInfo(plan, tree));
        menu.add(newRun);

        // ✏ Rename
        JMenuItem rename = new JMenuItem("✏ Rename");
        rename.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog("Rename Test Plan:", plan.getName());
            if (newName != null && !newName.isBlank()) {
                new sql().execute("UPDATE nafath_tp_tree SET name = ? WHERE id = ?", newName, plan.getId());
                plan.setName(newName);
                ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
            }
        });
        menu.add(rename);

        // 🗑 Delete
        JMenuItem delete = new JMenuItem("🗑 Delete");
        delete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(tree, "Delete this plan and its test cases?");
            if (confirm == JOptionPane.YES_OPTION) {
                new sql().execute("DELETE FROM nafath_tp_testcases WHERE plan_id = ?", plan.getId());
                new sql().execute("DELETE FROM nafath_tp_tree WHERE id = ?", plan.getId());
                ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(node);
            }
        });
        menu.add(delete);

        // ℹ More Info
        JMenuItem moreInfo = new JMenuItem("ℹ More Info");
        moreInfo.addActionListener(e -> TestPlanInfoPopup.show(plan));
        menu.add(moreInfo);




        // add above
        return menu;
    }

}

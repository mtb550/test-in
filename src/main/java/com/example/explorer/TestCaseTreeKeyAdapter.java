package com.example.explorer;

import com.example.editor.TestCaseEditor;
import com.example.pojo.Tree;
import com.example.util.*;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.MessageType;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestCaseTreeKeyAdapter {
    private static final List<DefaultMutableTreeNode> clipboard = new ArrayList<>();
    private static final Set<Integer> cutNodeIds = new HashSet<>();
    private static boolean isCut = false;

    public static boolean isCutNode(int id) {
        return cutNodeIds.contains(id);
    }

    public static void register(JTree tree, Project project) {
        InputMap im = tree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = tree.getActionMap();

        System.out.println("[KEY ADAPTER] Registering keyboard actions for TestCase Tree");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openTestCase");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copyNode");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), "cutNode");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "pasteNode");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteNode");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undoAction");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redoAction");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK), "renameNode");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearClipboard");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), "addNewNode");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "showContextMenu"); //TODO:: not working

        am.put("openTestCase", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath path = tree.getSelectionPath();
                if (path == null) return;

                Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                if (userObject instanceof Tree treeItem) {
                    if (treeItem.getType() == NodeType.FEATURE.getCode()) {
                        TestCaseEditor.open(treeItem.getId());
                        System.out.printf("[ENTER] Opened test case: %s%n", treeItem.getName());
                    }
                }
            }
        });

        am.put("copyNode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath[] paths = tree.getSelectionPaths();
                if (paths == null) return;

                clipboard.clear();
                cutNodeIds.clear();

                for (TreePath path : paths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    clipboard.add(node);
                }

                isCut = false;
                tree.repaint();
                ActionHistory.showStatus(project);
                System.out.printf("[COPY] Copied %d node(s)%n", clipboard.size());
                StatusUtil.showBalloon(project, "Copied " + clipboard.size() + " node(s)", MessageType.INFO);

            }

        });

        am.put("cutNode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath[] paths = tree.getSelectionPaths();
                if (paths == null) return;

                clipboard.clear();
                cutNodeIds.clear();

                for (TreePath path : paths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    clipboard.add(node);
                    cutNodeIds.add(((Tree) node.getUserObject()).getId());
                }

                isCut = true;
                tree.repaint();
                ActionHistory.showStatus(project);
                System.out.printf("[CUT] Marked %d node(s) for move%n", clipboard.size());
            }
        });

        am.put("pasteNode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clipboard.isEmpty()) return;

                TreePath targetPath = tree.getSelectionPath();
                if (targetPath == null) return;

                if (isCut && !UiDialogs.confirmMove(tree, clipboard.size())) return;

                DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) targetPath.getLastPathComponent();
                Object userObject = targetNode.getUserObject();
                if (!(userObject instanceof Tree targetTreeItem)) return;

                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

                for (DefaultMutableTreeNode node : clipboard) {
                    Tree sourceTreeItem = (Tree) node.getUserObject();

                    if (!isCut) {
                        sql db = new sql();
                        int newNodeId = db.get("INSERT INTO tree (name, type, link, created_by) VALUES (?, ?, ?, ?) RETURNING id;", sourceTreeItem.getName() + " (Copy)", sourceTreeItem.getType(), targetTreeItem.getId(), System.getProperty("user.name")).asType(Integer.class);

                        Tree treeItem = new Tree()
                                .setName(sourceTreeItem.getName() + " (Copy)")
                                .setType(sourceTreeItem.getType())
                                .setId(newNodeId);

                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(treeItem);
                        model.insertNodeInto(newNode, targetNode, targetNode.getChildCount());

                        ActionHistory.register(
                                // Undo: remove the newly inserted node from model and DB
                                () -> {
                                    model.removeNodeFromParent(newNode);
                                    new sql().execute("DELETE FROM tree WHERE id = ?", treeItem.getId());
                                    tree.repaint();
                                },
                                // Redo: re-insert the same node again into model and DB
                                () -> {
                                    model.insertNodeInto(newNode, targetNode, targetNode.getChildCount());
                                    new sql().execute("INSERT INTO tree (id, name, type, link, created_by, created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))",
                                            treeItem.getId(), treeItem.getName(), treeItem.getType(), targetTreeItem.getId(), System.getProperty("user.name"));
                                    tree.repaint();
                                }
                        );


                    } else {
                        DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) node.getParent();
                        model.removeNodeFromParent(node);
                        model.insertNodeInto(node, targetNode, targetNode.getChildCount());

                        new sql().execute("UPDATE tree SET link = ? WHERE id = ?", targetTreeItem.getId(), sourceTreeItem.getId());

                        ActionHistory.register(
                                // Undo: move the node back to old parent
                                () -> {
                                    model.removeNodeFromParent(node);
                                    model.insertNodeInto(node, oldParent, oldParent.getChildCount());
                                    new sql().execute("UPDATE tree SET link = ? WHERE id = ?",
                                            ((Tree) oldParent.getUserObject()).getId(), sourceTreeItem.getId());
                                    tree.repaint();
                                },
                                // Redo: reapply move to target node
                                () -> {
                                    model.removeNodeFromParent(node);
                                    model.insertNodeInto(node, targetNode, targetNode.getChildCount());
                                    new sql().execute("UPDATE tree SET link = ? WHERE id = ?",
                                            targetTreeItem.getId(), sourceTreeItem.getId());
                                    tree.repaint();
                                }
                        );

                    }
                }

                cutNodeIds.clear();
                clipboard.clear();
                isCut = false;
                tree.repaint();
                ActionHistory.showStatus(project);
                System.out.println("[PASTE] Completed");
                StatusUtil.showBalloon(project, "Paste complete", MessageType.INFO);

            }
        });

        am.put("deleteNode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath[] paths = tree.getSelectionPaths();
                if (paths == null) return;

                if (!UiDialogs.confirmDelete(tree, paths.length)) return;

                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

                for (TreePath path : paths) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    Object userObject = node.getUserObject();
                    if (userObject instanceof Tree treeItem) {
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        int index = parent.getIndex(node);

                        model.removeNodeFromParent(node);
                        new sql().execute("DELETE FROM tree WHERE id = ?", treeItem.getId());

                        ActionHistory.register(
                                // Undo: restore the deleted node
                                () -> {
                                    model.insertNodeInto(node, parent, index);
                                    new sql().execute("INSERT INTO tree (id, name, type, link, created_by, created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))",
                                            treeItem.getId(), treeItem.getName(), treeItem.getType(), treeItem.getLink(), System.getProperty("user.name"));
                                    tree.repaint();
                                },
                                // Redo: remove it again
                                () -> {
                                    model.removeNodeFromParent(node);
                                    new sql().execute("DELETE FROM tree WHERE id = ?", treeItem.getId());
                                    tree.repaint();
                                }
                        );

                    }
                }

                ActionHistory.showStatus(project);
                StatusUtil.showBalloon(project, "Deleted node(s)", MessageType.WARNING);

            }
        });

        am.put("undoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActionHistory.undo();
                ActionHistory.showStatus(project);
                StatusUtil.showBalloon(project, "Undo executed", MessageType.INFO);

            }

        });

        am.put("redoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActionHistory.redo();
                ActionHistory.showStatus(project);
                StatusUtil.showBalloon(project, "Undo executed", MessageType.INFO);

            }
        });

        am.put("renameNode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath path = tree.getSelectionPath();
                if (path == null) return;

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();

                if (!(userObject instanceof Tree treeItem)) return;

                String newName = JOptionPane.showInputDialog(tree, "Rename node:", treeItem.getName());
                if (newName == null || newName.isBlank() || newName.equals(treeItem.getName())) return;

                String oldName = treeItem.getName();
                treeItem.setName(newName);
                ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

                new sql().execute("UPDATE tree SET name = ? WHERE id = ?", newName, treeItem.getId());

                ActionHistory.register(
                        // Undo: revert to old name
                        () -> {
                            treeItem.setName(oldName);
                            ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
                            new sql().execute("UPDATE tree SET name = ? WHERE id = ?", oldName, treeItem.getId());
                            tree.repaint();
                        },
                        // Redo: reapply new name
                        () -> {
                            treeItem.setName(newName);
                            ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
                            new sql().execute("UPDATE tree SET name = ? WHERE id = ?", newName, treeItem.getId());
                            tree.repaint();
                        }
                );


                ActionHistory.showStatus(project);
                StatusUtil.showBalloon(project, "Renamed to: " + newName, MessageType.INFO);
                System.out.printf("[RENAME] Node id=%d renamed '%s' -> '%s'%n", treeItem.getId(), oldName, newName);
            }
        });

        am.put("clearClipboard", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clipboard.isEmpty()) {
                    System.out.println("[ESCAPE] Nothing to clear.");
                    return;
                }

                clipboard.clear();
                cutNodeIds.clear();
                isCut = false;
                tree.repaint();

                System.out.println("[ESCAPE] Clipboard cleared.");
                StatusUtil.showBalloon(project, "Clipboard cleared", MessageType.INFO);
                ActionHistory.showStatus(project);
            }
        });

        am.put("addNewNode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath path = tree.getSelectionPath();
                DefaultMutableTreeNode selected = path != null ? (DefaultMutableTreeNode) path.getLastPathComponent() : null;
                Tree selectedInfo = selected != null ? (Tree) selected.getUserObject() : null;

                JBPanel<?> panel = new JBPanel<>(new BorderLayout(5, 5));
                JBTextField nameField = new JBTextField();
                String[] types = {"Project", "Suite", "Feature"};
                JComboBox<String> typeCombo = new ComboBox<>(types);

                panel.add(new JBLabel("Type:"), BorderLayout.NORTH);
                panel.add(typeCombo, BorderLayout.CENTER);
                panel.add(new JBLabel("Name:"), BorderLayout.SOUTH);
                panel.add(nameField, BorderLayout.PAGE_END);

                JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                JDialog dialog = optionPane.createDialog(tree, "Add New Node");

                // Ensure typeCombo is focused when dialog appears
                dialog.addWindowFocusListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowGainedFocus(java.awt.event.WindowEvent e) {
                        typeCombo.requestFocusInWindow();
                    }
                });

                dialog.setVisible(true);

                // Get the selected option after dialog closes
                Object resultObj = optionPane.getValue();
                int result = (resultObj instanceof Integer) ? (Integer) resultObj : JOptionPane.CANCEL_OPTION;

                if (result != JOptionPane.OK_OPTION) return;

                String name = nameField.getText().trim();
                if (name.isEmpty()) return;

                String type = (String) typeCombo.getSelectedItem();
                int newType = switch (type) {
                    case "Project" -> 0;
                    case "Suite" -> 1;
                    case "Feature" -> 2;
                    default -> -1;
                };

                int parentId = 0;

                if (newType == NodeType.SUITE.getCode() && (selectedInfo == null || selectedInfo.getType() != NodeType.PROJECT.getCode())) {
                    JOptionPane.showMessageDialog(tree, "Please select a project to add a suite.");
                    return;
                }

                if (newType == NodeType.FEATURE.getCode() && (selectedInfo == null || selectedInfo.getType() != NodeType.SUITE.getCode())) {
                    JOptionPane.showMessageDialog(tree, "Please select a suite to add a feature.");
                    return;
                }

                if (newType == NodeType.SUITE.getCode() || newType == NodeType.FEATURE.getCode()) {
                    parentId = selectedInfo.getId();
                }

                sql db = new sql();
                db.execute("INSERT INTO tree (name, type, link, created_by) VALUES (?, ?, ?, ?)",
                        name, newType, parentId, System.getProperty("user.name"));

                Tree newInfo = db.get("SELECT TOP 1 * FROM tree WHERE name = ? ORDER BY created_at DESC", name).as(Tree.class);
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newInfo);

                if (newType == 0) {
                    ((DefaultTreeModel) tree.getModel()).insertNodeInto(newNode, (DefaultMutableTreeNode) tree.getModel().getRoot(), tree.getModel().getChildCount(tree.getModel().getRoot()));
                } else {
                    ((DefaultTreeModel) tree.getModel()).insertNodeInto(newNode, selected, selected.getChildCount());
                    tree.expandPath(new TreePath(selected.getPath()));
                }

                TreePath newPath = new TreePath(newNode.getPath());
                tree.setSelectionPath(newPath);
                tree.scrollPathToVisible(newPath);
            }
        });

        am.put("showContextMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath path = tree.getSelectionPath();
                if (path == null) return;

                Rectangle bounds = tree.getPathBounds(path);
                if (bounds == null) return;

                // Use bounds to show popup at center of selected row
                int x = bounds.x + bounds.width / 2;
                int y = bounds.y + bounds.height / 2;

                ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction("TestTreeContextMenuGroup");
                ActionPopupMenu popup = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group);
                popup.getComponent().show(tree, x, y);
            }
        });


        // add here
    }
}

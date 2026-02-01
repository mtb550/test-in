package testGit.editorPanel;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.TestCase;
import testGit.viewPanel.TestCaseToolWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.List;

public class EditorPanel extends UserDataHolderBase implements FileEditor {
    private final JBPanel<?> panel;
    private final VirtualFile file;


    public EditorPanel(@NotNull List<TestCase> testCases, @NotNull String featurePath, @NotNull VirtualFile file) {
        System.out.println("TableEditor.TableEditor(). file: " + file.getPath());
        System.out.println("TableEditor.TableEditor(). file: " + file.getCanonicalPath());
        System.out.println("TableEditor.TableEditor(). file: " + file);

        testCases.forEach(testCase -> System.out.println(testCase.getTitle()));
        panel = new JBPanel<>(new BorderLayout());
        this.file = file;

        // 1) Build a sorted, mutable list model
        DefaultListModel<TestCase> model = new DefaultListModel<>();
        testCases.stream()
                .sorted(Comparator.comparingInt(TestCase::getSort))
                .forEach(model::addElement);

        // 2) JList with multi​-select and drag​-to​-reorder
        JBList<TestCase> list = new JBList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new ListItemReorderHandler(model));

        // === Auto-update TestCaseDetails tool window when selection changes ===
        list.addListSelectionListener(e -> {
            // Prevent double-triggering during list model adjustments
            if (!e.getValueIsAdjusting()) {
                int idx = list.getSelectedIndex();
                if (idx >= 0) {
                    // Show the selected test case in the details tool window
                    TestCaseToolWindow.show(model.getElementAt(idx));
                }
            }
        });

        // 3) Render each TestCase as a TestCaseCard
        list.setCellRenderer((JList<? extends TestCase> l,
                              TestCase tc,
                              int index,
                              boolean isSelected,
                              boolean cellHasFocus) -> {
            TestCaseCard card = new TestCaseCard(index, tc);
            if (isSelected) {
                card.setBorder(BorderFactory.createLineBorder(JBColor.CYAN, 2)); /// change all from Color to JBColor
            }
            return card;
        });

        // 4) Global mouse listener for context​-menu & double​-click
        list.addMouseListener(new MouseAdapter() {
            private void maybeShowPopup(MouseEvent e) {
                //System.out.println("TableEditor.maybeShowPopup()");

                if (!e.isPopupTrigger()) return;
                int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;
                if (!list.isSelectedIndex(idx)) {
                    list.setSelectedIndex(idx);
                }
                TestCase tc = model.getElementAt(idx);

                //JPopupMenu menu = EditorContext.create(featurePath, file, list, model, tc);
                //menu.show(list, e.getX(), e.getY());

                ContextMenu contextMenu = new ContextMenu(featurePath, file, list, model, tc);
                ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(
                        ActionPlaces.TOOLWINDOW_POPUP,
                        contextMenu
                );
                popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());

            }

            @Override
            public void mousePressed(MouseEvent e) {
                //System.out.println("TableEditor.mousePressed()");

                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                //System.out.println("TableEditor.mouseReleased()");
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                //System.out.println("TableEditor.mouseClicked()");

                if (e.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(e)) {
                    int idx = list.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        TestCaseToolWindow.show(model.getElementAt(idx));
                    }
                }
            }
        });

        // 5) Wrap in scroll pane
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        // Ctrl + M → Open AddTestCase tool window
        KeyStroke ctrlM = KeyStroke.getKeyStroke("control M");
        InputMap inputMap = list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = list.getActionMap();

        inputMap.put(ctrlM, "addTestCase");
        actionMap.put("addTestCase", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("TableEditor.actionPerformed()");
                // === OLD CODE - Simple dialog ===
                // String title = JOptionPane.showInputDialog(list, "Enter title for new test case:", "New Test Case", JOptionPane.PLAIN_MESSAGE);
                // if (title != null && !title.trim().isEmpty()) {
                //     TestCase newCase = new TestCase();
                //     newCase.setTitle(title.trim());
                //     newCase.setSteps("Step 1: ...");
                //     newCase.setExpectedResult("Expected result...");
                //     newCase.setPriority("medium");
                //     newCase.setAutomationRef("");
                //     newCase.setSort(model.getSize() + 1);
                //
                //     model.addElement(newCase);
                //     list.ensureIndexIsVisible(model.getSize() - 1);
                //     list.setSelectedIndex(model.getSize() - 1);
                // }

                // === NEW CODE - Show AddTestCase tool window ===
                TestCaseToolWindow.addTestCase(newCase -> {
                    // Set the sort order
                    newCase.setSort(model.getSize() + 1);

                    // Add to the list model
                    model.addElement(newCase);

                    // Scroll to and select the new test case
                    list.ensureIndexIsVisible(model.getSize() - 1);
                    list.setSelectedIndex(model.getSize() - 1);

                    // Show in details panel
                    TestCaseToolWindow.show(newCase);
                });
            }
        });

    }

    @Override
    public @NotNull JComponent getComponent() {
        //System.out.println("TableEditor.getComponent()");

        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        //System.out.println("TableEditor.getPreferredFocusedComponent()");
        return panel;
    }

    @Override
    public @NotNull String getName() {
        System.out.println("TableEditor.getName()");
        return "Test Case Cards";
    }

    @Override
    public void dispose() {
        System.out.println("TableEditor.dispose()");

    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        System.out.println("TableEditor.addPropertyChangeListener()");
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        System.out.println("TableEditor.removePropertyChangeListener()");
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        System.out.println("TableEditor.setState()");
    }

    @Override
    public boolean isModified() {
        System.out.println("TableEditor.isModified()");
        return false;
    }

    @Override
    public boolean isValid() {
        // infinite sout
        //System.out.println("TableEditor.isValid()");
        return true;
    }

    @Override
    public @NotNull VirtualFile getFile() {
        // infinite sout
        //System.out.println("TableEditor.getFile()");
        return file;
    }
}
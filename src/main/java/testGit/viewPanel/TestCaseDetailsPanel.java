package testGit.viewPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import testGit.actions.CancelTestCaseEdit;
import testGit.actions.EditTestCase;
import testGit.actions.SaveTestCase;
import testGit.actions.NavigateToCode;
import testGit.actions.RunTestCase;
import testGit.pojo.DB;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestCaseHistoryDto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TestCaseDetailsPanel {
    @Getter
    private final JBPanel<?> panel;
    private final JBTabbedPane tabbedPane;
    @Getter
    private final JBPanel<?> detailsTab;
    @Getter
    private final JBPanel<?> historyTab;
    @Getter
    private final JBPanel<?> bugTab;

    private JBTextField titleField, expectedArea, stepsArea;
    private JBTextField autoRefField, busiRefField;

    private ComboBox<Priority> priorityComboBox;
    private JBList<Groups> groupsList;

    private JButton saveButton;
    @Getter
    private TestCaseDto currentTestCaseDto;

    @Getter
    private boolean isEditing = false;

    public TestCaseDetailsPanel() {
        panel = new JBPanel<>(new BorderLayout());
        tabbedPane = new JBTabbedPane();

        detailsTab = new JBPanel<>(new GridBagLayout());
        historyTab = new JBPanel<>(new BorderLayout());
        bugTab = new JBPanel<>(new BorderLayout());

        tabbedPane.addTab("Details", detailsTab);
        tabbedPane.addTab("History", historyTab);
        tabbedPane.addTab("Open Bugs", bugTab);

        panel.add(tabbedPane, BorderLayout.CENTER);

        new EditTestCase(this, detailsTab);
        new CancelTestCaseEdit(detailsTab);
        new SaveTestCase(this, detailsTab);
    }

    public void update(TestCaseDto testCaseDto) {
        this.currentTestCaseDto = testCaseDto;

        if (testCaseDto == null) {
            detailsTab.removeAll();
            JBLabel placeholder = new JBLabel("Select a test case to view details");
            placeholder.setForeground(JBColor.GRAY);
            detailsTab.add(placeholder, new GridBagConstraints());
            detailsTab.revalidate();
            detailsTab.repaint();
            return;
        }

        toggleEditMode(false);
        loadHistoryAndBugs();
    }

    public void toggleEditMode(boolean editable) {
        if (currentTestCaseDto == null) return;

        isEditing = editable;
        detailsTab.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(8, 16);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        if (editable) {
            setupEditMode(gbc, row);
        } else {
            setupViewMode(gbc, row);
        }

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void setupEditMode(GridBagConstraints gbc, int row) {
        titleField = new JBTextField(currentTestCaseDto.getTitle());
        expectedArea = new JBTextField(currentTestCaseDto.getExpected());
        stepsArea = new JBTextField(currentTestCaseDto.getSteps());
        autoRefField = new JBTextField(currentTestCaseDto.getAutoRef());
        busiRefField = new JBTextField(currentTestCaseDto.getBusiRef());

        priorityComboBox = new ComboBox<>(Priority.values());
        priorityComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Priority) setText(((Priority) value).getDescription());
                return this;
            }
        });

        if (currentTestCaseDto.getPriority() != null) {
            priorityComboBox.setSelectedItem(currentTestCaseDto.getPriority());
        } else {
            priorityComboBox.setSelectedIndex(-1);
        }

        groupsList = new JBList<>(Groups.values());
        groupsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        groupsList.setVisibleRowCount(Groups.values().length);

        if (currentTestCaseDto.getGroups() != null) {
            List<Integer> selectedIndices = new ArrayList<>();
            Groups[] allGroups = Groups.values();
            for (Object g : currentTestCaseDto.getGroups()) {
                for (int i = 0; i < allGroups.length; i++) {
                    if (allGroups[i].name().equals(g.toString())) {
                        selectedIndices.add(i);
                        break;
                    }
                }
            }
            groupsList.setSelectedIndices(selectedIndices.stream().mapToInt(i -> i).toArray());
        }

        addRow("Title:", titleField, detailsTab, gbc, row++);
        addRow("Expected Result:", expectedArea, detailsTab, gbc, row++);
        addRow("Steps:", stepsArea, detailsTab, gbc, row++);
        addRow("Priority:", priorityComboBox, detailsTab, gbc, row++);
        addRow("Automation Ref:", autoRefField, detailsTab, gbc, row++);
        addRow("Business Ref:", busiRefField, detailsTab, gbc, row++);

        JBScrollPane groupsScrollPane = new JBScrollPane(groupsList);
        addRow("Groups:", groupsScrollPane, detailsTab, gbc, row++);

        saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> saveChanges());
        gbc.gridx = 1;
        gbc.gridy = row + 10;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        detailsTab.add(saveButton, gbc);

        SwingUtilities.invokeLater(() -> titleField.requestFocusInWindow());
    }

    private void setupViewMode(GridBagConstraints gbc, int row) {
        JBLabel idBadge = new JBLabel(currentTestCaseDto.getId()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new JBColor(Gray._230, Gray._80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); // حواف دائرية
                g2.dispose();
                super.paintComponent(g);
            }
        };
        idBadge.setFont(JBUI.Fonts.smallFont());
        idBadge.setForeground(new JBColor(Gray._130, Gray._170));
        idBadge.setBorder(JBUI.Borders.empty(3, 10)); // هوامش داخلية للون الفضي
        idBadge.setOpaque(false);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2; // يمتد على العمودين معاً
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(8, 16, 2, 16); // مسافة سفلية صغيرة ليفصل عن العنوان
        detailsTab.add(idBadge, gbc);

        gbc.gridwidth = 1;
        gbc.insets = JBUI.insets(6, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow("Title:", createValueLabel(currentTestCaseDto.getTitle()), detailsTab, gbc, row++);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setOpaque(false);

        Icon navIcon = AllIcons.Nodes.Class;
        JLabel navLabel = new JLabel(navIcon);
        navLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        navLabel.setToolTipText("Navigate to Code");

        int navTargetWidth = (int) (navIcon.getIconWidth() * 1.5f);
        int navTargetHeight = (int) (navIcon.getIconHeight() * 1.5f);
        navLabel.setPreferredSize(new Dimension(navTargetWidth, navTargetHeight));
        navLabel.setHorizontalAlignment(SwingConstants.CENTER);
        navLabel.setVerticalAlignment(SwingConstants.CENTER);

        navLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                navLabel.setIcon(IconUtil.scale(navIcon, navLabel, 1.5f));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                navLabel.setIcon(navIcon);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                NavigateToCode.execute(currentTestCaseDto);
            }
        });

        Icon runIcon = AllIcons.RunConfigurations.TestState.Run;
        JLabel runLabel = new JLabel(runIcon);
        runLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runLabel.setToolTipText("Run Test");

        int runTargetWidth = (int) (runIcon.getIconWidth() * 1.5f);
        int runTargetHeight = (int) (runIcon.getIconHeight() * 1.5f);
        runLabel.setPreferredSize(new Dimension(runTargetWidth, runTargetHeight));
        runLabel.setHorizontalAlignment(SwingConstants.CENTER);
        runLabel.setVerticalAlignment(SwingConstants.CENTER);

        runLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                runLabel.setIcon(IconUtil.scale(runIcon, runLabel, 1.5f));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                runLabel.setIcon(runIcon);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                RunTestCase.execute(currentTestCaseDto);
            }
        });

        actionsPanel.add(navLabel);
        actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(4)));
        actionsPanel.add(runLabel);

        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        detailsTab.add(actionsPanel, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow("Expected Result:", createValueLabel(currentTestCaseDto.getExpected()), detailsTab, gbc, row++);
        addRow("Steps:", createValueLabel(currentTestCaseDto.getSteps()), detailsTab, gbc, row++);
        addRow("Priority:", createValueLabel(currentTestCaseDto.getPriority() != null ? currentTestCaseDto.getPriority().getDescription() : "-"), detailsTab, gbc, row++);
        addRow("Automation Ref:", createValueLabel(currentTestCaseDto.getAutoRef()), detailsTab, gbc, row++);
        addRow("Business Ref:", createValueLabel(currentTestCaseDto.getBusiRef()), detailsTab, gbc, row++);
        addRow("Groups:", createValueLabel(currentTestCaseDto.getGroups() != null && !currentTestCaseDto.getGroups().isEmpty() ? currentTestCaseDto.getGroups().toString() : "-"), detailsTab, gbc, row++);
        addRow("UID:", createValueLabel(String.valueOf(currentTestCaseDto.getUid())), detailsTab, gbc, row++);
        addRow("Module:", createValueLabel(currentTestCaseDto.getModule()), detailsTab, gbc, row++);
        addRow("Created By:", createValueLabel(currentTestCaseDto.getCreateBy()), detailsTab, gbc, row++);
        addRow("Updated By:", createValueLabel(currentTestCaseDto.getUpdateBy()), detailsTab, gbc, row++);
        addRow("Created At:", createValueLabel(currentTestCaseDto.getCreateAt() != null ? currentTestCaseDto.getCreateAt().toString() : "-"), detailsTab, gbc, row++);
        addRow("Updated At:", createValueLabel(currentTestCaseDto.getUpdateAt() != null ? currentTestCaseDto.getUpdateAt().toString() : "-"), detailsTab, gbc, row++);
    }

    public void saveChanges() {
        if (currentTestCaseDto == null) return;

        currentTestCaseDto.setTitle(titleField.getText().trim());
        currentTestCaseDto.setExpected(expectedArea.getText().trim());
        currentTestCaseDto.setSteps(stepsArea.getText().trim());

        if (priorityComboBox.getSelectedItem() != null) {
            currentTestCaseDto.setPriority((Priority) priorityComboBox.getSelectedItem());
        }

        List<Groups> selectedGroups = groupsList.getSelectedValuesList();
        currentTestCaseDto.setGroups(selectedGroups);

        toggleEditMode(false);
        System.out.println("Saved: " + currentTestCaseDto.getId());
    }

    private void loadHistoryAndBugs() {
        historyTab.removeAll();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (TestCaseHistoryDto h : DB.loadTestCaseHistory()) {
            model.addElement(h.getTimestamp() + " - " + h.getChangeSummary());
        }
        JBList<String> list = new JBList<>(model);
        historyTab.add(new JBScrollPane(list), BorderLayout.CENTER);

        bugTab.removeAll();
        bugTab.add(new JBLabel("No bugs found for this test case."), BorderLayout.NORTH);
    }

    private JBLabel createValueLabel(String text) {
        JBLabel label = new JBLabel((text == null || text.isEmpty()) ? "-" : text);
        label.setFont(JBUI.Fonts.label(14));
        return label;
    }

    private void addRow(String label, JComponent input, JBPanel<?> panel, GridBagConstraints gbc, int row) {
        JBLabel keyLabel = new JBLabel(label);
        keyLabel.setFont(JBUI.Fonts.label(14).asBold());

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(keyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(input, gbc);
    }
}
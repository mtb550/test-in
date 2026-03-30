package testGit.viewPanel;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import testGit.actions.NavigateToCode;
import testGit.actions.RunTestCase;
import testGit.editorPanel.Shared;
import testGit.pojo.Config;
import testGit.pojo.DB;
import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestCaseHistoryDto;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestCaseDetailsPanel {

    @Getter
    private final JBPanel<?> panel, detailsTab, historyTab, bugTab;
    private final JBTabbedPane tabbedPane;

    @Getter
    private TestCaseDto currentTestCaseDto;
    @Getter
    private Path currentPath;

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

    }

    private static String format(final String text) {
        if (StringUtil.isEmptyOrSpaces(text)) return "";
        String s = text.trim();
        return StringUtil.capitalize(s) + ".";
    }

    public void update(final TestCaseDto testCaseDto, final Path path) {
        this.currentTestCaseDto = testCaseDto;
        this.currentPath = path;

        detailsTab.removeAll();

        if (testCaseDto == null) {
            JBLabel placeholder = new JBLabel("Select a test case to view details");
            placeholder.setForeground(JBColor.GRAY);
            detailsTab.add(placeholder, new GridBagConstraints());

        } else {

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = JBUI.insets(8, 16);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            int row = 0;

            setupViewMode(gbc, row);
            loadHistoryAndBugs();
        }

        detailsTab.revalidate();
        detailsTab.repaint();

    }

    private void setupViewMode(final GridBagConstraints gbc, int row) {

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insetsTop(12);
        detailsTab.add(navigationBar(currentPath), gbc);

        JBLabel idBadge = new JBLabel(currentTestCaseDto.getId()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new JBColor(Gray._230, Gray._80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        idBadge.setFont(JBUI.Fonts.smallFont());
        idBadge.setForeground(new JBColor(Gray._130, Gray._170));
        idBadge.setBorder(JBUI.Borders.empty(3, 10));
        idBadge.setOpaque(false);

        JBLabel copyIcon = new JBLabel(AllIcons.Actions.Copy);
        copyIcon.setToolTipText("Copy ID");
        copyIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        copyIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(currentTestCaseDto.getId()));

                copyIcon.setIcon(AllIcons.General.InspectionsOK);
                Timer timer = new Timer(1500, evt -> copyIcon.setIcon(AllIcons.Actions.Copy));
                timer.setRepeats(false);
                timer.start();
            }
        });

        JPanel idContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
        idContainer.setOpaque(false);
        idContainer.add(idBadge);
        idContainer.add(copyIcon);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(8, 16, 2, 16);

        detailsTab.add(idContainer, gbc);

        JBLabel mainTitleLabel = new JBLabel(format(currentTestCaseDto.getTitle()));
        mainTitleLabel.setFont(JBFont.label().deriveFont(Font.BOLD, UIUtil.getLabelFont().getSize() + 10.0f));
        mainTitleLabel.setForeground(UIUtil.getLabelForeground());

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 16, 4, 16);
        detailsTab.add(mainTitleLabel, gbc);

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
        runLabel.setToolTipText("Run Test Case");

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

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 16, 8, 16);
        detailsTab.add(actionsPanel, gbc);

        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        badgesPanel.setOpaque(false);

        if (currentTestCaseDto.getPriority() != null) {
            badgesPanel.add(Shared.createPriorityBadge(currentTestCaseDto));
        }

        if (currentTestCaseDto.getGroups() != null && !currentTestCaseDto.getGroups().isEmpty()) {
            for (Object g : currentTestCaseDto.getGroups()) {
                try {
                    Groups groupEnum = (g instanceof Groups) ? (Groups) g : Groups.valueOf(g.toString());
                    badgesPanel.add(Shared.createGroupBadge(groupEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 16, 16, 16);
        detailsTab.add(badgesPanel, gbc);

        gbc.gridwidth = 1;

        addRow("Expected Result:", createValueLabel(currentTestCaseDto.getExpected()), detailsTab, gbc, row++);
        addRow("Steps:", createStepsLabel(currentTestCaseDto.getSteps()), detailsTab, gbc, row++);
        addRow("Automation Ref:", createValueLabel(currentTestCaseDto.getAutoRef()), detailsTab, gbc, row++);
        addRow("Business Ref:", createValueLabel(currentTestCaseDto.getBusiRef()), detailsTab, gbc, row++);
        addRow("Module:", createValueLabel(currentTestCaseDto.getModule()), detailsTab, gbc, row++);
        addRow("Created By:", createValueLabel(currentTestCaseDto.getCreateBy()), detailsTab, gbc, row++);
        addRow("Updated By:", createValueLabel(currentTestCaseDto.getUpdateBy()), detailsTab, gbc, row++);
        addRow("Created At:", createValueLabel(currentTestCaseDto.getCreateAt() != null ? currentTestCaseDto.getCreateAt().toString() : "-"), detailsTab, gbc, row++);
        addRow("Updated At:", createValueLabel(currentTestCaseDto.getUpdateAt() != null ? currentTestCaseDto.getUpdateAt().toString() : "-"), detailsTab, gbc, row++);
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

    private JBLabel createStepsLabel(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            JBLabel label = new JBLabel("-");
            label.setFont(JBUI.Fonts.label(14));
            return label;
        }

        StringBuilder html = new StringBuilder("<html><body style='padding: 0; margin: 0;'>");
        for (int i = 0; i < steps.size(); i++) {
            String formatted = format(steps.get(i));
            String escaped = formatted.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

            html.append("<p style='margin-top: 3px; margin-bottom: 5px;'>")
                    .append("<b>").append((i + 1)).append("-</b> ").append(escaped)
                    .append("</p>");
        }
        html.append("</body></html>");

        JBLabel label = new JBLabel(html.toString());
        label.setFont(JBUI.Fonts.label(14));
        return label;
    }

    private JBLabel createValueLabel(String text) {
        if (text == null || text.trim().isEmpty()) {
            JBLabel label = new JBLabel("-");
            label.setFont(JBUI.Fonts.label(14));
            return label;
        }

        String formatted = format(text);
        String escaped = formatted.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        StringBuilder html = new StringBuilder("<html><body style='padding: 0; margin: 0;'>");
        String[] lines = escaped.split("\n");
        for (String line : lines) {
            html.append("<p style='margin-top: 3px; margin-bottom: 5px;'>").append(line).append("</p>");
        }
        html.append("</body></html>");

        JBLabel label = new JBLabel(html.toString());
        label.setFont(JBUI.Fonts.label(14));
        return label;
    }

    private void addRow(String label, JComponent input, JBPanel<?> panel, GridBagConstraints gbc, int row) {
        JBLabel keyLabel = new JBLabel(label);
        keyLabel.setFont(JBUI.Fonts.label(14));
        keyLabel.setForeground(Gray._120);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(8, 16, 6, 10);
        panel.add(keyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(6, 0, 6, 16);
        panel.add(input, gbc);
    }

    // =========================================================================

    private JComponent navigationBar(Path currentPath) {

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pathPanel.setOpaque(false);

        if (currentPath == null || Config.getProject() == null) {
            return pathPanel;
        }

        List<File> fileList = new ArrayList<>();
        String basePathString = Config.getProject().getBasePath();
        File currentDir = currentPath.toFile();

        if (basePathString != null) {
            File baseDir = new File(basePathString);
            while (currentDir != null && !currentDir.getAbsolutePath().equalsIgnoreCase(baseDir.getAbsolutePath())) {
                fileList.addFirst(currentDir);
                currentDir = currentDir.getParentFile();
            }

            fileList.addFirst(baseDir);
        } else {
            fileList.addFirst(currentDir);
        }

        for (int i = 0; i < fileList.size(); i++) {
            Project project = Config.getProject();
            File file = fileList.get(i);
            String labelText = (i == 0) ? project.getName() : file.getName();
            boolean isTestSet = (i == fileList.size() - 1);

            JBLabel folderLabel = new JBLabel(labelText);

            folderLabel.setFont(JBUI.Fonts.label(14));
            folderLabel.setForeground(Gray._120);
            folderLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            folderLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    folderLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
                    setUnderline(folderLabel, true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    folderLabel.setForeground(Gray._120);
                    setUnderline(folderLabel, false);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    com.intellij.openapi.vfs.VirtualFile vf =
                            com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByIoFile(file);
                    if (vf == null) return;

                    if (isTestSet) {
                        testGit.pojo.dto.dirs.TestSetDirectoryDto ts = new testGit.pojo.dto.dirs.TestSetDirectoryDto();
                        ts.setPath(file.toPath());
                        ts.setName(file.getName());
                        testGit.editorPanel.testCaseEditor.TestEditor.open(ts);

                        /* 💡 ملاحظة هامة حول "Focus on selected test case":
                         لكي ينتقل التحديد (Selection) إلى الـ Test Case المفتوح حالياً بمجرد فتح الإضافة،
                         ستحتاج إلى تمرير الـ ID إلى دالة TestEditor.open(ts, currentTestCaseDto.getId())
                         ثم جعل الإضافة الخاصة بك تبحث عن هذا الـ ID في الـ JBList وتعمل list.setSelectedValue(dto)
                        */
                    } else {
                        ProjectView.getInstance(project).select(null, vf, true);
                    }
                }
            });

            pathPanel.add(folderLabel);

            if (i < fileList.size() - 1) {
                JBLabel separator = new JBLabel(AllIcons.General.ArrowRight);
                separator.setBorder(JBUI.Borders.empty(0, 6)); // تكبير المسافة لتناسب الخط الجديد
                pathPanel.add(separator);
            }
        }

        pathPanel.setBorder(JBUI.Borders.empty(0, 16, 12, 16));
        return pathPanel;
    }

    private void setUnderline(JLabel label, boolean underline) {
        Font font = label.getFont();
        java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>(font.getAttributes());
        attributes.put(java.awt.font.TextAttribute.UNDERLINE, underline ? java.awt.font.TextAttribute.UNDERLINE_ON : -1);
        label.setFont(font.deriveFont(attributes));
    }
}
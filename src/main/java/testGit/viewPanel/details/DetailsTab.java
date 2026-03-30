package testGit.viewPanel.details;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.history.HistoryTab;
import testGit.viewPanel.openBugs.OpenBugsTab;

import java.awt.*;
import java.nio.file.Path;

@Getter
public class DetailsTab {
    private final JBPanel<?> panel;
    private final JBPanel<?> detailsTab;
    private final JBPanel<?> historyTab;
    private final JBPanel<?> bugTab;
    private final JBTabbedPane tabbedPane;
    private TestCaseDto currentTestCaseDto;
    private Path currentPath;

    public DetailsTab() {
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

    public void update(@Nullable final TestCaseDto testCaseDto, @Nullable final Path path) {
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

            // تحميل البيانات في التبويبات الأخرى باستخدام الفئات المنفصلة
            HistoryTab.load(historyTab);
            OpenBugsTab.load(bugTab);
        }

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private void setupViewMode(@NotNull final GridBagConstraints gbc, int row) {
        // 1. شريط التنقل العلوي
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insetsTop(12);
        detailsTab.add(NavigationBarUI.create(currentPath), gbc);

        // 2. حاوية الـ ID وزر النسخ
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(8, 16, 2, 16);
        detailsTab.add(HeaderUI.createIdContainer(currentTestCaseDto), gbc);

        // 3. العنوان الرئيسي
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(0, 16, 4, 16);
        detailsTab.add(HeaderUI.createTitleLabel(currentTestCaseDto), gbc);

        // 4. أزرار المهام (Navigate, Run)
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 16, 8, 16);
        detailsTab.add(ActionsUI.create(currentTestCaseDto), gbc);

        // 5. الشارات (Badges)
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 16, 16, 16);
        detailsTab.add(BadgesUI.create(currentTestCaseDto), gbc);

        // 6. صفوف المعلومات (Expected Result, Steps... etc)
        gbc.gridwidth = 1;
        InfoRowsUI.build(detailsTab, gbc, currentTestCaseDto, row);
    }
}
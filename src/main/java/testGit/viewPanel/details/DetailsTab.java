package testGit.viewPanel.details;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.details.components.*;
import testGit.viewPanel.details.components.Module;

import java.awt.*;
import java.nio.file.Path;

public class DetailsTab {
    public static void load(@NotNull JBPanel<?> detailsTab, @Nullable TestCaseDto dto, @Nullable Path currentPath) {
        detailsTab.removeAll();

        if (dto == null) {
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

            setupViewMode(detailsTab, gbc, dto, currentPath, row);
        }

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    private static void setupViewMode(@NotNull JBPanel<?> detailsTabPanel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, @Nullable Path currentPath, int row) {
        gbc.gridwidth = 2;

        row = new NavigationBar(currentPath).render(detailsTabPanel, gbc, dto, row);
        row = new Id().render(detailsTabPanel, gbc, dto, row);
        row = new Title().render(detailsTabPanel, gbc, dto, row);
        row = new ActionIcons().render(detailsTabPanel, gbc, dto, row);
        row = new Badges().render(detailsTabPanel, gbc, dto, row);

        gbc.gridwidth = 1;

        row = new ExpectedResult().render(detailsTabPanel, gbc, dto, row);
        row = new Steps().render(detailsTabPanel, gbc, dto, row);
        row = new AutomationReferrence().render(detailsTabPanel, gbc, dto, row);
        row = new BusinessReferrence().render(detailsTabPanel, gbc, dto, row);
        row = new Module().render(detailsTabPanel, gbc, dto, row);
        row = new CreateBy().render(detailsTabPanel, gbc, dto, row);
        row = new UpdateBy().render(detailsTabPanel, gbc, dto, row);
        row = new CreateAt().render(detailsTabPanel, gbc, dto, row);
        row = new UpdateAt().render(detailsTabPanel, gbc, dto, row);
    }
}
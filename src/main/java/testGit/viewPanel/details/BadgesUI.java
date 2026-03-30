package testGit.viewPanel.details;

import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.Shared;
import testGit.pojo.Groups;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

public class BadgesUI {

    @NotNull
    public static JPanel create(@NotNull TestCaseDto dto) {
        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        badgesPanel.setOpaque(false);

        if (dto.getPriority() != null) {
            badgesPanel.add(Shared.createPriorityBadge(dto));
        }

        if (dto.getGroups() != null && !dto.getGroups().isEmpty()) {
            for (Groups groups : dto.getGroups()) {
                if (groups != null) {
                    badgesPanel.add(Shared.createGroupBadge(groups));
                }
            }
        }

        return badgesPanel;
    }
}
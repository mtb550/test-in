package testGit.viewPanel.details.components;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;

public class CreateAt extends BaseDetails {
    @Override
    public int render(@NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int currentRow) {
        addRow("Created At:", createValueLabel(dto.getCreateAt() != null ? dto.getCreateAt().toString() : "-"), panel, gbc, currentRow);
        return currentRow + 1;
    }
}
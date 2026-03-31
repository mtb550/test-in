package testGit.viewPanel.details.components;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;

public class BusinessReferrence extends BaseDetails {
    @Override
    public int render(@NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int currentRow) {
        addRow("Business Referrence:", createValueLabel(dto.getBusiRef()), panel, gbc, currentRow);
        return currentRow + 1;
    }
}
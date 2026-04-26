package testGit.viewPanel.details.components;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;

import java.awt.*;

public class Fqcn extends BaseDetails {

    private static final String LABEL_TEXT = "FQCN:";

    @Override
    public int render(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {
        return addRow(panel, gbc, LABEL_TEXT, dto.getFqcn().toString(), currentRow);
    }
}
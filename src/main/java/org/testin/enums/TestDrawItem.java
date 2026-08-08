package org.testin.enums;

import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.util.List;

@FunctionalInterface
public interface TestDrawItem {
    List<JComponent> execute(final @NotNull TestCaseDto tc);
}

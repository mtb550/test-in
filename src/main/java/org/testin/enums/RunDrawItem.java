package org.testin.enums;

import org.testin.mappers.TestRunItems;

import javax.swing.*;
import java.util.List;

@FunctionalInterface
public interface RunDrawItem {
    List<JComponent> execute(final TestRunItems item);
}

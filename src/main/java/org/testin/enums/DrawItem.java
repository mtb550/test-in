package org.testin.enums;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

@FunctionalInterface
public interface DrawItem<T> {
    List<JComponent> execute(final T item);
}

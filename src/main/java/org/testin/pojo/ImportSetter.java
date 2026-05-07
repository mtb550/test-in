package org.testin.pojo;

import org.testin.actions.ImportExcel;
import org.testin.pojo.dto.TestCaseDto;

@FunctionalInterface
public interface ImportSetter {
    void accept(final ImportExcel action, final TestCaseDto tc, final String value);
}

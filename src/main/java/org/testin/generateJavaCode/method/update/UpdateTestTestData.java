package org.testin.generateJavaCode.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.util.logger.Logger;

public class UpdateTestTestData extends UpdateTestBase implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        // Test Data is a data-only field: the caller already persists it via indexer.putTestCase,
        // and it has no @Test annotation attribute to change.
        Logger.info("UpdateTestTestData: data-only field, no Java code change");
    }
}

package org.testin.generateJavaCode.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.util.logger.Logger;

public class UpdateTestSteps extends UpdateTestBase implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        // Steps are a data-only field: the caller already persists them via indexer.putTestCase,
        // and they have no @Test annotation attribute to change.
        Logger.info("UpdateTestSteps: data-only field, no Java code change");
    }
}

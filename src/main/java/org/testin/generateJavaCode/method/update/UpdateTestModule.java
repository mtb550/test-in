package org.testin.generateJavaCode.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.logger.Logger;

public class UpdateTestModule extends UpdateTestBase implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        // Module is a data-only field: the caller already persists it via indexer.putTestCase,
        // and it has no @Test annotation attribute to change.
        Logger.info("UpdateTestModule: data-only field, no Java code change");
    }
}

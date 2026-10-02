/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.testin.codegen.JavaCode;
import org.testin.config.TestinYml;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class AbstractCodegenIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String TEST_PROJECT = "NAFATH";

    private static final @NotNull LightProjectDescriptor JAVA_TEST_SOURCE_ROOT = new LightProjectDescriptor() {
        @Override
        protected @NotNull JpsModuleSourceRootType<?> getSourceRootType() {
            return JavaSourceRootType.TEST_SOURCE;
        }

        @Override
        protected void configureModule(final @NotNull Module module, final @NotNull ModifiableRootModel model, final @NotNull ContentEntry contentEntry) {
            final @NotNull Path testng = Path.of(PathUtil.getJarPathForClass(Test.class));
            PsiTestUtil.addLibrary(model, "TestNG", String.valueOf(testng.getParent()), String.valueOf(testng.getFileName()));
        }
    };

    private TestProjectDirectoryDto testProject;

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return JAVA_TEST_SOURCE_ROOT;
    }

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull Path folder = TestinYml.savePath(getProject()).map(Path::getParent).orElseThrow(() -> new AssertionError("the project has no folder for " + TestinYml.fileName()));
        try {
            Files.createDirectories(folder);
        } catch (final IOException ex) {
            throw new AssertionError("could not make the project's folder " + folder + ": " + ex.getMessage(), ex);
        }
        assertTrue("could not write " + TestinYml.fileName(), TestinYml.save(getProject(), TestinYml.lines(TEST_PROJECT)));

        testProject = WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectDirectoryDto tp = Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(root.resolve(TEST_PROJECT));
            nodes().addTestProject(tp);
            return tp;
        });
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    protected @NotNull TestSetDirectoryDto createdTestSet(final @NotNull String name) {
        final @NotNull TestSetDirectoryDto ts = WriteAction.computeAndWait(() -> {
            final @NotNull TestSetDirectoryDto created = Services.getInstance(getProject(), DirectoryMapper.class)
                    .getTestSetNode(testProject.getTestCasesDirectory().getPath().resolve(name), testProject.getTestCasesDirectory());
            nodes().addTestSet(created);
            return created;
        });

        JavaCode.of(DirectoryType.TS).getCreated().execute(getProject(), ts);
        return ts;
    }

    protected @NotNull Optional<PsiClass> generatedClass(final @NotNull String qualifiedName) {
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        return Optional.ofNullable(JavaPsiFacade.getInstance(getProject()).findClass(qualifiedName, GlobalSearchScope.projectScope(getProject())));
    }
}

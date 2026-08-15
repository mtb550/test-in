package org.testin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Services {

    public static <T> @NotNull T getInstance(final @NotNull Project p, final @NotNull Class<T> clazz) {
        return p.getService(clazz);
    }

    /**
     * For a caller that genuinely has no project — the application settings page
     * is built once for the IDE, not once per project (#70).
     * <p>
     * The project form resolves application-level services too, because a project
     * container delegates to the application container, so most callers can and
     * do use it. This one is for the places where there is no project to pass.
     */
    public static <T> @NotNull T getInstance(final @NotNull Class<T> clazz) {
        return ApplicationManager.getApplication().getService(clazz);
    }

}

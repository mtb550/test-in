package org.testin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Services {

    /**
     * Which classes belong to the application, worked out once each.
     * <p>
     * Reading an annotation is cheap but this is on the path of every service
     * lookup in the plugin, and the answer for a class can never change while
     * the IDE is running.
     */
    private static final @NotNull Map<Class<?>, Boolean> APPLICATION_LEVEL = new ConcurrentHashMap<>();

    /**
     * The service, from whichever container owns it.
     * <p>
     * A project service comes from this project. An <em>application</em> service
     * comes from the application even though a project was passed, because there
     * is only ever one of it - and asking the project container for one does not
     * fetch the application's, it builds a second instance inside the project,
     * with its own persisted state.
     * <p>
     * That is not theoretical. The settings page has no project, so it wrote the
     * tester's name into the application's copy; every run, marker and verdict
     * passed a project and read the project's. The result was two files called
     * testinSettings.xml holding different names, and a tester whose name in
     * Settings never reached the runs they created.
     * <p>
     * Decided here rather than at the call sites: which container owns a service
     * is the service's own declaration, and twenty callers should not each have
     * to remember which form to use.
     */
    public static <T> @NotNull T getInstance(final @NotNull Project p, final @NotNull Class<T> clazz) {
        if (isApplicationLevel(clazz)) return getInstance(clazz);

        return p.getService(clazz);
    }

    /**
     * For a caller that genuinely has no project — the application settings page
     * is built once for the IDE, not once per project (#70).
     */
    public static <T> @NotNull T getInstance(final @NotNull Class<T> clazz) {
        return ApplicationManager.getApplication().getService(clazz);
    }

    /**
     * Whether this class declares itself the application's.
     * <p>
     * Read from the class's own {@code @Service} annotation, so a service added
     * later is routed by saying what it is, not by being listed here.
     */
    static boolean isApplicationLevel(final @NotNull Class<?> clazz) {
        return APPLICATION_LEVEL.computeIfAbsent(clazz, Services::declaresApplicationLevel);
    }

    /**
     * A class with no {@code @Service} annotation, or one that does not name a
     * level, is left to the project container - which is where every service in
     * this plugin that does not say otherwise belongs.
     */
    private static boolean declaresApplicationLevel(final @NotNull Class<?> clazz) {
        final @Nullable Service service = clazz.getAnnotation(Service.class);

        return service != null && Arrays.asList(service.value()).contains(Service.Level.APP);
    }

    /**
     * Whether this project has already built the service. Asking never builds
     * one - which is the whole point, because {@link #getInstance} does: a
     * project that never opened the Testin tool window has no explorer panel,
     * and constructing one just to refresh it starts indexing in a project
     * nobody asked about (#77).
     * <p>
     * The platform answers with a null, and it stays in here so no caller has to
     * hold a maybe-a-service.
     */
    public static boolean isCreated(final @NotNull Project p, final @NotNull Class<?> clazz) {
        return p.getServiceIfCreated(clazz) != null;
    }

}

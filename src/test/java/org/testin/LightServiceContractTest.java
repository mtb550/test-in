package org.testin;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.fail;

/**
 * Every {@code @Service} class keeps the contract the platform asks of a light
 * service: it is final, and it declares a constructor the platform will call.
 * <p>
 * Neither is visible to the compiler. The platform checks both by reflection the
 * first time something asks for the service, and throws a {@code PluginException}
 * there - which is to say, in front of the tester, in whichever feature happened
 * to ask first. Two shipped that way in one day: a service that was not final,
 * so running a test set died on the menu click, and one whose generated
 * constructor had grown a second parameter, so nothing that touched it worked at
 * all.
 * <p>
 * A generated constructor is the sharper of the two, because it changes shape on
 * its own: Lombok's {@code @AllArgsConstructor} skips a final field with an
 * initializer and includes a non-final one without, so adding an ordinary field
 * silently rewrites the signature the platform is looking for.
 * <p>
 * Written as a scan of the sources rather than of the classpath, and for the
 * same reason as {@link EnumNullContractTest}: tests run under the platform's
 * own class loader, which serves classes but will not enumerate a package
 * directory as a resource.
 */
public class LightServiceContractTest {

    private static final @NotNull Path SOURCE_ROOT = Paths.get("src", "main", "java");

    private static final @NotNull String ROOT_PACKAGE = "org.testin";

    /**
     * The constructors {@code ComponentManagerImpl} will call, as parameter
     * counts by type name. A coroutine scope is named rather than imported so
     * this test does not depend on the Kotlin coroutines classes being on the
     * test classpath.
     */
    private static final @NotNull List<String> ACCEPTED = List.of(
            "",
            "com.intellij.openapi.project.Project",
            "kotlinx.coroutines.CoroutineScope",
            "com.intellij.openapi.project.Project,kotlinx.coroutines.CoroutineScope");

    @Test
    public void everyLightServiceCanBeBuiltByThePlatform() throws Exception {
        final List<Class<?>> services = findServices();
        if (services.isEmpty()) {
            fail("No @Service classes found under " + ROOT_PACKAGE + ": the scan is looking in the wrong place");
        }

        System.out.println("Checked " + services.size() + " light services");

        final List<String> breaches = new ArrayList<>();

        for (final Class<?> service : services) {
            if (!Modifier.isFinal(service.getModifiers())) {
                breaches.add(service.getName() + " is not final");
            }

            if (Arrays.stream(service.getDeclaredConstructors()).noneMatch(LightServiceContractTest::isAccepted)) {
                breaches.add(service.getName() + " declares no constructor the platform will call, only "
                        + Arrays.stream(service.getDeclaredConstructors())
                        .map(LightServiceContractTest::signature)
                        .toList());
            }
        }

        if (!breaches.isEmpty()) {
            fail("Light services the platform will refuse to build:\n  " + String.join("\n  ", breaches));
        }
    }

    private static boolean isAccepted(final @NotNull Constructor<?> constructor) {
        return ACCEPTED.contains(signature(constructor));
    }

    private static @NotNull String signature(final @NotNull Constructor<?> constructor) {
        return String.join(",", Arrays.stream(constructor.getParameterTypes()).map(Class::getName).toList());
    }

    private static @NotNull List<Class<?>> findServices() throws Exception {
        final ClassLoader loader = LightServiceContractTest.class.getClassLoader();
        final Path dir = SOURCE_ROOT.resolve(ROOT_PACKAGE.replace('.', '/'));

        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("No sources at " + dir.toAbsolutePath()
                    + " - this test expects to run from the project root");
        }

        final List<Class<?>> services = new ArrayList<>();

        try (Stream<Path> files = Files.walk(dir)) {
            files.filter(f -> f.toString().endsWith(".java"))
                    .map(LightServiceContractTest::toClassName)
                    .forEach(name -> {
                        final Class<?> type = loadWithoutInitializing(loader, name);
                        if (type != null && type.isAnnotationPresent(Service.class)) services.add(type);
                    });
        }

        return services;
    }

    private static @NotNull String toClassName(final @NotNull Path file) {
        final String relative = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
        return relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
    }

    /**
     * Loads without running the initializer: this test asks what a class
     * declares, and a class that cannot initialize outside a running IDE still
     * has to declare it correctly.
     */
    private static @Nullable Class<?> loadWithoutInitializing(final @NotNull ClassLoader loader,
                                                              final @NotNull String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (final Throwable t) {
            return null;
        }
    }
}

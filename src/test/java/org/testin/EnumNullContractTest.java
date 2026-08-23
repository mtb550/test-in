package org.testin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.fail;

/**
 * Every enum constant keeps the null contract its own fields declare.
 * <p>
 * A wrong {@code @NotNull} on an enum field is invisible to the compiler:
 * javac treats the annotation as a marker and lets the null through. What
 * enforces it is the platform's bytecode instrumentation, which inserts a
 * runtime check - and enum constants are built in the class initializer, so
 * the check fires while the class is loading. The class then fails to
 * initialize, and every feature that touches it dies with
 * {@code ExceptionInInitializerError} rather than a diagnosable error.
 * <p>
 * That instrumentation is active here, so this test needs no reflection over
 * the annotations: loading an enum runs exactly the check the IDE runs. What
 * was missing was only that nothing loaded them. Three shipped defects came
 * through that gap (#45), each found by opening the feature instead.
 * <p>
 * Failures unrelated to nullability are reported and skipped, so an enum that
 * cannot initialize outside a running IDE does not turn into a false alarm.
 */
public class EnumNullContractTest {

    private static final @NotNull Path SOURCE_ROOT = Paths.get("src", "main", "java");
    private static final @NotNull String ROOT_PACKAGE = "org.testin";

    private static @NotNull List<Class<?>> findEnums() {

        try {
            final ClassLoader loader = EnumNullContractTest.class.getClassLoader();

            // Listed from the sources rather than the classpath: tests run under the
            // platform's own class loader, which serves classes but does not
            // enumerate a package directory as a resource.
            final Path dir = SOURCE_ROOT.resolve(ROOT_PACKAGE.replace('.', '/'));
            if (!Files.isDirectory(dir)) {
                throw new IllegalStateException("No sources at " + dir.toAbsolutePath()
                        + " - this test expects to run from the project root");
            }

            final List<Class<?>> enums = new ArrayList<>();

            try (Stream<Path> files = Files.walk(dir)) {
                files.filter(f -> f.toString().endsWith(".java"))
                        .map(EnumNullContractTest::toClassName)
                        .forEach(name -> {
                            final Class<?> type = loadWithoutInitializing(loader, name);
                            if (type != null && type.isEnum()) enums.add(type);
                        });
            }

            return enums;

        } catch (final Exception ex) {

            throw new AssertionError(ex);

        }

    }

    private static @NotNull String toClassName(final @NotNull Path file) {
        final String relative = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
        return relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
    }

    /**
     * Loads without running the initializer, so the scan itself never triggers
     * the very failure it is looking for - that is left to the checked call.
     */
    private static @Nullable Class<?> loadWithoutInitializing(final @NotNull ClassLoader loader,
                                                              final @NotNull String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (final Throwable t) {
            return null;
        }
    }

    private static boolean isNullContractBreach(final @NotNull Throwable cause) {
        return cause instanceof IllegalArgumentException
                && cause.getMessage() != null
                && cause.getMessage().contains("must not be null");
    }

    private static @NotNull Throwable rootCause(final @NotNull Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return current;
    }

    @Test
    public void everyEnumConstantHonoursItsNullContract() {
        try {
            final List<String> breaches = new ArrayList<>();
            final List<String> skipped = new ArrayList<>();

            final List<Class<?>> enums = findEnums();
            if (enums.isEmpty()) fail("No enums found under " + ROOT_PACKAGE + ": the scan is looking in the wrong place");
            System.out.println("Checked " + enums.size() + " enums");

            for (final Class<?> type : enums) {
                try {
                    // Initializes the class, which builds every constant.
                    type.getEnumConstants();
                } catch (final Throwable t) {
                    final Throwable cause = rootCause(t);
                    if (isNullContractBreach(cause)) breaches.add(type.getName() + ": " + cause.getMessage());
                    else skipped.add(type.getName() + ": " + cause);
                }
            }

            if (!skipped.isEmpty()) {
                System.out.println("Enums that could not initialize for reasons unrelated to nullability:");
                skipped.forEach(s -> System.out.println("  " + s));
            }

            if (!breaches.isEmpty()) {
                fail("An enum constant passes null to a field annotated @NotNull. Annotate the field @Nullable if "
                        + "null is legitimate, or give the constant a value:\n  " + String.join("\n  ", breaches));
            }
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }
}

package org.testin.util;

import java.lang.reflect.Constructor;

/**
 * The serializer the plugin actually uses, outside an IDE.
 * <p>
 * {@link Mapper} is a project-level service, so a test with no {@code Project}
 * cannot ask {@code Services} for one. Building a second {@code ObjectMapper}
 * configured the same way would be worse than no test at all: it would pass
 * while the real one changed underneath it, which is exactly the divergence
 * these tests exist to catch. So the real class is constructed reflectively,
 * private constructor and all, and every question is asked of the thing that
 * ships.
 * <p>
 * Here because seven test classes had written this same block - the git ones,
 * the sftp ones and {@code MapperFailureTest} - and a helper copied seven times
 * is seven places to fix when the constructor changes.
 */
public final class RealMapper {

    private RealMapper() {
    }

    public static Mapper build() {
        try {
            final Constructor<Mapper> constructor = Mapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            return constructor.newInstance();
        } catch (final ReflectiveOperationException ex) {
            throw new AssertionError("Could not build the real Mapper for a test", ex);
        }
    }
}

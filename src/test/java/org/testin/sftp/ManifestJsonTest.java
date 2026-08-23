package org.testin.sftp;

import com.fasterxml.jackson.core.type.TypeReference;
import org.testin.util.Mapper;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * The manifest has to survive the trip to the server and back (#94).
 * <p>
 * It is the only thing that tells a sync what the other side holds. If it
 * cannot be read back, every sync sees an empty server - and then every file
 * this machine has looks like something the server deleted, which is 2,000
 * questions the tester never needed to be asked.
 */
public class ManifestJsonTest {

    private static final TypeReference<Map<String, Manifest.Entry>> ENTRIES = new TypeReference<>() {
    };

    private static Mapper mapper() {
        try {
            final Constructor<Mapper> constructor = Mapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not build a Mapper for the test", ex);
        }
    }

    @Test
    public void aManifestSurvivesBeingWrittenAndReadBack() {
        final Mapper mapper = mapper();
        final Manifest before = Manifest.of(Map.of(
                "Test Cases/pkg1/Login/a.json", "{\"description\":\"Sign in\"}".getBytes(StandardCharsets.UTF_8),
                ".tp", "{}".getBytes(StandardCharsets.UTF_8)));

        final String json = mapper.writeValueAsString(before.entries());
        System.out.println("[manifest json] " + json.replaceAll("\\s+", " "));

        assertEquals(mapper.readValue(json, ENTRIES), before.entries(),
                "a manifest that cannot be read back makes every sync see an empty server");
    }

    /**
     * The written form carries what a manifest is, and nothing derived from it.
     * <p>
     * {@code isAbsent()} is a question the record answers, not a fact about the
     * file. Written out it becomes a property the record has no component for,
     * and reading it back is then a failure rather than a manifest.
     */
    @Test
    public void nothingDerivedIsWrittenIntoTheFile() {
        final String json = mapper().writeValueAsString(
                Manifest.of(Map.of("a.json", "{}".getBytes(StandardCharsets.UTF_8))).entries());

        assertFalse(json.contains("absent"), "the file should hold the hash and the size, and nothing else: " + json);
    }
}

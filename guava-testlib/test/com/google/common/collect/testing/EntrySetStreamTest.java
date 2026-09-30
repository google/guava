package com.google.common.collect.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Collectors;

public class EntrySetStreamTest extends TestCase {

    public void testEntrySetStreamToList_BasicImmutableMap() {
        Map<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);

        assertThat(map.entrySet().stream().collect(Collectors.toList()))
                .containsExactlyElementsIn(map.entrySet())
                .inOrder();
    }

    public void testEntrySetStreamToList_WithNullValueInHashMap() {
        Map<String, String> map = new HashMap<>();
        map.put("x", null);
        map.put("y", "yes");

        assertThat(map.entrySet().stream().collect(Collectors.toList()))
                .containsExactlyElementsIn(map.entrySet())
                .inOrder();
    }

    public void testSpliteratorCharacteristics_ImmutableMap() {
        Map<String, String> map = ImmutableMap.of("a", "1", "b", "2");

        Spliterator<Map.Entry<String, String>> spliterator = map.entrySet().spliterator();

        // NONNULL should be set since ImmutableMap does not allow nulls
        assertTrue((spliterator.characteristics() & Spliterator.NONNULL) != 0);
    }

    public void testSpliteratorCharacteristics_HashMapWithNullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        map.put("b", null);

        Spliterator<Map.Entry<String, String>> spliterator = map.entrySet().spliterator();

        // NONNULL should NOT be set because value is null
        assertFalse((spliterator.characteristics() & Spliterator.NONNULL) != 0);
    }
}

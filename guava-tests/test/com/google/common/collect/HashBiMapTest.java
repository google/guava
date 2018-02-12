/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestBiMapGenerator;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Tests for {@link HashBiMap}.
 *
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
public class HashBiMapTest extends TestCase {

  private static final ImmutableList<HashBiMapGenerator> GENERATORS =
      ImmutableList.of(new HashBiMapGenerator(), new HashBiMapJdkBackedGenerator());

  public static class HashBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected HashBiMap<String, String> create(Entry<String, String>[] entries) {
      HashBiMap<String, String> result = HashBiMap.create();
      for (Entry<String, String> entry : entries) {
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }
  }

  private static final class HashBiMapJdkBackedGenerator extends HashBiMapGenerator {
    @Override
    protected HashBiMap<String, String> create(Entry<String, String>[] entries) {
      HashBiMap<String, String> map = super.create(entries);
      map.switchToFloodProtection();
      return map;
    }
  }

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        BiMapTestSuiteBuilder.using(new HashBiMapGenerator())
            .named("HashBiMap")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTest(
        BiMapTestSuiteBuilder.using(new HashBiMapJdkBackedGenerator())
            .named("HashBiMap [JDK backed]")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTestSuite(HashBiMapTest.class);
    return suite;
  }

  public void testMapConstructor() {
    /* Test with non-empty Map. */
    Map<String, String> map =
        ImmutableMap.of(
            "canada", "dollar",
            "chile", "peso",
            "switzerland", "franc");
    HashBiMap<String, String> bimap = HashBiMap.create(map);
    assertEquals("dollar", bimap.get("canada"));
    assertEquals("canada", bimap.inverse().get("dollar"));
  }

  private static final int N = 1000;

  public void testBashIt() throws Exception {
    BiMap<Integer, Integer> bimap = HashBiMap.create(N);
    BiMap<Integer, Integer> inverse = bimap.inverse();

    for (int i = 0; i < N; i++) {
      assertNull(bimap.put(2 * i, 2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i + 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      int oldValue = bimap.get(2 * i);
      assertEquals(2 * i + 1, (int) bimap.put(2 * i, oldValue - 2));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i - 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i - 1));
    }
    Set<Entry<Integer, Integer>> entries = bimap.entrySet();
    for (Entry<Integer, Integer> entry : entries) {
      entry.setValue(entry.getValue() + 2 * N);
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * N + 2 * i - 1, (int) bimap.get(2 * i));
    }
  }

  public void testBiMapEntrySetIteratorRemove() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map = generator.create(mapEntry("1", "one"));
      Set<Entry<String, String>> entries = map.entrySet();
      Iterator<Entry<String, String>> iterator = entries.iterator();
      Entry<String, String> entry = iterator.next();
      entry.setValue("two"); // changes the iterator's current entry value
      assertEquals("two", map.get("1"));
      assertEquals("1", map.inverse().get("two"));
      iterator.remove(); // removes the updated entry
      assertTrue(map.isEmpty());
    }
  }

  public void testInsertionOrder() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map =
          generator.create(mapEntry("foo", "1"), mapEntry("bar", "2"), mapEntry("quux", "3"));
      assertThat(map).containsExactly("foo", "1", "bar", "2", "quux", "3").inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveFirst() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map =
          generator.create(mapEntry("foo", "1"), mapEntry("bar", "2"), mapEntry("quux", "3"));
      map.remove("foo");
      assertThat(map).containsExactly("bar", "2", "quux", "3").inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveMiddle() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map =
          generator.create(mapEntry("foo", "1"), mapEntry("bar", "2"), mapEntry("quux", "3"));
      map.remove("bar");
      assertThat(map).containsExactly("foo", "1", "quux", "3").inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveLast() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map =
          generator.create(mapEntry("foo", "1"), mapEntry("bar", "2"), mapEntry("quux", "3"));
      map.remove("quux");
      assertThat(map).containsExactly("foo", "1", "bar", "2").inOrder();
    }
  }

  public void testInsertionOrderAfterForcePut() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map =
          generator.create(mapEntry("foo", "1"), mapEntry("bar", "2"), mapEntry("quux", "3"));
      map.forcePut("quux", "1");
      assertThat(map).containsExactly("bar", "2", "quux", "1").inOrder();
      assertThat(map.inverse()).containsExactly("2", "bar", "1", "quux").inOrder();
    }
  }

  public void testInsertionOrderAfterInverseForcePut() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map =
          generator.create(mapEntry("foo", "1"), mapEntry("bar", "2"), mapEntry("quux", "3"));
      map.inverse().forcePut("1", "quux");
      assertThat(map).containsExactly("bar", "2", "quux", "1").inOrder();
      assertThat(map.inverse()).containsExactly("2", "bar", "1", "quux").inOrder();
    }
  }

  public void testInverseInsertionOrderAfterInverse() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map = generator.create(mapEntry("bar", "2"), mapEntry("quux", "1"));
      assertThat(map.inverse()).containsExactly("2", "bar", "1", "quux").inOrder();
    }
  }

  public void testInverseInsertionOrderAfterInverseForcePutPresentKey() {
    for (TestBiMapGenerator<String, String> generator : GENERATORS) {
      BiMap<String, String> map =
          generator.create(
              mapEntry("foo", "1"),
              mapEntry("bar", "2"),
              mapEntry("quux", "3"),
              mapEntry("nab", "4"));
      map.inverse().forcePut("4", "bar");
      assertThat(map).containsExactly("foo", "1", "bar", "4", "quux", "3").inOrder();
    }
  }

  public void testInverseEntrySetValueNewKey() {
    BiMap<Integer, String> map = HashBiMap.create();
    map.put(1, "a");
    map.put(2, "b");
    Iterator<Entry<String, Integer>> inverseEntryItr = map.inverse().entrySet().iterator();
    Entry<String, Integer> entry = inverseEntryItr.next();
    entry.setValue(3);
    assertEquals(Maps.immutableEntry("b", 2), inverseEntryItr.next());
    assertFalse(inverseEntryItr.hasNext());
    assertThat(map.entrySet())
        .containsExactly(Maps.immutableEntry(2, "b"), Maps.immutableEntry(3, "a"))
        .inOrder();
  }

  /**
   * A Comparable wrapper around a String which executes callbacks on calls to hashCode, equals, and
   * compareTo.
   */
  private static class CountsHashCodeAndEquals implements Comparable<CountsHashCodeAndEquals> {
    private final String delegateString;
    private final Runnable onHashCode;
    private final Runnable onEquals;
    private final Runnable onCompareTo;

    CountsHashCodeAndEquals(
        String delegateString, Runnable onHashCode, Runnable onEquals, Runnable onCompareTo) {
      this.delegateString = delegateString;
      this.onHashCode = onHashCode;
      this.onEquals = onEquals;
      this.onCompareTo = onCompareTo;
    }

    @Override
    public int hashCode() {
      onHashCode.run();
      return delegateString.hashCode();
    }

    @Override
    public boolean equals(@NullableDecl Object other) {
      onEquals.run();
      return other instanceof CountsHashCodeAndEquals
          && delegateString.equals(((CountsHashCodeAndEquals) other).delegateString);
    }

    @Override
    public int compareTo(CountsHashCodeAndEquals o) {
      onCompareTo.run();
      return delegateString.compareTo(o.delegateString);
    }
  }

  /** A holder of counters for calls to hashCode, equals, and compareTo. */
  private static final class CallsCounter {
    long hashCode;
    long equals;
    long compareTo;

    long total() {
      return hashCode + equals + compareTo;
    }

    void zero() {
      hashCode = 0;
      equals = 0;
      compareTo = 0;
    }
  }

  /** All the ways to create an ImmutableBiMap. */
  enum ConstructionPathway {
    COPY_OF_MAP {
      @Override
      BiMap<?, ?> create(List<? extends Entry<?, ?>> entries, CallsCounter counter) {
        Map<Object, Object> sourceMap = Maps.newLinkedHashMap();
        for (Entry<?, ?> entry : entries) {
          if (sourceMap.put(entry.getKey(), entry.getValue()) != null) {
            throw new UnsupportedOperationException("duplicate key");
          }
        }
        counter.zero();
        return HashBiMap.create(sourceMap);
      }
    },
    PUT_ONE_BY_ONE {
      @Override
      BiMap<?, ?> create(List<? extends Entry<?, ?>> entries, CallsCounter counter) {
        BiMap<Object, Object> map = HashBiMap.create();
        for (Entry<?, ?> entry : entries) {
          map.put(entry.getKey(), entry.getValue());
        }
        return map;
      }
    },
    PUT_ALL_MAP {
      @Override
      BiMap<?, ?> create(List<? extends Entry<?, ?>> entries, CallsCounter counter) {
        Map<Object, Object> sourceMap = Maps.newLinkedHashMap();
        for (Entry<?, ?> entry : entries) {
          if (sourceMap.put(entry.getKey(), entry.getValue()) != null) {
            throw new UnsupportedOperationException("duplicate key");
          }
        }
        counter.zero();
        BiMap<Object, Object> map = HashBiMap.create();
        map.putAll(sourceMap);
        return map;
      }
    };

    @CanIgnoreReturnValue
    abstract BiMap<?, ?> create(List<? extends Entry<?, ?>> entries, CallsCounter counter);
  }

  /**
   * Returns a list of objects with the same hash code, of size 2^power, counting calls to equals,
   * hashCode, and compareTo in counter.
   */
  static List<CountsHashCodeAndEquals> createAdversarialObjects(int power, CallsCounter counter) {
    String str1 = "Aa";
    String str2 = "BB";
    assertEquals(str1.hashCode(), str2.hashCode());
    List<String> haveSameHashes2 = Arrays.asList(str1, str2);
    List<CountsHashCodeAndEquals> result =
        Lists.newArrayList(
            Lists.transform(
                Lists.cartesianProduct(Collections.nCopies(power, haveSameHashes2)),
                strs ->
                    new CountsHashCodeAndEquals(
                        String.join("", strs),
                        () -> counter.hashCode++,
                        () -> counter.equals++,
                        () -> counter.compareTo++)));
    assertEquals(
        result.get(0).delegateString.hashCode(),
        result.get(result.size() - 1).delegateString.hashCode());
    return result;
  }

  enum AdversaryType {
    ADVERSARIAL_KEYS {
      @Override
      List<? extends Entry<?, ?>> createAdversarialEntries(int power, CallsCounter counter) {
        return createAdversarialObjects(power, counter)
            .stream()
            .map(k -> Maps.immutableEntry(k, new Object()))
            .collect(toList());
      }
    },
    ADVERSARIAL_VALUES {
      @Override
      List<? extends Entry<?, ?>> createAdversarialEntries(int power, CallsCounter counter) {
        return createAdversarialObjects(power, counter)
            .stream()
            .map(k -> Maps.immutableEntry(new Object(), k))
            .collect(toList());
      }
    },
    ADVERSARIAL_KEYS_AND_VALUES {
      @Override
      List<? extends Entry<?, ?>> createAdversarialEntries(int power, CallsCounter counter) {
        List<?> keys = createAdversarialObjects(power, counter);
        List<?> values = createAdversarialObjects(power, counter);
        return Streams.zip(keys.stream(), values.stream(), Maps::immutableEntry).collect(toList());
      }
    };

    abstract List<? extends Entry<?, ?>> createAdversarialEntries(int power, CallsCounter counter);
  }

  @GwtIncompatible
  public void testResistsHashFloodingInConstruction() {
    for (AdversaryType adversary : AdversaryType.values()) {
      CallsCounter smallCounter = new CallsCounter();
      List<? extends Entry<?, ?>> smallEntries =
          adversary.createAdversarialEntries(10, smallCounter);
      int smallSize = smallEntries.size();

      CallsCounter largeCounter = new CallsCounter();
      List<? extends Entry<?, ?>> largeEntries =
          adversary.createAdversarialEntries(15, largeCounter);
      int largeSize = largeEntries.size();

      for (ConstructionPathway pathway : ConstructionPathway.values()) {
        smallCounter.zero();
        pathway.create(smallEntries, smallCounter);
        long smallOps = smallCounter.total();

        largeCounter.zero();
        pathway.create(largeEntries, largeCounter);
        long largeOps = largeCounter.total();

        double ratio = (double) largeOps / smallOps;
        assertThat(ratio)
            .named(
                "ratio of equals/hashCode/compareTo operations to build an HashBiMap with %s"
                    + " via %s with %s entries versus %s entries",
                adversary, pathway, largeSize, smallSize)
            .isAtMost(2 * (largeSize * Math.log(largeSize)) / (smallSize * Math.log(smallSize)));
        // allow up to 2x wobble in the constant factors
      }
    }
  }

  @GwtIncompatible
  public void testResistsHashFloodingOnForwardGet() {
    for (AdversaryType adversary : AdversaryType.values()) {
      CallsCounter smallCounter = new CallsCounter();
      List<? extends Entry<?, ?>> smallEntries =
          adversary.createAdversarialEntries(10, smallCounter);
      BiMap<?, ?> smallMap = ConstructionPathway.PUT_ONE_BY_ONE.create(smallEntries, smallCounter);
      int smallSize = smallEntries.size();
      long smallOps = worstCaseQueryOperations(smallMap, smallCounter);

      CallsCounter largeCounter = new CallsCounter();
      List<? extends Entry<?, ?>> largeEntries =
          adversary.createAdversarialEntries(15, largeCounter);
      BiMap<?, ?> largeMap = ConstructionPathway.PUT_ONE_BY_ONE.create(largeEntries, largeCounter);
      int largeSize = largeEntries.size();
      long largeOps = worstCaseQueryOperations(largeMap, largeCounter);

      if (smallOps == 0 && largeOps == 0) {
        continue; // no queries on the CHCAE objects
      }

      double ratio = (double) largeOps / smallOps;
      assertThat(ratio)
          .named(
              "Ratio of worst case get operations for an HashBiMap with %s of size "
                  + "%s versus %s",
              adversary, largeSize, smallSize)
          .isAtMost(2 * Math.log(largeSize) / Math.log(smallSize));
      // allow up to 2x wobble in the constant factors
    }
  }

  @GwtIncompatible
  public void testResistsHashFloodingOnInverseGet() {
    for (AdversaryType adversary : AdversaryType.values()) {
      CallsCounter smallCounter = new CallsCounter();
      List<? extends Entry<?, ?>> smallEntries =
          adversary.createAdversarialEntries(10, smallCounter);
      BiMap<?, ?> smallMap = ConstructionPathway.PUT_ONE_BY_ONE.create(smallEntries, smallCounter);
      int smallSize = smallEntries.size();
      long smallOps = worstCaseQueryOperations(smallMap.inverse(), smallCounter);

      CallsCounter largeCounter = new CallsCounter();
      List<? extends Entry<?, ?>> largeEntries =
          adversary.createAdversarialEntries(15, largeCounter);
      BiMap<?, ?> largeMap = ConstructionPathway.PUT_ONE_BY_ONE.create(largeEntries, largeCounter);
      int largeSize = largeEntries.size();
      long largeOps = worstCaseQueryOperations(largeMap.inverse(), largeCounter);

      if (smallOps == 0 && largeOps == 0) {
        continue; // no queries on the CHCAE objects
      }
      double ratio = (double) largeOps / smallOps;
      assertThat(ratio)
          .named(
              "Ratio of worst case get operations for an HashBiMap with %s of size "
                  + "%s versus %s",
              adversary, largeSize, smallSize)
          .isAtMost(2 * Math.log(largeSize) / Math.log(smallSize));
      // allow up to 2x wobble in the constant factors
    }
  }

  private static long worstCaseQueryOperations(Map<?, ?> map, CallsCounter counter) {
    long worstCalls = 0;
    for (Object k : map.keySet()) {
      counter.zero();
      Object unused = map.get(k);
      worstCalls = Math.max(worstCalls, counter.total());
    }
    return worstCalls;
  }
}

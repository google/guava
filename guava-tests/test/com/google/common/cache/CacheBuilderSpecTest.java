/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.cache;

import static com.google.common.cache.CacheBuilderSpec.parse;
import static com.google.common.cache.TestingWeighers.constantWeigher;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Suppliers;
import com.google.common.cache.LocalCache.Strength;
import com.google.common.testing.EqualsTester;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

/**
 * Tests CacheBuilderSpec. TODO(user): tests of a few invalid input conditions, boundary
 * conditions.
 *
 * @author Adam Winer
 */
public class CacheBuilderSpecTest extends TestCase {
  public void testParse_empty() {
    CacheBuilderSpec spec = parse("");
    assertNull(spec.initialCapacity);
    assertNull(spec.maximumSize);
    assertNull(spec.maximumWeight);
    assertNull(spec.concurrencyLevel);
    assertNull(spec.keyStrength);
    assertNull(spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder(), CacheBuilder.from(spec));
  }

  public void testParse_initialCapacity() {
    CacheBuilderSpec spec = parse("initialCapacity=10");
    assertEquals(10, spec.initialCapacity.intValue());
    assertNull(spec.maximumSize);
    assertNull(spec.maximumWeight);
    assertNull(spec.concurrencyLevel);
    assertNull(spec.keyStrength);
    assertNull(spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().initialCapacity(10), CacheBuilder.from(spec));
  }

  public void testParse_initialCapacityRepeated() {
    assertThrows(
        IllegalArgumentException.class, () -> parse("initialCapacity=10, initialCapacity=20"));
  }

  public void testParse_maximumSize() {
    CacheBuilderSpec spec = parse("maximumSize=9000");
    assertNull(spec.initialCapacity);
    assertEquals(9000, spec.maximumSize.longValue());
    assertNull(spec.concurrencyLevel);
    assertNull(spec.keyStrength);
    assertNull(spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().maximumSize(9000), CacheBuilder.from(spec));
  }

  public void testParse_maximumSizeRepeated() {
    assertThrows(IllegalArgumentException.class, () -> parse("maximumSize=10, maximumSize=20"));
  }

  public void testParse_maximumWeight() {
    CacheBuilderSpec spec = parse("maximumWeight=9000");
    assertNull(spec.initialCapacity);
    assertEquals(9000, spec.maximumWeight.longValue());
    assertNull(spec.concurrencyLevel);
    assertNull(spec.keyStrength);
    assertNull(spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().maximumWeight(9000), CacheBuilder.from(spec));
  }

  public void testParse_maximumWeightRepeated() {
    assertThrows(IllegalArgumentException.class, () -> parse("maximumWeight=10, maximumWeight=20"));
  }

  public void testParse_maximumSizeAndMaximumWeight() {
    assertThrows(IllegalArgumentException.class, () -> parse("maximumSize=10, maximumWeight=20"));
  }

  public void testParse_concurrencyLevel() {
    CacheBuilderSpec spec = parse("concurrencyLevel=32");
    assertNull(spec.initialCapacity);
    assertNull(spec.maximumSize);
    assertNull(spec.maximumWeight);
    assertEquals(32, spec.concurrencyLevel.intValue());
    assertNull(spec.keyStrength);
    assertNull(spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().concurrencyLevel(32), CacheBuilder.from(spec));
  }

  public void testParse_concurrencyLevelRepeated() {
    assertThrows(
        IllegalArgumentException.class, () -> parse("concurrencyLevel=10, concurrencyLevel=20"));
  }

  public void testParse_weakKeys() {
    CacheBuilderSpec spec = parse("weakKeys");
    assertNull(spec.initialCapacity);
    assertNull(spec.maximumSize);
    assertNull(spec.maximumWeight);
    assertNull(spec.concurrencyLevel);
    assertEquals(Strength.WEAK, spec.keyStrength);
    assertNull(spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder().weakKeys(), CacheBuilder.from(spec));
  }

  public void testParse_weakKeysCannotHaveValue() {
    assertThrows(IllegalArgumentException.class, () -> parse("weakKeys=true"));
  }

  public void testParse_repeatedKeyStrength() {
    assertThrows(IllegalArgumentException.class, () -> parse("weakKeys, weakKeys"));
  }

  public void testParse_softValues() {
    CacheBuilderSpec spec = parse("softValues");
    assertNull(spec.initialCapacity);
    assertNull(spec.maximumSize);
    assertNull(spec.maximumWeight);
    assertNull(spec.concurrencyLevel);
    assertNull(spec.keyStrength);
    assertEquals(Strength.SOFT, spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder().softValues(), CacheBuilder.from(spec));
  }

  public void testParse_softValuesCannotHaveValue() {
    assertThrows(IllegalArgumentException.class, () -> parse("softValues=true"));
  }

  public void testParse_weakValues() {
    CacheBuilderSpec spec = parse("weakValues");
    assertNull(spec.initialCapacity);
    assertNull(spec.maximumSize);
    assertNull(spec.maximumWeight);
    assertNull(spec.concurrencyLevel);
    assertNull(spec.keyStrength);
    assertEquals(Strength.WEAK, spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder().weakValues(), CacheBuilder.from(spec));
  }

  public void testParse_weakValuesCannotHaveValue() {
    assertThrows(IllegalArgumentException.class, () -> parse("weakValues=true"));
  }

  public void testParse_repeatedValueStrength() {
    assertThrows(IllegalArgumentException.class, () -> parse("softValues, softValues"));

    assertThrows(IllegalArgumentException.class, () -> parse("softValues, weakValues"));

    assertThrows(IllegalArgumentException.class, () -> parse("weakValues, softValues"));

    assertThrows(IllegalArgumentException.class, () -> parse("weakValues, weakValues"));
  }

  public void testParse_writeExpirationDays() {
    CacheBuilderSpec spec = parse("expireAfterWrite=10d");
    assertNull(spec.initialCapacity);
    assertNull(spec.maximumSize);
    assertNull(spec.maximumWeight);
    assertNull(spec.concurrencyLevel);
    assertNull(spec.keyStrength);
    assertNull(spec.valueStrength);
    assertEquals(TimeUnit.DAYS, spec.writeExpirationTimeUnit);
    assertEquals(10L, spec.writeExpirationDuration);
    assertNull(spec.accessExpirationTimeUnit);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.DAYS), CacheBuilder.from(spec));
  }

  public void testParse_writeExpirationHours() {
    CacheBuilderSpec spec = parse("expireAfterWrite=150h");
    assertEquals(TimeUnit.HOURS, spec.writeExpirationTimeUnit);
    assertEquals(150L, spec.writeExpirationDuration);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterWrite(150L, TimeUnit.HOURS), CacheBuilder.from(spec));
  }

  public void testParse_writeExpirationMinutes() {
    CacheBuilderSpec spec = parse("expireAfterWrite=10m");
    assertEquals(TimeUnit.MINUTES, spec.writeExpirationTimeUnit);
    assertEquals(10L, spec.writeExpirationDuration);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES), CacheBuilder.from(spec));
  }

  public void testParse_writeExpirationSeconds() {
    CacheBuilderSpec spec = parse("expireAfterWrite=10s");
    assertEquals(TimeUnit.SECONDS, spec.writeExpirationTimeUnit);
    assertEquals(10L, spec.writeExpirationDuration);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS), CacheBuilder.from(spec));
  }

  public void testParse_writeExpirationRepeated() {
    assertThrows(
        IllegalArgumentException.class, () -> parse("expireAfterWrite=10s,expireAfterWrite=10m"));
  }

  public void testParse_accessExpirationDays() {
    CacheBuilderSpec spec = parse("expireAfterAccess=10d");
    assertNull(spec.initialCapacity);
    assertNull(spec.maximumSize);
    assertNull(spec.maximumWeight);
    assertNull(spec.concurrencyLevel);
    assertNull(spec.keyStrength);
    assertNull(spec.valueStrength);
    assertNull(spec.writeExpirationTimeUnit);
    assertEquals(TimeUnit.DAYS, spec.accessExpirationTimeUnit);
    assertEquals(10L, spec.accessExpirationDuration);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.DAYS), CacheBuilder.from(spec));
  }

  public void testParse_accessExpirationHours() {
    CacheBuilderSpec spec = parse("expireAfterAccess=150h");
    assertEquals(TimeUnit.HOURS, spec.accessExpirationTimeUnit);
    assertEquals(150L, spec.accessExpirationDuration);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(150L, TimeUnit.HOURS), CacheBuilder.from(spec));
  }

  public void testParse_accessExpirationMinutes() {
    CacheBuilderSpec spec = parse("expireAfterAccess=10m");
    assertEquals(TimeUnit.MINUTES, spec.accessExpirationTimeUnit);
    assertEquals(10L, spec.accessExpirationDuration);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES),
        CacheBuilder.from(spec));
  }

  public void testParse_accessExpirationSeconds() {
    CacheBuilderSpec spec = parse("expireAfterAccess=10s");
    assertEquals(TimeUnit.SECONDS, spec.accessExpirationTimeUnit);
    assertEquals(10L, spec.accessExpirationDuration);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.SECONDS),
        CacheBuilder.from(spec));
  }

  public void testParse_accessExpirationRepeated() {
    assertThrows(
        IllegalArgumentException.class, () -> parse("expireAfterAccess=10s,expireAfterAccess=10m"));
  }

  public void testParse_recordStats() {
    CacheBuilderSpec spec = parse("recordStats");
    assertTrue(spec.recordStats);
    assertCacheBuilderEquivalence(CacheBuilder.newBuilder().recordStats(), CacheBuilder.from(spec));
  }

  public void testParse_recordStatsValueSpecified() {
    assertThrows(IllegalArgumentException.class, () -> parse("recordStats=True"));
  }

  public void testParse_recordStatsRepeated() {
    assertThrows(IllegalArgumentException.class, () -> parse("recordStats,recordStats"));
  }

  public void testParse_accessExpirationAndWriteExpiration() {
    CacheBuilderSpec spec = parse("expireAfterAccess=10s,expireAfterWrite=9m");
    assertEquals(TimeUnit.MINUTES, spec.writeExpirationTimeUnit);
    assertEquals(9L, spec.writeExpirationDuration);
    assertEquals(TimeUnit.SECONDS, spec.accessExpirationTimeUnit);
    assertEquals(10L, spec.accessExpirationDuration);
    assertCacheBuilderEquivalence(
        CacheBuilder.newBuilder()
            .expireAfterAccess(10L, TimeUnit.SECONDS)
            .expireAfterWrite(9L, TimeUnit.MINUTES),
        CacheBuilder.from(spec));
  }

  public void testParse_multipleKeys() {
    CacheBuilderSpec spec =
        parse(
            "initialCapacity=10,maximumSize=20,concurrencyLevel=30,"
                + "weakKeys,weakValues,expireAfterAccess=10m,expireAfterWrite=1h");
    assertEquals(10, spec.initialCapacity.intValue());
    assertEquals(20, spec.maximumSize.intValue());
    assertNull(spec.maximumWeight);
    assertEquals(30, spec.concurrencyLevel.intValue());
    assertEquals(Strength.WEAK, spec.keyStrength);
    assertEquals(Strength.WEAK, spec.valueStrength);
    assertEquals(TimeUnit.HOURS, spec.writeExpirationTimeUnit);
    assertEquals(TimeUnit.MINUTES, spec.accessExpirationTimeUnit);
    assertEquals(1L, spec.writeExpirationDuration);
    assertEquals(10L, spec.accessExpirationDuration);
    CacheBuilder<?, ?> expected =
        CacheBuilder.newBuilder()
            .initialCapacity(10)
            .maximumSize(20)
            .concurrencyLevel(30)
            .weakKeys()
            .weakValues()
            .expireAfterAccess(10L, TimeUnit.MINUTES)
            .expireAfterWrite(1L, TimeUnit.HOURS);
    assertCacheBuilderEquivalence(expected, CacheBuilder.from(spec));
  }

  public void testParse_whitespaceAllowed() {
    CacheBuilderSpec spec =
        parse(
            " initialCapacity=10,\nmaximumSize=20,\t\r"
                + "weakKeys \t ,softValues \n , \r  expireAfterWrite \t =  15s\n\n");
    assertEquals(10, spec.initialCapacity.intValue());
    assertEquals(20, spec.maximumSize.intValue());
    assertNull(spec.maximumWeight);
    assertNull(spec.concurrencyLevel);
    assertEquals(Strength.WEAK, spec.keyStrength);
    assertEquals(Strength.SOFT, spec.valueStrength);
    assertEquals(TimeUnit.SECONDS, spec.writeExpirationTimeUnit);
    assertEquals(15L, spec.writeExpirationDuration);
    assertNull(spec.accessExpirationTimeUnit);
    CacheBuilder<?, ?> expected =
        CacheBuilder.newBuilder()
            .initialCapacity(10)
            .maximumSize(20)
            .weakKeys()
            .softValues()
            .expireAfterWrite(15L, TimeUnit.SECONDS);
    assertCacheBuilderEquivalence(expected, CacheBuilder.from(spec));
  }

  public void testParse_unknownKey() {
    assertThrows(IllegalArgumentException.class, () -> parse("foo=17"));
  }

  public void testParse_extraCommaIsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> parse("weakKeys,"));

    assertThrows(IllegalArgumentException.class, () -> parse(",weakKeys"));

    assertThrows(IllegalArgumentException.class, () -> parse("weakKeys,,softValues"));
  }

  public void testEqualsAndHashCode() {
    new EqualsTester()
        .addEqualityGroup(parse(""), parse(""))
        .addEqualityGroup(parse("concurrencyLevel=7"), parse("concurrencyLevel=7"))
        .addEqualityGroup(parse("concurrencyLevel=15"), parse("concurrencyLevel=15"))
        .addEqualityGroup(parse("initialCapacity=7"), parse("initialCapacity=7"))
        .addEqualityGroup(parse("initialCapacity=15"), parse("initialCapacity=15"))
        .addEqualityGroup(parse("maximumSize=7"), parse("maximumSize=7"))
        .addEqualityGroup(parse("maximumSize=15"), parse("maximumSize=15"))
        .addEqualityGroup(parse("maximumWeight=7"), parse("maximumWeight=7"))
        .addEqualityGroup(parse("maximumWeight=15"), parse("maximumWeight=15"))
        .addEqualityGroup(parse("expireAfterAccess=60s"), parse("expireAfterAccess=1m"))
        .addEqualityGroup(parse("expireAfterAccess=60m"), parse("expireAfterAccess=1h"))
        .addEqualityGroup(parse("expireAfterWrite=60s"), parse("expireAfterWrite=1m"))
        .addEqualityGroup(parse("expireAfterWrite=60m"), parse("expireAfterWrite=1h"))
        .addEqualityGroup(parse("weakKeys"), parse("weakKeys"))
        .addEqualityGroup(parse("softValues"), parse("softValues"))
        .addEqualityGroup(parse("weakValues"), parse("weakValues"))
        .addEqualityGroup(parse("recordStats"), parse("recordStats"))
        .testEquals();
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testMaximumWeight_withWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.from(parse("maximumWeight=9000"));
    builder.weigher(constantWeigher(42)).build(CacheLoader.from(Suppliers.ofInstance(null)));
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testMaximumWeight_withoutWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.from(parse("maximumWeight=9000"));
    assertThrows(
        IllegalStateException.class,
        () -> builder.build(CacheLoader.from(Suppliers.ofInstance(null))));
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testMaximumSize_withWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.from(parse("maximumSize=9000"));
    builder.weigher(constantWeigher(42)).build(CacheLoader.from(Suppliers.ofInstance(null)));
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testMaximumSize_withoutWeigher() {
    CacheBuilder<Object, Object> builder = CacheBuilder.from(parse("maximumSize=9000"));
    builder.build(CacheLoader.from(Suppliers.ofInstance(null)));
  }

  public void testDisableCaching() {
    // Functional test: assert that CacheBuilderSpec.disableCaching()
    // disables caching.  It's irrelevant how it does so.
    CacheBuilder<Object, Object> builder = CacheBuilder.from(CacheBuilderSpec.disableCaching());
    Object key = new Object();
    Object value = new Object();
    LoadingCache<Object, Object> cache =
        builder.build(CacheLoader.from(Suppliers.ofInstance(value)));
    assertSame(value, cache.getUnchecked(key));
    assertEquals(0, cache.size());
    assertFalse(cache.asMap().containsKey(key));
  }

  public void testCacheBuilderFrom_string() {
    CacheBuilder<?, ?> fromString =
        CacheBuilder.from(
            "initialCapacity=10,maximumSize=20,concurrencyLevel=30,"
                + "weakKeys,weakValues,expireAfterAccess=10m");
    CacheBuilder<?, ?> expected =
        CacheBuilder.newBuilder()
            .initialCapacity(10)
            .maximumSize(20)
            .concurrencyLevel(30)
            .weakKeys()
            .weakValues()
            .expireAfterAccess(10L, TimeUnit.MINUTES);
    assertCacheBuilderEquivalence(expected, fromString);
  }

  private static void assertCacheBuilderEquivalence(CacheBuilder<?, ?> a, CacheBuilder<?, ?> b) {
    assertEquals("concurrencyLevel", a.concurrencyLevel, b.concurrencyLevel);
    assertEquals("expireAfterAccessNanos", a.expireAfterAccessNanos, b.expireAfterAccessNanos);
    assertEquals("expireAfterWriteNanos", a.expireAfterWriteNanos, b.expireAfterWriteNanos);
    assertEquals("initialCapacity", a.initialCapacity, b.initialCapacity);
    assertEquals("maximumSize", a.maximumSize, b.maximumSize);
    assertEquals("maximumWeight", a.maximumWeight, b.maximumWeight);
    assertEquals("refreshNanos", a.refreshNanos, b.refreshNanos);
    assertEquals("keyEquivalence", a.keyEquivalence, b.keyEquivalence);
    assertEquals("keyStrength", a.keyStrength, b.keyStrength);
    assertEquals("removalListener", a.removalListener, b.removalListener);
    assertEquals("weigher", a.weigher, b.weigher);
    assertEquals("valueEquivalence", a.valueEquivalence, b.valueEquivalence);
    assertEquals("valueStrength", a.valueStrength, b.valueStrength);
    assertEquals("statsCounterSupplier", a.statsCounterSupplier, b.statsCounterSupplier);
    assertEquals("ticker", a.ticker, b.ticker);
    assertEquals("recordStats", a.isRecordingStats(), b.isRecordingStats());
  }
}

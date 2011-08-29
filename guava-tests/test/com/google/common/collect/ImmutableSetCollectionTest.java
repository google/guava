/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.SetGenerators.DegeneratedImmutableSetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetAsListGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetCopyOfGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSetWithBadHashesGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetAsListGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetAsListSubListGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetCopyOfGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetExplicitComparator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetExplicitSuperclassComparatorGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetHeadsetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetReversedOrderGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetSubsetAsListGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetSubsetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetTailsetGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedSetUnhashableGenerator;
import com.google.common.collect.testing.google.SetGenerators.ImmutableSortedsetSubsetAsListSubListGenerator;
import com.google.common.collect.testing.testers.SetHashCodeTester;
import com.google.common.testing.SerializableTester;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Collection tests for {@link ImmutableSet} and {@link ImmutableSortedSet}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtIncompatible("suite") // handled by collect/gwt/suites
public class ImmutableSetCollectionTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(SetTestSuiteBuilder.using(new ImmutableSetCopyOfGenerator())
        .named(ImmutableSetTest.class.getName())
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            Set<String> set = ImmutableSet.copyOf(elements);
            return SerializableTester.reserialize(set);
          }
        })
        .named(ImmutableSetTest.class.getName() + ", reserialized")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSetWithBadHashesGenerator())
        .named(ImmutableSetTest.class.getName() + ", with bad hashes")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new DegeneratedImmutableSetGenerator())
        .named(ImmutableSetTest.class.getName() + ", degenerate")
        .withFeatures(CollectionSize.ONE, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSortedSetCopyOfGenerator())
        .named(ImmutableSortedSetTest.class.getName())
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSortedSetGenerator() {
          @Override protected SortedSet<String> create(String[] elements) {
            SortedSet<String> set = ImmutableSortedSet.copyOf(elements);
            return SerializableTester.reserialize(set);
          }
        })
        .named(ImmutableSortedSetTest.class.getName() + ", reserialized")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSortedSetHeadsetGenerator())
        .named(ImmutableSortedSetTest.class.getName() + ", headset")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSortedSetTailsetGenerator())
        .named(ImmutableSortedSetTest.class.getName() + ", tailset")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSortedSetSubsetGenerator())
        .named(ImmutableSortedSetTest.class.getName() + ", subset")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new TestStringSortedSetGenerator() {
          @Override protected SortedSet<String> create(String[] elements) {
            List<String> list = Lists.newArrayList(elements);
            list.add("zzz");
            return SerializableTester.reserialize(
                ImmutableSortedSet.copyOf(list).headSet("zzy"));
          }
        })
        .named(
            ImmutableSortedSetTest.class.getName() + ", headset, reserialized")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new TestStringSortedSetGenerator() {
          @Override protected SortedSet<String> create(String[] elements) {
            List<String> list = Lists.newArrayList(elements);
            list.add("\0");
            return SerializableTester.reserialize(
                ImmutableSortedSet.copyOf(list).tailSet("\0\0"));
          }
        })
        .named(
            ImmutableSortedSetTest.class.getName() + ", tailset, reserialized")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new TestStringSortedSetGenerator() {
          @Override protected SortedSet<String> create(String[] elements) {
            List<String> list = Lists.newArrayList(elements);
            list.add("\0");
            list.add("zzz");
            return SerializableTester.reserialize(
                ImmutableSortedSet.copyOf(list).subSet("\0\0", "zzy"));
          }
        })
        .named(
            ImmutableSortedSetTest.class.getName() + ", subset, reserialized")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSortedSetExplicitComparator())
        .named(ImmutableSortedSetTest.class.getName()
            + ", explicit comparator, vararg")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSortedSetExplicitSuperclassComparatorGenerator())
        .named(ImmutableSortedSetTest.class.getName()
            + ", explicit superclass comparator, iterable")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSortedSetReversedOrderGenerator())
        .named(ImmutableSortedSetTest.class.getName()
            + ", reverseOrder, iterator")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(
        new ImmutableSortedSetUnhashableGenerator())
        .suppressing(SetHashCodeTester.getHashCodeMethods())
        .named(ImmutableSortedSetTest.class.getName() + ", unhashable")
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(new ImmutableSetAsListGenerator())
        .named("ImmutableSet.asList")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(
        new ImmutableSortedSetAsListGenerator())
        .named("ImmutableSortedSet.asList")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(
        new ImmutableSortedSetSubsetAsListGenerator())
        .named("ImmutableSortedSet.subSet.asList")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(
        new ImmutableSortedSetAsListSubListGenerator())
        .named("ImmutableSortedSet.asList.subList")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    suite.addTest(ListTestSuiteBuilder.using(
        new ImmutableSortedsetSubsetAsListSubListGenerator())
        .named("ImmutableSortedSet.subSet.asList.subList")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .createTestSuite());

    return suite;
  }
}
